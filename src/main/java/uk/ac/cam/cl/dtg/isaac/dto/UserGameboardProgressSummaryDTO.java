package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

import java.util.List;

public class UserGameboardProgressSummaryDTO {
    private UserSummaryDTO user;
    private List<GameboardProgressSummaryDTO> progress;

    public UserGameboardProgressSummaryDTO() {
        this.user = null;
    }

    public UserSummaryDTO getUser() {
        return user;
    }

    public void setUser(UserSummaryDTO user) {
        this.user = user;
    }

    public List<GameboardProgressSummaryDTO> getProgress() {
        return progress;
    }

    public void setProgress(List<GameboardProgressSummaryDTO> progress) {
        this.progress = progress;
    }

}
