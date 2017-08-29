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
 * Represents an instance of a hashing scheme used in Segue.
 * Implemented for backwards compatibility.
 *
 */
public class SeguePBKDF2v1 extends SeguePBKDF2 implements ISegueHashingAlgorithm {
    private static final String CRYPTO_ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final String SALTING_ALGORITHM = "SHA1PRNG";
    private static final Integer ITERATIONS = 1000;
    private static final Integer KEY_LENGTH = 512;
    private static final int SALT_SIZE = 16;

    public SeguePBKDF2v1() {
        super(CRYPTO_ALGORITHM, KEY_LENGTH, ITERATIONS, SALTING_ALGORITHM, SALT_SIZE);
    }

    @Override
    public String hashingAlgorithmName() {
        return "SeguePBKDF2v1";
    }
}
