package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

public class UserGameboardProgressSummaryDTO {
  private UserSummaryDTO user;
  private List<GameboardProgressSummaryDTO> progress;

  public UserGameboardProgressSummaryDTO() {
    this.user = null;
  }

  public UserSummaryDTO getUser() {
    return user;
  }

  public void setUser(final UserSummaryDTO user) {
    this.user = user;
  }

  public List<GameboardProgressSummaryDTO> getProgress() {
    return progress;
  }

  public void setProgress(final List<GameboardProgressSummaryDTO> progress) {
    this.progress = progress;
  }

}
