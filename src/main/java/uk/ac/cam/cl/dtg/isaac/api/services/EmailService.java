/*
 * Copyright 2021 Raspberry Pi Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.api.services;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IAssignmentLike;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.GroupMembershipDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.comm.MailGunEmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.annotation.Nullable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.util.NameFormatter.getFilteredGroupNameFromGroup;
import static uk.ac.cam.cl.dtg.util.NameFormatter.getTeacherNameFromUser;

public class EmailService {

    public interface HasTitleOrId {
        String getId();
        @Nullable String getTitle();
    }

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final AbstractConfigLoader properties;
    private final EmailManager emailManager;
    private final MailGunEmailManager mailGunEmailManager;
    private final GroupManager groupManager;
    private final UserAccountManager userManager;

    @Inject
    public EmailService(final AbstractConfigLoader properties, final EmailManager emailManager,
                        final GroupManager groupManager, final UserAccountManager userManager,
                        final MailGunEmailManager mailGunEmailManager) {
        this.properties = properties;
        this.emailManager = emailManager;
        this.groupManager = groupManager;
        this.userManager = userManager;
        this.mailGunEmailManager = mailGunEmailManager;
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy");

    private boolean userInMailGunBetaList(final Long userId) {
        String optInIds = properties.getProperty(MAILGUN_EMAILS_BETA_OPT_IN);
        if (Strings.isNullOrEmpty(optInIds)) {
            return false;
        }
        return Arrays.stream(optInIds.split(",")).anyMatch(id -> id.equals(userId.toString()));
    }

    public void sendAssignmentEmailToGroup(final IAssignmentLike assignment, final HasTitleOrId on, final Map<String, String> tokenToValueMapping, final String templateName) throws SegueDatabaseException {
        try {
            // This isn't nice, but it avoids augmenting the group unnecessarily!
            UserGroupDTO userGroupDTO = groupManager.getGroupsByIds(Collections.singletonList(assignment.getGroupId()), false).get(0);

            String dueDate = "";
            if (assignment.getDueDate() != null) {
                dueDate = String.format(" (due on %s)", DATE_FORMAT.format(assignment.getDueDate()));
            }

            String name = on.getId();
            if (on.getTitle() != null) {
                name = on.getTitle();
            }

            String groupName = getFilteredGroupNameFromGroup(userGroupDTO);
            String assignmentOwner;
            if (assignment.getAssignerSummary() != null) {
                assignmentOwner = getTeacherNameFromUser(assignment.getAssignerSummary());
            } else {
                RegisteredUserDTO assignmentOwnerDTO = this.userManager.getUserDTOById(assignment.getOwnerUserId());
                assignmentOwner = getTeacherNameFromUser(assignmentOwnerDTO);
            }

            final Map<String, Object> variables = ImmutableMap.<String, Object>builder()
                .put("gameboardName", name) // Legacy name
                .put("assignmentName", name)
                .put("assignmentDueDate", dueDate)
                .put("groupName", groupName)
                .put("assignmentOwner", assignmentOwner)
                .putAll(tokenToValueMapping)
                .build();

            // Get email template
            EmailTemplateDTO emailTemplate = emailManager.getEmailTemplateDTO(templateName);

            // Only email users active in the group:
            Map<Long, GroupMembershipDTO> userMembershipMapforGroup = this.groupManager.getUserMembershipMapForGroup(userGroupDTO.getId());
            List<RegisteredUserDTO> usersToEmail = groupManager.getUsersInGroup(userGroupDTO).stream()
                    // filter users so those who are inactive in the group aren't emailed
                    .filter(user -> GroupMembershipStatus.ACTIVE.equals(userMembershipMapforGroup.get(user.getId()).getStatus()))
                    .collect(Collectors.toList());

            // Send the email using MailGun if owner on list or if scheduled:
            if (this.userInMailGunBetaList(assignment.getOwnerUserId()) || assignment.getScheduledStartDate() != null) {
                Iterables.partition(usersToEmail, MAILGUN_BATCH_SIZE)
                    .forEach(userBatch -> mailGunEmailManager.sendBatchEmails(
                        userBatch,
                        emailTemplate,
                        EmailType.ASSIGNMENTS,
                        variables,
                        null)
                    );
            } else {
                // Otherwise, use our standard email method:
                usersToEmail.forEach(user -> {
                    try {
                        emailManager.sendTemplatedEmailToUser(user, emailTemplate, variables, EmailType.ASSIGNMENTS);
                    } catch (ContentManagerException | SegueDatabaseException e) {
                        log.error(String.format("Problem sending assignment email for user %s.", user.getId()), e);
                    }
                });
            }
        } catch (NoUserException e) {
            log.error("Could not send assignment email because owner did not exist.", e);
        } catch (ContentManagerException | ResourceNotFoundException e) {
            log.error("Could not send assignment email because of content error.", e);
        } catch (IndexOutOfBoundsException e) {
            log.error("Could not send assignment email because group did not exist.", e);
        }
    }
}
