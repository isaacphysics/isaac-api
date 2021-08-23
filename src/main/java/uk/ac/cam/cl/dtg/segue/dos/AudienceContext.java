package uk.ac.cam.cl.dtg.segue.dos;

import com.google.api.client.util.Lists;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;

import javax.annotation.Nullable;
import javax.validation.constraints.Null;
import java.util.List;
import java.util.stream.Collectors;

public class AudienceContext {
    private List<Stage> stage;
    private List<ExamBoard> examBoard;
    private List<Difficulty> difficulty;

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

    public AudienceContext() {
        stage = Lists.newArrayList();
        examBoard = Lists.newArrayList();
        difficulty = Lists.newArrayList();
    }

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
}
