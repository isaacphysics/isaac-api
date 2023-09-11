package uk.ac.cam.cl.dtg.util;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;
import uk.ac.cam.cl.dtg.isaac.dos.Difficulty;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

public class UserQuestionInformation {
  private static final int PROGRESS_MAX_RECENT_QUESTIONS = 5;
  private int correctQuestions = 0;
  private int attemptedQuestions = 0;
  private int correctQuestionParts = 0;
  private int attemptedQuestionParts = 0;
  private int correctQuestionsThisAcademicYear = 0;
  private int attemptedQuestionsThisAcademicYear = 0;
  private int correctQuestionPartsThisAcademicYear = 0;
  private int attemptedQuestionPartsThisAcademicYear = 0;
  private final Map<String, Integer> questionAttemptsByTagStats = Maps.newHashMap();
  private final Map<String, Integer> questionsCorrectByTagStats = Maps.newHashMap();
  private final Map<Stage, Map<Difficulty, Integer>> questionAttemptsByStageAndDifficultyStats = Maps.newHashMap();
  private final Map<Stage, Map<Difficulty, Integer>> questionsCorrectByStageAndDifficultyStats = Maps.newHashMap();
  private final Map<String, Integer> questionAttemptsByLevelStats = Maps.newHashMap();
  private final Map<String, Integer> questionsCorrectByLevelStats = Maps.newHashMap();
  private final Map<String, Integer> questionAttemptsByTypeStats = Maps.newHashMap();
  private final Map<String, Integer> questionsCorrectByTypeStats = Maps.newHashMap();
  private final List<ContentDTO> questionPagesNotComplete = Lists.newArrayList();
  private final Queue<ContentDTO> mostRecentlyAttemptedQuestionPages =
      new CircularFifoQueue<>(PROGRESS_MAX_RECENT_QUESTIONS);

  public UserQuestionInformation() {
  }

  public void incrementCorrectQuestions() {
    correctQuestions++;
  }

  public void incrementAttemptedQuestions() {
    attemptedQuestions++;
  }

  public void incrementCorrectQuestionParts() {
    correctQuestionParts++;
  }

  public void incrementAttemptedQuestionParts() {
    attemptedQuestionParts++;
  }

  public void incrementCorrectQuestionsThisAcademicYear() {
    correctQuestionsThisAcademicYear++;
  }

  public void incrementAttemptedQuestionsThisAcademicYear() {
    attemptedQuestionsThisAcademicYear++;
  }

  public void incrementCorrectQuestionPartsThisAcademicYear() {
    correctQuestionPartsThisAcademicYear++;
  }

  public void incrementAttemptedQuestionPartsThisAcademicYear() {
    attemptedQuestionPartsThisAcademicYear++;
  }

  public void incrementQuestionsByTagStats(final String tag, final boolean questionIsCorrect) {
    // Count the attempt at the Question:
    incrementQuestionAttemptsByTagStats(tag);
    // If it's correct, count this too:
    if (questionIsCorrect) {
      incrementQuestionsCorrectByTagStats(tag);
    }
  }

  private void incrementQuestionAttemptsByTagStats(final String tag) {
    if (questionAttemptsByTagStats.containsKey(tag)) {
      questionAttemptsByTagStats.put(tag, questionAttemptsByTagStats.get(tag) + 1);
    } else {
      questionAttemptsByTagStats.put(tag, 1);
    }
  }

  private void incrementQuestionsCorrectByTagStats(final String tag) {
    if (questionsCorrectByTagStats.containsKey(tag)) {
      questionsCorrectByTagStats.put(tag, questionsCorrectByTagStats.get(tag) + 1);
    } else {
      questionsCorrectByTagStats.put(tag, 1);
    }
  }

  public void incrementQuestionsByStageAndDifficulty(final AudienceContext audience, final boolean questionIsCorrect) {
    // Check the question has both a stage and a difficulty
    if (audience.getStage() != null && audience.getDifficulty() != null) {
      Stage currentStage = audience.getStage().get(0);
      Difficulty currentDifficulty = audience.getDifficulty().get(0);
      // Count the attempt at the question
      incrementQuestionAttemptsByStageAndDifficulty(currentStage, currentDifficulty);

      // If correct, count this too:
      if (questionIsCorrect) {
        incrementQuestionsCorrectByStageAndDifficulty(currentStage, currentDifficulty);
      }
    }
  }

  private void incrementQuestionAttemptsByStageAndDifficulty(final Stage currentStage,
                                                             final Difficulty currentDifficulty) {
    if (questionAttemptsByStageAndDifficultyStats.containsKey(currentStage)) {
      if (questionAttemptsByStageAndDifficultyStats.get(currentStage).containsKey(currentDifficulty)) {
        questionAttemptsByStageAndDifficultyStats.get(currentStage)
            .put(currentDifficulty,
                questionAttemptsByStageAndDifficultyStats.get(currentStage).get(currentDifficulty) + 1);
      } else {
        questionAttemptsByStageAndDifficultyStats.get(currentStage).put(currentDifficulty, 1);
      }
    } else {
      Map<Difficulty, Integer> newDifficultyMap = Maps.newHashMap();
      newDifficultyMap.put(currentDifficulty, 1);
      questionAttemptsByStageAndDifficultyStats.put(currentStage, newDifficultyMap);
    }
  }

