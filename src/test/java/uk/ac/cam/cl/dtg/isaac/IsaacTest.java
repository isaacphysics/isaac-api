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
package uk.ac.cam.cl.dtg.isaac;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizManager;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembership;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Gender;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuizSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserGroupPersistenceManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.partialMockBuilder;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.easymock.PowerMock.reset;
import static org.powermock.api.easymock.PowerMock.verify;

public class IsaacTest {
    protected static Date somePastDate = new Date(System.currentTimeMillis() - 7*24*60*60*1000);
    protected static Date someFurtherPastDate = new Date(System.currentTimeMillis() - 14*24*60*60*1000);
    protected static Date someFutureDate = new Date(System.currentTimeMillis() + 7*24*60*60*1000);
    protected static Date someDateBeforeQuizAnswerView = new Date(Constants.QUIZ_VIEW_STUDENT_ANSWERS_RELEASE_TIMESTAMP - 24*60*60*1000);
    protected static Date someDateAfterQuizAnswerView = new Date(Constants.QUIZ_VIEW_STUDENT_ANSWERS_RELEASE_TIMESTAMP + 24*60*60*1000);
    protected static Date someDateMuchAfterQuizAnswerView = new Date(System.currentTimeMillis() + 14*24*60*60*1000);
    protected IsaacQuizDTO studentQuiz;
    protected IsaacQuizDTO studentQuizPreQuizAnswerChange;
    protected IsaacQuizDTO studentQuizPostQuizAnswerChange;
    protected IsaacQuiz studentQuizDO;
    protected IsaacQuizDTO teacherQuiz;
    protected IsaacQuizDTO otherQuiz;
    protected IsaacQuizSectionDTO quizSection1;
    protected IsaacQuizSectionDTO quizSection2;

    protected QuizSummaryDTO studentQuizSummary;
    protected QuizSummaryDTO teacherQuizSummary;
    protected RegisteredUserDTO student;
    protected RegisteredUserDTO teacher;
    protected RegisteredUserDTO secondTeacher;
    protected RegisteredUserDTO otherTeacher;
    protected RegisteredUserDTO noone;
    protected AnonymousUserDTO anonUser;
    protected RegisteredUserDTO secondStudent;
    protected RegisteredUserDTO otherStudent;
    protected RegisteredUserDTO adminUser;
    protected List<RegisteredUserDTO> everyone;
    protected UserGroupDTO studentGroup;
    protected UserGroupDTO studentInactiveGroup;

    protected QuizAssignmentDTO studentAssignment;
    protected QuizAssignmentDTO studentAssignmentPreQuizAnswerChange;
    protected QuizAssignmentDTO studentAssignmentPostQuizAnswerChange;
    protected QuizAssignmentDTO overdueAssignment;
    private QuizAssignmentDTO completedAssignment;
    protected ImmutableList<Long> studentGroups;
    protected List<QuizAssignmentDTO> teacherAssignmentsToTheirGroups;
    private QuizAssignmentDTO otherAssignment;
    protected QuizAssignmentDTO studentInactiveIgnoredAssignment;
    protected QuizAssignmentDTO studentInactiveAssignment;
    protected List<QuizAssignmentDTO> studentAssignments;
    protected QuizAttemptDTO studentAttempt;
    protected QuizAttemptDTO overdueAttempt;
    protected QuizAttemptDTO completedAttempt;
    protected QuizAttemptDTO completedAttemptPreQuizAnswerChange;
    protected QuizAttemptDTO completedAttemptPostQuizAnswerChange;
    protected QuizAttemptDTO overdueCompletedAttempt;
    protected QuizAttemptDTO otherAttempt;
    protected QuizAttemptDTO ownAttempt;
    protected QuizAttemptDTO ownCompletedAttempt;
    protected QuizAttemptDTO attemptOnNullFeedbackModeQuiz;
    protected List<QuizAttemptDTO> studentAttempts;
    protected IsaacQuestionBaseDTO question;
    protected IsaacQuestionBaseDTO question2;
    protected IsaacQuestionBaseDTO question3;
    protected IsaacQuestionBase questionDO;
    protected IsaacQuestionBaseDTO questionPageQuestion;
    protected IsaacQuestionBase questionPageQuestionDO;

