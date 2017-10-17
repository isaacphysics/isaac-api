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
package uk.ac.cam.cl.dtg.segue.dao.users;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.users.LocalUserCredential;

/**
 * Interface representing an abstract persistence mechanism for passwords.
 */
public interface IPasswordDataManager {

    /**
     * getLocalUserCredential.
     *
     * @param userId - userId to look up.
     * @return LocalUserCredentials pojo
     * @throws SegueDatabaseException - if something goes wrong in the database.
     */
    LocalUserCredential getLocalUserCredential(Long userId) throws SegueDatabaseException;

    /**
     * Get a user by password reset token.
     *
     * @param resetToken
     *            - password reset token
     * @return A localUserCredential object.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */

    LocalUserCredential getLocalUserCredentialByResetToken(String resetToken) throws SegueDatabaseException;

    /**
     * Create or update a credential for a given user.
     *
     * @param credsToSave - the credentials to persist
     * @return the persisted values
     * @throws SegueDatabaseException - if something goes wrong in the database.
     */
    LocalUserCredential createOrUpdateLocalUserCredential(LocalUserCredential credsToSave) throws SegueDatabaseException;
}
