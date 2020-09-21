package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

import java.util.List;

public class UserGameboardProgressSummaryDTO {
    private UserSummaryDTO userSummary;
    private List<GameboardProgressSummaryDTO> progress;

    public UserGameboardProgressSummaryDTO() {
        this.userSummary = null;
    }

    public UserSummaryDTO getUserSummary() {
        return userSummary;
    }

    public void setUserSummary(UserSummaryDTO userSummary) {
        this.userSummary = userSummary;
    }

    public List<GameboardProgressSummaryDTO> getProgress() {
        return progress;
    }

    public void setProgress(List<GameboardProgressSummaryDTO> progress) {
        this.progress = progress;
    }
}
