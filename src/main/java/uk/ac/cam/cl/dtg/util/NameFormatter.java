package uk.ac.cam.cl.dtg.util;

import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

/**
 *  A utility class to format group and teacher names to preserve privacy and maintain consistency.
 */
public final class NameFormatter {

    /**
     *  It does not make sense to create one of these!
     */
    private NameFormatter() {
    }

    /**
     * Form the short version of a teacher name with only a first initial.
     * @param teacherUser - The user summary object of the teacher user
     * @return The short name with first initial
     */
    public static String getTeacherNameFromUser(final UserSummaryDTO teacherUser) {
        return formatTeacherName(teacherUser.getGivenName(), teacherUser.getFamilyName());
    }


    /**
     * Form the short version of a teacher name with only a first initial.
     * @param teacherUser - The user summary object of the teacher user
     * @return The short name with first initial
     */
    public static String getTeacherNameFromUser(final RegisteredUserDTO teacherUser) {
        return formatTeacherName(teacherUser.getGivenName(), teacherUser.getFamilyName());
    }

    /**
     * Form the short version of a teacher name with only a first initial.
     * @param givenName The user's first name
     * @param familyName The user's last name
     * @return The short name with first initial
     */
    public static String formatTeacherName(final String givenName, final String familyName) {
        String teacherName = "Unknown";
        if (familyName != null) {
            teacherName = familyName;
        }

        if (givenName != null && !givenName.isEmpty()) {
            teacherName = givenName.substring(0, 1) + ". " + teacherName;
        }
        return teacherName;
    }

    /**
     * Get the group name, if it is allowed to be shared, else a placeholder.
     * @param group - the group to extract the name from
     * @return the group name to show to students.
     */
    public static String getFilteredGroupNameFromGroup(final UserGroupDTO group) {
        // Check the group has a last updated date: if not it means that we shouldn't show students the group name
        // as teachers may not have realised the names are public:
        String groupName = String.format("Group %s", group.getId());
        if (group.getLastUpdated() != null) {
            groupName = group.getGroupName();
        }
        return groupName;
    }
}
