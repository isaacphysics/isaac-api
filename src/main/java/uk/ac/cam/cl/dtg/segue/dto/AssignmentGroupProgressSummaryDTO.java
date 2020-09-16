package uk.ac.cam.cl.dtg.segue.dto;

import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserGameboardProgressSummaryDTO;

import java.util.List;

public class AssignmentGroupProgressSummaryDTO {
    private String gameboardId;
    private String gameboardTitle;
    private List<UserGameboardProgressSummaryDTO> groupMembersProgress;

    public AssignmentGroupProgressSummaryDTO() {
        this.gameboardId = null;
        this.gameboardTitle = null;
        this.groupMembersProgress = null;
    }

    public String getGameboardId() {
        return gameboardId;
    }

    public void setGameboardId(String gameboardId) {
        this.gameboardId = gameboardId;
    }

    public String getGameboardTitle() {
        return gameboardTitle;
    }

    public void setGameboardTitle(String gameboardTitle) {
        this.gameboardTitle = gameboardTitle;
    }

    public List<UserGameboardProgressSummaryDTO> getGroupMembersProgress() {
        return groupMembersProgress;
    }

    public void setGroupMembersProgress(List<UserGameboardProgressSummaryDTO> groupMembersProgress) {
        this.groupMembersProgress = groupMembersProgress;
    }
}
