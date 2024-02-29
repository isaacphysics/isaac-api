package uk.ac.cam.cl.dtg.isaac.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.ac.cam.cl.dtg.CustomAssertions.assertDeepEquals;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;
import uk.ac.cam.cl.dtg.isaac.dos.Difficulty;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacEventPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacItemQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacPod;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dos.RoleRequirement;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dos.content.AnvilApp;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.EmailTemplate;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.isaac.dos.content.Image;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFreeTextQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacItemQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacPodDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacWildcardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizFeedbackDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.AnvilAppDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.CodeSnippetDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ItemDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuizSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.util.locations.Address;
import uk.ac.cam.cl.dtg.util.locations.Location;

class ContentMapperTest {
  private ContentMapper contentMapper;
  private static final Date testDate = new Date();

  @BeforeEach
  void beforeEach() {
    contentMapper = ContentMapper.INSTANCE;
  }

  @ParameterizedTest
  @MethodSource("testCasesDOtoDTO")
  <S extends Content, T extends ContentDTO> void mappingDOReturnsExpectedDTO(S source, T expected) {
    S sourceWithCommonProperties = setOriginalCommonContentProperties(source);
    T expectedWithCommonProperties = setMappedCommonContentDTOProperties(expected);
    ContentDTO actual = (ContentDTO) contentMapper.map(sourceWithCommonProperties);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expectedWithCommonProperties, actual);
  }

  @ParameterizedTest
  @MethodSource("testCasesDTOtoDO")
  <S extends ContentDTO, T extends Content> void mappingDTOReturnsExpectedDO(S source, T expected) {
    S sourceWithCommonProperties = setOriginalCommonContentDTOProperties(source);
    T expectedWithCommonProperties = setMappedCommonContentProperties(expected);
    Content actual = (Content) contentMapper.map(sourceWithCommonProperties);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expectedWithCommonProperties, actual);
  }

  @ParameterizedTest
  @MethodSource("testCasesFromContentDTO")
  <T> void defaultMappingMethodFrom_ContentDTO_returnsRequestedClass(ContentDTO source, Class<T> targetClass, T expected) {
    T actual = contentMapper.map(source, targetClass);
    assertEquals(targetClass, actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @ParameterizedTest
  @MethodSource("testCasesFromContent")
  <T> void defaultMappingMethodFrom_Content_returnsRequestedClass(Content source, Class<T> targetClass, T expected) {
    T actual = contentMapper.map(source, targetClass);
    assertEquals(targetClass, actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void defaultMappingMethodFrom_ContentDTO_throwsUnimplementedMappingExceptionForUnexpectedTarget() {
    ContentDTO source = new ContentDTO();
    Exception exception = assertThrows(UnimplementedMappingException.class, () -> contentMapper.map(source, IsaacPodDTO.class));
    assertEquals("Invocation of unimplemented mapping from ContentDTO to IsaacPodDTO", exception.getMessage());
  }

  @Test
  void defaultMappingMethodFrom_Content_throwsUnimplementedMappingExceptionForUnexpectedTarget() {
    Content source = new Content();
    Exception exception = assertThrows(UnimplementedMappingException.class, () -> contentMapper.map(source, IsaacPod.class));
    assertEquals("Invocation of unimplemented mapping from Content to IsaacPod", exception.getMessage());
  }

  @ParameterizedTest
  @MethodSource("testCasesCopyDTO")
  void copyContentDTOReturnsNewObjectWithSameProperties(ContentDTO source) {
    ContentDTO actual = contentMapper.copy(source);
    assertEquals(actual.getClass(), source.getClass());
    assertNotSame(actual, source);
    assertDeepEquals(actual, source);
  }

  private static Stream<Arguments> testCasesDOtoDTO() {
    return Stream.of(
        Arguments.of(new Content(), new ContentDTO()),
        Arguments.of(prepareAnvilAppDO(), prepareAnvilAppDTO()),
        Arguments.of(prepareOriginalChoiceDO(), prepareChoiceDTO()),
        Arguments.of(prepareOriginalChoiceQuestionDO(new ChoiceQuestion()), prepareMappedChoiceQuestionDTO(new ChoiceQuestionDTO())),
        Arguments.of(prepareOriginalChoiceQuestionDO(new IsaacFreeTextQuestion()), prepareMappedChoiceQuestionDTO(new IsaacFreeTextQuestionDTO())),
        Arguments.of(prepareEmailTemplateDO(), prepareEmailTemplateDTO()),
        Arguments.of(prepareImageDO(), prepareImageDTO()),
        Arguments.of(prepareIsaacEventPageDO(), prepareMappedIsaacEventPageDTO()),
        Arguments.of(prepareOriginalIsaacItemQuestionDO(), prepareMappedIsaacItemQuestionDTO()),
        Arguments.of(prepareIsaacQuestionPageDO(), prepareIsaacQuestionPageDTO()),
        Arguments.of(prepareIsaacQuizDO(), prepareMappedIsaacQuizDTO()),
        Arguments.of(prepareOriginalIsaacSymbolicQuestionDO(), prepareMappedIsaacSymbolicQuestionDTO()),
        Arguments.of(prepareOriginalQuestionDO(), prepareMappedQuestionDTO()),
        Arguments.of(prepareSeguePageDO(), prepareSeguePageDTO())
    );
  }

  private static Stream<Arguments> testCasesDTOtoDO() {
    return Stream.of(
        Arguments.of(new ContentDTO(), new Content()),
        Arguments.of(prepareAnvilAppDTO(), prepareAnvilAppDO()),
        Arguments.of(prepareChoiceDTO(), prepareMappedChoiceDO()),
        Arguments.of(prepareOriginalChoiceQuestionDTO(new ChoiceQuestionDTO()), prepareMappedChoiceQuestionDO(new ChoiceQuestion())),
        Arguments.of(prepareOriginalChoiceQuestionDTO(new IsaacFreeTextQuestionDTO()), prepareMappedChoiceQuestionDO(new IsaacFreeTextQuestion())),
        Arguments.of(prepareEmailTemplateDTO(), prepareEmailTemplateDO()),
        Arguments.of(prepareImageDTO(), prepareImageDO()),
        Arguments.of(prepareOriginalIsaacEventPageDTO(), prepareIsaacEventPageDO()),
        Arguments.of(prepareOriginalIsaacItemQuestionDTO(), prepareMappedIsaacItemQuestionDO()),
        Arguments.of(prepareIsaacQuestionPageDTO(), prepareIsaacQuestionPageDO()),
        Arguments.of(prepareOriginalIsaacQuizDTO(), prepareIsaacQuizDO()),
        Arguments.of(prepareOriginalIsaacSymbolicQuestionDTO(), prepareMappedIsaacSymbolicQuestionDO()),
        Arguments.of(prepareOriginalQuestionDTO(), prepareMappedQuestionDO()),
        Arguments.of(prepareSeguePageDTO(), prepareSeguePageDO())
    );
  }

  private static Stream<Arguments> testCasesFromContentDTO() {
    return Stream.of(
        Arguments.of(setOriginalCommonContentDTOProperties(new ContentDTO()), ContentSummaryDTO.class, prepareContentSummaryDTOFromContentDTO()),
        Arguments.of(setOriginalCommonContentDTOProperties(prepareIsaacQuestionPageDTO()), ContentSummaryDTO.class, prepareContentSummaryDTOFromIsaacQuestionPageDTO()),
        Arguments.of(setOriginalCommonContentDTOProperties(prepareCodeSnippetDTO()), ContentSummaryDTO.class, prepareContentSummaryDTOFromCodeSnippetDTO()),
        Arguments.of(setOriginalCommonContentDTOProperties(new ContentDTO()), QuizSummaryDTO.class, prepareQuizSummaryDTOFromContentDTO()),
        Arguments.of(setOriginalCommonContentDTOProperties(prepareOriginalIsaacQuizDTO()), QuizSummaryDTO.class, prepareQuizSummaryDTOFromIsaacQuiz()),
        Arguments.of(setOriginalCommonContentDTOProperties(new ContentDTO()), IsaacWildcard.class, prepareIsaacWildcardFromContentDTO()),
        Arguments.of(setOriginalCommonContentDTOProperties(prepareIsaacWildcardDTO()), IsaacWildcard.class, prepareIsaacWildcardFromIsaacWildcardDTO()),
        Arguments.of(setOriginalCommonContentDTOProperties(new ContentDTO()), GameboardItem.class, prepareGameboardItemFromContentDTO()),
        Arguments.of(setOriginalCommonContentDTOProperties(new ContentDTO()), Content.class, setMappedCommonContentProperties(new Content()))
        );
  }

  private static Stream<Arguments> testCasesFromContent() {
    return Stream.of(
        Arguments.of(setOriginalCommonContentProperties(new Content()), IsaacWildcard.class, prepareIsaacWildcardFromContent()),
        Arguments.of(setOriginalCommonContentProperties(prepareIsaacWildcard()), IsaacWildcard.class, prepareIsaacWildcardFromIsaacWildcard())
    );
  }

  private static Stream<Arguments> testCasesCopyDTO() {
    return Stream.of(
        Arguments.of(setOriginalCommonContentDTOProperties(new ContentDTO())),
        Arguments.of(setOriginalCommonContentDTOProperties(prepareOriginalIsaacEventPageDTO()))
    );
  }

  // Common properties for Content objects, including those inherited from the abstract ContentBase class
  private static <S extends Content> S setOriginalCommonContentProperties(S source) {
    // Set ContentBase properties
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    Set<String> tags = new LinkedHashSet<>();
    Collections.addAll(tags, "tag1", "tag2");

    source.setId("id");
    source.setType("type");
    source.setTags(tags);
    source.setCanonicalSourceFile("sourceFile");
    source.setVersion("version");
    source.setAudience(List.of(audience));
    source.setDisplay(Map.of("displayKey", List.of("value1", "value2")));

    // Set Content properties
    Content childContent1 = new Content();
    childContent1.setId("child1");
    childContent1.setPublished(true);
    Content childContent2 = new Content();
    childContent2.setId("child2");
    childContent2.setPublished(true);

    source.setTitle("title");
    source.setSubtitle("subtitle");
    source.setAuthor("author");
    source.setEncoding("encoding");
    source.setLayout("layout");
    source.setChildren(List.of(childContent1, childContent2));
    source.setValue("value");
    source.setAttribution("attribution");
    source.setRelatedContent(List.of("relatedId1", "relatedId2"));
    source.setPublished(true);
    source.setDeprecated(false);
    source.setLevel(2);
    source.setSearchableContent("searchable");
    source.setExpandable(false);

    return source;
  }

  private static <S extends Content> S setMappedCommonContentProperties(S source) {
    // Set ContentBase properties
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    Set<String> tags = new LinkedHashSet<>();
    Collections.addAll(tags, "tag1", "tag2");

    source.setId("id");
    source.setType("type");
    source.setTags(tags);
    source.setCanonicalSourceFile("sourceFile");
    source.setVersion("version");
    source.setAudience(List.of(audience));
    source.setDisplay(Map.of("displayKey", List.of("value1", "value2")));

    // Set Content properties
    Content childContent1 = new Content();
    childContent1.setId("child1");
    childContent1.setPublished(true);
    childContent1.setTags(Set.of());
    Content childContent2 = new Content();
    childContent2.setId("child2");
    childContent2.setPublished(true);
    childContent2.setTags(Set.of());

    source.setTitle("title");
    source.setSubtitle("subtitle");
    source.setAuthor("author");
    source.setEncoding("encoding");
    source.setLayout("layout");
    source.setChildren(List.of(childContent1, childContent2));
    source.setValue("value");
    source.setAttribution("attribution");
    source.setRelatedContent(List.of("relatedId1", "relatedId2"));
    source.setPublished(true);
    source.setDeprecated(false);
    source.setLevel(2);
    // The DTO does not have the searchableContent property
    source.setSearchableContent(null);
    source.setExpandable(false);

    return source;
  }

  private static <T extends ContentDTO> T setOriginalCommonContentDTOProperties(T source) {
    // Set ContentBaseDTO properties
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    Set<String> tags = new LinkedHashSet<>();
    Collections.addAll(tags, "tag1", "tag2");

    source.setId("id");
    source.setType("type");
    source.setTags(tags);
    source.setCanonicalSourceFile("sourceFile");
    source.setVersion("version");
    source.setAudience(List.of(audience));
    source.setDisplay(Map.of("displayKey", List.of("value1", "value2")));

    // Set ContentDTO properties
    ContentDTO childContent1 = new ContentDTO();
    childContent1.setId("child1");
    childContent1.setPublished(true);
    ContentDTO childContent2 = new ContentDTO();
    childContent2.setId("child2");
    childContent2.setPublished(true);

    ContentSummaryDTO relatedContent1 = new ContentSummaryDTO();
    relatedContent1.setId("relatedId1");
    ContentSummaryDTO relatedContent2 = new ContentSummaryDTO();
    relatedContent2.setId("relatedId2");

    source.setTitle("title");
    source.setSubtitle("subtitle");
    source.setAuthor("author");
    source.setEncoding("encoding");
    source.setLayout("layout");
    source.setChildren(List.of(childContent1, childContent2));
    source.setValue("value");
    source.setAttribution("attribution");
    source.setRelatedContent(List.of(relatedContent1, relatedContent2));
    source.setPublished(true);
    source.setDeprecated(false);
    source.setLevel(2);
    source.setExpandable(false);

    return source;
  }

  private static <T extends ContentDTO> T setMappedCommonContentDTOProperties(T source) {
    // Set ContentBaseDTO properties
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    Set<String> tags = new LinkedHashSet<>();
    Collections.addAll(tags, "tag1", "tag2");

    source.setId("id");
    source.setType("type");
    source.setTags(tags);
    source.setCanonicalSourceFile("sourceFile");
    source.setVersion("version");
    source.setAudience(List.of(audience));
    source.setDisplay(Map.of("displayKey", List.of("value1", "value2")));

    // Set ContentDTO properties
    ContentDTO childContent1 = new ContentDTO();
    childContent1.setId("child1");
    childContent1.setPublished(true);
    ContentDTO childContent2 = new ContentDTO();
    childContent2.setId("child2");
    childContent2.setPublished(true);

    ContentSummaryDTO relatedContent1 = new ContentSummaryDTO();
    relatedContent1.setId("relatedId1");
    ContentSummaryDTO relatedContent2 = new ContentSummaryDTO();
    relatedContent2.setId("relatedId2");

    source.setTitle("title");
    source.setSubtitle("subtitle");
    source.setAuthor("author");
    source.setEncoding("encoding");
    source.setLayout("layout");
    source.setChildren(List.of(childContent1, childContent2));
    source.setValue("value");
    source.setAttribution("attribution");
    source.setRelatedContent(List.of(relatedContent1, relatedContent2));
    source.setPublished(true);
    source.setDeprecated(false);
    source.setLevel(2);
    source.setExpandable(false);

    return source;
  }

  private static AnvilApp prepareAnvilAppDO() {
    AnvilApp object = new AnvilApp();
    object.setAppId("appId");
    object.setAppAccessKey("accessKey");
    return object;
  }

  private static AnvilAppDTO prepareAnvilAppDTO() {
    AnvilAppDTO object = new AnvilAppDTO();
    object.setAppId("appId");
    object.setAppAccessKey("accessKey");
    return object;
  }

  // Choice
  private static Choice prepareOriginalChoiceDO() {
    return prepareOriginalChoiceDO(new Choice());
  }

  private static Choice prepareOriginalChoiceDO(Choice object) {
    ContentBase explanation = new Content();
    explanation.setId("explanationId");

    object.setCorrect(true);
    object.setExplanation(explanation);
    return object;
  }

  private static Choice prepareMappedChoiceDO() {
    // DTO does not have explanation or correct, so these fields will be default
    return new Choice();
  }

  private static ChoiceDTO prepareChoiceDTO() {
    return prepareChoiceDTO(new ChoiceDTO());
  }

  private static ChoiceDTO prepareChoiceDTO(ChoiceDTO object) {
    return object;
  }

  // Choice Question
  private static <T extends ChoiceQuestion> T prepareOriginalChoiceQuestionDO(T object) {
    Choice choice1 = new Choice();
    choice1.setId("choice1");
    Choice choice2 = new Choice();
    choice2.setId("choice2");

    T objectWithQuestionFields = prepareOriginalQuestionDO(object);
    objectWithQuestionFields.setChoices(List.of(choice1, choice2));
    objectWithQuestionFields.setRandomiseChoices(true);
    return objectWithQuestionFields;
  }

  private static <T extends ChoiceQuestion> T prepareMappedChoiceQuestionDO(T object) {
    Choice choice1 = new Choice();
    choice1.setId("choice1");
    choice1.setTags(Set.of());
    Choice choice2 = new Choice();
    choice2.setId("choice2");
    choice2.setTags(Set.of());

    T objectWithQuestionFields = prepareMappedQuestionDO(object);
    objectWithQuestionFields.setChoices(List.of(choice1, choice2));
    objectWithQuestionFields.setRandomiseChoices(true);
    return objectWithQuestionFields;
  }

  private static <T extends ChoiceQuestionDTO> T prepareOriginalChoiceQuestionDTO(T object) {
    ChoiceDTO choice1 = new ChoiceDTO();
    choice1.setId("choice1");
    ChoiceDTO choice2 = new ChoiceDTO();
    choice2.setId("choice2");

    T objectWithQuestionFields = prepareOriginalQuestionDTO(object);
    objectWithQuestionFields.setChoices(List.of(choice1, choice2));
    objectWithQuestionFields.setRandomiseChoices(true);
    return objectWithQuestionFields;
  }

  private static <T extends ChoiceQuestionDTO> T prepareMappedChoiceQuestionDTO(T object) {
    ChoiceDTO choice1 = new ChoiceDTO();
    choice1.setId("choice1");
    ChoiceDTO choice2 = new ChoiceDTO();
    choice2.setId("choice2");

    T objectWithQuestionFields = prepareMappedQuestionDTO(object);
    objectWithQuestionFields.setChoices(List.of(choice1, choice2));
    objectWithQuestionFields.setRandomiseChoices(true);
    return objectWithQuestionFields;
  }

  // CodeSnippet
  private static CodeSnippetDTO prepareCodeSnippetDTO() {
    return new CodeSnippetDTO("language", "code", false, "url");
  }

  // EmailTemplate
  private static EmailTemplate prepareEmailTemplateDO() {
    EmailTemplate object = new EmailTemplate();
    object.setSubject("subject");
    object.setPlainTextContent("textContent");
    object.setHtmlContent("htmlContent");
    object.setOverrideFromAddress("fromAddress");
    object.setOverrideFromName("fromName");
    object.setOverrideEnvelopeFrom("envelopeFrom");
    object.setReplyToEmailAddress("replyEmail");
    object.setReplyToName("replyName");
    return object;
  }

  private static EmailTemplateDTO prepareEmailTemplateDTO() {
    EmailTemplateDTO object = new EmailTemplateDTO();
    object.setSubject("subject");
    object.setPlainTextContent("textContent");
    object.setHtmlContent("htmlContent");
    object.setOverrideFromAddress("fromAddress");
    object.setOverrideFromName("fromName");
    object.setOverrideEnvelopeFrom("envelopeFrom");
    object.setReplyToEmailAddress("replyEmail");
    object.setReplyToName("replyName");
    return object;
  }

  // Image
  private static Image prepareImageDO() {
    Image object = new Image();
    object.setSrc("imageSource");
    object.setAltText("altText");
    object.setClickUrl("clickUrl");
    object.setClickTarget("clickTarget");
    return object;
  }

  private static ImageDTO prepareImageDTO() {
    ImageDTO object = new ImageDTO();
    object.setSrc("imageSource");
    object.setAltText("altText");
    object.setClickUrl("clickUrl");
    object.setClickTarget("clickTarget");
    return object;
  }

  // IsaacEventPage
  private static IsaacEventPage prepareIsaacEventPageDO() {
    Address address = new Address();
    Location location = new Location(address, 3.0, 7.0);
    ExternalReference preResource1 = new ExternalReference();
    preResource1.setTitle("title1");
    preResource1.setUrl("url1");
    ExternalReference preResource2 = new ExternalReference();
    preResource2.setTitle("title2");
    preResource2.setUrl("url2");
    Content preResourceContent1 = new Content();
    preResourceContent1.setId("preResourceContent1");
    preResourceContent1.setTags(Set.of());
    Content preResourceContent2 = new Content();
    preResourceContent2.setId("preResourceContent2");
    preResourceContent2.setTags(Set.of());
    ExternalReference postResource1 = new ExternalReference();
    postResource1.setTitle("title1");
    postResource1.setUrl("url1");
    ExternalReference postResource2 = new ExternalReference();
    postResource2.setTitle("title2");
    postResource2.setUrl("url2");
    Content postResourceContent1 = new Content();
    postResourceContent1.setId("postResourceContent1");
    postResourceContent1.setTags(Set.of());
    Content postResourceContent2 = new Content();
    postResourceContent2.setId("postResourceContent2");
    postResourceContent2.setTags(Set.of());
    Image eventThumbnail = new Image();
    eventThumbnail.setSrc("thumbnailSource");
    eventThumbnail.setPublished(true);
    eventThumbnail.setTags(Set.of());

    IsaacEventPage object = new IsaacEventPage();
    object.setDate(testDate);
    object.setEndDate(testDate);
    object.setBookingDeadline(testDate);
    object.setPrepWorkDeadline(testDate);
    object.setLocation(location);
    object.setPreResources(List.of(preResource1, preResource2));
    object.setPreResourceContent(List.of(preResourceContent1, preResourceContent2));
    object.setEmailEventDetails("emailEventDetails");
    object.setEmailConfirmedBookingText("confirmedBookingText");
    object.setEmailWaitingListBookingText("waitingListText");
    object.setPostResources(List.of(postResource1, postResource2));
    object.setPostResourceContent(List.of(postResourceContent1, postResourceContent2));
    object.setEventThumbnail(eventThumbnail);
    object.setNumberOfPlaces(100);
    object.setEventStatus(EventStatus.OPEN);
    object.setIsaacGroupToken("groupToken");
    object.setGroupReservationLimit(20);
    object.setAllowGroupReservations(true);
    object.setPrivateEvent(true);
    return object;
  }

  private static IsaacEventPageDTO prepareOriginalIsaacEventPageDTO() {
    IsaacEventPageDTO object = prepareIsaacEventPageDTO(new IsaacEventPageDTO());
    object.setUserBookingStatus(BookingStatus.CONFIRMED);
    return object;
  }

  private static IsaacEventPageDTO prepareMappedIsaacEventPageDTO() {
    IsaacEventPageDTO object = prepareIsaacEventPageDTO(new IsaacEventPageDTO());
    object.setUserBookingStatus(null);
    return object;
  }

  private static IsaacEventPageDTO prepareIsaacEventPageDTO(IsaacEventPageDTO object) {
    Address address = new Address();
    Location location = new Location(address, 3.0, 7.0);
    ExternalReference preResource1 = new ExternalReference();
    preResource1.setTitle("title1");
    preResource1.setUrl("url1");
    ExternalReference preResource2 = new ExternalReference();
    preResource2.setTitle("title2");
    preResource2.setUrl("url2");
    ContentDTO preResourceContent1 = new ContentDTO();
    preResourceContent1.setId("preResourceContent1");
    ContentDTO preResourceContent2 = new ContentDTO();
    preResourceContent2.setId("preResourceContent2");
    ExternalReference postResource1 = new ExternalReference();
    postResource1.setTitle("title1");
    postResource1.setUrl("url1");
    ExternalReference postResource2 = new ExternalReference();
    postResource2.setTitle("title2");
    postResource2.setUrl("url2");
    ContentDTO postResourceContent1 = new ContentDTO();
    postResourceContent1.setId("postResourceContent1");
    ContentDTO postResourceContent2 = new ContentDTO();
    postResourceContent2.setId("postResourceContent2");
    ImageDTO eventThumbnail = new ImageDTO();
    eventThumbnail.setSrc("thumbnailSource");
    eventThumbnail.setPublished(true);

    object.setDate(testDate);
    object.setEndDate(testDate);
    object.setBookingDeadline(testDate);
    object.setPrepWorkDeadline(testDate);
    object.setLocation(location);
    object.setPreResources(List.of(preResource1, preResource2));
    object.setPreResourceContent(List.of(preResourceContent1, preResourceContent2));
    object.setEmailEventDetails("emailEventDetails");
    object.setEmailConfirmedBookingText("confirmedBookingText");
    object.setEmailWaitingListBookingText("waitingListText");
    object.setPostResources(List.of(postResource1, postResource2));
    object.setPostResourceContent(List.of(postResourceContent1, postResourceContent2));
    object.setEventThumbnail(eventThumbnail);
    object.setNumberOfPlaces(100);
    object.setEventStatus(EventStatus.OPEN);
    object.setIsaacGroupToken("groupToken");
    object.setGroupReservationLimit(20);
    object.setAllowGroupReservations(true);
    object.setPrivateEvent(true);
    return object;
  }

  // IsaacItemQuestion
  private static IsaacItemQuestion prepareOriginalIsaacItemQuestionDO() {
    Item item1 = new Item();
    item1.setId("item1");
    item1.setTags(Set.of());
    Item item2 = new Item();
    item2.setId("item1");
    item2.setTags(Set.of());

    IsaacItemQuestion object = prepareOriginalChoiceQuestionDO(new IsaacItemQuestion());
    object.setItems(List.of(item1, item2));
    object.setRandomiseItems(true);
    return object;
  }

  private static IsaacItemQuestion prepareMappedIsaacItemQuestionDO() {
    Item item1 = new Item();
    item1.setId("item1");
    item1.setTags(Set.of());
    Item item2 = new Item();
    item2.setId("item1");
    item2.setTags(Set.of());

    IsaacItemQuestion object = prepareMappedChoiceQuestionDO(new IsaacItemQuestion());
    object.setItems(List.of(item1, item2));
    object.setRandomiseItems(true);
    return object;
  }

  private static IsaacItemQuestionDTO prepareOriginalIsaacItemQuestionDTO() {
    ItemDTO item1 = new ItemDTO();
    item1.setId("item1");
    ItemDTO item2 = new ItemDTO();
    item2.setId("item1");

    IsaacItemQuestionDTO object = prepareMappedChoiceQuestionDTO(new IsaacItemQuestionDTO());
    object.setItems(List.of(item1, item2));
    object.setRandomiseItems(true);
    return object;
  }

  private static IsaacItemQuestionDTO prepareMappedIsaacItemQuestionDTO() {
    ItemDTO item1 = new ItemDTO();
    item1.setId("item1");
    ItemDTO item2 = new ItemDTO();
    item2.setId("item1");

    IsaacItemQuestionDTO object = prepareMappedChoiceQuestionDTO(new IsaacItemQuestionDTO());
    object.setItems(List.of(item1, item2));
    object.setRandomiseItems(true);
    return object;
  }

  // IsaacQuestionPage
  private static IsaacQuestionPage prepareIsaacQuestionPageDO() {
    IsaacQuestionPage object = prepareSeguePageDO(new IsaacQuestionPage());
    object.setPassMark(50F);
    object.setSupersededBy("newVersion");
    object.setDifficulty(Difficulty.challenge_1);
    return object;
  }

  private static IsaacQuestionPageDTO prepareIsaacQuestionPageDTO() {
    IsaacQuestionPageDTO object = prepareSeguePageDTO(new IsaacQuestionPageDTO());
    object.setPassMark(50F);
    object.setSupersededBy("newVersion");
    object.setDifficulty(Difficulty.challenge_1);
    return object;
  }

  // IsaacQuiz
  private static IsaacQuiz prepareIsaacQuizDO() {
    Content rubric = new Content();
    rubric.setId("rubricId");
    rubric.setPublished(true);
    rubric.setTags(Set.of());

    IsaacQuiz object = prepareSeguePageDO(new IsaacQuiz());
    object.setHiddenFromRoles(List.of("blockedRole1", "blockedRole2"));
    object.setRubric(rubric);
    return object;
  }

  private static IsaacQuizDTO prepareOriginalIsaacQuizDTO() {
    ContentDTO rubric = new ContentDTO();
    rubric.setId("rubricId");
    rubric.setPublished(true);
    QuizFeedbackDTO individualFeedback = new QuizFeedbackDTO(new QuizFeedbackDTO.Mark(),
        Map.of("sectionA", new QuizFeedbackDTO.Mark(), "sectionB", new QuizFeedbackDTO.Mark()),
        Map.of("question1", new QuizFeedbackDTO.Mark(), "question2", new QuizFeedbackDTO.Mark()));

    IsaacQuizDTO object = prepareSeguePageDTO(new IsaacQuizDTO());
    object.setHiddenFromRoles(List.of("blockedRole1", "blockedRole2"));
    object.setDefaultFeedbackMode(QuizFeedbackMode.OVERALL_MARK);
    object.setRubric(rubric);
    object.setTotal(75);
    object.setSectionTotals(Map.of("section1", 40, "section2", 35));
    object.setIndividualFeedback(individualFeedback);
    return object;
  }

  private static IsaacQuizDTO prepareMappedIsaacQuizDTO() {
    ContentDTO rubric = new ContentDTO();
    rubric.setId("rubricId");
    rubric.setPublished(true);

    IsaacQuizDTO object = prepareSeguePageDTO(new IsaacQuizDTO());
    object.setHiddenFromRoles(List.of("blockedRole1", "blockedRole2"));
    object.setRubric(rubric);
    return object;
  }

  // IsaacSymbolicQuestion
  private static IsaacSymbolicQuestion prepareOriginalIsaacSymbolicQuestionDO() {
    IsaacSymbolicQuestion object = prepareOriginalChoiceQuestionDO(new IsaacSymbolicQuestion());
    object.setFormulaSeed("formulaSeed");
    object.setAvailableSymbols(List.of("symbol1", "symbol2"));
    return object;
  }

  private static IsaacSymbolicQuestion prepareMappedIsaacSymbolicQuestionDO() {
    IsaacSymbolicQuestion object = prepareMappedChoiceQuestionDO(new IsaacSymbolicQuestion());
    object.setFormulaSeed("formulaSeed");
    object.setAvailableSymbols(List.of("symbol1", "symbol2"));
    return object;
  }

  private static IsaacSymbolicQuestionDTO prepareOriginalIsaacSymbolicQuestionDTO() {
    IsaacSymbolicQuestionDTO object = prepareOriginalChoiceQuestionDTO(new IsaacSymbolicQuestionDTO());
    object.setFormulaSeed("formulaSeed");
    object.setAvailableSymbols(List.of("symbol1", "symbol2"));
    return object;
  }

  private static IsaacSymbolicQuestionDTO prepareMappedIsaacSymbolicQuestionDTO() {
    IsaacSymbolicQuestionDTO object = prepareMappedChoiceQuestionDTO(new IsaacSymbolicQuestionDTO());
    object.setFormulaSeed("formulaSeed");
    object.setAvailableSymbols(List.of("symbol1", "symbol2"));
    return object;
  }

  // Question
  private static Question prepareOriginalQuestionDO() {
    return prepareOriginalQuestionDO(new Question());
  }

  private static <T extends Question> T prepareOriginalQuestionDO(T object) {
    ContentBase answer = new Content();
    answer.setId("answerId");
    Content hint1 = new Content();
    hint1.setId("hintId1");
    hint1.setPublished(true);
    Content hint2 = new Content();
    hint2.setId("hintId2");
    hint2.setPublished(true);
    Content feedback = new Content();
    feedback.setId("feedbackId");

    object.setAnswer(answer);
    object.setHints(List.of(hint1, hint2));
    object.setDefaultFeedback(feedback);
    return object;
  }

  private static Question prepareMappedQuestionDO() {
    return prepareMappedQuestionDO(new Question());
  }

  private static <T extends Question> T prepareMappedQuestionDO(T object) {
    ContentBase answer = new Content();
    answer.setId("answerId");
    answer.setTags(Set.of());
    Content hint1 = new Content();
    hint1.setId("hintId1");
    hint1.setPublished(true);
    hint1.setTags(Set.of());
    Content hint2 = new Content();
    hint2.setId("hintId2");
    hint2.setPublished(true);
    hint2.setTags(Set.of());

    object.setAnswer(answer);
    object.setHints(List.of(hint1, hint2));
    // The DTO does not have the defaultFeedback property
    object.setDefaultFeedback(null);
    return object;
  }

  private static QuestionDTO prepareOriginalQuestionDTO() {
    return prepareOriginalQuestionDTO(new QuestionDTO());
  }

  private static <T extends QuestionDTO> T prepareOriginalQuestionDTO(T object) {
    ContentBaseDTO answer = new ContentDTO();
    answer.setId("answerId");
    ContentDTO hint1 = new ContentDTO();
    hint1.setId("hintId1");
    hint1.setPublished(true);
    ContentDTO hint2 = new ContentDTO();
    hint2.setId("hintId2");
    hint2.setPublished(true);
    QuestionValidationResponseDTO bestAttempt = new QuestionValidationResponseDTO();

    object.setAnswer(answer);
    object.setHints(List.of(hint1, hint2));
    object.setBestAttempt(bestAttempt);
    return object;
  }

  private static QuestionDTO prepareMappedQuestionDTO() {
    return prepareMappedQuestionDTO(new QuestionDTO());
  }

  private static <T extends QuestionDTO> T prepareMappedQuestionDTO(T object) {
    ContentBaseDTO answer = new ContentDTO();
    answer.setId("answerId");
    ContentDTO hint1 = new ContentDTO();
    hint1.setId("hintId1");
    hint1.setPublished(true);
    ContentDTO hint2 = new ContentDTO();
    hint2.setId("hintId2");
    hint2.setPublished(true);

    object.setAnswer(answer);
    object.setHints(List.of(hint1, hint2));
    // The DO does not have the bestAttempt property
    object.setBestAttempt(null);
    return object;
  }

  // SeguePage
  private static SeguePage prepareSeguePageDO() {
    return prepareSeguePageDO(new SeguePage());
  }

  private static <T extends SeguePage> T prepareSeguePageDO(T object) {
    object.setSummary("summary");
    return object;
  }

  private static SeguePageDTO prepareSeguePageDTO() {
    return prepareSeguePageDTO(new SeguePageDTO());
  }

  private static <T extends SeguePageDTO> T prepareSeguePageDTO(T object) {
    object.setSummary("summary");
    return object;
  }

  // ContentSummaryDTO
  private static ContentSummaryDTO prepareContentSummaryDTOFromContentDTO() {
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    ContentSummaryDTO object = new ContentSummaryDTO();
    object.setId("id");
    object.setTitle("title");
    object.setSummary(null);
    object.setType("type");
    object.setLevel(2);
    object.setTags(List.of("tag1", "tag2"));
    object.setUrl(null);
    // Note: there do not appear to be any ContentDTO subclasses that currently make use of the correct field
    // While Choice does have such a field, ChoiceDTO does not
    object.setCorrect(null);
    object.setQuestionPartIds(List.of());
    object.setSupersededBy(null);
    object.setDeprecated(false);
    object.setDifficulty(null);
    object.setAudience(List.of(audience));
    return object;
  }

  private static ContentSummaryDTO prepareContentSummaryDTOFromIsaacQuestionPageDTO() {
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    ContentSummaryDTO object = new ContentSummaryDTO();
    object.setId("id");
    object.setTitle("title");
    object.setSummary("summary");
    object.setType("type");
    object.setLevel(2);
    object.setTags(List.of("tag1", "tag2"));
    object.setUrl(null);
    object.setCorrect(null);
    object.setQuestionPartIds(List.of());
    object.setSupersededBy("newVersion");
    object.setDeprecated(false);
    object.setDifficulty(Difficulty.challenge_1);
    object.setAudience(List.of(audience));
    return object;
  }

  private static ContentSummaryDTO prepareContentSummaryDTOFromCodeSnippetDTO() {
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    ContentSummaryDTO object = new ContentSummaryDTO();
    object.setId("id");
    object.setTitle("title");
    object.setSummary(null);
    object.setType("type");
    object.setLevel(2);
    object.setTags(List.of("tag1", "tag2"));
    object.setUrl("url");
    object.setCorrect(null);
    object.setQuestionPartIds(List.of());
    object.setSupersededBy(null);
    object.setDeprecated(false);
    object.setDifficulty(null);
    object.setAudience(List.of(audience));
    return object;
  }

  private static QuizSummaryDTO prepareQuizSummaryDTOFromContentDTO() {
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    QuizSummaryDTO object = new QuizSummaryDTO();
    object.setId("id");
    object.setTitle("title");
    object.setSummary(null);
    object.setType("type");
    object.setLevel(2);
    object.setTags(List.of("tag1", "tag2"));
    object.setUrl(null);
    object.setCorrect(null);
    object.setQuestionPartIds(List.of());
    object.setSupersededBy(null);
    object.setDeprecated(false);
    object.setDifficulty(null);
    object.setAudience(List.of(audience));
    return object;
  }

  private static QuizSummaryDTO prepareQuizSummaryDTOFromIsaacQuiz() {
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    QuizSummaryDTO object = new QuizSummaryDTO();
    object.setId("id");
    object.setTitle("title");
    object.setSummary("summary");
    object.setType("type");
    object.setLevel(2);
    object.setTags(List.of("tag1", "tag2"));
    object.setUrl(null);
    object.setCorrect(null);
    object.setQuestionPartIds(List.of());
    object.setSupersededBy(null);
    object.setDeprecated(false);
    object.setDifficulty(null);
    object.setAudience(List.of(audience));

    object.setHiddenFromRoles(List.of("blockedRole1", "blockedRole2"));
    return object;
  }

  // Wildcard
  private static IsaacWildcard prepareIsaacWildcard() {
    IsaacWildcard object = new IsaacWildcard();
    object.setDescription("description");
    object.setUrl("url");
    return object;
  }

  private static IsaacWildcardDTO prepareIsaacWildcardDTO() {
    IsaacWildcardDTO object = new IsaacWildcardDTO();
    object.setDescription("description");
    object.setUrl("url");
    return object;
  }

  private static IsaacWildcard prepareIsaacWildcardFromContent() {
    return setOriginalCommonContentProperties(new IsaacWildcard());
  }

  private static IsaacWildcard prepareIsaacWildcardFromContentDTO() {
    return setMappedCommonContentProperties(new IsaacWildcard());
  }

  private static IsaacWildcard prepareIsaacWildcardFromIsaacWildcard() {
    IsaacWildcard object = setOriginalCommonContentProperties(new IsaacWildcard());
    object.setDescription("description");
    object.setUrl("url");
    return object;
  }

  private static IsaacWildcard prepareIsaacWildcardFromIsaacWildcardDTO() {
    IsaacWildcard object = setMappedCommonContentProperties(new IsaacWildcard());
    object.setDescription("description");
    object.setUrl("url");
    return object;
  }

  // GameboardItem
  private static GameboardItem prepareGameboardItemFromContentDTO() {
    AudienceContext audience = new AudienceContext();
    audience.setStage(List.of(Stage.a_level));
    audience.setExamBoard(List.of(ExamBoard.aqa));
    audience.setDifficulty(List.of(Difficulty.challenge_2));
    audience.setRole(List.of(RoleRequirement.logged_in));

    GameboardItem object  = new GameboardItem();
    object.setId("id");
    object.setContentType(null);
    object.setTitle("title");
    object.setDescription(null);
    object.setUri(null);
    object.setTags(List.of("tag1", "tag2"));
    object.setAudience(List.of(audience));
    object.setCreationContext(null);
    object.setLevel(2);
    object.setDifficulty(null);
    object.setQuestionPartsCorrect(null);
    object.setQuestionPartsIncorrect(null);
    object.setQuestionPartsNotAttempted(null);
    object.setQuestionPartsTotal(null);
    object.setPassMark(null);
    object.setState(null);
    object.setQuestionPartStates(List.of());
    return object;
  }
}