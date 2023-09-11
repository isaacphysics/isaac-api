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

public class SegueSCryptv1 extends SegueSCrypt implements ISegueHashingAlgorithm {
  private static final Integer ITERATIONS = 65536;
  private static final Integer BLOCK_SIZE = 8;
  private static final Integer PARALLELISM_FACTOR = 1;
  private static final Integer KEY_LENGTH = 64;  // bytes
  private static final String SALTING_ALGORITHM = "SHA1PRNG";
  private static final int SALT_SIZE = 16;  // bytes

  public SegueSCryptv1() {
    super(ITERATIONS, BLOCK_SIZE, PARALLELISM_FACTOR, KEY_LENGTH, SALTING_ALGORITHM, SALT_SIZE);
  }

  @Override
  public String hashingAlgorithmName() {
    return "SegueSCryptv1";
  }

}
