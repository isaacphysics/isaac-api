package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;

import jakarta.annotation.Nullable;
import java.util.Calendar;
import java.util.Date;

public interface IAssignmentLike {
    Long getId();

    Long getGroupId();

    Long getOwnerUserId();

    void setAssignerSummary(UserSummaryDTO userSummaryDTO);

    Date getCreationDate();

    @Nullable
    Date getDueDate();

    @Nullable
    Date getScheduledStartDate();

    default boolean dueDateIsAfter(Date date) {
        Date dueDate = this.getDueDate();
        if (null == dueDate) {
            // This interprets null due dates as "assignment is due by the end of the universe"
            return false;
        }
        // Compare date against midnight of the due date
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dueDate.getTime());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, 1);
        return cal.getTime().after(date);
    }

    default boolean scheduledStartDateIsBefore(Date date) {
        Date scheduledStartDate = this.getScheduledStartDate();
        if (null == scheduledStartDate) {
            // This interprets null scheduled start dates as "always already started"
            return true;
        }

        return scheduledStartDate.before(date);
    }

    interface Details<T extends IAssignmentLike> {
        String getAssignmentLikeName(T assignment) throws SegueDatabaseException;

        String getAssignmentLikeUrl(T assignment);
    }
}
