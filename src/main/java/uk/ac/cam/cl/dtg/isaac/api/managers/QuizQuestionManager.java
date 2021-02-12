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

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizFeedbackDTO;
import uk.ac.cam.cl.dtg.segue.api.ErrorResponseWrapper;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.segue.api.Constants.ESCAPED_ID_SEPARATOR;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ID_SEPARATOR;

public class QuizQuestionManager {
    private final QuestionManager questionManager;
    private final ContentMapper mapper;
    private final IQuizQuestionAttemptPersistenceManager quizQuestionAttemptManager;
    private final QuizManager quizManager;

    private static final Logger log = LoggerFactory.getLogger(QuizQuestionManager.class);

    /**
     * Manage questions on quizzes.
     *
     * Delegates some behaviour to QuestionManager for the safety of consumers who must not confuse questions with
     * quiz questions.
     *
     * @param questionManager
     *            - for parsing and validating question answers.
     * @param mapper
     *            - an auto mapper to allow us to convert to and from QuestionValidationResponseDOs and DTOs.
     * @param quizQuestionAttemptManager
     *            - for quiz question attempt persistence.
     * @param quizManager
     *            - for quiz sections.
     */
    @Inject
    public QuizQuestionManager(final QuestionManager questionManager, final ContentMapper mapper, final IQuizQuestionAttemptPersistenceManager quizQuestionAttemptManager, QuizManager quizManager) {
        this.questionManager = questionManager;
        this.mapper = mapper;
        this.quizQuestionAttemptManager = quizQuestionAttemptManager;
        this.quizManager = quizManager;
    }

    public ChoiceDTO convertJsonAnswerToChoice(String jsonAnswer) throws ErrorResponseWrapper {
        return questionManager.convertJsonAnswerToChoice(jsonAnswer);
    }

