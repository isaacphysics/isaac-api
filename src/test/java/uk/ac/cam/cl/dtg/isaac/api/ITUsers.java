package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.collect.Sets;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.util.Set;


public class ITUsers {

    public final RegisteredUserDTO TEST_STUDENT;
    public final RegisteredUserDTO ALICE_STUDENT;
    public final RegisteredUserDTO ERIKA_STUDENT;
    public final RegisteredUserDTO TEST_TEACHER;
    public final RegisteredUserDTO DAVE_TEACHER;
    public final RegisteredUserDTO TEST_ADMIN;
    public final RegisteredUserDTO TEST_TUTOR;
    public final RegisteredUserDTO TEST_EVENTMANAGER;
    public final RegisteredUserDTO TEST_EDITOR;

    public final Set<RegisteredUserDTO> ALL;
    public final Set<RegisteredUserDTO> TUTOR_AND_BELOW;
    public final Set<RegisteredUserDTO> TEACHER_AND_BELOW;
    public final Set<RegisteredUserDTO> EDITOR_AND_BELOW;


    /**
     * Helper class for integration tests that retrieves RegisteredUserDTOs for the initial state of test users in the
     * database.
     *
     * @param userAccountManager to retrieve user information.
     * @throws NoUserException not expected.
     * @throws SegueDatabaseException not expected.
     */
    ITUsers(UserAccountManager userAccountManager) throws NoUserException, SegueDatabaseException {
        TEST_STUDENT = userAccountManager.getUserDTOById(ITConstants.TEST_STUDENT_ID);
        ALICE_STUDENT = userAccountManager.getUserDTOById(ITConstants.ALICE_STUDENT_ID);
        ERIKA_STUDENT = userAccountManager.getUserDTOById(ITConstants.ERIKA_STUDENT_ID);
        TEST_TUTOR = userAccountManager.getUserDTOById(ITConstants.TEST_TUTOR_ID);
        TEST_TEACHER = userAccountManager.getUserDTOById(ITConstants.TEST_TEACHER_ID);
        DAVE_TEACHER = userAccountManager.getUserDTOById(ITConstants.DAVE_TEACHER_ID);
        TEST_EVENTMANAGER = userAccountManager.getUserDTOById(ITConstants.TEST_EVENTMANAGER_ID);
        TEST_EDITOR = userAccountManager.getUserDTOById(ITConstants.TEST_EDITOR_ID);
        TEST_ADMIN = userAccountManager.getUserDTOById(ITConstants.TEST_ADMIN_ID);

        TUTOR_AND_BELOW = Set.of(TEST_STUDENT, TEST_TUTOR);
        TEACHER_AND_BELOW = Sets.union(TUTOR_AND_BELOW, Set.of(TEST_TEACHER));
        EDITOR_AND_BELOW = Sets.union(TEACHER_AND_BELOW, Set.of(TEST_EDITOR, TEST_EVENTMANAGER));
        ALL = Sets.union(EDITOR_AND_BELOW, Set.of(TEST_ADMIN));
    }
}
