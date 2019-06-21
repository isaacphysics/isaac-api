/*
 * Copyright 2019 Stephen Cummins
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
import uk.ac.cam.cl.dtg.segue.dos.users.AnonymousUser;


/**
 * Interface for managing and persisting user specific data in segue.
 * 
 * @author Stephen Cummins
 */
public interface IAnonymousUserDataManager {

    /**
     * Save the anonymous user object in the data store.
     * 
     * @param user
     *            - the user object to persist.
     * 
     * @return user which was saved.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    AnonymousUser storeAnonymousUser(AnonymousUser user) throws SegueDatabaseException;

    /**
     * Delete a anonymous user by id.
     * 
     * @param userToDelete
     *            - the user account id to remove.
     * @throws SegueDatabaseException
     *             if an error occurs
     */
    void deleteAnonymousUser(AnonymousUser userToDelete) throws SegueDatabaseException;

    /**
     * Retrieve and extend the life of an anonymous user in our db.
     * @param id - unique identifier of the user
     * @return anonymous user
     * @throws SegueDatabaseException - if we can't access the database
     */
    AnonymousUser getById(final String id) throws SegueDatabaseException;

    /**
     * Find out how many live anonymous users we have currently in the database.
     *
     * @return count of anonymous users
     */
    Long getCountOfAnonymousUsers() throws SegueDatabaseException;;
}
