package uk.ac.cam.cl.dtg.isaac.mappers;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static uk.ac.cam.cl.dtg.CustomAssertions.assertDeepEquals;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.AssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;
import uk.ac.cam.cl.dtg.isaac.dos.Difficulty;
import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardContentDescriptor;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardCreationMethod;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dos.QuizAssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dos.QuizAttemptDO;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dos.RoleRequirement;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizFeedbackDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizUserFeedbackDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

class MiscMapperTest {

  MiscMapper miscMapper = MiscMapper.INSTANCE;
  private static final Date testDate = new Date();
  private static final Date newTestDate = Date.from(now().plus(10000L, ChronoUnit.SECONDS));

  @BeforeEach
  void beforeEach() {
    miscMapper = MiscMapper.INSTANCE;
  }

  @Test
  void mapGameboardDTO() {
    GameboardDO source = prepareGameboardDO();
    GameboardDTO expected = prepareMappedGameboardDTO();
    GameboardDTO actual = miscMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mapGameboardDO() {
    GameboardDTO source = prepareOriginalGameboardDTO();
    GameboardDO expected = prepareGameboardDO();
    GameboardDO actual = miscMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mapAssignmentDTO() {
    AssignmentDO source = prepareAssignmentDO();
    AssignmentDTO expected = prepareMappedAssignmentDTO();
    AssignmentDTO actual = miscMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mapAssignmentDO() {
    AssignmentDTO source = prepareOriginalAssignmentDTO();
    AssignmentDO expected = prepareAssignmentDO();
    AssignmentDO actual = miscMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mapQuizAssignmentDTO() {
    QuizAssignmentDO source = prepareQuizAssignmentDO();
    QuizAssignmentDTO expected = prepareMappedQuizAssignmentDTO();
    QuizAssignmentDTO actual = miscMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mapQuizAssignmentDO() {
    QuizAssignmentDTO source = prepareOriginalQuizAssignmentDTO();
    QuizAssignmentDO expected = prepareQuizAssignmentDO();
    QuizAssignmentDO actual = miscMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mapQuizAttemptDTO() {
    QuizAttemptDO source = prepareQuizAttemptDO();
    QuizAttemptDTO expected = prepareMappedQuizAttemptDTO();
    QuizAttemptDTO actual = miscMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void mapQuizAttemptDO() {
    QuizAttemptDTO source = prepareOriginalQuizAttemptDTO();
    QuizAttemptDO expected = prepareQuizAttemptDO();
    QuizAttemptDO actual = miscMapper.map(source);
    assertEquals(expected.getClass(), actual.getClass());
    assertDeepEquals(expected, actual);
  }

  @Test
  void copyResultsWrapperOfString() {
    ResultsWrapper<String> source = new ResultsWrapper<>(List.of("result1", "result2", "result3"), 3L);
    ResultsWrapper<String> actual = miscMapper.copy(source);
    assertEquals(actual.getClass(), source.getClass());
    assertNotSame(actual, source);
    assertNotSame(actual.getResults(), source.getResults());
    assertDeepEquals(actual, source);
  }

  // Gameboard
  private static GameboardDO prepareGameboardDO() {
    AudienceContext audience1 = new AudienceContext();
    audience1.setStage(List.of(Stage.a_level));
    audience1.setExamBoard(List.of(ExamBoard.aqa));
    audience1.setDifficulty(List.of(Difficulty.challenge_1, Difficulty.challenge_2));
    audience1.setRole(List.of(RoleRequirement.logged_in));
    GameboardContentDescriptor descriptor1 = new GameboardContentDescriptor();
    descriptor1.setId("descriptor1Id");
    descriptor1.setContentType("descriptor1ContentType");
    descriptor1.setContext(audience1);

    AudienceContext audience2 = new AudienceContext();
    audience2.setStage(List.of(Stage.gcse));
    audience2.setExamBoard(List.of(ExamBoard.edexcel));
    audience2.setDifficulty(List.of(Difficulty.practice_1, Difficulty.practice_2));
    audience2.setRole(List.of(RoleRequirement.logged_in));
    GameboardContentDescriptor descriptor2 = new GameboardContentDescriptor();
    descriptor2.setId("descriptor2Id");
    descriptor2.setContentType("descriptor2ContentType");
    descriptor2.setContext(audience2);

    IsaacWildcard wildcard = new IsaacWildcard();
    wildcard.setId("wildcardId");

    GameFilter gameFilter = prepareGameFilter();

    Set<String> tags = new LinkedHashSet<>();
    Collections.addAll(tags, "tag1", "tag2");
    
    GameboardDO object = new GameboardDO();
    object.setId("gameboardId");
    object.setTitle("title");
    object.setContents(List.of(descriptor1, descriptor2));
    object.setWildCard(wildcard);
    object.setWildCardPosition(7);
    object.setCreationDate(testDate);
    object.setGameFilter(gameFilter);
    object.setOwnerUserId(3L);
    object.setCreationMethod(GameboardCreationMethod.BUILDER);
    object.setTags(tags);
    return object;
  }

  private static GameboardDTO prepareOriginalGameboardDTO() {
    UserSummaryDTO owner = new UserSummaryDTO();
    owner.setId(3L);

    GameboardDTO object = prepareGameboardDTO(new GameboardDTO());
    object.setOwnerUserInformation(owner);
    object.setSavedToCurrentUser(true);
    object.setPercentageCompleted(42);
    object.setLastVisited(testDate);
    object.setStartedQuestion(true);
    return object;
  }

  private static GameboardDTO prepareMappedGameboardDTO() {
    GameboardDTO object = prepareGameboardDTO(new GameboardDTO());
    object.setOwnerUserInformation(null);
    object.setSavedToCurrentUser(null);
    object.setPercentageCompleted(null);
    object.setLastVisited(null);
    object.setStartedQuestion(false);
    return object;
  }

  private static GameboardDTO prepareGameboardDTO(GameboardDTO object) {
    AudienceContext audience1 = new AudienceContext();
    audience1.setStage(List.of(Stage.a_level));
    audience1.setExamBoard(List.of(ExamBoard.aqa));
    audience1.setDifficulty(List.of(Difficulty.challenge_1, Difficulty.challenge_2));
    audience1.setRole(List.of(RoleRequirement.logged_in));
    GameboardItem item1 = new GameboardItem();
    item1.setId("descriptor1Id");
    item1.setContentType("descriptor1ContentType");
    item1.setCreationContext(audience1);

    AudienceContext audience2 = new AudienceContext();
    audience2.setStage(List.of(Stage.gcse));
    audience2.setExamBoard(List.of(ExamBoard.edexcel));
    audience2.setDifficulty(List.of(Difficulty.practice_1, Difficulty.practice_2));
    audience2.setRole(List.of(RoleRequirement.logged_in));
    GameboardItem item2 = new GameboardItem();
    item2.setId("descriptor2Id");
    item2.setContentType("descriptor2ContentType");
    item2.setCreationContext(audience2);

    IsaacWildcard wildcard = new IsaacWildcard();
    wildcard.setId("wildcardId");

    GameFilter gameFilter = prepareGameFilter();

    Set<String> tags = new LinkedHashSet<>();
    Collections.addAll(tags, "tag1", "tag2");

    object.setId("gameboardId");
    object.setTitle("title");
    object.setContents(List.of(item1, item2));
    object.setWildCard(wildcard);
    object.setWildCardPosition(7);
    object.setCreationDate(testDate);
    object.setGameFilter(gameFilter);
    object.setOwnerUserId(3L);
    object.setTags(tags);
    object.setCreationMethod(GameboardCreationMethod.BUILDER);
    return object;
  }

  private static GameFilter prepareGameFilter() {
    GameFilter gameFilter = new GameFilter();
    gameFilter.setSubjects(List.of("subject1", "subject2"));
    gameFilter.setFields(List.of("field1", "field2"));
    gameFilter.setTopics(List.of("topic1", "topic2"));
    gameFilter.setLevels(List.of(1, 2));
    gameFilter.setStages(List.of("stage1", "stage2"));
    gameFilter.setDifficulties(List.of("difficulty1", "difficulty2"));
    gameFilter.setExamBoards(List.of("examBoard1", "examBoard2"));
    gameFilter.setConcepts(List.of("concept1", "concept2"));
    gameFilter.setQuestionCategories(List.of("category1", "category2"));
    return gameFilter;
  }

  // Assignment
  private static AssignmentDO prepareAssignmentDO() {
    AssignmentDO object = new AssignmentDO();
    object.setId(6L);
    object.setGameboardId("gameboardId");
    object.setGroupId(3L);
    object.setOwnerUserId(7L);
    object.setNotes("notes");
    object.setCreationDate(testDate);
    object.setDueDate(newTestDate);
    object.setScheduledStartDate(testDate);
    return object;
  }

  private static AssignmentDTO prepareOriginalAssignmentDTO() {
    GameboardDTO gameboard = new GameboardDTO();
    gameboard.setId("gameboardId");

    UserSummaryDTO assigner = new UserSummaryDTO();
    assigner.setId(9L);

    AssignmentDTO object = prepareAssignmentDTO(new AssignmentDTO());
    object.setGameboard(gameboard);
    object.setGroupName("groupName");
    object.setAssignerSummary(assigner);
    return object;
  }

  private static AssignmentDTO prepareMappedAssignmentDTO() {
    AssignmentDTO object = prepareAssignmentDTO(new AssignmentDTO());
    object.setGameboard(null);
    object.setGroupName(null);
    object.setAssignerSummary(null);
    return object;
  }

  private static AssignmentDTO prepareAssignmentDTO(AssignmentDTO object) {
    object.setId(6L);
    object.setGameboardId("gameboardId");
    object.setGroupId(3L);
    object.setOwnerUserId(7L);
    object.setNotes("notes");
    object.setCreationDate(testDate);
    object.setDueDate(newTestDate);
    object.setScheduledStartDate(testDate);
    return object;
  }

  // QuizAssignment
  private static QuizAssignmentDO prepareQuizAssignmentDO() {
    QuizAssignmentDO object = new QuizAssignmentDO();
    object.setId(6L);
    object.setQuizId("quizId");
    object.setGroupId(3L);
    object.setOwnerUserId(7L);
    object.setCreationDate(testDate);
    object.setDueDate(newTestDate);
    object.setQuizFeedbackMode(QuizFeedbackMode.OVERALL_MARK);
    return object;
  }

  private static QuizAssignmentDTO prepareOriginalQuizAssignmentDTO() {
    ContentSummaryDTO summary = new ContentSummaryDTO();
    summary.setId("quizSummaryId");
    summary.setTitle("title");

    UserSummaryDTO assigner = new UserSummaryDTO();
    assigner.setId(9L);

    QuizAttemptDTO attempt = new QuizAttemptDTO();
    attempt.setId(2L);

    UserSummaryDTO feedbackUser = new UserSummaryDTO();
    feedbackUser.setId(11L);
    QuizFeedbackDTO.Mark mark = new QuizFeedbackDTO.Mark();
    mark.setCorrect(4);
    mark.setIncorrect(2);
    mark.setNotAttempted(1);
    QuizFeedbackDTO quizFeedback = new QuizFeedbackDTO(mark, Map.of("section1", mark), Map.of("question1", mark));
    QuizUserFeedbackDTO userFeedback = new QuizUserFeedbackDTO(feedbackUser, quizFeedback);

    IsaacQuizDTO quiz = new IsaacQuizDTO();
    quiz.setId("quizId");

    QuizAssignmentDTO object = prepareQuizAssignmentDTO(new QuizAssignmentDTO());
    object.setQuizSummary(summary);
    object.setAssignerSummary(assigner);
    object.setAttempt(attempt);
    object.setUserFeedback(List.of(userFeedback));
    object.setQuiz(quiz);
    return object;
  }

  private static QuizAssignmentDTO prepareMappedQuizAssignmentDTO() {
    QuizAssignmentDTO object = prepareQuizAssignmentDTO(new QuizAssignmentDTO());
    object.setQuizSummary(null);
    object.setAssignerSummary(null);
    object.setAttempt(null);
    object.setUserFeedback(null);
    object.setQuiz(null);
    return object;
  }

  private static QuizAssignmentDTO prepareQuizAssignmentDTO(QuizAssignmentDTO object) {
    object.setId(6L);
    object.setQuizId("quizId");
    object.setGroupId(3L);
    object.setOwnerUserId(7L);
    object.setCreationDate(testDate);
    object.setDueDate(newTestDate);
    object.setQuizFeedbackMode(QuizFeedbackMode.OVERALL_MARK);
    return object;
  }

  // QuizAttempt
  private static QuizAttemptDO prepareQuizAttemptDO() {
    QuizAttemptDO object = new QuizAttemptDO();
    object.setId(2L);
    object.setUserId(7L);
    object.setQuizId("quizId");
    object.setQuizAssignmentId(4L);
    object.setStartDate(testDate);
    object.setCompletedDate(newTestDate);
    return object;
  }

  private static QuizAttemptDTO prepareOriginalQuizAttemptDTO() {
    ContentSummaryDTO summary = new ContentSummaryDTO();
    summary.setId("quizSummaryId");
    summary.setTitle("title");

    IsaacQuizDTO quiz = new IsaacQuizDTO();
    quiz.setId("quizId");

    QuizAttemptDTO object = prepareQuizAttemptDTO(new QuizAttemptDTO());
    object.setQuizSummary(summary);
    object.setQuiz(quiz);
    object.setQuizAssignment(prepareOriginalQuizAssignmentDTO());
    object.setFeedbackMode(QuizFeedbackMode.OVERALL_MARK);
    return object;
  }

  private static QuizAttemptDTO prepareMappedQuizAttemptDTO() {
    QuizAttemptDTO object = prepareQuizAttemptDTO(new QuizAttemptDTO());
    object.setQuizSummary(null);
    object.setQuiz(null);
    object.setQuizAssignment(null);
    object.setFeedbackMode(null);
    return object;
  }

  private static QuizAttemptDTO prepareQuizAttemptDTO(QuizAttemptDTO object) {
    object.setId(2L);
    object.setUserId(7L);
    object.setQuizId("quizId");
    object.setQuizAssignmentId(4L);
    object.setStartDate(testDate);
    object.setCompletedDate(newTestDate);
    return object;
  }
}