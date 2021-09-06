package uk.ac.cam.cl.dtg.segue.dos;

import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  A class to specify a restricted and *validated* audience context which the user is allowed to specify when creating
 *  a gameboard.
 */
public class AudienceContext {
    private List<Stage> stage;
    private List<ExamBoard> examBoard;
    private List<Difficulty> difficulty;
    private List<RoleRequirement> role;

    @Nullable
    public static AudienceContext fromFilter(@Nullable final GameFilter gameFilter) {
        if (gameFilter == null) {return null;}
        return new AudienceContext() {{
            if (gameFilter.getStages() != null) {
                setStage(gameFilter.getStages().stream().map(Stage::valueOf).collect(Collectors.toList()));
            }
            if (gameFilter.getExamBoards() != null) {
                setExamBoard(gameFilter.getExamBoards().stream().map(ExamBoard::valueOf).collect(Collectors.toList()));
            }
            if (gameFilter.getDifficulties() != null) {
                setDifficulty(gameFilter.getDifficulties().stream().map(Difficulty::valueOf).collect(Collectors.toList()));
            }
        }};
    }

    public AudienceContext() {}

    public List<Stage> getStage() {
        return stage;
    }

    public void setStage(List<Stage> stage) {
        this.stage = stage;
    }

    public List<ExamBoard> getExamBoard() {
        return examBoard;
    }

    public void setExamBoard(List<ExamBoard> examBoard) {
        this.examBoard = examBoard;
    }

    public List<Difficulty> getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(List<Difficulty> difficulty) {
        this.difficulty = difficulty;
    }

    public List<RoleRequirement> getRole() {
        return role;
    }

    public void setRole(List<RoleRequirement> role) {
        this.role = role;
    }
}
