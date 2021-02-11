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
package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.api.managers.DueBeforeNowException;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateAssignmentException;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAttemptManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizQuestionManager;
import uk.ac.cam.cl.dtg.isaac.api.services.AssignmentService;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replay;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.QUIZ_SECTION;

public class QuizFacadeTest extends AbstractFacadeTest {

    private QuizFacade quizFacade;

    private Request requestForCaching;

    private AssignmentService assignmentService;
    private QuizAttemptManager quizAttemptManager;
    private QuizQuestionManager quizQuestionManager;
    private QuizAssignmentManager quizAssignmentManager;
    private ILogManager logManager;

    @Before
    public void setUp() throws ContentManagerException {
        assignmentService = createMock(AssignmentService.class);

        requestForCaching = createMock(Request.class);
        expect(requestForCaching.evaluatePreconditions((EntityTag) anyObject())).andStubReturn(null);

        PropertiesLoader properties = createMock(PropertiesLoader.class);
        logManager = createNiceMock(ILogManager.class); // Nice mock because we're not generally bothered about logging.
        IContentManager contentManager = createMock(IContentManager.class);
        quizAssignmentManager = createMock(QuizAssignmentManager.class);
        quizAttemptManager = createMock(QuizAttemptManager.class);
        quizQuestionManager = createMock(QuizQuestionManager.class);

        quizFacade = new QuizFacade(properties, logManager, contentManager, quizManager, userManager,
            groupManager, quizAssignmentManager, assignmentService, quizAttemptManager, quizQuestionManager);

        registerDefaultsFor(quizAssignmentManager, m -> {
            expect(m.getAssignedQuizzes(anyObject(RegisteredUserDTO.class))).andStubAnswer(() -> {
                Object[] arguments = getCurrentArguments();
                if (arguments[0] == student) {
                    return studentAssignments;
                } else {
                    return Collections.emptyList();
                }
            });

            expect(m.getById(anyLong())).andStubAnswer(() -> {
                Object[] arguments = getCurrentArguments();
                return studentAssignments.stream()
                    .filter(assignment -> assignment.getId() == arguments[0])
                    .findFirst()
                    .orElseThrow(() -> new SegueDatabaseException("No such assignment."));
            });

            expect(m.getActiveQuizAssignments(anyObject(), anyObject())).andStubAnswer(() -> {
                Object[] arguments = getCurrentArguments();
                if (arguments[1] != student) return Collections.emptyList();
                return Collections.singletonList(studentAssignment);
            });
        });

        registerDefaultsFor(quizAttemptManager, m -> {
            expect(m.getById(anyLong())).andStubAnswer(() -> {
                Object[] arguments = getCurrentArguments();
                return studentAttempts.stream()
                    .filter(assignment -> assignment.getId() == arguments[0])
                    .findFirst()
                    .orElseThrow(() -> new SegueDatabaseException("No such attempt."));
            });
        });

        String currentSHA = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";
        expect(contentManager.getCurrentContentSHA()).andStubReturn(currentSHA);
        expect(contentManager.extractContentSummary(studentQuiz)).andStubReturn(studentQuizSummary);
        expect(contentManager.extractContentSummary(teacherQuiz)).andStubReturn(teacherQuizSummary);
        expect(contentManager.getContentDOById(currentSHA, questionDO.getId())).andStubReturn(questionDO);
        expect(contentManager.getContentDOById(currentSHA, studentQuizDO.getId())).andStubReturn(studentQuizDO);
        expect(contentManager.getContentDOById(currentSHA, questionPageQuestionDO.getId())).andStubReturn(questionPageQuestionDO);

        replay(requestForCaching, properties, logManager, contentManager, quizManager, groupManager, quizAssignmentManager, assignmentService, quizAttemptManager, quizQuestionManager);
    }

    @Test
    public void availableQuizzes() {
        forEndpoint(() -> quizFacade.getAvailableQuizzes(request),
            requiresLogin(),
            as(anyOf(student, otherStudent),
                check((response) ->
                    assertEquals(Collections.singletonList(studentQuizSummary), extractResults(response)))
            ),
            as(teacher,
                check((response) ->
                    assertEquals(ImmutableList.of(studentQuizSummary, teacherQuizSummary), extractResults(response)))
            )
        );
    }

