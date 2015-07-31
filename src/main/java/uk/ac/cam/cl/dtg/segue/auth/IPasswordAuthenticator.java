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

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;

/**
 * An interface defining the password authentication process.
 * 
 * Note: This is used for authenticating users using locally held credentials.
 * 
 * @author Stephen Cummins
 */
public interface IPasswordAuthenticator extends IAuthenticator {

    /**
     * Registers a new user with the system.
     * 
     * @param user
     *            object to register containing all user information to be stored including the plain text password.
     * @param plainTextPassword
     *            - plain text password to be hashed.
     * @throws InvalidPasswordException
     *             - if the password specified does not meet the complexity requirements or is empty.
     */
    void setOrChangeUsersPassword(RegisteredUser user, final String plainTextPassword) throws InvalidPasswordException;

    /**
     * authenticate This method authenticates a given user based on the given e-mail address and password.
     *
     * @param usersEmailAddress
     *            - the users email address aka username.
     * @param plainTextPassword
     *            - the users plain text password to be hashed.
     * @return the user object or Authenticator Security Exception.
     * @throws IncorrectCredentialsProvidedException
     *             - if invalid credentials are provided.
     * @throws NoUserException
     *             - if we cannot find the user specified.
     * @throws NoCredentialsAvailableException
     *             - No credentials are configured on this account so we cannot authenticate the user.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    RegisteredUser authenticate(String usersEmailAddress, String plainTextPassword)
            throws IncorrectCredentialsProvidedException, NoUserException, NoCredentialsAvailableException,
            SegueDatabaseException;

    /**
     * Creates a password reset token and attaches it to the UserDO ready to be persisted.
     * 
     * @param userToAttachToken
     *            - the user which should have token information added. -
     * @return UserDO which has the associated password reset details attached. This user still needs to be persisted.
     * @throws NoSuchAlgorithmException
     *             - if the configured algorithm is not valid.
     * @throws InvalidKeySpecException
     *             - if the preconfigured key spec is invalid.
     */
    RegisteredUser createPasswordResetTokenForUser(RegisteredUser userToAttachToken) throws NoSuchAlgorithmException,
            InvalidKeySpecException;

    /**
     * Creates an email verification token and attaches it to the UserDO ready to be persisted.
     * 
     * @param userToAttachVerificationToken
     *            - the user who's email we're verifying
     *            
     * @param email
     *            - the email we're verifying
     * 
     * @return UserDO which has the associated verification token.
     * @throws NoSuchAlgorithmException
     *             - if the configured algorithm is not valid.
     * @throws InvalidKeySpecException
     *             - if the preconfigured key spec is invalid.
     */
    RegisteredUser createEmailVerificationTokenForUser(final RegisteredUser userToAttachVerificationToken, 
            final String email) throws NoSuchAlgorithmException, InvalidKeySpecException;

    /**
     * This method will test if the user's reset token is valid reset token.
     *
     * @param user
     *            - The user object to test
     * @return true if the reset token is valid
     */
    boolean isValidResetToken(final RegisteredUser user);

    /**
     * This method tests whether the verification token is valid.
     * @param user
     *            - user
     * @param email
     *            - email the user wants to verify
     * @param token
     *            - verification token send to the user

     * @return - the validity of the token
     */
    boolean isValidEmailVerificationToken(final RegisteredUser user, final String email, final String token);
}
