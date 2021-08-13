package uk.ac.cam.cl.dtg.segue.dos.users;

public class UserContext {
    public enum STAGE {gcse, a_level, further_a, university, none}
    public enum EXAM_BOARD {AQA, OCR, CIE, EDEXCEL, EDUCAS, WJEC, OTHER, NONE}

    private STAGE stage;
    private EXAM_BOARD examBoard;

    public UserContext() {;}

    public STAGE getStage() {
        return stage;
    }

    public void setStage(STAGE stage) {
        this.stage = stage;
    }

    public EXAM_BOARD getExamBoard() {
        return examBoard;
    }

    public void setExamBoard(EXAM_BOARD examBoard) {
        this.examBoard = examBoard;
    }
}
