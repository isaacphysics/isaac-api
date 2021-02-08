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
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

/**
 * Manage quiz assignments
 */
public class QuizAssignmentManager {
    private static final Logger log = LoggerFactory.getLogger(QuizAssignmentManager.class);

    private final IQuizAssignmentPersistenceManager quizAssignmentPersistenceManager;
    private final EmailService emailService;
	private final QuizManager quizManager;
	private final GroupManager groupManager;
    private final PropertiesLoader properties;

    /**
     * AssignmentManager.
     * @param quizAssignmentPersistenceManager
     *            - to save quiz assignments
     * @param emailService
     *            - service for sending group emails.
     * @param quizManager
     *            - for information about quizzes.
     * @param groupManager
     *            - for group membership info.
     */
    @Inject
    public QuizAssignmentManager(final IQuizAssignmentPersistenceManager quizAssignmentPersistenceManager,
                                 final EmailService emailService, final QuizManager quizManager,
                                 final GroupManager groupManager, final PropertiesLoader properties) {
        this.quizAssignmentPersistenceManager = quizAssignmentPersistenceManager;
        this.quizManager = quizManager;
        this.emailService = emailService;
        this.groupManager = groupManager;
        this.properties = properties;
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
            throw new DueBeforeNowException();
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

        final String quizURL = String.format("https://%s/quiz/%s/assignment/%d", properties.getProperty(HOST_NAME),
            quiz.getId(), newAssignment.getId());

        emailService.sendAssignmentEmailToGroup(newAssignment, quiz, ImmutableMap.of("quizURL", quizURL) ,
            "email-template-group-quiz-assignment");

        return newAssignment;
    }

    public List<QuizAssignmentDTO> getAssignedQuizzes(RegisteredUserDTO user) throws SegueDatabaseException {
        List<QuizAssignmentDTO> assignments = getAllAssignments(user);

        return this.groupManager.filterItemsBasedOnMembershipContext(assignments, user.getId());
    }

    public QuizAssignmentDTO getById(Long quizAssignmentId) throws SegueDatabaseException, AssignmentCancelledException {
        return this.quizAssignmentPersistenceManager.getAssignmentById(quizAssignmentId);
    }

    public List<QuizAssignmentDTO> getActiveQuizAssignments(IsaacQuizDTO quiz, RegisteredUserDTO user) throws SegueDatabaseException {
        List<QuizAssignmentDTO> allAssignedAndDueQuizzes = getAllActiveAssignments(user);
        return allAssignedAndDueQuizzes.stream().filter(qa -> qa.getQuizId().equals(quiz.getId())).collect(Collectors.toList());
    }

    public List<QuizAssignmentDTO> getAssignmentsForGroups(List<UserGroupDTO> groups) throws SegueDatabaseException {
        List<Long> groupIds = groups.stream().map(UserGroupDTO::getId).collect(Collectors.toList());
        if (groups.size() == 0) {
            return Lists.newArrayList();
        }
        return this.quizAssignmentPersistenceManager.getAssignmentsByGroupList(groupIds);
    }

    public void cancelAssignment(QuizAssignmentDTO assignment) throws SegueDatabaseException {
        this.quizAssignmentPersistenceManager.cancelAssignment(assignment);
    }

    private List<QuizAssignmentDTO> getAllAssignments(RegisteredUserDTO user) throws SegueDatabaseException {
        // Find the groups the user is in
        List<UserGroupDTO> groups = groupManager.getGroupMembershipList(user, false);

        return getAssignmentsForGroups(groups);
    }

    private List<QuizAssignmentDTO> getAllActiveAssignments(RegisteredUserDTO user) throws SegueDatabaseException {
        List<QuizAssignmentDTO> allAssignedQuizzes = getAllAssignments(user);
        Date now = new Date();
        return allAssignedQuizzes.stream().filter(qa -> qa.getDueDate() == null || qa.getDueDate().after(now)).collect(Collectors.toList());
    }
}
