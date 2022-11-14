/**
 * Copyright 2021 Raspberry Pi Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.dto.IAssignmentLike;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.comm.MailGunEmailManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

import jakarta.annotation.Nullable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import static uk.ac.cam.cl.dtg.util.NameFormatter.getFilteredGroupNameFromGroup;
import static uk.ac.cam.cl.dtg.util.NameFormatter.getTeacherNameFromUser;

public class EmailService {

    public interface HasTitleOrId {
        String getId();
        @Nullable String getTitle();
    }

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailManager emailManager;
    private final MailGunEmailManager mailGunEmailManager;
    private final GroupManager groupManager;
    private final UserAccountManager userManager;

    @Inject
    public EmailService(final EmailManager emailManager, final GroupManager groupManager,
                        final UserAccountManager userManager, final MailGunEmailManager mailGunEmailManager) {
        this.emailManager = emailManager;
        this.groupManager = groupManager;
        this.userManager = userManager;
        this.mailGunEmailManager = mailGunEmailManager;
    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy");

    public void sendAssignmentEmailToGroup(final IAssignmentLike assignment, final HasTitleOrId on, final Map<String, String> tokenToValueMapping, final String templateName) throws SegueDatabaseException {
        UserGroupDTO userGroupDTO = groupManager.getGroupById(assignment.getGroupId());

        String dueDate = "";
        if (assignment.getDueDate() != null) {
            dueDate = String.format(" (due on %s)", DATE_FORMAT.format(assignment.getDueDate()));
        }

        String name = on.getId();
        if (on.getTitle() != null) {
            name = on.getTitle();
        }

        try {
            RegisteredUserDTO assignmentOwnerDTO = this.userManager.getUserDTOById(assignment.getOwnerUserId());
            String groupName = getFilteredGroupNameFromGroup(userGroupDTO);
            String assignmentOwner = getTeacherNameFromUser(assignmentOwnerDTO);

            final Map<String, Object> variables = ImmutableMap.<String, Object>builder()
                .put("gameboardName", name) // Legacy name
                .put("assignmentName", name)
                .put("assignmentDueDate", dueDate)
                .put("groupName", groupName)
                .put("assignmentOwner", assignmentOwner)
                .putAll(tokenToValueMapping)
                .build();

            mailGunEmailManager.sendBatchEmails(
                    groupManager.getUsersInGroup(userGroupDTO),
                    emailManager.getEmailTemplateDTO(templateName),
                    EmailType.ASSIGNMENTS,
                    Constants.IsaacMailGunTemplate.ASSIGNMENT,
                    variables,
                    null
            );
        } catch (NoUserException e) {
            log.error("Could not send assignment email because owner did not exist.", e);
        } catch (ContentManagerException e) {
            log.error("Could not send assignment email because of content error.", e);
        } catch (FeignException e) {
            log.error("Error sending assignment email via MailGun API.", e);
        }
    }
}
