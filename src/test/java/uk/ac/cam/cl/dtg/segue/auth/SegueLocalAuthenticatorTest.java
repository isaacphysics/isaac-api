/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.auth;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.LocalUserCredential;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IPasswordDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.fail;

/**
 * Test class for the SegueLocalAuthenticator class.
 */
public class SegueLocalAuthenticatorTest {

    private IUserDataManager userDataManager;
    private IPasswordDataManager passwordDataManager;
    private AbstractConfigLoader propertiesLoader;

    private final ISegueHashingAlgorithm preferredAlgorithm = new SegueSCryptv1();
    private final ISegueHashingAlgorithm oldAlgorithm1 = new SeguePBKDF2v1();
    private final ISegueHashingAlgorithm oldAlgorithm2 = new SeguePBKDF2v2();
    private final ISegueHashingAlgorithm oldAlgorithm3 = new SeguePBKDF2v3();

    Map<String, ISegueHashingAlgorithm> possibleAlgorithms = ImmutableMap.of(
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
    @Before
    public final void setUp() throws Exception {
        this.userDataManager = createMock(IUserDataManager.class);
        this.passwordDataManager = createMock(IPasswordDataManager.class);
        this.propertiesLoader = createMock(AbstractConfigLoader.class);
    }

    /**
     * Verify that setOrChangeUsersPassword fails with bad input.
     */
    @Test
    public final void segueLocalAuthenticator_setOrChangeUsersPasswordEmptyPassword_exceptionsShouldBeThrown() throws InvalidKeySpecException, NoSuchAlgorithmException {
        RegisteredUser someUser = new RegisteredUser();
        someUser.setEmail("test@test.com");
        someUser.setId(533L);

        replay(userDataManager);

        SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(this.userDataManager, this.passwordDataManager,
                this.propertiesLoader, possibleAlgorithms, preferredAlgorithm);

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
     */
    @Test
    public final void segueLocalAuthenticator_setOrChangeUsersPasswordValidPassword_passwordAndHashShouldBePopulatedAsBase64() throws InvalidKeySpecException, NoSuchAlgorithmException {
        RegisteredUser someUser = new RegisteredUser();
        someUser.setEmail("test@test.com");
        someUser.setId(533L);
        String somePassword = "test5eguePassw0rd";
        replay(userDataManager);

        SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(this.userDataManager, this.passwordDataManager,
                this.propertiesLoader, possibleAlgorithms, preferredAlgorithm);

        try {
            segueAuthenticator.setOrChangeUsersPassword(someUser, somePassword);

            //TODO write test

        } catch (InvalidPasswordException e) {
            fail("This should be a valid password");
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Verify that setOrChangeUsersPassword fails on a bad password.
     *
     * @throws SegueDatabaseException
     * @throws NoCredentialsAvailableException
     * @throws NoUserException
     */
    @Test
    public final void segueLocalAuthenticator_authenticate_correctEmailAndIncorrectPasswordProvided()
            throws SegueDatabaseException, NoUserException, NoCredentialsAvailableException, InvalidKeySpecException, NoSuchAlgorithmException {
        String someCorrectPasswordPlainText = "test5eguePassw0rd";
        String someCorrectPasswordHashFromDB = "NyACfIYjYUGK7EbtlMAV48+dgyXpa+DPUKHmR1IjY/nAI2xydZUuqtVYc/shQnJ9fhquDOu56C57NGUPsxJ52Q==";
        String someCorrectSecureSaltFromDB = "P77Fhqu2/SAVGDCtu9IkHg==";
        String usersEmailAddress = "test@test.com";

        RegisteredUser userFromDatabase = new RegisteredUser();
        userFromDatabase.setId(533L);
        userFromDatabase.setEmail(usersEmailAddress);

        RegisteredUser someUser = new RegisteredUser();
        someUser.setEmail("test@test.com");
        someUser.setId(533L);
        String someIncorrectPassword = "password";

        LocalUserCredential luc = new LocalUserCredential();
        luc.setPassword(someCorrectPasswordHashFromDB);
        luc.setSecureSalt(someCorrectSecureSaltFromDB);
        luc.setSecurityScheme("SeguePBKDF2v1");

        expect(userDataManager.getByEmail(usersEmailAddress)).andReturn(userFromDatabase).once();

        expect(passwordDataManager.getLocalUserCredential(anyLong())).andReturn(luc).atLeastOnce();

        replay(userDataManager, passwordDataManager);

        SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(this.userDataManager, this.passwordDataManager,
                this.propertiesLoader, possibleAlgorithms, preferredAlgorithm);
        try {
            RegisteredUser authenticatedUser = segueAuthenticator.authenticate(usersEmailAddress, someIncorrectPassword);
            fail("This should fail as a bad password has been provided.");

        } catch (IncorrectCredentialsProvidedException e) {
            // success

        }
    }

    /**
     * Verify that setOrChangeUsersPassword fails on a bad e-mail and password.
     *
     * @throws SegueDatabaseException
     * @throws IncorrectCredentialsProvidedException
     * @throws NoCredentialsAvailableException
     */
    @Test
    public final void segueLocalAuthenticator_authenticate_badEmailAndIncorrectPasswordProvided()
            throws SegueDatabaseException, IncorrectCredentialsProvidedException,
            NoCredentialsAvailableException, InvalidKeySpecException, NoSuchAlgorithmException {
        String someCorrectPasswordPlainText = "test5eguePassw0rd";
        String someCorrectPasswordHashFromDB = "NyACfIYjYUGK7EbtlMAV48+dgyXpa+DPUKHmR1IjY/nAI2xydZUuqtVYc/shQnJ9fhquDOu56C57NGUPsxJ52Q==";
        String someCorrectSecureSaltFromDB = "P77Fhqu2/SAVGDCtu9IkHg==";
        String usersEmailAddress = "test@test.com";

        RegisteredUser userFromDatabase = new RegisteredUser();
        userFromDatabase.setId(533L);
        userFromDatabase.setEmail(usersEmailAddress);

        String someBadEmail = "badtest@test.com";
        String someIncorrectPassword = "password";

        expect(userDataManager.getByEmail(someBadEmail)).andReturn(null).once();

        replay(userDataManager);

        SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(this.userDataManager, this.passwordDataManager,
                this.propertiesLoader, possibleAlgorithms, preferredAlgorithm);
        try {
            RegisteredUser authenticatedUser = segueAuthenticator.authenticate(someBadEmail, someIncorrectPassword);
            fail("This should fail as a bad email and password has been provided.");

        } catch (NoUserException e) {
            // success. This is what we expect.
        }
    }

    /**
     * Verify that the authenticator creates and authenticates correctly..
     *
     * @throws SegueDatabaseException
     * @throws IncorrectCredentialsProvidedException
     * @throws NoCredentialsAvailableException
     * @throws InvalidPasswordException
     */
    @Test
    public final void segueLocalAuthenticator_setPasswordAndImmediateAuthenticate_correctEmailAndPasswordProvided()
            throws SegueDatabaseException, IncorrectCredentialsProvidedException,
            NoCredentialsAvailableException, InvalidPasswordException, InvalidKeySpecException, NoSuchAlgorithmException {
        String someCorrectPasswordPlainText = "test5eguePassw0rd";
        String usersEmailAddress = "test@test.com";
        String someCorrectPasswordHashFromDB = "NyACfIYjYUGK7EbtlMAV48+dgyXpa+DPUKHmR1IjY/nAI2xydZUuqtVYc/shQnJ9fhquDOu56C57NGUPsxJ52Q==";
        String someCorrectSecureSaltFromDB = "P77Fhqu2/SAVGDCtu9IkHg==";

        RegisteredUser userFromDatabase = new RegisteredUser();
        userFromDatabase.setId(533L);
        userFromDatabase.setEmail(usersEmailAddress);

        LocalUserCredential luc = new LocalUserCredential();
        luc.setPassword(someCorrectPasswordHashFromDB);
        luc.setSecureSalt(someCorrectSecureSaltFromDB);
        luc.setSecurityScheme("SeguePBKDF2v1");

        expect(userDataManager.getByEmail(usersEmailAddress)).andReturn(userFromDatabase).once();
        expect(passwordDataManager.getLocalUserCredential(anyLong())).andReturn(luc).atLeastOnce();
        expect(passwordDataManager.createOrUpdateLocalUserCredential(anyObject())).andReturn(luc).atLeastOnce();

        replay(userDataManager, passwordDataManager);

        SegueLocalAuthenticator segueAuthenticator = new SegueLocalAuthenticator(this.userDataManager, this.passwordDataManager,
                this.propertiesLoader, possibleAlgorithms, preferredAlgorithm);
        try {
            // first try and mutate the user object using the the set method.
            // this should set the password and secure hash on the user object.
            segueAuthenticator.setOrChangeUsersPassword(userFromDatabase, someCorrectPasswordPlainText);

            // now try and authenticate using the password we just created.
            RegisteredUser authenticatedUser = segueAuthenticator.authenticate(usersEmailAddress, someCorrectPasswordPlainText);

        } catch (NoUserException e) {
            fail("We expect a user to be returned");
        }
    }
}
