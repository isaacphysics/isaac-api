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
package uk.ac.cam.cl.dtg.segue.dao.users;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.users.TOTPSharedSecret;

/**
 * Interface representing an abstract persistence mechanism for 2FA secrets.
 */
public interface ITOTPDataManager {

    /**
     * get2FASharedSecret.
     *
     * @param userId - userId to look up.
     * @return TOTPSharedSecret pojo
     * @throws SegueDatabaseException - if something goes wrong in the database.
     */
    TOTPSharedSecret get2FASharedSecret(Long userId) throws SegueDatabaseException;

    /**
     * save2FASharedSecret.
     *
     * @param userId - userId to attach to.
     * @param secret - shared secret to store
     * @return TOTPSharedSecret stored pojo
     * @throws SegueDatabaseException - if something goes wrong in the database.
     */
    TOTPSharedSecret save2FASharedSecret(Long userId, TOTPSharedSecret secret) throws SegueDatabaseException;

    /**
     * Remove all MFA information for an account thereby making it appear as they never configured it.
     *
     * @param userId - userId to affect
     * @throws SegueDatabaseException - if something goes wrong in the database.
     */
    void delete2FACredentials(Long userId) throws SegueDatabaseException;

}
