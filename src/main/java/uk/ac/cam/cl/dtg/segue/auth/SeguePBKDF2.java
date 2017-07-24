/*
 * Copyright 2017 Stephen Cummins
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

import org.apache.commons.codec.binary.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

/**
 * Represents an instance of a hashing scheme used in Segue.
 *
 * This is a parent class for PBKDF2 algorithms.
 *
 */
public class SeguePBKDF2 {
    private final String algorithm;
    private final Integer keyLength;
    private final Integer iterations;
    private final String saltingAlgorithm;
    private final Integer saltSize;

    public SeguePBKDF2(String algorithm, Integer keyLength, Integer iterations, String saltingAlgorithm, Integer saltSize) {

        this.algorithm = algorithm;
        this.keyLength = keyLength;
        this.iterations = iterations;
        this.saltingAlgorithm = saltingAlgorithm;
        this.saltSize = saltSize;
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
    public String hashPassword(final String password, final String salt) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        BigInteger hashedPassword = new BigInteger(computeHash(password, salt, keyLength));

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
    public byte[] computeHash(final String str, final String salt, final int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        char[] strChars = str.toCharArray();
        byte[] saltBytes = salt.getBytes();

        PBEKeySpec spec = new PBEKeySpec(strChars, saltBytes, iterations, keyLength);

        SecretKeyFactory key = SecretKeyFactory.getInstance(algorithm);
        return key.generateSecret(spec).getEncoded();
    }

    /**
     * Helper method to generate a base64 encoded salt.
     *
     * @return generate a base64 encoded secure salt.
     * @throws NoSuchAlgorithmException
     *             - problem locating the algorithm.
     */
    public String generateSalt() throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance(saltingAlgorithm);

        byte[] salt = new byte[saltSize];
        sr.nextBytes(salt);

        return new String(Base64.encodeBase64(salt));
    }
}