    public QuestionValidationResponseDTO validateAnswer(Question question, ChoiceDTO answerFromClientDTO) throws ErrorResponseWrapper {
        Response response = questionManager.validateAnswer(question, answerFromClientDTO);
        if (response.getEntity() instanceof QuestionValidationResponseDTO) {
            return (QuestionValidationResponseDTO) response.getEntity();
        } else if (response.getEntity() instanceof SegueErrorResponse) {
            throw new ErrorResponseWrapper((SegueErrorResponse) response.getEntity());
        } else {
            throw new ErrorResponseWrapper(new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, response.getEntity().toString()));
        }
    }

    public void recordQuestionAttempt(QuizAttemptDTO quizAttempt, QuestionValidationResponseDTO questionResponse) throws SegueDatabaseException {
        QuestionValidationResponse questionResponseDO = this.mapper.getAutoMapper().map(questionResponse, QuestionValidationResponse.class);

        this.quizQuestionAttemptManager.registerQuestionAttempt(quizAttempt.getId(), questionResponseDO);
    }

    /**
     * This method will ensure any user question attempt information available is used to augment this question object.
     *
     * It will also ensure that any personalisation of questions is affected (e.g. randomised multichoice elements).
     *
     * Note: It will not do anything to related content
     *  @param quiz
     *            - to augment - this object may be mutated as a result of this method. i.e BestAttempt field set on
     *            question DTOs.
     * @param quizAttempt
     *            - which attempt at the quiz to get attempts for.
     * @param user
     *            - which user to augment the questions for.
     * @param includeCorrect
     *            - include whether the answers are correct.
     *
     * @return The quiz object augmented (generally a modified parameter).
     */
    public IsaacQuizDTO augmentQuestionsForUser(IsaacQuizDTO quiz, QuizAttemptDTO quizAttempt, RegisteredUserDTO user, boolean includeCorrect) throws SegueDatabaseException {
        List<QuestionDTO> questionsToAugment = GameManager.getAllMarkableQuestionPartsDFSOrder(quiz);

        Map<QuestionDTO, QuestionValidationResponse> answerMap = getAnswerMap(quizAttempt, questionsToAugment);

        this.augmentQuestionObjectWithAttemptInformation(answerMap, includeCorrect);

        questionManager.shuffleChoiceQuestionsChoices(user.getId().toString(), questionsToAugment);

        return quiz;
    }

    /**
     * Modify the quiz to contain feedback for the specified mode, and possibly the users answers and the correct answers.
     *
     *  @param quiz
     *            - to augment - this object may be mutated as a result of this method. i.e BestAttempt field set on
     *            question DTOs.
     * @param quizAttempt
     *            - which attempt at the quiz to get attempts for.
     * @param feedbackMode
     *            - the type of feedback to augment.
     */
    public IsaacQuizDTO augmentFeedbackFor(IsaacQuizDTO quiz, QuizAttemptDTO quizAttempt, QuizFeedbackMode feedbackMode) throws SegueDatabaseException, ContentManagerException {
        if (feedbackMode == QuizFeedbackMode.NONE) {
            // No feedback, no augmentation to do.
            return quiz;
        }

        // Go get the answers
        Collection<QuestionDTO> questionsToAugment = GameManager.getAllMarkableQuestionPartsDFSOrder(quiz);
        Map<QuestionDTO, QuestionValidationResponse> answerMap = getAnswerMap(quizAttempt, questionsToAugment);

        // Augment the feedback with answers if they should be available.
        if (feedbackMode == QuizFeedbackMode.DETAILED_FEEDBACK) {
            augmentQuestionObjectWithAttemptInformation(answerMap, true);
        }

        QuizFeedbackDTO feedback = getIndividualQuizFeedback(quiz, feedbackMode, questionsToAugment, answerMap);

        quiz.setIndividualFeedback(feedback);

        return quiz;
    }

    /**
     * Extract the latest attempt for each question.
     *
     * @param quizAttempt
     *            - which attempt at the quiz to get attempts for.
     * @param questionsToAugment
     *            - list of question objects to extract the latest attempt for.
     */
    @VisibleForTesting
    Map<QuestionDTO, QuestionValidationResponse> getAnswerMap(QuizAttemptDTO quizAttempt,
                                                              Collection<QuestionDTO> questionsToAugment)
        throws SegueDatabaseException {
        Map<String, List<QuestionValidationResponse>> answers = quizQuestionAttemptManager.getAllAnswersForQuizAttempt(quizAttempt.getId());

        Map<QuestionDTO, QuestionValidationResponse> results = new HashMap<>();

        for (QuestionDTO question : questionsToAugment) {
            List<QuestionValidationResponse> questionAttempts = answers.get(question.getId());

            QuestionValidationResponse lastResponse = null;

            if (questionAttempts != null && questionAttempts.size() > 0) {
                // The latest answer is the only answer we consider.
                lastResponse = questionAttempts.get(questionAttempts.size() - 1);
            }

            results.put(question, lastResponse);
        }

        return results;
    }

    /**
     * Modify the questions in a quiz to contain the latest answers if available.
     *
     * When we say bestAttempt, we actually mean latest attempt.
     *
     * @param answerMap Map from QuestionDTOs to the latest answer (or null if there is no latest answer).
     * @param includeCorrect Include whether the answers are correct.
     */
    @VisibleForTesting
    void augmentQuestionObjectWithAttemptInformation(Map<QuestionDTO, QuestionValidationResponse> answerMap,
                                                     boolean includeCorrect) {
        answerMap.forEach((question, lastResponse) -> {
            if (lastResponse != null) {
                QuestionValidationResponseDTO lastAttempt;
                if (includeCorrect) {
                    // Include full validation details.
                    lastAttempt = questionManager.convertQuestionValidationResponseToDTO(lastResponse);
                } else {
                    // Manual extract only the safe details (questionId, answer, date attempted).
                    Choice answer = lastResponse.getAnswer();
                    lastAttempt = new QuestionValidationResponseDTO();
                    DTOMapping dtoMapping = answer.getClass().getAnnotation(DTOMapping.class);
                    lastAttempt.setAnswer(mapper.getAutoMapper().map(lastResponse.getAnswer(),
                        (Class<? extends ChoiceDTO>) dtoMapping.value()));
                    lastAttempt.setQuestionId(lastResponse.getQuestionId());
                    lastAttempt.setDateAttempted(lastResponse.getDateAttempted());
                }
                question.setBestAttempt(lastAttempt);
            }
        });
    }

    /**
     * Get the feedback (marks) for an individual's answers to a quiz.
     *
     * @param quiz The quiz of interest.
     * @param feedbackMode What level of feedback to provide.
     * @param questionsToAugment The questions from the quiz.
     * @param answerMap The individual's answers.
     * @return The quiz feedback.
     * @throws ContentManagerException In DEV, if there is a malformed quiz (top-level non-Sections).
     */
    @VisibleForTesting
    QuizFeedbackDTO getIndividualQuizFeedback(IsaacQuizDTO quiz, QuizFeedbackMode feedbackMode, Collection<QuestionDTO> questionsToAugment, Map<QuestionDTO, QuestionValidationResponse> answerMap) throws ContentManagerException {
        if (feedbackMode == QuizFeedbackMode.NONE) {
            return null;
        }

        List<IsaacQuizSectionDTO> sections = quizManager.extractSectionObjects(quiz);

        // Make a score table
        Map<String, QuizFeedbackDTO.Mark> scoreTable = sections.stream().collect(Collectors.toMap(s -> s.getId(), s -> new QuizFeedbackDTO.Mark()));

        // Calculate the scores
        for (QuestionDTO question: questionsToAugment) {
            String sectionId = extractSectionIdFromQuizQuestionId(question.getId());
            QuizFeedbackDTO.Mark mark = scoreTable.get(sectionId);
            if (mark == null) {
                log.error("Missing quiz section id: " + sectionId + " in question " + question + " but not in section map " + sections);
                continue;
            }
            mark.questionPartsTotal++;
            QuestionValidationResponse response = answerMap.get(question);
            if (response != null) {
                if (response.isCorrect()) {
                    mark.questionPartsCorrect++;
                } else {
                    mark.questionPartsIncorrect++;
                }
            } else {
                mark.questionPartsNotAttempted++;
            }
        }

        QuizFeedbackDTO.Mark overall = consolidateMarks(scoreTable);

        QuizFeedbackDTO feedback;
        switch (feedbackMode) {
            case OVERALL_MARK:
                feedback = new QuizFeedbackDTO(overall, null);
                break;
            case SECTION_MARKS:
            case DETAILED_FEEDBACK:
                feedback = new QuizFeedbackDTO(overall, sections.stream().map(s -> new QuizFeedbackDTO.SectionMark(s.getId(), s.getTitle(), scoreTable.get(s.getId()))).collect(Collectors.toList()));
                break;
            default:
                throw new RuntimeException("Non-exhaustive switch on feedbackMode");
        }
        return feedback;
    }

    private QuizFeedbackDTO.Mark consolidateMarks(Map<String, QuizFeedbackDTO.Mark> scoreTable) {
        QuizFeedbackDTO.Mark result = new QuizFeedbackDTO.Mark();
        scoreTable.values().forEach(mark -> {
            result.questionPartsTotal += mark.questionPartsTotal;
            result.questionPartsCorrect += mark.questionPartsCorrect;
            result.questionPartsIncorrect += mark.questionPartsIncorrect;
            result.questionPartsNotAttempted += mark.questionPartsNotAttempted;
        });
        return result;
    }

    /**
     * Extract the fully-qualified section ID from the question ID.
     *
     * Note this means we extract "quizId|sectionId" from "quizId|sectionId|questionId".
     * It ends up being neater because all the section objects have ids that are fully-qualified.
     *
     * @param questionId A question ID of the form "quizId|sectionId|questionId".
     * @return A section ID of the form "quizId|sectionId".
     */
    private static String extractSectionIdFromQuizQuestionId(String questionId) {
        String[] ids = questionId.split(ESCAPED_ID_SEPARATOR, 3);
        return ids[0] + ID_SEPARATOR + ids[1];
    }
}
