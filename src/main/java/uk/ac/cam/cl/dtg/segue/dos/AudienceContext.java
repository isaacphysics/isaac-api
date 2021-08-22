package uk.ac.cam.cl.dtg.segue.dos;

import com.google.api.client.util.Lists;

import java.util.List;

public class AudienceContext {
    private List<Stage> stage;
    private List<ExamBoard> examBoard;
    private List<Difficulty> difficulty;

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
