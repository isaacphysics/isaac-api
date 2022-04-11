package uk.ac.cam.cl.dtg.isaac.dos.users;

import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;

public class UserContext {
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

    @Override
    public String toString() {
        return "UserContext [" +
                "stage=" + stage +
                ", examBoard=" + examBoard +
                ']';
    }
}
