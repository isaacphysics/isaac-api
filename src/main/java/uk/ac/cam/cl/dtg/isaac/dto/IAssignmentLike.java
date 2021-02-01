package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

import javax.annotation.Nullable;
import java.util.Date;

public interface IAssignmentLike {
    Long getId();

    Long getGroupId();

    Long getOwnerUserId();
    void setAssignerSummary(UserSummaryDTO userSummaryDTO);

    Date getCreationDate();

    @Nullable
    Date getDueDate();

}
