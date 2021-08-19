package uk.ac.cam.cl.dtg.segue.dos.users;

public class UserContext {
    public enum Stage {gcse, a_level, further_a, university, none}
    public enum ExamBoard {AQA, OCR, CIE, EDEXCEL, EDUQAS, WJEC, OTHER, NONE}

    private Stage stage;
    private ExamBoard examBoard;

    public UserContext() {;}

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public ExamBoard getExamBoard() {
        return examBoard;
    }

    public void setExamBoard(ExamBoard examBoard) {
        this.examBoard = examBoard;
    }
}
