/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.auth;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC_SALT;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.FailedToHashPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IPasswordDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dos.users.LocalUserCredential;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.Inject;

/**
 * Segue Local Authenticator. This provides a mechanism for users to create an account on the Segue CMS without the need
 * to use a 3rd party authenticator.
 * 
 * @author Stephen Cummins
 */
public class SegueLocalAuthenticator implements IPasswordAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(SegueLocalAuthenticator.class);
    private static final Integer SHORT_KEY_LENGTH = 128;

    private final IPasswordDataManager passwordDataManager;
    private final IUserDataManager userDataManager;
    private final PropertiesLoader properties;

    private final Map<String, ISegueHashingAlgorithm> possibleAlgorithms;
    private final ISegueHashingAlgorithm preferredAlgorithm;


    /**
     * Creates a segue local authenticator object to validate and create passwords to be stored by the Segue CMS.
     * 
     * @param userDataManager
     *            - the user data manager which allows us to store and query user information.
     * @param properties
     *            - so we can look up system properties.
     * @param possibleAlgorithms
     *            - Map of possibleAlgorithms
     */
    @Inject
    public SegueLocalAuthenticator(final IUserDataManager userDataManager, final IPasswordDataManager passwordDataManager,
                                   final PropertiesLoader properties,
                                   final Map<String, ISegueHashingAlgorithm> possibleAlgorithms,
                                   final ISegueHashingAlgorithm preferredAlgorithm) {
        this.userDataManager = userDataManager;
        this.properties = properties;
        this.possibleAlgorithms = possibleAlgorithms;
        this.preferredAlgorithm = preferredAlgorithm;
        this.passwordDataManager = passwordDataManager;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return AuthenticationProvider.SEGUE;
    }

    @Override
    public void setOrChangeUsersPassword(final RegisteredUser userToSetPasswordFor, final String plainTextPassword)
            throws InvalidPasswordException, SegueDatabaseException {
        if (null == plainTextPassword || plainTextPassword.isEmpty()) {
            throw new InvalidPasswordException("Invalid password. You cannot have an empty password.");
        }

        if (plainTextPassword.length() < 6) {
            throw new InvalidPasswordException("Password must be at least 6 characters in length.");
        }

        this.updateUsersPasswordWithoutValidation(userToSetPasswordFor, plainTextPassword);
    }

    @Override
    public RegisteredUser authenticate(final String usersEmailAddress, final String plainTextPassword)
            throws IncorrectCredentialsProvidedException, NoUserException, NoCredentialsAvailableException,
            SegueDatabaseException {

        if (null == usersEmailAddress || null == plainTextPassword) {
            throw new IncorrectCredentialsProvidedException("Incorrect credentials provided.");
        }

        RegisteredUser localUserAccount = userDataManager.getByEmail(usersEmailAddress);
        if (null == localUserAccount) {
            throw new NoUserException("No user found with this email!");
        }

        LocalUserCredential luc = passwordDataManager.getLocalUserCredential(localUserAccount.getId());
        if (null == luc || null == luc.getPassword() || null == luc.getSecureSalt()) {
            log.debug(String.format("No credentials available for this account id (%s)", localUserAccount.getId()));
            throw new NoCredentialsAvailableException("This user does not have any local credentials setup.");
        }

        try {
            // work out what algorithm is being used.
            ISegueHashingAlgorithm hashingAlgorithmUsed = this.possibleAlgorithms.get(luc.getSecurityScheme());

            if (hashingAlgorithmUsed.hashPassword(plainTextPassword, luc.getSecureSalt()).equals(
                    luc.getPassword())) {

                // success, now check if we should rehash the password or not.
                if (!preferredAlgorithm.hashingAlgorithmName().equals(hashingAlgorithmUsed.hashingAlgorithmName())) {

                    // update the password
                    this.updateUsersPasswordWithoutValidation(localUserAccount, plainTextPassword);
                    log.info(String.format("Account id (%s) password algorithm automatically upgraded.", localUserAccount.getId()));
                }

                return localUserAccount;
            } else {
                throw new IncorrectCredentialsProvidedException("Incorrect credentials provided.");
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("Error detecting security algorithm", e);
            return null;
        } catch (InvalidKeySpecException e) {
            log.error("Error building secret key specification", e);
            return null;
        }
    }

    @Override
    public boolean hasPasswordRegistered(RegisteredUser userToCheck) throws SegueDatabaseException {
        if (null == userToCheck) {
            return false;
        }

        LocalUserCredential localUserCredential = this.passwordDataManager.getLocalUserCredential(userToCheck.getId());
        if (null == localUserCredential || localUserCredential.getPassword() == null) {
            return false;
        }

        return true;
    }

    @Override
    public RegisteredUser createEmailVerificationTokenForUser(final RegisteredUser userToAttachVerificationToken, 
            final String email) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Validate.notNull(userToAttachVerificationToken);
        Validate.notNull(email, "Email used for verification cannot be null");
        
        //Get HMAC
        String key = properties.getProperty(HMAC_SALT);
        String token = UserAuthenticationManager.calculateHMAC(key, email);      

        userToAttachVerificationToken.setEmailVerificationToken(token.replace("=", "")
                                                                     .replace("/", "")
                                                                     .replace("+", ""));

        return userToAttachVerificationToken;
    }
    
    @Override
    public boolean isValidEmailVerificationToken(final RegisteredUser user, final String email, final String token) {
        Validate.notNull(user);
        
        String userToken = user.getEmailVerificationToken();
        if (userToken != null && userToken.startsWith(token)) {
            // Check if the email corresponds to the token
            String key = properties.getProperty(HMAC_SALT);
            String hmacToken = UserAuthenticationManager.calculateHMAC(key, email).replace("=", "")
                                                                    .replace("/", "")
                                                                    .replace("+", ""); 
            return userToken.equals(hmacToken);
        }
        return false;
    }

    @Override
    public String createPasswordResetTokenForUser(final RegisteredUser userToAttachToken)
            throws NoSuchAlgorithmException, InvalidKeySpecException, SegueDatabaseException, NoCredentialsAvailableException {
        Validate.notNull(userToAttachToken);

        LocalUserCredential luc = passwordDataManager.getLocalUserCredential(userToAttachToken.getId());
        if (null == luc) {
            // create a new luc as this user didn't have one before - they won't ever be able to login with this
            luc = new LocalUserCredential(userToAttachToken.getId(),
                    "LOCKED@" + new String(this.preferredAlgorithm.computeHash(UUID.randomUUID().toString(),
                            UUID.randomUUID().toString(), SHORT_KEY_LENGTH)),
                    new String(this.preferredAlgorithm.computeHash(UUID.randomUUID().toString(),
                            UUID.randomUUID().toString(), SHORT_KEY_LENGTH)),
                    this.preferredAlgorithm.hashingAlgorithmName());
        }

        // Trim the "=" padding off the end of the base64 encoded token so that the URL that is
        // eventually generated is correctly parsed in email clients
        String token = new String(Base64.encodeBase64(this.preferredAlgorithm.computeHash(UUID.randomUUID().toString(),
                luc.getSecureSalt(), SHORT_KEY_LENGTH))).replace("=", "").replace("/", "")
                .replace("+", "");

        luc.setResetToken(token);

        // Set expiry date
        // Java is useless at datetime maths
        Calendar c = Calendar.getInstance();
        c.setTime(new Date()); // Initialises the calendar to the current date/time
        c.add(Calendar.DATE, 1);
        luc.setResetExpiry(c.getTime());

        this.passwordDataManager.createOrUpdateLocalUserCredential(luc);

        return luc.getResetToken();
    }

    @Override
    public boolean isValidResetToken(final String token) throws SegueDatabaseException {
        LocalUserCredential luc = passwordDataManager.getLocalUserCredentialByResetToken(token);
        if (null == luc || null == luc.getSecureSalt() || luc.getResetToken() == null || luc.getResetExpiry() == null) {
            // if we cannot find it then it is not valid.
            return false;
        }

        // Get today's datetime; this is initialised to the time at which it was allocated,
        // measured to the nearest millisecond.
        Date now = new Date();

        // check the token matches and hasn't expired (I know that we have just looked it up but that might change so checking anyway)
        return luc.getResetToken().equals(token) && luc.getResetExpiry().after(now);
    }

    @Override
    public RegisteredUser getRegisteredUserByToken(String token) throws SegueDatabaseException {
        LocalUserCredential luc = passwordDataManager.getLocalUserCredentialByResetToken(token);
        if (!this.isValidResetToken(token)) {
            // if we cannot find it then it is not valid.
            return null;
        }

        return this.userDataManager.getById(luc.getUserId());
    }

    /**
     * Private method for creating / updating a users password.
     * @param userToSetPasswordFor - the user to affect
     * @param plainTextPassword - the new password.
     * @throws SegueDatabaseException
     */
    private void updateUsersPasswordWithoutValidation(final RegisteredUser userToSetPasswordFor, final String plainTextPassword)
            throws SegueDatabaseException {
        try {
            String passwordSalt = preferredAlgorithm.generateSalt();
            String hashedPassword = preferredAlgorithm.hashPassword(plainTextPassword, passwordSalt);

            LocalUserCredential luc = new LocalUserCredential(
                    userToSetPasswordFor.getId(),
                    hashedPassword, passwordSalt, preferredAlgorithm.hashingAlgorithmName());

            // now we want to update the database
            passwordDataManager.createOrUpdateLocalUserCredential(luc);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error detecting security algorithm", e);
            throw new FailedToHashPasswordException("Security algorithm configuration error.");
        } catch (InvalidKeySpecException e) {
            log.error("Error building secret key specification", e);
            throw new FailedToHashPasswordException("Security algorithm configuration error.");
        }
    }
}
