package uk.ac.cam.cl.dtg.isaac.api.managers;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Gender;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;

public class GroupManagerTest extends AbstractManagerTest {

  private GroupManager groupManager;

  @Before
  public final void setUp() {
    this.groupManager = createMock(GroupManager.class);
  }

  @Test
  public void orderUsersByName_ordersBySurnamePrimarily() throws Exception {
    List<RegisteredUserDTO> users = Stream.of(
            new RegisteredUserDTO("A", "Ab", "aab@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE,
                somePastDate, "", false),
            new RegisteredUserDTO("B", "Ar", "bar@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE,
                somePastDate, "", false),
            new RegisteredUserDTO("C", "Ax", "caz@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE,
                somePastDate, "", false),
            new RegisteredUserDTO(null, "Ax", "NONEax@test.com", EmailVerificationStatus.VERIFIED, somePastDate,
                Gender.FEMALE, somePastDate, "", false),
            new RegisteredUserDTO("A", "Ba", "dba@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE,
                somePastDate, "", false),
            new RegisteredUserDTO("B", "Bb", "ebb@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.MALE,
                somePastDate, "", false),
            new RegisteredUserDTO("C", "Bf", "fbf@test.com", EmailVerificationStatus.VERIFIED, somePastDate, Gender.FEMALE,
                somePastDate, "", false),
            new RegisteredUserDTO("A", null, "aNONE@test.com", EmailVerificationStatus.VERIFIED, somePastDate,
                Gender.FEMALE, somePastDate, "", false)
        ).peek(user -> user.setId((long) ("" + user.getGivenName() + user.getFamilyName()).hashCode()))
        .collect(Collectors.toList());

    List<RegisteredUserDTO> shuffledUsers = new ArrayList<>(users);
    Collections.shuffle(shuffledUsers);

    List<RegisteredUserDTO> sortedUsers = Whitebox.invokeMethod(groupManager, "orderUsersByName", shuffledUsers);
    assertEquals(users, sortedUsers);
  }
}
