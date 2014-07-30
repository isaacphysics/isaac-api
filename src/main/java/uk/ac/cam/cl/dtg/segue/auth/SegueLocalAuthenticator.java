package uk.ac.cam.cl.dtg.segue.auth;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.dao.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dos.users.User;

/**
 * Segue Local Authenticator. This provides a mechanism for users to create an
 * account on the Segue CMS without the need to use a 3rd party authenticator.
 * 
 * @author Stephen Cummins
 */
public class SegueLocalAuthenticator implements IPasswordAuthenticator {
	private static final Logger log = LoggerFactory
			.getLogger(SegueLocalAuthenticator.class);
	private final IUserDataManager userDataManager;

	private static final String CRYPTO_ALOGRITHM = "PBKDF2WithHmacSHA1";
	private static final String SALTING_ALGORITHM = "SHA1PRNG";
	private static final Integer ITERATIONS = 1000;
	private static final Integer KEY_LENGTH = 512;
	private static final int SALT_SIZE = 16;

	/**
	 * Creates a segue local authenticator object to validate and create
	 * passwords to be stored by the Segue CMS.
	 * 
	 * @param userDataManager
	 *            - the user data manager which allows us to store and query
	 *            user information.
	 */
	@Inject
	public SegueLocalAuthenticator(final IUserDataManager userDataManager) {
		this.userDataManager = userDataManager;
	}

	@Override
	public AuthenticationProvider getAuthenticationProvider() {
		return AuthenticationProvider.SEGUE;
	}

	@Override
	public void setOrChangeUsersPassword(final User userWithNewPassword)
		throws InvalidPasswordException, FailedToSetPasswordException {
		if (null == userWithNewPassword.getPassword()
				|| userWithNewPassword.getPassword().isEmpty()) {
			throw new InvalidPasswordException(
					"Empty passwords are not allowed if using local authentication.");
		}

		try {
			String passwordSalt = generateSalt();
			String hashedPassword = this.hashPassword(
					userWithNewPassword.getPassword(), passwordSalt);
			userWithNewPassword.setPassword(hashedPassword);
			userWithNewPassword.setSecureSalt(passwordSalt);

		} catch (NoSuchAlgorithmException e) {
			log.error("Error detecting security algorithm", e);
			throw new FailedToSetPasswordException(
					"Security algorithrm configuration error.");
		} catch (InvalidKeySpecException e) {
			log.error("Error building secret key specification", e);
			throw new FailedToSetPasswordException(
					"Security algorithrm configuration error.");
		}
	}

	@Override
	public User authenticate(final String usersEmailAddress,
			final String plainTextPassword)
		throws IncorrectCredentialsProvidedException, 
		NoUserIdException, 
		NoCredentialsAvailableException {
		if (null == usersEmailAddress) {
			throw new NoUserIdException();
		}
		if (null == plainTextPassword) {
			// TODO: create exception to say no password set.
			throw new NoUserIdException();
		}

		User localUserAccount = userDataManager.getByEmail(usersEmailAddress);

		if (null == localUserAccount) {
			throw new NoUserIdException();
		}
		if (null == localUserAccount.getPassword() || null == localUserAccount.getSecureSalt()) {
			log.info("No credentials available for this account");
			throw new NoCredentialsAvailableException("This user does not have any"
					+ " local credentials setup.");
		}
		
		try {
			if (this.hashPassword(plainTextPassword,
					localUserAccount.getSecureSalt()).equals(
					localUserAccount.getPassword())) {
				return localUserAccount;
			} else {
				throw new IncorrectCredentialsProvidedException(
						"Incorrect password");
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
	public void triggerLostPasswordFlow(final String usersEmailAddress) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}
	
	/**
	 * Hash the password using the preconfigured hashing function.
	 * 
	 * @param password
	 *            - password to hash
	 * @param salt
	 *            - random string to use as a salt.
	 * @return the hashed password.
	 * @throws NoSuchAlgorithmException
	 *             - if the configured algorithm is not valid.
	 * @throws InvalidKeySpecException
	 *             - if the preconfigured key spec is invalid.
	 */
	private String hashPassword(final String password, final String salt)
		throws NoSuchAlgorithmException, InvalidKeySpecException {
		char[] passwordChars = password.toCharArray();
		byte[] saltBytes = salt.getBytes();

		PBEKeySpec spec = new PBEKeySpec(passwordChars, saltBytes, ITERATIONS,
				KEY_LENGTH);

		SecretKeyFactory key = SecretKeyFactory.getInstance(CRYPTO_ALOGRITHM);
		byte[] hashedPassword = key.generateSecret(spec).getEncoded();
		return new BigInteger(hashedPassword).toString();
	}

	/**
	 * Helper method to generate a salt.
	 * 
	 * @return generate a secure salt.
	 * @throws NoSuchAlgorithmException
	 *             - problem locating the algorithm.
	 */
	private static String generateSalt() throws NoSuchAlgorithmException {
		SecureRandom sr = SecureRandom.getInstance(SALTING_ALGORITHM);
		byte[] salt = new byte[SALT_SIZE];
		sr.nextBytes(salt);
		return salt.toString();
	}
}
