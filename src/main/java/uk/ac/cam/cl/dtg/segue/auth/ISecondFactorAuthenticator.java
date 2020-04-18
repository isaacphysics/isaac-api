/*
 * Copyright 2020 Stephen Cummins
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

import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.users.TOTPSharedSecret;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

public interface ISecondFactorAuthenticator {

    /**
     * Determines whether the provided user has 2FA configured.
     *
     * @param user to check if 2FA flow applies
     * @return true if it should go through 2FA false if not.
     */
    boolean has2FAConfigured(RegisteredUserDTO user) throws SegueDatabaseException;

    /**
     * Generate a new shared secret and return it so the user can capture it.
     *
     * At this stage the secret is not confirmed against the user.
     *
     * @param user
     *
     * @return secret as a string or bas64 encoded image?
     */
    TOTPSharedSecret getNewSharedSecret(RegisteredUserDTO user);

    /**
     * Verification step.
     *
     * Make sure the user can generate a correct code.
     *
     * After this step 2FA becomes active and required for the user.
     *
     * @param user - user account to turn on 2FA
     * @param sharedSecret - the secret provided to the user
     * @param codeSubmitted - the verification code submitted by the user
     * @return true for success.
     * @throws SegueDatabaseException - if we cannot activate 2FA
     */
    boolean activate2FAForUser(RegisteredUserDTO user, String sharedSecret, Integer codeSubmitted) throws SegueDatabaseException;

    /**
     * Core authentication method for verifying user 2FA code.
     *
     * @param user - user account to check
     * @param codeSubmitted - code submitted
     * @return true for success false / exception for failure
     * @throws IncorrectCredentialsProvidedException - incorrect code provided
     * @throws NoCredentialsAvailableException - user has not configured 2FA
     */
    boolean authenticate2ndFactor(RegisteredUserDTO user, Integer codeSubmitted) throws IncorrectCredentialsProvidedException, NoCredentialsAvailableException, SegueDatabaseException;
}