    @Test
    public void getAssignedQuizzes() {
        forEndpoint(() -> quizFacade.getAssignedQuizzes(request),
            requiresLogin(),
            as(student,
                prepare(assignmentService, (s) -> {
                    s.augmentAssignerSummaries(studentAssignments);
                    expectLastCall().once();
                }),
                respondsWith(studentAssignments)),
            as(anyOf(teacher, otherStudent),
                prepare(assignmentService, (s) -> {
                    s.augmentAssignerSummaries(Collections.emptyList());
                    expectLastCall().once();
                }),
                respondsWith(Collections.emptyList())
            ));
    }

    @Test
    public void createQuizAssignment() {
        QuizAssignmentDTO newAssignment = new QuizAssignmentDTO(0xB8003111799L, otherQuiz.getId(), null, studentGroup.getId(), null, someFutureDate, QuizFeedbackMode.OVERALL_MARK);
        QuizAssignmentDTO assignmentRequest = new QuizAssignmentDTO(null, otherQuiz.getId(), null, studentGroup.getId(), null, someFutureDate, QuizFeedbackMode.OVERALL_MARK);
        forEndpoint((QuizAssignmentDTO assignment) -> () -> quizFacade.createQuizAssignment(request, assignment),
            with(assignmentRequest,
                requiresLogin(),
                as(studentsTeachersOrAdmin(),
                    prepare(quizAssignmentManager, m -> expect(m.createAssignment(assignmentRequest)).andReturn(newAssignment)),
                    respondsWith(newAssignment),
                    check(ignoreResponse -> assertEquals(currentUser().getId(), assignmentRequest.getOwnerUserId()))
                ),
                forbiddenForEveryoneElse()
            ),
            with(assignmentRequest,
                as(studentsTeachersOrAdmin(),
                    prepare(quizAssignmentManager, m -> expect(m.createAssignment(assignmentRequest)).andThrow(new DueBeforeNowException())),
                    failsWith(Status.BAD_REQUEST)
                )
            ),
            with(assignmentRequest,
                as(studentsTeachersOrAdmin(),
                    prepare(quizAssignmentManager, m -> expect(m.createAssignment(assignmentRequest)).andThrow(new DuplicateAssignmentException("Test"))),
                    failsWith(Status.BAD_REQUEST)
                )
            )
        );
    }

    @Test
    public void cancelQuizAssignment() {
        forEndpoint(() -> quizFacade.cancelQuizAssignment(request, studentAssignment.getId()),
            requiresLogin(),
            as(studentsTeachersOrAdmin(),
                prepare(quizAssignmentManager, m -> m.cancelAssignment(studentAssignment)),
                succeeds()
            ),
            forbiddenForEveryoneElse()
        );
    }

    @Test
    public void previewQuiz() {
        forEndpoint(() -> quizFacade.previewQuiz(requestForCaching, request, studentQuiz.getId()),
            requiresLogin(),
            as(student, failsWith(SegueErrorResponse.getIncorrectRoleResponse())),
            as(teacher, respondsWith(studentQuiz))
        );
    }

    @Test
    public void startQuizAttempt() {
        QuizAttemptDTO attempt = new QuizAttemptDTO();

        forEndpoint(
            (assignment) -> () -> quizFacade.startQuizAttempt(requestForCaching, request, assignment.getId()),
            with(studentAssignment,
                requiresLogin(),
                as(student,
                    prepare(quizAttemptManager, m -> expect(m.fetchOrCreate(studentAssignment, student)).andReturn(attempt)),
                    respondsWith(attempt)),
                forbiddenForEveryoneElse()
            ),
            with(overdueAssignment,
                as(student, failsWith(Status.FORBIDDEN))
            )
        );
    }

    @Test
    public void startFreeQuizAttempt() {
        QuizAttemptDTO attempt = new QuizAttemptDTO();

        forEndpoint((quiz) -> () -> quizFacade.startFreeQuizAttempt(requestForCaching, request, quiz.getId()),
            with(studentQuiz,
                requiresLogin(),
                as(otherStudent,
                    prepare(quizAttemptManager, m -> expect(m.fetchOrCreateFreeQuiz(studentQuiz, otherStudent)).andReturn(attempt)),
                    respondsWith(attempt)
                ),
                as(student,
                    failsWith(Status.FORBIDDEN)
                )
            ),
            with(teacherQuiz,
                as(anyOf(student, otherStudent),
                    failsWith(Status.FORBIDDEN)
                )
            )
        );
    }