  private void incrementQuestionsCorrectByStageAndDifficulty(final Stage currentStage,
                                                             final Difficulty currentDifficulty) {
    if (questionsCorrectByStageAndDifficultyStats.containsKey(currentStage)) {
      if (questionsCorrectByStageAndDifficultyStats.get(currentStage).containsKey(currentDifficulty)) {
        questionsCorrectByStageAndDifficultyStats.get(currentStage)
            .put(currentDifficulty,
                questionsCorrectByStageAndDifficultyStats.get(currentStage).get(currentDifficulty) + 1);
      } else {
        questionsCorrectByStageAndDifficultyStats.get(currentStage).put(currentDifficulty, 1);
      }
    } else {
      Map<Difficulty, Integer> newDifficultyMap = Maps.newHashMap();
      newDifficultyMap.put(currentDifficulty, 1);
      questionsCorrectByStageAndDifficultyStats.put(currentStage, newDifficultyMap);
    }
  }

  public void incrementQuestionAttemptsByLevelStats(final String questionLevel) {
    if (questionAttemptsByLevelStats.containsKey(questionLevel)) {
      questionAttemptsByLevelStats.put(questionLevel, questionAttemptsByLevelStats.get(questionLevel) + 1);
    } else {
      questionAttemptsByLevelStats.put(questionLevel, 1);
    }
  }

  public void incrementQuestionsCorrectByLevelStats(final String questionLevel) {
    if (questionsCorrectByLevelStats.containsKey(questionLevel)) {
      questionsCorrectByLevelStats.put(questionLevel, questionsCorrectByLevelStats.get(questionLevel) + 1);
    } else {
      questionsCorrectByLevelStats.put(questionLevel, 1);
    }
  }

  public void incrementQuestionAttemptsByTypeStats(final String questionPartType) {
    if (questionAttemptsByTypeStats.containsKey(questionPartType)) {
      questionAttemptsByTypeStats.put(questionPartType, questionAttemptsByTypeStats.get(questionPartType) + 1);
    } else {
      questionAttemptsByTypeStats.put(questionPartType, 1);
    }
  }

  public void incrementQuestionsCorrectByTypeStats(final String questionPartType) {
    if (questionsCorrectByTypeStats.containsKey(questionPartType)) {
      questionsCorrectByTypeStats.put(questionPartType, questionsCorrectByTypeStats.get(questionPartType) + 1);
    } else {
      questionsCorrectByTypeStats.put(questionPartType, 1);
    }
  }

  public boolean isQuestionPagesNotCompleteLessThanProgressMaxRecentQuestions() {
    return questionPagesNotComplete.size() < PROGRESS_MAX_RECENT_QUESTIONS;
  }

  public void addQuestionPageNotComplete(final ContentDTO questionContentDTO) {
    questionPagesNotComplete.add(questionContentDTO);
  }

  public void addMostRecentlyAttemptedQuestionPage(final ContentDTO questionContentDTO) {
    mostRecentlyAttemptedQuestionPages.add(questionContentDTO);  // Assumes questionAttemptsByUser is sorted!
  }

  public Map<String, Object> toMap(final UserSummaryDTO userOfInterestSummary,
                                   final Function<ContentDTO, ContentSummaryDTO> contentSummarisationFunction) {
    // Collate all the information into the JSON response as a Map:
    Map<String, Object> questionInfo = Maps.newHashMap();
    List<ContentSummaryDTO> mostRecentlyAttemptedQuestionsList = mostRecentlyAttemptedQuestionPages
        .stream().map(contentSummarisationFunction).collect(Collectors.toList());
    Collections.reverse(
        mostRecentlyAttemptedQuestionsList);  // We want most-recent first order and streams cannot reverse.
    List<ContentSummaryDTO> questionsNotCompleteList = questionPagesNotComplete
        .stream().map(contentSummarisationFunction).collect(Collectors.toList());

    questionInfo.put("totalQuestionsAttempted", attemptedQuestions);
    questionInfo.put("totalQuestionsCorrect", correctQuestions);
    questionInfo.put("totalQuestionPartsAttempted", attemptedQuestionParts);
    questionInfo.put("totalQuestionPartsCorrect", correctQuestionParts);
    questionInfo.put("totalQuestionsCorrectThisAcademicYear", correctQuestionsThisAcademicYear);
    questionInfo.put("totalQuestionsAttemptedThisAcademicYear", attemptedQuestionsThisAcademicYear);
    questionInfo.put("totalQuestionPartsCorrectThisAcademicYear", correctQuestionPartsThisAcademicYear);
    questionInfo.put("totalQuestionPartsAttemptedThisAcademicYear", attemptedQuestionPartsThisAcademicYear);
    questionInfo.put("attemptsByTag", questionAttemptsByTagStats);
    questionInfo.put("correctByTag", questionsCorrectByTagStats);
    questionInfo.put("attemptsByStageAndDifficulty", questionAttemptsByStageAndDifficultyStats);
    questionInfo.put("correctByStageAndDifficulty", questionsCorrectByStageAndDifficultyStats);
    questionInfo.put("attemptsByLevel", questionAttemptsByLevelStats);
    questionInfo.put("correctByLevel", questionsCorrectByLevelStats);
    questionInfo.put("attemptsByType", questionAttemptsByTypeStats);
    questionInfo.put("correctByType", questionsCorrectByTypeStats);
    questionInfo.put("oldestIncompleteQuestions", questionsNotCompleteList);
    questionInfo.put("mostRecentQuestions", mostRecentlyAttemptedQuestionsList);
    questionInfo.put("userDetails", userOfInterestSummary);

    return questionInfo;
  }
}
