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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizFeedbackDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replay;

public class QuizQuestionManagerTest extends AbstractManagerTest {

    private QuizQuestionManager quizQuestionManager;

    private QuestionManager questionManager;
    private IQuizQuestionAttemptPersistenceManager quizQuestionAttemptPersistenceManager;

    private QuestionValidationResponse correctResponse;
    private QuestionValidationResponse wrongResponse;
    private QuestionValidationResponseDTO correctResponseDTO;
    private QuestionValidationResponseDTO wrongResponseDTO;
    private ChoiceDTO correctAnswerDTO;
    private ChoiceDTO wrongAnswerDTO;
    private Choice correctAnswer;
    private Choice wrongAnswer;
    private QuantityValidationResponse quantityResponse;
    private List<QuestionDTO> questions;
    private Map<QuestionDTO, QuestionValidationResponse> answerMap;

    @Before
    public void setUp() {
        quizQuestionAttemptPersistenceManager = createMock(IQuizQuestionAttemptPersistenceManager.class);
        questionManager = createMock(QuestionManager.class);
        ContentMapper contentMapper = createMock(ContentMapper.class);
        MapperFacade mapperFacade = createMock(MapperFacade.class);

        quizQuestionManager = new QuizQuestionManager(questionManager, contentMapper, quizQuestionAttemptPersistenceManager, quizManager);

        expect(contentMapper.getAutoMapper()).andStubReturn(mapperFacade);
        expect(mapperFacade.map(correctAnswer, ChoiceDTO.class)).andStubReturn(correctAnswerDTO);
        expect(mapperFacade.map(wrongAnswer, ChoiceDTO.class)).andStubReturn(wrongAnswerDTO);

        registerDefaultsFor(questionManager, m -> {
            expect(m.convertQuestionValidationResponseToDTO(wrongResponse)).andStubReturn(wrongResponseDTO);
            expect(m.convertQuestionValidationResponseToDTO(correctResponse)).andStubReturn(correctResponseDTO);
        });

        replay(quizQuestionAttemptPersistenceManager, questionManager, contentMapper, mapperFacade);
    }

    @Before
    public void initializeAdditionalObjects() {
        correctAnswer = new Choice();
        correctAnswerDTO = new ChoiceDTO();
        wrongAnswer = new Choice();
        wrongAnswerDTO = new ChoiceDTO();
        Content correctExplanation = new Content();
        ContentDTO correctExplanationDTO = new ContentDTO();
        Content wrongExplanation = new Content();
        ContentDTO wrongExplanationDTO = new ContentDTO();
        correctResponse = new QuestionValidationResponse(question.getId(), correctAnswer, true, correctExplanation, somePastDate);
        wrongResponse = new QuestionValidationResponse(question.getId(), wrongAnswer, false, wrongExplanation, somePastDate);

        correctResponseDTO = new QuestionValidationResponseDTO(question.getId(), correctAnswerDTO, true, correctExplanationDTO, somePastDate);
        wrongResponseDTO = new QuestionValidationResponseDTO(question.getId(), wrongAnswerDTO, false, wrongExplanationDTO, somePastDate);

        quantityResponse = new QuantityValidationResponse(question.getId(), correctAnswer, true, correctExplanation, true, true, somePastDate);

        questions = ImmutableList.of(question, question2, question3);
        answerMap = ImmutableMap.of(
            question, correctResponse,
            question2, wrongResponse); // question3 is unanswered.
    }

    @Test
    public void augmentQuestionObjectWithAttemptInformationCanRemoveCorrectAndExplanation() throws SegueDatabaseException {
        for (boolean includeCorrect : Arrays.asList(true, false)) {
            quizQuestionManager.augmentQuestionObjectWithAttemptInformation(
                Collections.singletonMap(question, wrongResponse), includeCorrect);

            assertEquals(includeCorrect, question.getBestAttempt().isCorrect() != null);
            assertEquals(includeCorrect, question.getBestAttempt().getExplanation() !=  null);
        }
    }

    @Test
    public void getAnswerMapUsesLatestAttempt() throws SegueDatabaseException {
        Map<List<QuestionValidationResponse>, Optional<QuestionValidationResponse>> optionsToTest = ImmutableMap.of(
            Collections.emptyList(), Optional.empty(),
            singletonList(correctResponse), Optional.of(correctResponse),
            singletonList(wrongResponse), Optional.of(wrongResponse),
            Arrays.asList(correctResponse, wrongResponse), Optional.of(wrongResponse),
            Arrays.asList(wrongResponse, correctResponse), Optional.of(correctResponse)
        );

        for (final Map.Entry<List<QuestionValidationResponse>, Optional<QuestionValidationResponse>> option: optionsToTest.entrySet()) {
            with(quizQuestionAttemptPersistenceManager, m -> {
                expect(m.getAllAnswersForQuizAttempt(studentAttempt.getId()))
                    .andReturn(Collections.singletonMap(
                        question.getId(),
                        option.getKey()));
            });
            Map<QuestionDTO, QuestionValidationResponse> answerMap = quizQuestionManager.getAnswerMap(studentAttempt, singletonList(question));

            QuestionValidationResponse answer = answerMap.get(question);

            assertEquals(option.getValue().orElse(null), answer);
        }
    }

