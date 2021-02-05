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

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.api.ErrorResponseWrapper;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
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
import java.util.List;
import java.util.Map;

public class QuizQuestionManager {
    private final QuestionManager questionManager;
    private final ContentMapper mapper;
    private final IQuizQuestionAttemptPersistenceManager quizQuestionAttemptManager;

    private static final Logger log = LoggerFactory.getLogger(QuizQuestionManager.class);

    /**
     * Manage questions on quizzes.
     *
     * Delegates some behaviour to QuestionManager for the safety of consumers who msut not confuse questions with
     * quiz questions.
     *
     * @param questionManager
     *            - for parsing and validating question answers.
     * @param mapper
     *            - an auto mapper to allow us to convert to and from QuestionValidationResponseDOs and DTOs.
     * @param quizQuestionAttemptManager
     *            - for quiz question attempt persistence.
     */
    @Inject
    public QuizQuestionManager(final QuestionManager questionManager, final ContentMapper mapper, final IQuizQuestionAttemptPersistenceManager quizQuestionAttemptManager) {
        this.questionManager = questionManager;
        this.mapper = mapper;
        this.quizQuestionAttemptManager = quizQuestionAttemptManager;
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
        List<QuestionDTO> questionsToAugment = QuestionManager.extractQuestionObjects(quiz);

        this.augmentQuestionObjectWithAttemptInformation(quizAttempt, questionsToAugment, includeCorrect);

        questionManager.shuffleChoiceQuestionsChoices(user.getId().toString(), questionsToAugment);

        return quiz;
    }

    /**
     * Modify the questions in a quiz such that it contains bestAttempt information if we can provide it.
     *
     * When we say bestAttempt, we actually mean latest attempt.
     *  @param quizAttempt
     *            - which attempt at the quiz to get attempts for.
     * @param questionsToAugment
     *            - list of question objects to modify.
     * @param includeCorrect
     *            - include whether the answers are correct.
     */
    private void augmentQuestionObjectWithAttemptInformation(QuizAttemptDTO quizAttempt, List<QuestionDTO> questionsToAugment, boolean includeCorrect) throws SegueDatabaseException {
        Map<String, List<QuestionValidationResponse>> answers = quizQuestionAttemptManager.getAllAnswersForQuizAttempt(quizAttempt.getId());

        for (QuestionDTO question : questionsToAugment) {
            List<QuestionValidationResponse> questionAttempts = answers.get(question.getId());

            if (questionAttempts.size() > 0) {
                // The latest answer is the only answer we consider.
                QuestionValidationResponse lastResponse = questionAttempts.get(questionAttempts.size() - 1);

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
        }
    }
}
