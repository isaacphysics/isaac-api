/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.auth;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.ac.cam.cl.dtg.segue.api.Constants.PASSWORD_REQUIREMENTS_ERROR_MESSAGE;

import com.google.common.collect.ImmutableMap;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.ac.cam.cl.dtg.isaac.dos.users.LocalUserCredential;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IPasswordDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Test class for the SegueLocalAuthenticator class.
 */
public class SegueLocalAuthenticatorTest {

  private IUserDataManager userDataManager;
  private IPasswordDataManager passwordDataManager;
  private PropertiesLoader propertiesLoader;

  private final ISegueHashingAlgorithm preferredAlgorithm = new SegueSCryptv1();
  private final ISegueHashingAlgorithm oldAlgorithm1 = new SeguePBKDF2v1();
  private final ISegueHashingAlgorithm oldAlgorithm2 = new SeguePBKDF2v2();
  private final ISegueHashingAlgorithm oldAlgorithm3 = new SeguePBKDF2v3();

  private final Map<String, ISegueHashingAlgorithm> possibleAlgorithms = ImmutableMap.of(
      preferredAlgorithm.hashingAlgorithmName(), preferredAlgorithm,
      oldAlgorithm1.hashingAlgorithmName(), oldAlgorithm1,
      oldAlgorithm2.hashingAlgorithmName(), oldAlgorithm2,
      oldAlgorithm3.hashingAlgorithmName(), oldAlgorithm3
  );

  /**
   * Initial configuration of tests.
   *
   * @throws Exception - test exception
   */
  @BeforeEach
  public final void setUp() throws Exception {
    userDataManager = createMock(IUserDataManager.class);
    passwordDataManager = createMock(IPasswordDataManager.class);
    propertiesLoader = createMock(PropertiesLoader.class);
  }

  @Nested
  class SegueLocalAuthenticatorSetOrChangeUsersPassword {
    /**
     * Verify that setOrChangeUsersPassword fails with bad input.
     * * @throws InvalidKeySpecException
     *
     * @throws NoSuchAlgorithmException
     */
    @Test
    public final void emptyPasswordExceptionsShouldBeThrown() throws InvalidKeySpecException, NoSuchAlgorithmException {
      RegisteredUser someUser = new RegisteredUser();
      someUser.setEmail(TEST_USER_EMAIL);
      someUser.setId(TEST_USER_ID);

      replay(userDataManager);

      SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(userDataManager, passwordDataManager,
          propertiesLoader, possibleAlgorithms, preferredAlgorithm);

      try {
        segueAuthenticator.setOrChangeUsersPassword(someUser, null);
        fail("Expected InvalidPasswordException to be thrown as a null password was given.");
      } catch (InvalidPasswordException e) {
        // this is a pass

      } catch (SegueDatabaseException e) {
        e.printStackTrace();
      }

      try {
        segueAuthenticator.setOrChangeUsersPassword(someUser, "");
        fail("Expected InvalidPasswordException to be thrown as a empty password was given.");
      } catch (InvalidPasswordException e) {
        // this is a pass

      } catch (SegueDatabaseException e) {
        e.printStackTrace();
      }
    }

    /**
     * Verify that setOrChangeUsersPassword works with correct input and the
     * result is a user object with base64 encoded passwords and a secure salt.
     * * @throws InvalidKeySpecException
     *
     * @throws NoSuchAlgorithmException
     */
    @Test
    public final void validPasswordPasswordAndHashShouldBePopulatedAsBase64()
        throws InvalidKeySpecException, NoSuchAlgorithmException {
      RegisteredUser someUser = new RegisteredUser();
      someUser.setEmail(TEST_USER_EMAIL);
      someUser.setId(TEST_USER_ID);
      replay(userDataManager);

      SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(userDataManager, passwordDataManager,
          propertiesLoader, possibleAlgorithms, preferredAlgorithm);

      try {
        segueAuthenticator.setOrChangeUsersPassword(someUser, TEST_USER_CORRECT_PASSWORD);

        //TODO write test

      } catch (InvalidPasswordException e) {
        fail("This should be a valid password");
      } catch (SegueDatabaseException e) {
        e.printStackTrace();
      }
    }
  }