    protected GroupManager groupManager;
    protected IUserGroupPersistenceManager groupDatabase;
    protected QuizManager quizManager;

    protected Map<Object, MockConfigurer> defaultsMap = new HashMap<>();

    @Before
    public final void initializeIsaacTest() throws SegueDatabaseException, ContentManagerException {
        initializeIsaacObjects();
        initializeMocks();
    }

    protected void initializeIsaacObjects() {
        long id = 0L;
        studentQuizSummary = new QuizSummaryDTO();
        teacherQuizSummary = new QuizSummaryDTO();

        questionPageQuestion = new IsaacQuestionBaseDTO();
        questionPageQuestion.setId("questionPage|question1");
        questionPageQuestionDO = new IsaacQuestionBase();
        questionPageQuestionDO.setId(questionPageQuestion.getId());

        question = new IsaacQuestionBaseDTO();
        question.setId("studentQuiz|section1|question1");

        question2 = new IsaacQuestionBaseDTO();
        question2.setId("studentQuiz|section2|question2");

        question3 = new IsaacQuestionBaseDTO();
        question3.setId("studentQuiz|section2|question3");

        questionDO = new IsaacQuestionBase();
        questionDO.setId(question.getId());

        quizSection1 = new IsaacQuizSectionDTO();
        quizSection1.setId("studentQuiz|section1");
        quizSection1.setChildren(Collections.singletonList(question));

        quizSection2 = new IsaacQuizSectionDTO();
        quizSection2.setId("studentQuiz|section2");
        quizSection2.setChildren(ImmutableList.of(question2, question3));

        studentQuiz = new IsaacQuizDTO("studentQuiz", null, null, null, null, null, null, null, ImmutableList.of(quizSection1, quizSection2), null, null, null, false, null, null, null, null, null, null, QuizFeedbackMode.OVERALL_MARK, null);
        studentQuizPreQuizAnswerChange = new IsaacQuizDTO("studentQuizPreQuizAnswerChange", null, null, null, null, null, null, null, ImmutableList.of(quizSection1, quizSection2), null, null, null, false, null, null, null, null, null, null, QuizFeedbackMode.OVERALL_MARK, null);
        studentQuizPostQuizAnswerChange = new IsaacQuizDTO("studentQuizPostQuizAnswerChange", null, null, null, null, null, null, null, ImmutableList.of(quizSection1, quizSection2), null, null, null, false, null, null, null, null, null, null, QuizFeedbackMode.OVERALL_MARK, null);
        teacherQuiz = new IsaacQuizDTO("teacherQuiz", null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, null, null, ImmutableList.of("STUDENT"), null, null);
        otherQuiz = new IsaacQuizDTO("otherQuiz", null, null, null, null, null, null, null, Collections.singletonList(quizSection1), null, null, null, false, null, null, null, null, null, null, QuizFeedbackMode.DETAILED_FEEDBACK, null);

        // A bit scrappy, but hopefully sufficient.
        studentQuizDO = new IsaacQuiz("studentQuiz", null, null, null, null, null, null, null, null, null, null, null, false, null, null, null, null, null, null, null, null, null);

        student = new RegisteredUserDTO("Some", "Student", "test-student@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false);
        student.setRole(Role.STUDENT);
        student.setId(++id);

        teacher = new RegisteredUserDTO("Some", "Teacher", "test-teacher@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE, somePastDate, "", null, false);
        teacher.setRole(Role.TEACHER);
        teacher.setId(++id);

        secondTeacher = new RegisteredUserDTO("Second", "Teacher", "second-teacher@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.PREFER_NOT_TO_SAY, somePastDate, "", null, false);
        secondTeacher.setRole(Role.TEACHER);
        secondTeacher.setId(++id);

        otherTeacher = new RegisteredUserDTO("Other", "Teacher", "other-teacher@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.OTHER, somePastDate, "", null, false);
        otherTeacher.setRole(Role.TEACHER);
        otherTeacher.setId(++id);

        noone = null;
        anonUser = new AnonymousUserDTO("fake-session-id");

        secondStudent = new RegisteredUserDTO("Second", "Student", "second-student@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE, somePastDate, "", null, false);
        secondStudent.setRole(Role.STUDENT);
        secondStudent.setId(++id);

        otherStudent = new RegisteredUserDTO("Other", "Student", "other-student@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false);
        otherStudent.setRole(Role.STUDENT);
        otherStudent.setId(++id);

        adminUser = new RegisteredUserDTO("Test", "Admin", "test-admin@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.UNKNOWN, somePastDate, "", null, false);
        adminUser.setRole(Role.ADMIN);
        adminUser.setId(++id);

        everyone = anyOf(student, teacher, secondTeacher, otherTeacher, secondStudent, otherStudent, adminUser);

        studentGroup = new UserGroupDTO();
        studentGroup.setId(++id);
        studentGroup.setOwnerId(teacher.getId());
        UserSummaryWithEmailAddressDTO secondTeacherSummary = new UserSummaryWithEmailAddressDTO();
        secondTeacherSummary.setId(secondTeacher.getId());
        secondTeacherSummary.setEmail(secondTeacher.getEmail());
        secondTeacherSummary.setEmailVerificationStatus(secondTeacher.getEmailVerificationStatus());
        secondTeacherSummary.setRole(secondTeacher.getRole());
        secondTeacherSummary.setFamilyName(secondTeacher.getFamilyName());
        secondTeacherSummary.setGivenName(secondTeacher.getGivenName());

        studentGroup.setAdditionalManagers(Collections.singleton(secondTeacherSummary));

        studentInactiveGroup = new UserGroupDTO(++id, "studentInactiveGroup", teacher.getId(), somePastDate, somePastDate, false, false);

        studentGroups = ImmutableList.of(studentGroup.getId(), studentInactiveGroup.getId());

        completedAssignment = new QuizAssignmentDTO(++id, studentQuiz.getId(), teacher.getId(), studentGroup.getId(), someFurtherPastDate, somePastDate, null, QuizFeedbackMode.OVERALL_MARK);
        studentAssignment = new QuizAssignmentDTO(++id, studentQuiz.getId(), teacher.getId(), studentGroup.getId(), somePastDate, someFutureDate, null, QuizFeedbackMode.DETAILED_FEEDBACK);
        studentAssignmentPreQuizAnswerChange = new QuizAssignmentDTO(++id, studentQuizPreQuizAnswerChange.getId(), teacher.getId(), studentGroup.getId(), someDateBeforeQuizAnswerView, someDateAfterQuizAnswerView, null, QuizFeedbackMode.DETAILED_FEEDBACK);
        studentAssignmentPostQuizAnswerChange = new QuizAssignmentDTO(++id, studentQuizPostQuizAnswerChange.getId(), teacher.getId(), studentGroup.getId(), someDateAfterQuizAnswerView, someDateMuchAfterQuizAnswerView, null, QuizFeedbackMode.DETAILED_FEEDBACK);
        overdueAssignment = new QuizAssignmentDTO(++id, studentQuiz.getId(), teacher.getId(), studentGroup.getId(), someFurtherPastDate, somePastDate, null, QuizFeedbackMode.SECTION_MARKS);
        otherAssignment = new QuizAssignmentDTO(++id, teacherQuiz.getId(), teacher.getId(), studentGroup.getId(), somePastDate, someFutureDate, null, QuizFeedbackMode.OVERALL_MARK);

        studentInactiveIgnoredAssignment = new QuizAssignmentDTO(++id, teacherQuiz.getId(), teacher.getId(), studentInactiveGroup.getId(), somePastDate, someFutureDate, null, QuizFeedbackMode.OVERALL_MARK);
        studentInactiveAssignment = new QuizAssignmentDTO(++id, teacherQuiz.getId(), teacher.getId(), studentInactiveGroup.getId(), someFurtherPastDate, someFutureDate, null, QuizFeedbackMode.OVERALL_MARK);

        studentAssignments = ImmutableList.of(completedAssignment, studentAssignment, studentAssignmentPreQuizAnswerChange, studentAssignmentPostQuizAnswerChange, overdueAssignment, otherAssignment, studentInactiveAssignment);

        teacherAssignmentsToTheirGroups = ImmutableList.<QuizAssignmentDTO>builder().addAll(studentAssignments).add(studentInactiveIgnoredAssignment).build();

        studentAttempt = new QuizAttemptDTO(++id, student.getId(), studentQuiz.getId(), studentAssignment.getId(), somePastDate, null);
        overdueAttempt = new QuizAttemptDTO(++id, student.getId(), studentQuiz.getId(), overdueAssignment.getId(), somePastDate, null);
        completedAttempt = new QuizAttemptDTO(++id, student.getId(), studentQuiz.getId(), studentAssignment.getId(), somePastDate, new Date());
        completedAttemptPreQuizAnswerChange = new QuizAttemptDTO(++id, student.getId(), studentQuizPreQuizAnswerChange.getId(), studentAssignmentPreQuizAnswerChange.getId(), somePastDate, new Date());
        completedAttemptPostQuizAnswerChange = new QuizAttemptDTO(++id, student.getId(), studentQuizPostQuizAnswerChange.getId(), studentAssignmentPostQuizAnswerChange.getId(), somePastDate, new Date());
        overdueCompletedAttempt = new QuizAttemptDTO(++id, student.getId(), studentQuiz.getId(), overdueAssignment.getId(), somePastDate, new Date());
        otherAttempt = new QuizAttemptDTO(++id, student.getId(), teacherQuiz.getId(), otherAssignment.getId(), somePastDate, null);

        ownCompletedAttempt = new QuizAttemptDTO(++id, student.getId(), otherQuiz.getId(), null, somePastDate, somePastDate);
        ownAttempt = new QuizAttemptDTO(++id, student.getId(), otherQuiz.getId(), null, somePastDate, null);
        attemptOnNullFeedbackModeQuiz = new QuizAttemptDTO(101L, student.getId(), teacherQuiz.getId(), null, somePastDate, somePastDate);

        studentAttempts = ImmutableList.of(studentAttempt, overdueAttempt, completedAttempt, completedAttemptPreQuizAnswerChange, completedAttemptPostQuizAnswerChange, overdueCompletedAttempt, otherAttempt, ownAttempt, ownCompletedAttempt, attemptOnNullFeedbackModeQuiz);
    }

    protected void initializeMocks() throws ContentManagerException, SegueDatabaseException {
        quizManager = createMock(QuizManager.class);

        registerDefaultsFor(quizManager, m -> {
            expect(m.getAvailableQuizzes("STUDENT", 0, 9000)).andStubReturn(wrap(studentQuizSummary));
            expect(m.getAvailableQuizzes("TEACHER", 0, 9000)).andStubReturn(wrap(studentQuizSummary, teacherQuizSummary));
            expect(m.findQuiz(studentQuiz.getId())).andStubReturn(studentQuiz);
            expect(m.findQuiz(studentQuizPreQuizAnswerChange.getId())).andStubReturn(studentQuizPreQuizAnswerChange);
            expect(m.findQuiz(studentQuizPostQuizAnswerChange.getId())).andStubReturn(studentQuizPostQuizAnswerChange);
            expect(m.findQuiz(teacherQuiz.getId())).andStubReturn(teacherQuiz);
            expect(m.findQuiz(otherQuiz.getId())).andStubReturn(otherQuiz);
            expect(m.extractSectionObjects(studentQuiz)).andStubReturn(ImmutableList.of(quizSection1, quizSection2));
        });

        // We want to actually test the behaviour of the real GroupManager::filterItemsBasedOnMembershipContext, but
        // otherwise want to mock the methods. Unfortunately, this method internally calls the groupDatabase object
        // we cannot mock straightforwardly, hence this partial mock madness.
        groupDatabase = createMock(IUserGroupPersistenceManager.class);
        UserAccountManager userAccountManager = createMock(UserAccountManager.class);
        GameManager gameManager = createMock(GameManager.class);
        MapperFacade mapperFacade = createMock(MapperFacade.class);
        groupManager = partialMockBuilder(GroupManager.class)
                .withConstructor(groupDatabase, userAccountManager, gameManager, mapperFacade)
                .addMockedMethod("getGroupById").addMockedMethod("isUserInGroup").addMockedMethod("getGroupMembershipList", RegisteredUserDTO.class, boolean.class)
                .addMockedMethod("getUsersInGroup").addMockedMethod("getUserMembershipMapForGroup").addMockedMethod("getAllGroupsOwnedAndManagedByUser")
                .createMock();

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
            if ((arguments[0] == student) && (arguments[1] == studentGroup || arguments[1] == studentInactiveGroup)) {
                return true;
            } else if (arguments[0] == secondStudent && arguments[1] == studentGroup) {
                return true;
            } else {
                return false;
            }
        });
        expect(groupManager.getGroupMembershipList(student, false)).andStubReturn(ImmutableList.of(studentGroup, studentInactiveGroup));
        expect(groupManager.getGroupMembershipList(secondStudent, false)).andStubReturn(ImmutableList.of(studentGroup));
        expect(groupManager.getUsersInGroup(studentGroup)).andStubReturn(ImmutableList.of(student, secondStudent));
        Date beforeSomePastDate = new Date(somePastDate.getTime() - 1000L);
        expect(groupDatabase.getGroupMembershipMapForUser(student.getId())).andStubReturn(ImmutableMap.of(
                studentGroup.getId(), new GroupMembership(studentGroup.getId(), student.getId(), GroupMembershipStatus.ACTIVE, null, somePastDate),
                studentInactiveGroup.getId(), new GroupMembership(studentInactiveGroup.getId(), student.getId(), GroupMembershipStatus.INACTIVE, null, beforeSomePastDate)
        ));
        expect(groupDatabase.getGroupMembershipMapForUser(student.getId())).andStubReturn(ImmutableMap.of(
                studentGroup.getId(), new GroupMembership(studentGroup.getId(), secondStudent.getId(), GroupMembershipStatus.ACTIVE, null, somePastDate)
        ));

        replay(quizManager, groupManager, groupDatabase, userAccountManager, gameManager, mapperFacade);
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

    /**
     * If a mock needs to have some expectations as well as stub returns, put the stub return setup in a call to this function.
     */
    protected <T> void registerDefaultsFor(T mock, MockConfigurer<T> defaults) {
        defaultsMap.put(mock, defaults);
        defaults.configure(mock);
    }

    @SafeVarargs
    protected final <T> void withMock(T mock, MockConfigurer<T>... setups) {
        verify(mock);
        reset(mock);
        if (defaultsMap.containsKey(mock)) {
            ((MockConfigurer<T>) defaultsMap.get(mock)).configure(mock);
        }
        for (MockConfigurer<T> setup: setups) {
            setup.configure(mock);
        }
        replay(mock);
    }

    /**
     * MockConfigurer
     *
     * Basically a Consumer that can throw.
     *
     * @param <M> What it configures.
     */
    @FunctionalInterface
    public interface MockConfigurer<M> {
        void accept(M mock) throws Exception;

        default void configure(M mock) {
            try {
                accept(mock);
            } catch (Exception e) {
                throw new RuntimeException("Error configuring defaults for mock: " + mock.toString(), e);
            }
        }
    }
}
