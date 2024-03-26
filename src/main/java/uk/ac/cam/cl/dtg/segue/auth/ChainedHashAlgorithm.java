/*
 * Copyright 2022 James Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * Represents a chained hashing scheme for protecting insecure hashes.
 *
 * This is an abstract parent class for chained algorithms.
 *
 * If the plaintext password is unknown, we cannot upgrade an insecure hash to a more
 * secure algorithm directly. But we can wrap the insecure hash inside a more secure
 * hash. The stored hash is then a secure one, should it be leaked and brute-force attempts
 * made to find the plaintext, but login attempts using the original password will still
 * function correctly and trigger the usual algorithm upgrade process.
 *
 */
public abstract class ChainedHashAlgorithm implements ISegueHashingAlgorithm {
    private final ISegueHashingAlgorithm insecureInnerAlgorithm;
    private final ISegueHashingAlgorithm secureOuterAlgorithm;

    public ChainedHashAlgorithm(final ISegueHashingAlgorithm insecureInnerAlgorithm, final ISegueHashingAlgorithm secureOuterAlgorithm) {
        this.insecureInnerAlgorithm = insecureInnerAlgorithm;
        this.secureOuterAlgorithm = secureOuterAlgorithm;
    }

    /**
     * Hash the password using the preconfigured hashing functions.
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
    @Override
    public String hashPassword(final String password, final String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String innerHash = insecureInnerAlgorithm.hashPassword(password, salt);
        return secureOuterAlgorithm.hashPassword(innerHash, salt);
    }

    /**
     * Helper method to generate a base64 encoded salt.
     *
     * @return generate a base64 encoded secure salt.
     * @throws NoSuchAlgorithmException
     *             - problem locating the algorithm.
     */
    @Override
    public String generateSalt() throws NoSuchAlgorithmException {
        return secureOuterAlgorithm.generateSalt();
    }

    /**
     * Helper method to generate the new secure hash from the old insecure hash.
     *
     * @param insecureAlgorithmName the algorithm used for the insecure hash, to check it is correct for the class.
     * @param insecureHash the insecure hash using the old algorithm.
     * @param salt the secure salt.
     * @return the new hash using the secure algorithm.
     * @throws NoSuchAlgorithmException if the secure algorithm is invalid.
     * @throws InvalidKeySpecException if the configuration of the secure algorithm is incorrect.
     */
    public String upgradeHash(final String insecureAlgorithmName, final String insecureHash, final String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (!insecureInnerAlgorithm.hashingAlgorithmName().equals(insecureAlgorithmName)) {
            throw new UnsupportedOperationException(
                    String.format("The algorithm %s is not supported by this class, expected %s!",
                        insecureAlgorithmName, insecureInnerAlgorithm.hashingAlgorithmName())
            );
        }
        return secureOuterAlgorithm.hashPassword(insecureHash, salt);
    }
}
