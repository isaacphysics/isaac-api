/**
 * Copyright 2014 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.auth;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * An interface defining the password authentication process.
 * <p>
 * Note: This is used for authenticating users using locally held credentials.
 *
 * @author Stephen Cummins
 */
public interface IPasswordAuthenticator extends IAuthenticator {

  /**
   * Creates or updates a local set of credentials for a Segue user with the system.
   *
   * @param user
   *            object to register containing all user information to be stored including the plain text password.
   * @param plainTextPassword
   *            - plain text password to be hashed.
   * @throws InvalidPasswordException
   *             - if the password specified does not meet the complexity requirements or is empty.
   * @throws SegueDatabaseException
   *             - If there is an internal database error.
   */
  void setOrChangeUsersPassword(RegisteredUser user, String plainTextPassword)
      throws InvalidPasswordException, SegueDatabaseException, InvalidKeySpecException, NoSuchAlgorithmException;

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
   * @throws NoCredentialsAvailableException
   *             - No credentials are configured on this account so we cannot authenticate the user.
   * @throws SegueDatabaseException
   *             - If there is an internal database error.
   * @throws NoSuchAlgorithmException
   *             - if the configured algorithm is not valid.
   * @throws InvalidKeySpecException
   *             - if the preconfigured key spec is invalid.
   */
  RegisteredUser authenticate(String usersEmailAddress, String plainTextPassword)
      throws IncorrectCredentialsProvidedException, NoCredentialsAvailableException,
      SegueDatabaseException, InvalidKeySpecException, NoSuchAlgorithmException;

  /**
   * Method to check if a user has a password configured.
   *
   * @param userToCheck - user
   * @return true if so false if not.
   */
  boolean hasPasswordRegistered(RegisteredUser userToCheck) throws SegueDatabaseException;

  /**
   * Creates a password reset token and save it to the persistent engine.
   *
   * @param userToAttachToken
   *            - the user which should have token information added. -
   * @return The reset token
   */
  String createPasswordResetTokenForUser(RegisteredUser userToAttachToken) throws
      SegueDatabaseException, InvalidKeySpecException, NoSuchAlgorithmException;

  /**
   * This method will test if the user's reset token is valid reset token for a given user.
   *
   * @param token
   *            - Token to validate.
   * @return true if the reset token is valid
   */
  boolean isValidResetToken(String token) throws SegueDatabaseException;

  /**
   * This method will throw an exception for invalid passwords.
   *
   * @param password
   *            - Password to validate.
   * @throws InvalidPasswordException - if the password is invalid
   */
  void ensureValidPassword(String password) throws InvalidPasswordException;

  /**
   * This method will test if the user's reset token is valid reset token for a given user.
   *
   * @param token
   *            - Token to validate.
   * @return RegisteredUser
   */
  RegisteredUser getRegisteredUserByToken(String token) throws SegueDatabaseException;

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
   */
  RegisteredUser createEmailVerificationTokenForUser(RegisteredUser userToAttachVerificationToken, String email);

  /**
   * This method tests whether the verification token is valid.
   * @param user
   *            - user
   * @param token
   *            - verification token send to the user

   * @return - the validity of the token
   */
  boolean isValidEmailVerificationToken(RegisteredUser user, String token);
}