    @Test
    public void quantityValidationResponseIsStrippedCorrectly() throws SegueDatabaseException {
        quizQuestionManager.augmentQuestionObjectWithAttemptInformation(Collections.singletonMap(question, quantityResponse), false);

        // Don't return a QuantityValidationResponseDTO
        assertEquals(QuestionValidationResponseDTO.class, question.getBestAttempt().getClass());
    }

    @Test
    public void augmentQuestionsForUserShufflesQuestionChoices() throws SegueDatabaseException {
        with(quizQuestionAttemptPersistenceManager, m -> {
            expect(m.getAllAnswersForQuizAttempt(studentAttempt.getId()))
                .andReturn(Collections.singletonMap(
                    question.getId(),
                    singletonList(correctResponse)));
        });

        with(questionManager, m -> {
            m.shuffleChoiceQuestionsChoices(student.getId().toString(), questions);
        });

        quizQuestionManager.augmentQuestionsForUser(studentQuiz, studentAttempt, student, true);
    }

    @Test
    public void augmentFeedbackFor() throws SegueDatabaseException, ContentManagerException {
        with(quizQuestionAttemptPersistenceManager, m -> {
            expect(m.getAllAnswersForQuizAttempt(studentAttempt.getId()))
                .andReturn(answerMap.entrySet().stream().collect(Collectors.toMap(
                    a -> a.getKey().getId(),
                    a -> singletonList(a.getValue())
                )));
        });

        IsaacQuizDTO resultQuiz = quizQuestionManager.augmentFeedbackFor(studentQuiz, studentAttempt, QuizFeedbackMode.DETAILED_FEEDBACK);

        assertNotNull(resultQuiz.getIndividualFeedback());
        assertNotNull(resultQuiz.getIndividualFeedback().getOverallMark());
        assertNotNull(resultQuiz.getIndividualFeedback().getSectionMarks());
        assertNotNull(question.getBestAttempt());
        assertTrue(question.getBestAttempt().isCorrect());
        assertNotNull(question2.getBestAttempt());
        assertFalse(question2.getBestAttempt().isCorrect());
        assertNull(question3.getBestAttempt());
    }

    @Test
    public void augmentFeedbackForWithNoneFeedbackModeDoesNoWork() throws SegueDatabaseException, ContentManagerException {
        IsaacQuizDTO resultQuiz = quizQuestionManager.augmentFeedbackFor(studentQuiz, studentAttempt, QuizFeedbackMode.NONE);

        assertNull(resultQuiz.getIndividualFeedback());
    }

    @Test
    public void getIndividualQuizFeedback() throws ContentManagerException {
        List<QuizFeedbackMode> modes = ImmutableList.of(
            QuizFeedbackMode.NONE,
            QuizFeedbackMode.OVERALL_MARK,
            QuizFeedbackMode.SECTION_MARKS,
            QuizFeedbackMode.DETAILED_FEEDBACK
        );

        for (QuizFeedbackMode feedbackMode : modes) {
            QuizFeedbackDTO result = quizQuestionManager.getIndividualQuizFeedback(studentQuiz, feedbackMode, questions, answerMap);
            switch (feedbackMode) {
                case NONE:
                    assertNull(result);
                    break;
                default:
                    assertNotNull(result.getOverallMark());
                    break;
            }
            switch (feedbackMode) {
                case NONE:
                    assertNull(result);
                    break;
                case OVERALL_MARK:
                    assertNull(result.getSectionMarks());
                    break;
                default:
                    assertNotNull(result.getSectionMarks());
                    break;
            }
        }
    }

    @Test
    public void getIndividualQuizFeedbackCalculations() throws ContentManagerException {
        QuizFeedbackDTO result = quizQuestionManager.getIndividualQuizFeedback(studentQuiz, QuizFeedbackMode.SECTION_MARKS, questions, answerMap);

        assertMarks(3, 1, 1, 1, result.getOverallMark());
        assertMarks(1, 1, 0, 0, result.getSectionMarks().get(0).getMark());
        assertMarks(2, 0, 1, 1, result.getSectionMarks().get(1).getMark());
    }

    private void assertMarks(int total, int correct, int incorrect, int notAttempted, QuizFeedbackDTO.Mark mark) {
        assertEquals(total, mark.questionPartsTotal.intValue());
        assertEquals(correct, mark.questionPartsCorrect.intValue());
        assertEquals(incorrect, mark.questionPartsIncorrect.intValue());
        assertEquals(notAttempted, mark.questionPartsNotAttempted.intValue());
    }
}
