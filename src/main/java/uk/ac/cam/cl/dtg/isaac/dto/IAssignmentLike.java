package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

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
        cal.add(Calendar.DATE, 1);
        return cal.getTime().after(date);
    }

    interface Details<T extends IAssignmentLike> {
        String getAssignmentLikeName(T assignment) throws SegueDatabaseException, ContentManagerException;
        String getAssignmentLikeUrl(T assignment);
    }
}
