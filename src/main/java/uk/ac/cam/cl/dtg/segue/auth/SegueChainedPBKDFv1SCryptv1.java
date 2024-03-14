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

/**
 *  A hashing algorithm for blind-upgrading SeguePBKDFv1 hashes to use SegueSCryptv1.
 */
public class SegueChainedPBKDFv1SCryptv1 extends ChainedHashAlgorithm implements ISegueHashingAlgorithm {

    public SegueChainedPBKDFv1SCryptv1() {
        super(new SeguePBKDF2v1(), new SegueSCryptv1());
    }

    @Override
    public String hashingAlgorithmName() {
        return "SegueChainedPBKDFv1SCryptv1";
    }
}
