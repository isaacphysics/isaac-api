package uk.ac.cam.cl.dtg.segue.auth;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.auth.exceptions.FailedToHashPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
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
	private static final Integer SHORT_KEY_LENGTH = 128;
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
	public void setOrChangeUsersPassword(final User userToSetPasswordFor, final String plainTextPassword)
		throws InvalidPasswordException {
		if (null == userToSetPasswordFor.getPassword()
				|| userToSetPasswordFor.getPassword().isEmpty()) {
			throw new InvalidPasswordException(
					"Empty passwords are not allowed if using local authentication.");
		}

		try {
			String passwordSalt = generateSalt();
			String hashedPassword = this.hashPassword(
					plainTextPassword, passwordSalt);
			
			userToSetPasswordFor.setPassword(hashedPassword);
			userToSetPasswordFor.setSecureSalt(passwordSalt);

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
		NoCredentialsAvailableException, SegueDatabaseException {
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
	public String hashString(final String str, final String salt)
		throws NoSuchAlgorithmException, InvalidKeySpecException {
		return new String(Base64.encodeBase64(computeHash(str, salt, SHORT_KEY_LENGTH)));
	}

	/**
	 * Hash the password using the preconfigured hashing function.
	 *
	 * @param password
	 *            - password to hash
	 * @param salt
	 *            - random string to use as a salt.
	 * @return the hashed password
	 * @throws NoSuchAlgorithmException
	 *             - if the configured algorithm is not valid.
	 * @throws InvalidKeySpecException
	 *             - if the preconfigured key spec is invalid.
	 */
	private String hashPassword(final String password, final String salt)
		throws NoSuchAlgorithmException, InvalidKeySpecException {
		return new BigInteger(computeHash(password, salt, KEY_LENGTH)).toString();
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

		PBEKeySpec spec = new PBEKeySpec(strChars, saltBytes, ITERATIONS,
				keyLength);

		SecretKeyFactory key = SecretKeyFactory.getInstance(CRYPTO_ALOGRITHM);
		return key.generateSecret(spec).getEncoded();
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
