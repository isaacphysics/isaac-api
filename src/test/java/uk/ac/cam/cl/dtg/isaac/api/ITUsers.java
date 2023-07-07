package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.collect.Sets;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUsers;

import java.util.Set;


public class ITUsers {

    public final RegisteredUser TEST_STUDENT;
    public final RegisteredUser ALICE_STUDENT;
    public final RegisteredUser ERIKA_STUDENT;
    public final RegisteredUser TEST_TEACHER;
    public final RegisteredUser DAVE_TEACHER;
    public final RegisteredUser TEST_ADMIN;
    public final RegisteredUser TEST_TUTOR;
    public final RegisteredUser TEST_EVENTMANAGER;
    public final RegisteredUser TEST_EDITOR;

    public final Set<RegisteredUser> ALL;
    public final Set<RegisteredUser> TUTOR_AND_BELOW;
    public final Set<RegisteredUser> TEACHER_AND_BELOW;
    public final Set<RegisteredUser> EDITOR_AND_BELOW;


    /**
     * Helper class for integration tests that retrieves RegisteredUsers for the initial state of test users in the
     * database.
     *
     * @param userDataManager to retrieve user information.
     * @throws SegueDatabaseException not expected.
     */
    ITUsers(PgUsers userDataManager) throws SegueDatabaseException {
        TEST_STUDENT = userDataManager.getById(ITConstants.TEST_STUDENT_ID);
        ALICE_STUDENT = userDataManager.getById(ITConstants.ALICE_STUDENT_ID);
        ERIKA_STUDENT = userDataManager.getById(ITConstants.ERIKA_STUDENT_ID);
        TEST_TUTOR = userDataManager.getById(ITConstants.TEST_TUTOR_ID);
        TEST_TEACHER = userDataManager.getById(ITConstants.TEST_TEACHER_ID);
        DAVE_TEACHER = userDataManager.getById(ITConstants.DAVE_TEACHER_ID);
        TEST_EVENTMANAGER = userDataManager.getById(ITConstants.TEST_EVENTMANAGER_ID);
        TEST_EDITOR = userDataManager.getById(ITConstants.TEST_EDITOR_ID);
        TEST_ADMIN = userDataManager.getById(ITConstants.TEST_ADMIN_ID);

        TUTOR_AND_BELOW = Set.of(TEST_STUDENT, TEST_TUTOR);
        TEACHER_AND_BELOW = Sets.union(TUTOR_AND_BELOW, Set.of(TEST_TEACHER));
        EDITOR_AND_BELOW = Sets.union(TEACHER_AND_BELOW, Set.of(TEST_EDITOR, TEST_EVENTMANAGER));
        ALL = Sets.union(EDITOR_AND_BELOW, Set.of(TEST_ADMIN));
    }
}
