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

/**
 * Interface representing some kind of hashing service, specially written for Segue.
 */
public interface ISegueHashingAlgorithm {
    /**
     * Hash the plain text password provided with a given salt.
     * @param password - plain text password
     * @param salt - salt
     * @return hashed password
     */
    String hashPassword(final String password, final String salt);

    /**
     * Generate a salt value
     * @return random salt
     */
    String generateSalt();

    /**
     * A unique identifier for this Segue Compatible algorithm.
     * @return hashingAlgorithm name as a string
     */
    String hashingAlgorithmName();

    /**
     * Compute a hash as a byte array
     * @param str - string value
     * @param salt -- salt value
     * @param keyLength - key length
     * @return hash as a byte array
     */
    byte[] computeHash(final String str, final String salt, final int keyLength)
    ;
}