    @Test
    public void getQuizAttempt() {
        IsaacQuizDTO augmentedQuiz = new IsaacQuizDTO();
        forEndpoint((attempt) -> () -> quizFacade.getQuizAttempt(request, attempt.getId()),
            with(studentAttempt,
                requiresLogin(),
                as(otherStudent,
                    failsWith(Status.FORBIDDEN)
                ),
                as(student,
                    prepare(quizQuestionManager, m ->
                        expect(m.augmentQuestionsForUser(studentQuiz, studentAttempt, student, false)).andReturn(augmentedQuiz)),
                    respondsWith(augmentedQuiz)
                )
            ),
            with(overdueAttempt,
                as(student,
                    failsWith(Status.FORBIDDEN)
                )
            ),
            with(completedAttempt,
                as(student,
                    failsWith(Status.FORBIDDEN)
                )
            )
        );
    }

    @Test
    public void completeQuizAttempt() {
        forEndpoint((attempt) -> () -> quizFacade.completeQuizAttempt(request, attempt.getId()),
            with(studentAttempt,
                requiresLogin(),
                as(student,
                    prepare(quizAttemptManager, m -> m.updateAttemptCompletionStatus(studentAttempt, true)),
                    succeeds()
                ),
                everyoneElse(
                    failsWith(Status.FORBIDDEN)
                )
            ),
            with(completedAttempt,
                as(everyone,
                    failsWith(Status.FORBIDDEN)
                )
            )
        );
    }

    @Test
    public void completeQuizAttemptMarkIncompleteByTeacher() {
        forEndpoint((attempt) -> () -> quizFacade.markIncompleteQuizAttempt(request, attempt.getId()),
            with(studentAttempt,
                requiresLogin(),
                as(everyone,
                    failsWith(Status.FORBIDDEN)
                )
            ),
            with(completedAttempt,
                as(studentsTeachersOrAdmin(),
                    prepare(quizAttemptManager, m -> m.updateAttemptCompletionStatus(completedAttempt, false)),
                    succeeds()
                ),
                everyoneElse(
                    failsWith(Status.FORBIDDEN)
                )
            ),
            with(overdueCompletedAttempt,
                as(studentsTeachersOrAdmin(),
                    failsWith(Status.BAD_REQUEST)
                ),
                everyoneElse(
                    failsWith(Status.FORBIDDEN)
                )
            )
        );
    }

    @Test
    public void answerQuestion() {
        String jsonAnswer = "jsonAnswer";
        ChoiceDTO choice = new ChoiceDTO();
        QuestionValidationResponseDTO validationResponse = new QuestionValidationResponseDTO();

        forEndpoint((attempt) -> () -> quizFacade.answerQuestion(request, attempt.getId(), question.getId(), jsonAnswer),
            with(studentAttempt,
                requiresLogin(),
                as(student,
                    prepare(quizQuestionManager, m -> {
                        expect(m.convertJsonAnswerToChoice(jsonAnswer)).andReturn(choice);
                        expect(m.validateAnswer(questionDO, choice)).andReturn(validationResponse);
                        //expect(m.augmentQuestionsForUser(studentQuiz, studentAttempt, student, false)).andReturn(quiz);
                        m.recordQuestionAttempt(studentAttempt, validationResponse);
                    }),
                    succeeds()
                ),
                forbiddenForEveryoneElse()
            ),
            with(completedAttempt,
                as(everyone,
                    failsWith(Status.FORBIDDEN)
                )
            ),
            with(overdueAttempt,
                as(everyone,
                    failsWith(Status.FORBIDDEN)
                )
            )
        );
    }

    @Test
    public void answerQuestionOnWrongQuiz() {
        String jsonAnswer = "jsonAnswer";

        forEndpoint(() -> quizFacade.answerQuestion(request, otherAttempt.getId(), question.getId(), jsonAnswer),
            as(student,
                failsWith(Status.BAD_REQUEST)
            )
        );
    }

    @Test
    public void answerQuestionOnNonQuiz() {
        String jsonAnswer = "jsonAnswer";

        forEndpoint(() -> quizFacade.answerQuestion(request, studentAttempt.getId(), questionPageQuestion.getId(), jsonAnswer),
            as(student,
                failsWith(Status.BAD_REQUEST)
            )
        );
    }

