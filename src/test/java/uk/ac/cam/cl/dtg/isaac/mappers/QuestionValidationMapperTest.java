package uk.ac.cam.cl.dtg.isaac.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.CustomAssertions.assertDeepEquals;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dos.FormulaValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.ItemValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.FormulaValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ItemValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuantityValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;

class QuestionValidationMapperTest {

  private QuestionValidationMapper questionValidationMapper;
  private static final Instant testDate = Instant.now();

  @BeforeEach
  void beforeEach() {
    questionValidationMapper = QuestionValidationMapper.INSTANCE;
  }

  @ParameterizedTest
  @MethodSource("testCasesDOtoDTO")
  <S extends QuestionValidationResponse, T extends QuestionValidationResponseDTO> void mappingDOReturnsExpectedDTO(S source, T expected) {
    QuestionValidationResponseDTO actual = questionValidationMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @ParameterizedTest
  @MethodSource("testCasesDTOtoDO")
  <S extends QuestionValidationResponseDTO, T extends QuestionValidationResponse> void mappingDTOReturnsExpectedDO(S source, T expected) {
    QuestionValidationResponse actual = questionValidationMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  private static Stream<Arguments> testCasesDOtoDTO() {
    return Stream.of(
        Arguments.of(prepareOriginalQuestionValidationResponse(), prepareQuestionValidationResponseDTO()),
        Arguments.of(prepareOriginalFormulaValidationResponse(), prepareMappedFormulaValidationResponseDTO()),
        Arguments.of(prepareOriginalItemValidationResponse(), prepareItemValidationResponseDTO()),
        Arguments.of(prepareOriginalQuantityValidationResponse(), prepareQuantityValidationResponseDTO())
    );
  }

  private static Stream<Arguments> testCasesDTOtoDO() {
    return Stream.of(
        Arguments.of(prepareQuestionValidationResponseDTO(), prepareMappedQuestionValidationResponse()),
        Arguments.of(prepareOriginalFormulaValidationResponseDTO(), prepareMappedFormulaValidationResponse()),
        Arguments.of(prepareItemValidationResponseDTO(), prepareMappedItemValidationResponse()),
        Arguments.of(prepareQuantityValidationResponseDTO(), prepareMappedQuantityValidationResponse())
    );
  }

  private static QuestionValidationResponse prepareOriginalQuestionValidationResponse() {
    return prepareOriginalQuestionValidationResponse(new QuestionValidationResponse());
  }

  private static <T extends QuestionValidationResponse> T prepareOriginalQuestionValidationResponse(T object) {
    Content answerExplanation = new Content();
    answerExplanation.setId("choiceExplanationId");
    answerExplanation.setValue("answerExplanation");
    answerExplanation.setPublished(true);
    answerExplanation.setTags(Set.of());

    Choice answer = new Choice();
    answer.setId("answerId");
    answer.setCorrect(true);
    answer.setExplanation(answerExplanation);
    answer.setPublished(true);
    answer.setTags(Set.of());

    T partiallyPreparedObject = prepareQuestionValidationResponse(object);
    partiallyPreparedObject.setAnswer(answer);
    return partiallyPreparedObject;
  }

  private static FormulaValidationResponse prepareOriginalFormulaValidationResponse() {
    FormulaValidationResponse object = prepareOriginalQuestionValidationResponse(new FormulaValidationResponse());
    object.setMatchType("matchType");
    return object;
  }

  private static ItemValidationResponse prepareOriginalItemValidationResponse() {
    ItemValidationResponse object = prepareOriginalQuestionValidationResponse(new ItemValidationResponse());
    object.setItemsCorrect(List.of(true, false, true));
    return object;
  }

  private static QuantityValidationResponse prepareOriginalQuantityValidationResponse() {
    QuantityValidationResponse object = prepareOriginalQuestionValidationResponse(new QuantityValidationResponse());
    object.setCorrectValue(true);
    object.setCorrectValue(true);
    return object;
  }

  private static QuestionValidationResponse prepareMappedQuestionValidationResponse() {
    return prepareMappedQuestionValidationResponse(new QuestionValidationResponse());
  }

  private static <T extends QuestionValidationResponse> T prepareMappedQuestionValidationResponse(T object) {
    Choice answer = new Choice();
    answer.setId("answerId");
    answer.setCorrect(false);
    answer.setExplanation(null);
    answer.setPublished(true);
    answer.setTags(Set.of());

    T partiallyPreparedObject = prepareQuestionValidationResponse(object);
    partiallyPreparedObject.setAnswer(answer);
    return partiallyPreparedObject;
  }

  private static FormulaValidationResponse prepareMappedFormulaValidationResponse() {
    FormulaValidationResponse object = prepareMappedQuestionValidationResponse(new FormulaValidationResponse());
    object.setMatchType(null);
    return object;
  }

  private static ItemValidationResponse prepareMappedItemValidationResponse() {
    ItemValidationResponse object = prepareMappedQuestionValidationResponse(new ItemValidationResponse());
    object.setItemsCorrect(List.of(true, false, true));
    return object;
  }

  private static QuantityValidationResponse prepareMappedQuantityValidationResponse() {
    QuantityValidationResponse object = prepareMappedQuestionValidationResponse(new QuantityValidationResponse());
    object.setCorrectValue(true);
    object.setCorrectValue(true);
    return object;
  }

  private static <T extends QuestionValidationResponse> T prepareQuestionValidationResponse(T object) {
    Content responseExplanation = new Content();
    responseExplanation.setId("responseExplanationId");
    responseExplanation.setValue("responseExplanation");
    responseExplanation.setPublished(true);
    responseExplanation.setTags(Set.of());

    object.setQuestionId("questionId");
    object.setCorrect(true);
    object.setDateAttempted(testDate);
    object.setExplanation(responseExplanation);
    return object;
  }

  private static QuestionValidationResponseDTO prepareQuestionValidationResponseDTO() {
    return prepareQuestionValidationResponseDTO(new QuestionValidationResponseDTO());
  }

  private static <T extends QuestionValidationResponseDTO> T prepareQuestionValidationResponseDTO(T object) {
    ChoiceDTO answer = new ChoiceDTO();
    answer.setId("answerId");
    answer.setPublished(true);

    ContentDTO responseExplanation = new ContentDTO();
    responseExplanation.setId("responseExplanationId");
    responseExplanation.setValue("responseExplanation");
    responseExplanation.setPublished(true);

    object.setQuestionId("questionId");
    object.setCorrect(true);
    object.setDateAttempted(testDate);
    object.setAnswer(answer);
    object.setExplanation(responseExplanation);
    return object;
  }

  private static FormulaValidationResponseDTO prepareOriginalFormulaValidationResponseDTO() {
    FormulaValidationResponseDTO object = prepareQuestionValidationResponseDTO(new FormulaValidationResponseDTO());
    object.setCorrectExact(true);
    object.setCorrectSymbolic(true);
    object.setCorrectNumeric(true);
    return object;
  }

  private static FormulaValidationResponseDTO prepareMappedFormulaValidationResponseDTO() {
    FormulaValidationResponseDTO object = prepareQuestionValidationResponseDTO(new FormulaValidationResponseDTO());
    object.setCorrectExact(null);
    object.setCorrectSymbolic(null);
    object.setCorrectNumeric(null);
    return object;
  }

  private static ItemValidationResponseDTO prepareItemValidationResponseDTO() {
    ItemValidationResponseDTO object = prepareQuestionValidationResponseDTO(new ItemValidationResponseDTO());
    object.setItemsCorrect(List.of(true, false, true));
    return object;
  }

  private static QuantityValidationResponseDTO prepareQuantityValidationResponseDTO() {
    QuantityValidationResponseDTO object = prepareQuestionValidationResponseDTO(new QuantityValidationResponseDTO());
    object.setCorrectValue(true);
    object.setCorrectValue(true);
    return object;
  }
}