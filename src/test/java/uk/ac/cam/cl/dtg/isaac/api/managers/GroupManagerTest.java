package uk.ac.cam.cl.dtg.isaac.api.managers;

import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Arrays;
import java.util.List;

import org.powermock.reflect.Whitebox;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;

public class GroupManagerTest extends AbstractManagerTest {

    private GroupManager groupManager;

    @Before
    public final void setUp() {
        this.groupManager = createMock(GroupManager.class);
    }

    @Test
    public void orderUsersByName_ordersBySurnamePrimarily() throws Exception {
        List<RegisteredUserDTO> users = Arrays.asList(
                new RegisteredUserDTO("C", "Ab", "aab@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, ""),
                new RegisteredUserDTO("B", "Ar", "bar@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE, somePastDate, ""),
                new RegisteredUserDTO("A", "Az", "caz@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, ""),
                new RegisteredUserDTO("F", "Ba", "dba@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE, somePastDate, ""),
                new RegisteredUserDTO("E", "Bb", "ebb@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, ""),
                new RegisteredUserDTO("D", "Bf", "fbf@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE, somePastDate, "")
        );
        List<RegisteredUserDTO> sortedUsers = Whitebox.invokeMethod(groupManager, "orderUsersByName", users);
        assertEquals(users, sortedUsers);
    }
}
