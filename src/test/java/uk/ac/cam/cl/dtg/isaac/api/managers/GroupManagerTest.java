package uk.ac.cam.cl.dtg.isaac.api.managers;

import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Gender;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.powermock.reflect.Whitebox;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.*;

public class GroupManagerTest extends AbstractManagerTest {

    private GroupManager groupManager;

    @Before
    public final void setUp() {
        this.groupManager = createMock(GroupManager.class);
    }

    @Test
    public void orderUsersByName_ordersBySurnamePrimarily() throws Exception {
        List<RegisteredUserDTO> users = Stream.of(
                new RegisteredUserDTO("A",  "Ab",  "a1@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO("B",  "Ar",  "a2@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO("C",  "Ax",  "a3@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO(null, "Ax",  "a4@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO("A",  "Ba",  "b1@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO("B",  "Bb",  "b2@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO("C",  "Bf",  "b3@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO("A",  "O'A", "o1@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO("A",  "Obe", "o2@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO("A",  "O'c", "o3@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO("A",  "Oc",  "o4@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO("A",  null,  "-1@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false),
                new RegisteredUserDTO(null, null,  "--@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE, somePastDate, "", null, false)
        ).peek(user -> user.setId((long) ("" + user.getGivenName() + user.getFamilyName()).hashCode())).collect(Collectors.toList());

        List<RegisteredUserDTO> shuffledUsers = new ArrayList<>(users);
        Collections.shuffle(shuffledUsers);

        assertNotEquals(users, shuffledUsers);
        Whitebox.invokeMethod(groupManager, "orderUsersByName", shuffledUsers);
        assertEquals(users, shuffledUsers);
    }
}
