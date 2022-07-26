/*
 * Copyright 2022 James Sharkey
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
import org.bouncycastle.crypto.generators.SCrypt;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Represents an instance of a hashing scheme used in Segue.
 *
 * This is a parent class for Scrypt versions.
 *
 */
public class SegueSCrypt {
    private final Integer iterations;
    private final Integer blockSize;
    private final Integer parallelismFactor;
    private final Integer keyLength;
    private final String saltingAlgorithm;
    private final Integer saltSize;

    public SegueSCrypt(final Integer iterations, final Integer blockSize, final Integer parallelismFactor,
                       final Integer keyLength, final String saltingAlgorithm, final Integer saltSize) {
        this.iterations = iterations;
        this.blockSize = blockSize;
        this.parallelismFactor = parallelismFactor;
        this.keyLength = keyLength;
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
     */
    public String hashPassword(final String password, final String salt) {

        byte[] hashedPassword = computeHash(password, salt, keyLength);
        return new String(Base64.encodeBase64(hashedPassword));
    }

    /**
     * Compute the hash of a string using the Scrypt hashing function.
     *
     * @param str
     *            - string to hash
     * @param salt
     *            - random string to use as a salt.
     * @param keyLength
     *            - the desired output key length
     * @return a byte array of the hash
     */
    public byte[] computeHash(final String str, final String salt, final int keyLength) {
        byte[] strBytes = str.getBytes();
        byte[] saltBytes = salt.getBytes();

        return SCrypt.generate(strBytes, saltBytes, iterations, blockSize, parallelismFactor, keyLength);
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
