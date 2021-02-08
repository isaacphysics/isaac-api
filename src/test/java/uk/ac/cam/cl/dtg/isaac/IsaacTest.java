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
package uk.ac.cam.cl.dtg.isaac;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizManager;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.GroupMembershipDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryWithEmailAddressDTO;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createPartialMockForAllMethodsExcept;
import static org.powermock.api.easymock.PowerMock.replay;

public class IsaacTest {
    protected static Date somePastDate = new Date(System.currentTimeMillis() - 7*24*60*60*1000);
    protected static Date someFurtherPastDate = new Date(System.currentTimeMillis() - 14*24*60*60*1000);
    protected static Date someFutureDate = new Date(System.currentTimeMillis() + 7*24*60*60*1000);
    protected IsaacQuizDTO studentQuiz;
    protected IsaacQuiz studentQuizDO;
    protected IsaacQuizDTO teacherQuiz;
    protected IsaacQuizDTO otherQuiz;
    protected ContentSummaryDTO studentQuizSummary;
    protected ContentSummaryDTO teacherQuizSummary;
    protected RegisteredUserDTO student;
    protected RegisteredUserDTO teacher;
    protected RegisteredUserDTO secondTeacher;
    protected RegisteredUserDTO otherTeacher;
    protected RegisteredUserDTO noone;
    protected RegisteredUserDTO otherStudent;
    protected RegisteredUserDTO adminUser;
    protected List<RegisteredUserDTO> everyone;
    protected UserGroupDTO studentGroup;
    protected UserGroupDTO studentInactiveGroup;

    protected QuizAssignmentDTO studentAssignment;
    protected QuizAssignmentDTO overdueAssignment;
    protected ImmutableList<Long> studentGroups;
    private QuizAssignmentDTO otherAssignment;
    protected QuizAssignmentDTO studentInactiveIgnoredAssignment;
    protected QuizAssignmentDTO studentInactiveAssignment;
    protected List<QuizAssignmentDTO> studentAssignments;
    protected QuizAttemptDTO studentAttempt;
    protected QuizAttemptDTO overdueAttempt;
    protected QuizAttemptDTO completedAttempt;
    protected QuizAttemptDTO otherAttempt;
    protected QuizAttemptDTO ownAttempt;
    protected QuizAttemptDTO ownCompletedAttempt;
    protected List<QuizAttemptDTO> studentAttempts;
    protected IsaacQuestionBaseDTO question;
    protected IsaacQuestionBase questionDO;
    protected IsaacQuestionBaseDTO questionPageQuestion;
    protected IsaacQuestionBase questionPageQuestionDO;

    protected GroupManager groupManager;
    protected QuizManager quizManager;

    @Before
    public final void initializeIsaacTest() throws SegueDatabaseException, ContentManagerException {
        initializeIsaacObjects();
        initializeMocks();
    }

