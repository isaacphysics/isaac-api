package uk.ac.cam.cl.dtg.segue.auth;

import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.users.User;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

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
	 * @param plainTextPassword
	 *            - plain text password to be hashed.
	 * @throws InvalidPasswordException
	 *             - if the password specified does not meet the complexity
	 *             requirements or is empty.
	 */
	void setOrChangeUsersPassword(User user, final String plainTextPassword) throws InvalidPasswordException;

	/**
	 * authenticate This method authenticates a given user based on the given
	 * e-mail address and password.
	 *
	 * @param usersEmailAddress - the users email address aka username.
	 * @param plainTextPassword - the users plain text password to be hashed.
	 * @return the user object or Authenticator Security Exception.
	 * @throws IncorrectCredentialsProvidedException - if invalid credentials are provided.
	 * @throws NoUserException                       - if we cannot find the user specified.
	 * @throws NoCredentialsAvailableException       - No credentials are configured on this account so we cannot
	 *                                               authenticate the user.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	User authenticate(String usersEmailAddress, String plainTextPassword)
		throws IncorrectCredentialsProvidedException, NoUserException,
			NoCredentialsAvailableException, SegueDatabaseException;

	/**
	 * Hash a string using a hashing function.
	 *
	 * @param str  - string to hash
	 * @param salt - random string to use as a salt.
	 * @return base64 encoded hash
	 * @throws NoSuchAlgorithmException - if the configured algorithm is not valid.
	 * @throws InvalidKeySpecException  - if the preconfigured key spec is invalid.
	 */
	String hashString(String str, String salt)
		throws NoSuchAlgorithmException, InvalidKeySpecException;
}
