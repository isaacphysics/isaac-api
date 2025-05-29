package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

import java.util.List;

/**
 * This class is the DTO used to transfer user assignment progress information.
 */
public class AssignmentProgressDTO {
    public UserSummaryDTO user;
    public List<Integer> correctPartResults;
    public List<Integer> incorrectPartResults;
    public List<Constants.GameboardItemState> questionResults;
    public List<List<Constants.QuestionPartState>> questionPartResults;

    /**
     * Complete AssignmentProgressDTO constructor with all dependencies.
     * @param user
     *            - UserSummaryDTO of the user.
     * @param correctPartResults
     *            - List of correct part results.
     * @param incorrectPartResults
     *            - List of incorrect part results.
     * @param results
     *            - Array of results.
     */
    public AssignmentProgressDTO(UserSummaryDTO user, List<Integer> correctPartResults, List<Integer> incorrectPartResults, List<Constants.GameboardItemState> results, List<List<Constants.QuestionPartState>> partResults) {
        this.user = user;
        this.correctPartResults = correctPartResults;
        this.incorrectPartResults = incorrectPartResults;
        this.questionResults = results;
        this.questionPartResults = partResults;
    }

    public AssignmentProgressDTO() {
    }

    public UserSummaryDTO getUser() {
        return user;
    }

    public void setUser(UserSummaryDTO user) {
        this.user = user;
    }

    public List<Integer> getCorrectPartResults() {
        return correctPartResults;
    }

    public void setCorrectPartResults(List<Integer> correctPartResults) {
        this.correctPartResults = correctPartResults;
    }

    public List<Integer> getIncorrectPartResults() {
        return incorrectPartResults;
    }

    public void setIncorrectPartResults(List<Integer> incorrectPartResults) {
        this.incorrectPartResults = incorrectPartResults;
    }

    public List<Constants.GameboardItemState> getQuestionResults() {
        return questionResults;
    }

    public void setQuestionResults(List<Constants.GameboardItemState> questionResults) {
        this.questionResults = questionResults;
    }


    public List<List<Constants.QuestionPartState>> getQuestionPartResults() {
        return questionPartResults;
    }

    public void setQuestionPartResults(List<List<Constants.QuestionPartState>> questionPartResults) {
        this.questionPartResults = questionPartResults;
    }
}
