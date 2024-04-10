package uk.ac.cam.cl.dtg.isaac.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;

public interface IAssignmentLike {
  Long getId();

  Long getGroupId();

  Long getOwnerUserId();

  void setAssignerSummary(UserSummaryDTO userSummaryDTO);

  Instant getCreationDate();

  @Nullable
  Instant getDueDate();

  default boolean dueDateIsAfter(Instant date) {
    Instant dueDate = this.getDueDate();
    if (null == dueDate) {
      // This interprets null due dates as "assignment is due by the end of the universe"
      return false;
    }
    // Compare date against midnight of the due date
    return dueDate.truncatedTo(ChronoUnit.DAYS).plus(1L, ChronoUnit.DAYS).isAfter(date);
  }

  interface Details<T extends IAssignmentLike> {
    String getAssignmentLikeName(T assignment) throws SegueDatabaseException, ContentManagerException;

    String getAssignmentLikeUrl(T assignment);
  }
}
