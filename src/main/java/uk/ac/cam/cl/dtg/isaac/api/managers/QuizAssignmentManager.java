/*
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
package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.IGroupObserver;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.GroupMembershipDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.util.NameFormatter.getFilteredGroupNameFromGroup;
import static uk.ac.cam.cl.dtg.util.NameFormatter.getTeacherNameFromUser;

/**
 * Manage quiz assignments
 */
public class QuizAssignmentManager implements IGroupObserver {
    private static final Logger log = LoggerFactory.getLogger(QuizAssignmentManager.class);

    private final IQuizAssignmentPersistenceManager quizAssignmentPersistenceManager;
    private final GroupManager groupManager;
    private final EmailManager emailManager;
    private final UserAccountManager userManager;
	private final QuizManager quizManager;
    private final PropertiesLoader properties;

    /**
     * AssignmentManager.
     * @param quizAssignmentPersistenceManager
     *            - to save quiz assignments
     * @param groupManager
     *            - to allow communication with the group manager.
     * @param emailManager
     *            - email manager
     * @param userManager
     *            - the user manager object
     * @param quizManager
     *            - the quiz manager.
     */
    @Inject
    public QuizAssignmentManager(final IQuizAssignmentPersistenceManager quizAssignmentPersistenceManager,
                                 final GroupManager groupManager, final EmailManager emailManager,
                                 final UserAccountManager userManager, final QuizManager quizManager,
                                 final PropertiesLoader properties) {
        this.quizAssignmentPersistenceManager = quizAssignmentPersistenceManager;
        this.quizManager = quizManager;
        this.groupManager = groupManager;
        this.emailManager = emailManager;
        this.userManager = userManager;
        this.properties = properties;
        groupManager.registerInterestInGroups(this);
    }

    /**
     * Create a quiz assignment.
     *
     * @param newAssignment
     *            - to create - will be modified to include new id.
     * @return the assignment object now with the id field populated.
     * @throws SegueDatabaseException
     *             - if we cannot complete a required database operation.
     * @throws ContentManagerException
     *             - if we cannot find the quiz in the content.
     */
    public QuizAssignmentDTO createAssignment(final QuizAssignmentDTO newAssignment) throws SegueDatabaseException, ContentManagerException {
        Validate.isTrue(newAssignment.getId() == null, "The id field must be empty.");
        Validate.notNull(newAssignment.getQuizId());
        Validate.notNull(newAssignment.getGroupId());

        Date now = new Date();

        if (newAssignment.getDueDate() != null && newAssignment.getDueDate().before(now)) {
            throw new DueBeforeNowException("You cannot set a quiz with a due date in the past.");
        }

        List<QuizAssignmentDTO> existingQuizAssignments = quizAssignmentPersistenceManager.getAssignmentsByQuizIdAndGroup(newAssignment.getQuizId(),
            newAssignment.getGroupId());

        if (existingQuizAssignments.size() != 0) {
            if (existingQuizAssignments.stream().anyMatch(qa -> qa.getDueDate() == null || qa.getDueDate().after(now))) {
                log.error(String.format("Duplicated Quiz Assignment Exception - cannot assign the same work %s to a group %s when due date not passed",
                    newAssignment.getQuizId(), newAssignment.getGroupId()));
                throw new DuplicateAssignmentException("You cannot reassign a quiz until the due date has passed.");
            }
        }

        IsaacQuizDTO quiz = quizManager.findQuiz(newAssignment.getQuizId());

        newAssignment.setCreationDate(now);
        newAssignment.setId(this.quizAssignmentPersistenceManager.saveAssignment(newAssignment));

        UserGroupDTO userGroupDTO = groupManager.getGroupById(newAssignment.getGroupId());
        List<RegisteredUserDTO> usersToEmail = Lists.newArrayList();
        Map<Long, GroupMembershipDTO> userMembershipMapforGroup = this.groupManager.getUserMembershipMapForGroup(userGroupDTO.getId());

        // filter users so those who are inactive in the group aren't emailed
        for (RegisteredUserDTO user : groupManager.getUsersInGroup(userGroupDTO)) {
            if (GroupMembershipStatus.ACTIVE.equals(userMembershipMapforGroup.get(user.getId()).getStatus())) {
                usersToEmail.add(user);
            }
        }

		// inform all members of the group that there is now an assignment for them.
        // FIXME: This is super-repetitive of assignment
        try {
            final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy");
            final String quizURL = String.format("https://%s/quiz/%s/assignment/%d", properties.getProperty(HOST_NAME),
                    quiz.getId(), newAssignment.getId());

            String dueDate = "";
            if (newAssignment.getDueDate() != null) {
                dueDate = String.format(" (due on %s)", DATE_FORMAT.format(newAssignment.getDueDate()));
            }

            String quizName = quiz.getId();
            if (quiz.getTitle() != null) {
                quizName = quiz.getTitle();
            }

            RegisteredUserDTO assignmentOwnerDTO = this.userManager.getUserDTOById(newAssignment.getOwnerUserId());

            String groupName = getFilteredGroupNameFromGroup(userGroupDTO);
            String assignmentOwner = getTeacherNameFromUser(assignmentOwnerDTO);

            for (RegisteredUserDTO userDTO : usersToEmail) {
                emailManager.sendTemplatedEmailToUser(userDTO,
                        emailManager.getEmailTemplateDTO("email-template-group-quiz-assignment"),
                        ImmutableMap.of(
                                "guizURL", quizURL,
                                "guizName", quizName,
                                "assignmentDueDate", dueDate,
                                "groupName", groupName,
                                "assignmentOwner", assignmentOwner
                        ), EmailType.ASSIGNMENTS);
            }

        } catch (ContentManagerException e) {
            log.error("Could not send group assignment emails due to content issue", e);
        } catch (NoUserException e) {
            log.error("Could not send group assignment emails because owner did not exist.", e);
        }

        return newAssignment;
    }

    @Override
    public void onGroupMembershipRemoved(UserGroupDTO group, RegisteredUserDTO user) {
        // TODO
    }

    @Override
    public void onMemberAddedToGroup(UserGroupDTO group, RegisteredUserDTO user) {
        // TODO
    }

    @Override
    public void onAdditionalManagerAddedToGroup(UserGroupDTO group, RegisteredUserDTO additionalManagerUser) {
        // TODO
    }
}
