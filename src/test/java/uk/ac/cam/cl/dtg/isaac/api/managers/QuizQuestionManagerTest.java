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

import com.google.common.collect.ImmutableMap;
import ma.glasnost.orika.MapperFacade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizQuestionAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.QuantityValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replay;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UserAccountManager.class})
@PowerMockIgnore({ "javax.ws.*", "javax.management.*", "javax.script.*" })
public class QuizQuestionManagerTest extends AbstractManagerTest {

    private QuizQuestionManager quizQuestionManager;

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

    @Before
    public void setUp() {
        quizQuestionAttemptPersistenceManager = createMock(IQuizQuestionAttemptPersistenceManager.class);
        QuestionManager questionManager = createMock(QuestionManager.class);
        ContentMapper contentMapper = createMock(ContentMapper.class);
        MapperFacade mapperFacade = createMock(MapperFacade.class);

        quizQuestionManager = new QuizQuestionManager(questionManager, contentMapper, quizQuestionAttemptPersistenceManager);

        expect(contentMapper.getAutoMapper()).andStubReturn(mapperFacade);
        expect(mapperFacade.map(correctAnswer, ChoiceDTO.class)).andStubReturn(correctAnswerDTO);
        expect(mapperFacade.map(wrongAnswer, ChoiceDTO.class)).andStubReturn(wrongAnswerDTO);

        expect(questionManager.convertQuestionValidationResponseToDTO(wrongResponse)).andStubReturn(wrongResponseDTO);
        expect(questionManager.convertQuestionValidationResponseToDTO(correctResponse)).andStubReturn(correctResponseDTO);

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

        correctResponseDTO = new QuestionValidationResponseDTO(question.getId(), correctAnswerDTO, false, correctExplanationDTO, somePastDate);
        wrongResponseDTO = new QuestionValidationResponseDTO(question.getId(), wrongAnswerDTO, false, wrongExplanationDTO, somePastDate);

        quantityResponse = new QuantityValidationResponse(question.getId(), correctAnswer, true, correctExplanation, true, true, somePastDate);
    }

    @Test
    public void augmentQuestionObjectWithAttemptInformationCanRemoveCorrectAndExplanation() throws SegueDatabaseException {
        for (boolean includeCorrect : Arrays.asList(true, false)) {
            with(quizQuestionAttemptPersistenceManager, m -> expect(m.getAllAnswersForQuizAttempt(studentAttempt.getId()))
                .andReturn(Collections.singletonMap(
                    question.getId(),
                    Collections.singletonList(wrongResponse))));
            quizQuestionManager.augmentQuestionObjectWithAttemptInformation(studentAttempt, Collections.singletonList(question), includeCorrect);

            assertEquals(includeCorrect, question.getBestAttempt().isCorrect() != null);
            assertEquals(includeCorrect, question.getBestAttempt().getExplanation() !=  null);
        }
    }

    @Test
    public void augmentQuestionObjectWithAttemptInformationUsesLatestAttempt() throws SegueDatabaseException {
        Map<List<QuestionValidationResponse>, Optional<ChoiceDTO>> optionsToTest = ImmutableMap.of(
            Collections.emptyList(), Optional.empty(),
            Collections.singletonList(correctResponse), Optional.of(correctAnswerDTO),
            Collections.singletonList(wrongResponse), Optional.of(wrongAnswerDTO),
            Arrays.asList(correctResponse, wrongResponse), Optional.of(wrongAnswerDTO),
            Arrays.asList(wrongResponse, correctResponse), Optional.of(correctAnswerDTO)
        );

        for (final Map.Entry<List<QuestionValidationResponse>, Optional<ChoiceDTO>> option: optionsToTest.entrySet()) {
            with(quizQuestionAttemptPersistenceManager, m -> {
                expect(m.getAllAnswersForQuizAttempt(studentAttempt.getId()))
                    .andReturn(Collections.singletonMap(
                        question.getId(),
                        option.getKey()));
            });
            quizQuestionManager.augmentQuestionObjectWithAttemptInformation(studentAttempt, Collections.singletonList(question), true);

            ChoiceDTO answer = question.getBestAttempt() != null ? question.getBestAttempt().getAnswer() : null;
            assertEquals(option.getValue().orElse(null), answer);
        }
    }

    @Test
    public void quantityValidationResponseIsStrippedCorrectly() throws SegueDatabaseException {
        with(quizQuestionAttemptPersistenceManager, m -> expect(m.getAllAnswersForQuizAttempt(studentAttempt.getId()))
            .andReturn(Collections.singletonMap(
                question.getId(),
                Collections.singletonList(quantityResponse))));

        quizQuestionManager.augmentQuestionObjectWithAttemptInformation(studentAttempt, Collections.singletonList(question), false);

        // Don't return a QuantityValidationResponseDTO
        assertEquals(QuestionValidationResponseDTO.class, question.getBestAttempt().getClass());
    }

}
