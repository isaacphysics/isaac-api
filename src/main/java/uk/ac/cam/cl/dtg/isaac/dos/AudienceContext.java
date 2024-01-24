package uk.ac.cam.cl.dtg.isaac.dos;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;

/**
 * A class to specify a restricted and *validated* audience context which the user is allowed to specify when creating
 * a gameboard.
 */
public class AudienceContext {
  private List<Stage> stage;
  private List<ExamBoard> examBoard;
  private List<Difficulty> difficulty;
  private List<RoleRequirement> role;

  @Nullable
  public static AudienceContext fromFilter(@Nullable final GameFilter gameFilter) {
    if (gameFilter == null) {
      return null;
    }

    AudienceContext audienceContext = new AudienceContext();

    if (gameFilter.getStages() != null) {
      audienceContext.setStage(gameFilter.getStages().stream().map(Stage::valueOf).collect(Collectors.toList()));
    }
    if (gameFilter.getExamBoards() != null) {
      audienceContext.setExamBoard(
          gameFilter.getExamBoards().stream().map(ExamBoard::valueOf).collect(Collectors.toList())
      );
    }
    if (gameFilter.getDifficulties() != null) {
      audienceContext.setDifficulty(
          gameFilter.getDifficulties().stream().map(Difficulty::valueOf).collect(Collectors.toList())
      );
    }

    return audienceContext;
  }

  public AudienceContext() {
  }

  public List<Stage> getStage() {
    return stage;
  }

  public void setStage(final List<Stage> stage) {
    this.stage = stage;
  }

  public List<ExamBoard> getExamBoard() {
    return examBoard;
  }

  public void setExamBoard(final List<ExamBoard> examBoard) {
    this.examBoard = examBoard;
  }

  public List<Difficulty> getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(final List<Difficulty> difficulty) {
    this.difficulty = difficulty;
  }

  public List<RoleRequirement> getRole() {
    return role;
  }

  public void setRole(final List<RoleRequirement> role) {
    this.role = role;
  }
}