  @Nested
  class SegueLocalAuthenticatorAuthenticate {
    /**
     * Verify that setOrChangeUsersPassword fails on a bad password.
     * * @throws SegueDatabaseException
     */
    @Test
    public final void correctEmailAndIncorrectPasswordProvided()
        throws SegueDatabaseException {
      String usersEmailAddress = TEST_USER_EMAIL;

      RegisteredUser userFromDatabase = new RegisteredUser();
      userFromDatabase.setId(TEST_USER_ID);
      userFromDatabase.setEmail(usersEmailAddress);

      RegisteredUser someUser = new RegisteredUser();
      someUser.setEmail(TEST_USER_EMAIL);
      someUser.setId(TEST_USER_ID);

      LocalUserCredential luc = new LocalUserCredential();
      luc.setPassword(TEST_USER_CORRECT_PASSWORD_HASH_FROM_DB);
      luc.setSecureSalt(TEST_USER_CORRECT_SECURE_SALT_FROM_DB);
      luc.setSecurityScheme("SeguePBKDF2v1");

      expect(userDataManager.getByEmail(usersEmailAddress)).andReturn(userFromDatabase).once();

      expect(passwordDataManager.getLocalUserCredential(anyLong())).andReturn(luc).atLeastOnce();

      replay(userDataManager, passwordDataManager);

      SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(userDataManager, passwordDataManager,
          propertiesLoader, possibleAlgorithms, preferredAlgorithm);

      assertThrows(IncorrectCredentialsProvidedException.class,
          () -> segueAuthenticator.authenticate(usersEmailAddress, TEST_USER_INCORRECT_PASSWORD));
    }

    /**
     * Verify that setOrChangeUsersPassword fails on a bad e-mail and password.
     * * @throws SegueDatabaseException
     */
    @Test
    public final void badEmailAndIncorrectPasswordProvided()
        throws SegueDatabaseException {

      RegisteredUser userFromDatabase = new RegisteredUser();
      userFromDatabase.setId(TEST_USER_ID);
      userFromDatabase.setEmail(TEST_USER_EMAIL);

      String someBadEmail = TEST_USER_BAD_EMAIL;

      expect(userDataManager.getByEmail(someBadEmail)).andReturn(null).once();

      replay(userDataManager);

      SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(userDataManager, passwordDataManager,
          propertiesLoader, possibleAlgorithms, preferredAlgorithm);

      assertThrows(IncorrectCredentialsProvidedException.class,
          () -> segueAuthenticator.authenticate(someBadEmail, TEST_USER_INCORRECT_PASSWORD));
    }

  }

