package uk.ac.cam.cl.dtg.segue.auth;

import uk.ac.cam.cl.dtg.segue.auth.exceptions.FailedToSetPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserIdException;
import uk.ac.cam.cl.dtg.segue.dos.users.User;

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
	 *            object to register containing all user information to be
	 *            stored including the plain text password.
	 * @throws InvalidPasswordException
	 *             - if the password specified does not meet the complexity
	 *             requirements or is empty.
	 * @throws FailedToSetPasswordException
	 *             - if we couldn't set a password for another reason.
	 */
	void setOrChangeUsersPassword(User user) throws InvalidPasswordException,
			FailedToSetPasswordException;

	/**
	 * authenticate This method authenticates a given user based on the given
	 * e-mail address and password.
	 * 
	 * @param usersEmailAddress
	 *            - the users email address aka username.
	 * @param plainTextPassword
	 *            - the users plain text password to be hashed.
	 * @return the user object or Authenticator Security Exception.
	 * @throws IncorrectCredentialsProvidedException
	 *             - if invalid credentials are provided.
	 * @throws NoUserIdException
	 *             - if we cannot find the user specified.
	 * @throws NoCredentialsAvailableException
	 *             - No credentials are configured on this account so we cannot
	 *             authenticate the user.
	 */
	User authenticate(String usersEmailAddress, String plainTextPassword)
		throws IncorrectCredentialsProvidedException, NoUserIdException,
			NoCredentialsAvailableException;

	/**
	 * Triggers the lost password work flow.
	 * 
	 * @param usersEmailAddress
	 *            - the users email address to send password reset e-mail to.
	 */
	void triggerLostPasswordFlow(String usersEmailAddress);
}