    protected void initializeIsaacObjects() {
        long id = 0L;
        studentQuizSummary = new ContentSummaryDTO();
        teacherQuizSummary = new ContentSummaryDTO();

        questionPageQuestion = new IsaacQuestionBaseDTO();
        questionPageQuestion.setId("questionPage|question1");
        questionPageQuestionDO = new IsaacQuestionBase();
        questionPageQuestionDO.setId(questionPageQuestion.getId());

        question = new IsaacQuestionBaseDTO();
        question.setId("studentQuiz|section1|question1");

        questionDO = new IsaacQuestionBase();
        questionDO.setId(question.getId());

        IsaacQuizSectionDTO quizSection = new IsaacQuizSectionDTO();
        quizSection.setId("studentQuiz|section1");
        quizSection.setChildren(Collections.singletonList(question));

        studentQuiz = new IsaacQuizDTO("studentQuiz", null, null, null, null, null, null, null, Collections.singletonList(quizSection), null, null, null, false, null, null, true);
        teacherQuiz = new IsaacQuizDTO("teacherQuiz", null, null, null, null, null, null, null, null, null, null, null, false, null, null, false);
        otherQuiz = new IsaacQuizDTO("otherQuiz", null, null, null, null, null, null, null, Collections.singletonList(quizSection), null, null, null, false, null, null, true);

        // A bit scrappy, but hopefully sufficient.
        studentQuizDO = new IsaacQuiz("studentQuiz", null, null, null, null, null, null, null, null, null, null, null, false, null, null, true);

        student = new RegisteredUserDTO();
        student.setRole(Role.STUDENT);
        student.setId(++id);

        teacher = new RegisteredUserDTO();
        teacher.setRole(Role.TEACHER);
        teacher.setId(++id);

        secondTeacher = new RegisteredUserDTO();
        secondTeacher.setRole(Role.TEACHER);
        secondTeacher.setId(++id);

        otherTeacher = new RegisteredUserDTO();
        otherTeacher.setRole(Role.TEACHER);
        otherTeacher.setId(++id);

        noone = null;

        otherStudent = new RegisteredUserDTO();
        otherStudent.setRole(Role.STUDENT);
        otherStudent.setId(++id);

        adminUser = new RegisteredUserDTO();
        adminUser.setRole(Role.ADMIN);
        adminUser.setId(++id);

        everyone = anyOf(student, teacher, secondTeacher, otherTeacher, otherStudent, adminUser);

        studentGroup = new UserGroupDTO();
        studentGroup.setId(++id);
        studentGroup.setOwnerId(teacher.getId());
        UserSummaryWithEmailAddressDTO secondTeacherSummary = new UserSummaryWithEmailAddressDTO();
        secondTeacherSummary.setId(secondTeacher.getId());
        studentGroup.setAdditionalManagers(Collections.singleton(secondTeacherSummary));

        studentInactiveGroup = new UserGroupDTO(++id, "studentInactiveGroup", teacher.getId(), somePastDate, somePastDate, false);

        studentGroups = ImmutableList.of(studentGroup.getId(), studentInactiveGroup.getId());

        studentAssignment = new QuizAssignmentDTO(++id, studentQuiz.getId(), teacher.getId(), studentGroup.getId(), somePastDate, someFutureDate, QuizFeedbackMode.OVERALL_MARK);
        overdueAssignment = new QuizAssignmentDTO(++id, studentQuiz.getId(), teacher.getId(), studentGroup.getId(), somePastDate, somePastDate, QuizFeedbackMode.OVERALL_MARK);
        otherAssignment = new QuizAssignmentDTO(++id, teacherQuiz.getId(), teacher.getId(), studentGroup.getId(), somePastDate, someFutureDate, QuizFeedbackMode.OVERALL_MARK);

        studentInactiveIgnoredAssignment = new QuizAssignmentDTO(++id, teacherQuiz.getId(), teacher.getId(), studentInactiveGroup.getId(), somePastDate, someFutureDate, QuizFeedbackMode.OVERALL_MARK);
        studentInactiveAssignment = new QuizAssignmentDTO(++id, teacherQuiz.getId(), teacher.getId(), studentInactiveGroup.getId(), someFurtherPastDate, someFutureDate, QuizFeedbackMode.OVERALL_MARK);

        studentAssignments = ImmutableList.of(studentAssignment, overdueAssignment, otherAssignment, studentInactiveAssignment);

        studentAttempt = new QuizAttemptDTO(++id, student.getId(), studentQuiz.getId(), studentAssignment.getId(), somePastDate, null);
        overdueAttempt = new QuizAttemptDTO(++id, student.getId(), studentQuiz.getId(), overdueAssignment.getId(), somePastDate, null);
        completedAttempt = new QuizAttemptDTO(++id, student.getId(), studentQuiz.getId(), studentAssignment.getId(), somePastDate, new Date());
        otherAttempt = new QuizAttemptDTO(++id, student.getId(), teacherQuiz.getId(), otherAssignment.getId(), somePastDate, null);

        ownCompletedAttempt = new QuizAttemptDTO(++id, student.getId(), otherQuiz.getId(), null, somePastDate, somePastDate);
        ownAttempt = new QuizAttemptDTO(++id, student.getId(), otherQuiz.getId(), null, somePastDate, null);

        studentAttempts = ImmutableList.of(studentAttempt, overdueAttempt, completedAttempt, otherAttempt, ownAttempt, ownCompletedAttempt);
    }

    protected void initializeMocks() throws ContentManagerException, SegueDatabaseException {
        quizManager = createMock(QuizManager.class);
        expect(quizManager.getAvailableQuizzes(true, null, null)).andStubReturn(wrap(studentQuizSummary));
        expect(quizManager.getAvailableQuizzes(false, null, null)).andStubReturn(wrap(studentQuizSummary, teacherQuizSummary));
        expect(quizManager.findQuiz(studentQuiz.getId())).andStubReturn(studentQuiz);
        expect(quizManager.findQuiz(teacherQuiz.getId())).andStubReturn(teacherQuiz);
        expect(quizManager.findQuiz(otherQuiz.getId())).andStubReturn(otherQuiz);

        groupManager = createPartialMockForAllMethodsExcept(GroupManager.class, "filterItemsBasedOnMembershipContext");
        expect(groupManager.getGroupById(anyLong())).andStubAnswer(() -> {
            Object[] arguments = getCurrentArguments();
            if (arguments[0] == studentGroup.getId()) {
                return studentGroup;
            } else {
                throw new SegueDatabaseException("No such group.");
            }
        });
        expect(groupManager.isUserInGroup(anyObject(), anyObject())).andStubAnswer(() -> {
            Object[] arguments = getCurrentArguments();
            if (arguments[0] == student && (arguments[1] == studentGroup || arguments[1] == studentInactiveGroup)) {
                return true;
            } else {
                return false;
            }
        });
        expect(groupManager.getGroupMembershipList(student, false)).andStubReturn(ImmutableList.of(studentGroup, studentInactiveGroup));
        expect(groupManager.getUserMembershipMapForGroup(studentGroup.getId())).andStubReturn(
            Collections.singletonMap(student.getId(), new GroupMembershipDTO(studentGroup.getId(), student.getId(), GroupMembershipStatus.ACTIVE, null, somePastDate))
        );
        Date beforeSomePastDate = new Date(somePastDate.getTime() - 1000L);
        expect(groupManager.getUserMembershipMapForGroup(studentInactiveGroup.getId())).andStubReturn(
            Collections.singletonMap(student.getId(), new GroupMembershipDTO(studentGroup.getId(), student.getId(), GroupMembershipStatus.INACTIVE, null, beforeSomePastDate))
        );

        replay(quizManager, groupManager);
    }


    protected List<RegisteredUserDTO> anyOf(RegisteredUserDTO... users) {
        return Arrays.asList(users);
    }

    protected List<RegisteredUserDTO> studentsTeachersOrAdmin() {
        return anyOf(teacher, secondTeacher, adminUser);
    }

    @SafeVarargs
    protected final <T> ResultsWrapper<T> wrap(T... items) {
        return new ResultsWrapper<>(Arrays.asList(items), (long) items.length);
    }
}
