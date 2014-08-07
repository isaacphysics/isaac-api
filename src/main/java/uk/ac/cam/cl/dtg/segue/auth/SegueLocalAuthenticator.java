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

import uk.ac.cam.cl.dtg.segue.auth.exceptions.FailedToHashPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
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
		throws InvalidPasswordException {
		if (null == userWithNewPassword.getPassword()
				|| userWithNewPassword.getPassword().isEmpty()) {
			throw new InvalidPasswordException(
					"Empty passwords are not allowed if using local authentication.");
		}

		try {
			String passwordSalt = generateSalt();
			String hashedPassword = this.hashString(
					userWithNewPassword.getPassword(), passwordSalt);
			userWithNewPassword.setPassword(hashedPassword);
			userWithNewPassword.setSecureSalt(passwordSalt);

		} catch (NoSuchAlgorithmException e) {
			log.error("Error detecting security algorithm", e);
			throw new FailedToHashPasswordException(
					"Security algorithrm configuration error.");
		} catch (InvalidKeySpecException e) {
			log.error("Error building secret key specification", e);
			throw new FailedToHashPasswordException(
					"Security algorithrm configuration error.");
		}
	}

	@Override
	public User authenticate(final String usersEmailAddress,
			final String plainTextPassword)
		throws IncorrectCredentialsProvidedException, 
		NoUserException, 
		NoCredentialsAvailableException {
		if (null == usersEmailAddress) {
			throw new IllegalArgumentException();
		}
		if (null == plainTextPassword) {
			throw new IllegalArgumentException();
		}

		User localUserAccount = userDataManager.getByEmail(usersEmailAddress);

		if (null == localUserAccount) {
			throw new NoUserException();
		}
		if (null == localUserAccount.getPassword() || null == localUserAccount.getSecureSalt()) {
			log.info("No credentials available for this account");
			throw new NoCredentialsAvailableException("This user does not have any"
					+ " local credentials setup.");
		}
		
		try {
			if (this.hashString(plainTextPassword,
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
		// TODO Lost password flow.
		throw new UnsupportedOperationException(
				"This method is not implemented yet.");
	}

	@Override
	public String hashString(final String str, final String salt)
		throws NoSuchAlgorithmException, InvalidKeySpecException {
		char[] strChars = str.toCharArray();
		byte[] saltBytes = salt.getBytes();

		PBEKeySpec spec = new PBEKeySpec(strChars, saltBytes, ITERATIONS,
				KEY_LENGTH);

		SecretKeyFactory key = SecretKeyFactory.getInstance(CRYPTO_ALOGRITHM);
		byte[] hashedString = key.generateSecret(spec).getEncoded();
		return new BigInteger(hashedString).toString();
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