    @Test
    public void abandonQuizAttempt() {
        forEndpoint((attempt) -> () -> quizFacade.abandonQuizAttempt(request, attempt.getId()),
            with(studentAttempt,
                requiresLogin(),
                as(everyone,
                    failsWith(Status.FORBIDDEN)
                )
            ),
            with(ownAttempt,
                as(student,
                    prepare(quizAttemptManager, m -> m.deleteAttempt(ownAttempt)),
                    succeeds()
                ),
                forbiddenForEveryoneElse()
            ),
            with(ownCompletedAttempt,
                as(everyone,
                    failsWith(Status.FORBIDDEN)
                )
            )
        );
    }

    @Test
    public void logQuizSectionView() {
        int sectionNumber = 3;
        forEndpoint(() -> quizFacade.logQuizSectionView(request, studentAttempt.getId(), sectionNumber),
            requiresLogin(),
            as(student,
                prepare(logManager, m -> {
                    m.logEvent(eq(student), eq(request), eq(Constants.IsaacServerLogType.VIEW_QUIZ_SECTION), anyObject());
                    expectLastCall().andAnswer(() -> {
                        Object[] arguments = getCurrentArguments();
                        Map<String, Object> event = (Map<String, Object>) arguments[3];
                        assertEquals(sectionNumber, event.get(QUIZ_SECTION));
                        return null;
                    });
                }),
                succeeds()
            ),
            forbiddenForEveryoneElse()
        );
    }

    @Test
    public void getAllQuizAssignments() {
        List<UserGroupDTO> studentGroups = ImmutableList.of(studentGroup, studentInactiveGroup);
        forEndpoint((Long groupIdOfInterest) -> () -> quizFacade.getQuizAssignments(request, groupIdOfInterest),
            with(null,
                requiresLogin(),
                as(anyOf(teacher, secondTeacher),
                    prepare(groupManager, m -> expect(m.getAllGroupsOwnedAndManagedByUser(currentUser(), false))
                        .andReturn(studentGroups)),
                    prepare(quizAssignmentManager, m -> expect(m.getAssignmentsForGroups(studentGroups))
                        .andReturn(teacherAssignmentsToTheirGroups)),
                    respondsWith(teacherAssignmentsToTheirGroups)
                ),
                as(otherTeacher,
                    prepare(groupManager, m -> expect(m.getAllGroupsOwnedAndManagedByUser(currentUser(), false))
                        .andReturn(Collections.emptyList())),
                    prepare(quizAssignmentManager, m -> expect(m.getAssignmentsForGroups(Collections.emptyList()))
                        .andReturn(Collections.emptyList())),
                    respondsWith(Collections.emptyList())),
                as(anyOf(student, otherStudent),
                    failsWith(SegueErrorResponse.getIncorrectRoleResponse())
                )
            ),
            with(studentInactiveGroup.getId(),
                requiresLogin(),
                as(anyOf(teacher, adminUser),
                    prepare(groupManager, m -> expect(m.getGroupById(studentInactiveGroup.getId()))
                        .andReturn(studentInactiveGroup)),
                    prepare(quizAssignmentManager, m -> expect(m.getAssignmentsForGroups(Collections.singletonList(studentInactiveGroup)))
                        .andReturn(ImmutableList.of(studentInactiveAssignment, studentInactiveIgnoredAssignment))),
                    respondsWith(ImmutableList.of(studentInactiveAssignment, studentInactiveIgnoredAssignment))
                ),
                as(anyOf(student, otherStudent),
                    failsWith(SegueErrorResponse.getIncorrectRoleResponse())
                ),
                everyoneElse(
                    prepare(groupManager, m -> expect(m.getGroupById(studentInactiveGroup.getId()))
                        .andReturn(studentInactiveGroup)),
                    failsWith(Status.FORBIDDEN)
                )
            )
        );
    }

    public Testcase forbiddenForEveryoneElse() {
        return everyoneElse(
            failsWith(Status.FORBIDDEN)
        );
    }

    private List<ContentSummaryDTO> extractResults(Response availableQuizzes) {
        return ((ResultsWrapper<ContentSummaryDTO>) availableQuizzes.getEntity()).getResults();
    }
}