  /**
   * Verify that the authenticator creates and authenticates correctly..
   *
   * @throws SegueDatabaseException
   * @throws NoCredentialsAvailableException
   * @throws InvalidPasswordException
   * @throws InvalidKeySpecException
   * @throws NoSuchAlgorithmException
   */
  @Test
  public final void segueLocalAuthenticatorSetPasswordAndImmediateAuthenticateCorrectEmailAndPasswordProvided()
      throws SegueDatabaseException,
      NoCredentialsAvailableException, InvalidPasswordException, InvalidKeySpecException, NoSuchAlgorithmException {
    String someCorrectPasswordPlainText = TEST_USER_CORRECT_PASSWORD;
    String usersEmailAddress = TEST_USER_EMAIL;

    RegisteredUser userFromDatabase = new RegisteredUser();
    userFromDatabase.setId(TEST_USER_ID);
    userFromDatabase.setEmail(usersEmailAddress);

    LocalUserCredential luc = new LocalUserCredential();
    luc.setPassword(TEST_USER_CORRECT_PASSWORD_HASH_FROM_DB);
    luc.setSecureSalt(TEST_USER_CORRECT_SECURE_SALT_FROM_DB);
    luc.setSecurityScheme("SeguePBKDF2v1");

    expect(userDataManager.getByEmail(usersEmailAddress)).andReturn(userFromDatabase).once();
    expect(passwordDataManager.getLocalUserCredential(anyLong())).andReturn(luc).atLeastOnce();
    expect(passwordDataManager.createOrUpdateLocalUserCredential(anyObject())).andReturn(luc).atLeastOnce();

    replay(userDataManager, passwordDataManager);

    SegueLocalAuthenticator segueAuthenticator =
        new SegueLocalAuthenticator(userDataManager, passwordDataManager,
            propertiesLoader, possibleAlgorithms, preferredAlgorithm);
    try {
      // first try and mutate the user object using the set method.
      // this should set the password and secure hash on the user object.
      segueAuthenticator.setOrChangeUsersPassword(userFromDatabase, someCorrectPasswordPlainText);

      // now try and authenticate using the password we just created.
      RegisteredUser authenticatedUser =
          segueAuthenticator.authenticate(usersEmailAddress, someCorrectPasswordPlainText);
      assertNotNull(authenticatedUser);

    } catch (IncorrectCredentialsProvidedException e) {
      fail("We expect a user to be returned");
    }
  }

  @Nested
  class EnsureValidPasswordValidStrings {
    @Test
    public void validString() {
      SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(userDataManager, passwordDataManager,
          propertiesLoader, possibleAlgorithms, preferredAlgorithm);
      try {
        segueAuthenticator.ensureValidPassword("Password123!");
      } catch (InvalidPasswordException e) {
        fail("We expect this password to be accepted as valid");
      }
    }

    @Test
    public void validSpecialCharacters() {
      SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(userDataManager, passwordDataManager,
          propertiesLoader, possibleAlgorithms, preferredAlgorithm);
      try {
        segueAuthenticator.ensureValidPassword("Password123 !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~");
      } catch (InvalidPasswordException e) {
        fail("We expect all of these special characters to be permitted");
      }
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  @MethodSource("ensureValidPasswordInvalidStrings")
  public void ensureValidPasswordInvalidStrings(final String password) {
    SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(userDataManager, passwordDataManager,
        propertiesLoader, possibleAlgorithms, preferredAlgorithm);
    Exception exception =
        assertThrows(InvalidPasswordException.class, () -> segueAuthenticator.ensureValidPassword(password));
    assertEquals(PASSWORD_REQUIREMENTS_ERROR_MESSAGE, exception.getMessage());
  }

  private static Stream<Arguments> ensureValidPasswordInvalidStrings() {
    return Stream.of(
        Arguments.of("password123"), // Password must equal or exceed the minimum length
        Arguments.of("password123!"), // Password must contain an upper case letter
        Arguments.of("PASSWORD123!"), // Password must contain a lower case letter
        Arguments.of("Passwordabc!"), // Password must contain a number
        Arguments.of("Password1234") // Password must contain a special character
    );
  }

  // Test Constants
  private static final String TEST_USER_EMAIL = "test@test.com";
  private static final String TEST_USER_BAD_EMAIL = "badtest@test.com";
  private static final String TEST_USER_CORRECT_PASSWORD = "test5eguePassw0rd!";
  private static final String TEST_USER_INCORRECT_PASSWORD = "password";
  private static final String TEST_USER_CORRECT_PASSWORD_HASH_FROM_DB
      = "q41e8iZsi0TO5xB1MWwC06jkaWx8MTnSHMp3eP/FNOTBCwpTnPX5a/KsUTUJ2cwnCiXZeATVqWEF2FCbSQp9Fg==";
  private static final String TEST_USER_CORRECT_SECURE_SALT_FROM_DB = "P77Fhqu2/SAVGDCtu9IkHg==";
  private static final Long TEST_USER_ID = 533L;
}
