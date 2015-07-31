/**
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

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.FailedToHashPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
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
    private final IUserDataManager userDataManager;
    
    private final PropertiesLoader properties;

    private static final String CRYPTO_ALOGRITHM = "PBKDF2WithHmacSHA1";
    private static final String SALTING_ALGORITHM = "SHA1PRNG";
    private static final Integer ITERATIONS = 1000;
    private static final Integer KEY_LENGTH = 512;
    private static final Integer SHORT_KEY_LENGTH = 128;
    private static final int SALT_SIZE = 16;

    /**
     * Creates a segue local authenticator object to validate and create passwords to be stored by the Segue CMS.
     * 
     * @param userDataManager
     *            - the user data manager which allows us to store and query user information.
     */
    @Inject
    public SegueLocalAuthenticator(final IUserDataManager userDataManager, final PropertiesLoader properties) {
        this.userDataManager = userDataManager;
        this.properties = properties;
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return AuthenticationProvider.SEGUE;
    }

    @Override
    public void setOrChangeUsersPassword(final RegisteredUser userToSetPasswordFor, final String plainTextPassword)
            throws InvalidPasswordException {
        if (null == plainTextPassword || plainTextPassword.isEmpty()) {
            throw new InvalidPasswordException("Empty passwords are not allowed if using local authentication.");
        }

        try {
            String passwordSalt = generateSalt();
            String hashedPassword = this.hashPassword(plainTextPassword, passwordSalt);

            userToSetPasswordFor.setPassword(hashedPassword);
            userToSetPasswordFor.setSecureSalt(passwordSalt);

        } catch (NoSuchAlgorithmException e) {
            log.error("Error detecting security algorithm", e);
            throw new FailedToHashPasswordException("Security algorithrm configuration error.");
        } catch (InvalidKeySpecException e) {
            log.error("Error building secret key specification", e);
            throw new FailedToHashPasswordException("Security algorithrm configuration error.");
        }
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
            throw new NoUserException();
        }
        if (null == localUserAccount.getPassword() || null == localUserAccount.getSecureSalt()) {
            log.debug("No credentials available for this account");
            throw new NoCredentialsAvailableException("This user does not have any" + " local credentials setup.");
        }

        try {
            if (this.hashPassword(plainTextPassword, localUserAccount.getSecureSalt()).equals(
                    localUserAccount.getPassword())) {
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
    public RegisteredUser createEmailVerificationTokenForUser(final RegisteredUser userToAttachVerificationToken, 
            final String email) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Validate.notNull(userToAttachVerificationToken);
        Validate.notNull(email, "Email used for verification cannot be null");
        
        //Get HMAC
        String key = properties.getProperty(HMAC_SALT);
        String token = UserManager.calculateHMAC(key, email);      

        userToAttachVerificationToken.setEmailVerificationToken(token.replace("=", "")
                                                                     .replace("/", "")
                                                                     .replace("+", ""));
        //Finally add an expiry date
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, 1);
        userToAttachVerificationToken.setEmailVerificationTokenExpiry(c.getTime());

        return userToAttachVerificationToken;
    }
    
    @Override
    public boolean isValidEmailVerificationToken(final RegisteredUser user, final String email, final String token) {
        Validate.notNull(user);
        
        String userToken = user.getEmailVerificationToken();
        if (userToken.startsWith(token)) {
            // Check if the email corresponds to the token
            String key = properties.getProperty(HMAC_SALT);
            String hmacToken = UserManager.calculateHMAC(key, email).replace("=", "")
                                                                    .replace("/", "")
                                                                    .replace("+", ""); 
            log.info("New HCMA token: " + hmacToken);
            log.info("User token: " + userToken);
            if (userToken.equals(hmacToken)) {
                // The key is valid
                Date now = new Date();
                return user.getEmailVerificationTokenExpiry().after(now);
            }
        }
        return false;
    }

    @Override
    public RegisteredUser createPasswordResetTokenForUser(final RegisteredUser userToAttachToken)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        Validate.notNull(userToAttachToken);
        // Trim the "=" padding off the end of the base64 encoded token so that the URL that is
        // eventually generated is correctly parsed in email clients
        String token = new String(Base64.encodeBase64(computeHash(UUID.randomUUID().toString(),
                userToAttachToken.getSecureSalt(), SHORT_KEY_LENGTH))).replace("=", "").replace("/", "")
                .replace("+", "");

        userToAttachToken.setResetToken(token);

        // Set expiry date
        // Java is useless at datetime maths
        Calendar c = Calendar.getInstance();
        c.setTime(new Date()); // Initialises the calendar to the current date/time
        c.add(Calendar.DATE, 1);
        userToAttachToken.setResetExpiry(c.getTime());

        return userToAttachToken;
    }

    @Override
    public boolean isValidResetToken(final RegisteredUser user) {
        // Get today's datetime; this is initialised to the time at which it was allocated,
        // measured to the nearest millisecond.
        Date now = new Date();

        return user != null && user.getResetExpiry().after(now);
    }



    /**
     * Hash the password using the preconfigured hashing function.
     *
     * @param password
     *            - password to hash
     * @param salt
     *            - random string to use as a salt.
     * @return the Base64 encoded hashed password
     * @throws NoSuchAlgorithmException
     *             - if the configured algorithm is not valid.
     * @throws InvalidKeySpecException
     *             - if the preconfigured key spec is invalid.
     */
    private String hashPassword(final String password, final String salt) throws NoSuchAlgorithmException,
            InvalidKeySpecException {

        BigInteger hashedPassword = new BigInteger(computeHash(password, salt, KEY_LENGTH));

        return new String(Base64.encodeBase64(hashedPassword.toByteArray()));
    }

    /**
     * Compute the hash of a string using the preconfigured hashing function.
     *
     * @param str
     *            - string to hash
     * @param salt
     *            - random string to use as a salt.
     * @param keyLength
     *            - the key length
     * @return a byte array of the hash
     * @throws NoSuchAlgorithmException
     *             - if the configured algorithm is not valid.
     * @throws InvalidKeySpecException
     *             - if the preconfigured key spec is invalid.
     */
    private byte[] computeHash(final String str, final String salt, final int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        char[] strChars = str.toCharArray();
        byte[] saltBytes = salt.getBytes();

        PBEKeySpec spec = new PBEKeySpec(strChars, saltBytes, ITERATIONS, keyLength);

        SecretKeyFactory key = SecretKeyFactory.getInstance(CRYPTO_ALOGRITHM);
        return key.generateSecret(spec).getEncoded();
    }

    /**
     * Helper method to generate a base64 encoded salt.
     * 
     * @return generate a base64 encoded secure salt.
     * @throws NoSuchAlgorithmException
     *             - problem locating the algorithm.
     */
    private static String generateSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance(SALTING_ALGORITHM);

        byte[] salt = new byte[SALT_SIZE];
        sr.nextBytes(salt);

        return new String(Base64.encodeBase64(salt));
    }

}
