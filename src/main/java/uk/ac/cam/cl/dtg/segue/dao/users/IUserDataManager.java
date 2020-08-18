/*
 * Copyright 2014 Stephen Cummins
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

import uk.ac.cam.cl.dtg.segue.api.Constants.SchoolInfoStatus;
import uk.ac.cam.cl.dtg.segue.api.Constants.TimeInterval;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.UserAuthenticationSettings;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Interface for managing and persisting user specific data in segue.
 * 
 * @author Stephen Cummins
 */
public interface IUserDataManager {

    /**
     * Register a user in the local data repository.
     * 
     * @param user
     *            - the user object to persist
     * @param provider
     *            - the provider that has authenticated the user.
     * @param providerUserId
     *            - the provider specific unique user id.
     * @return the local users id.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    RegisteredUser registerNewUserWithProvider(final RegisteredUser user, final AuthenticationProvider provider,
            final String providerUserId) throws SegueDatabaseException;

    /**
     * Determine whether the user has at least one linked account.
     * 
     * @param user
     *            with a valid id.
     * @return true if we can find at least one linked account, false if we can't.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    boolean hasALinkedAccount(RegisteredUser user) throws SegueDatabaseException;

    /**
     * GetAllLinked Accounts by user.
     * 
     * @param user
     *            - the user DO to search for.
     * @return List of authentication providers or an empty list.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    List<AuthenticationProvider> getAuthenticationProvidersByUser(final RegisteredUser user)
            throws SegueDatabaseException;

    /**
     * Get all the linked accounts by users in a list.
     *
     * @param users
     *             - the list of DOs to search for.
     * @return List of authentication providers (or empty list) per user.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    Map<RegisteredUser, List<AuthenticationProvider>> getAuthenticationProvidersByUsers(final List<RegisteredUser> users)
            throws SegueDatabaseException;

    /**
     * Get UserAuthenticationSettings Object
     * This object provides information on how a user can login based on linked accounts and if they have a Segue account
     *
     * @param userId - user of interest
     * @return UserAuthenticationSettings DO
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    UserAuthenticationSettings getUserAuthenticationSettings(Long userId) throws SegueDatabaseException;

    /**
     * Get whether a list of users have a Segue account.
     *
     * @param users
     *             - the list fo DOs to search for.
     * @return List of Segue account existence information.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    Map<RegisteredUser, Boolean> getSegueAccountExistenceByUsers(final List<RegisteredUser> users)
            throws SegueDatabaseException;

    /**
     * Find a user by their linked account information.
     * 
     * @param provider
     *            - the provider that has authenticated the user.
     * @param providerUserId
     *            - the provider specific unique user id.
     * @return a full populated user object based on the provider authentication information given.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    RegisteredUser getByLinkedAccount(final AuthenticationProvider provider, final String providerUserId)
            throws SegueDatabaseException;

    /**
     * Creates a link record, connecting a local user to an external provider for authentication purposes.
     * 
     * @param user
     *            - the local user object
     * @param provider
     *            - the provider that authenticated the user.
     * @param providerUserId
     *            - the providers unique id for the user.
     * @return true if success false if failure.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    boolean linkAuthProviderToAccount(final RegisteredUser user, final AuthenticationProvider provider,
            final String providerUserId) throws SegueDatabaseException;

    /**
     * Unlink providerFromUser.
     * 
     * This will delete the entry in the linkedAccounts table and prevent a user from authenticating using that linked
     * account in the future.
     * 
     * Note: It is best practice to make sure the user can login with some other means before doing this.
     * 
     * @param user
     *            - The user to use as a search term.
     * @param provider
     *            - the provider to search for.
     * @throws SegueDatabaseException
     *             - if we have a problem accessing the database.
     */
    void unlinkAuthProviderFromUser(final RegisteredUser user, final AuthenticationProvider provider)
            throws SegueDatabaseException;

    /**
     * @param id user id
     * @return the user the matches
     * @throws SegueDatabaseException - if there is a database problem.
     */
    RegisteredUser getById(Long id) throws SegueDatabaseException;

    /**
     * This function will also include tombstoned results back to the caller.
     * WARNING- Do not expect complete RegisteredUser Objects as data may be missing.
     *
     * @param id user id.
     * @param includeDeletedUsers true will allow inclusion of tombstoned users false will filter them out.
     * @return the user the matches
     * @throws SegueDatabaseException - if there is a database problem.
     */
    RegisteredUser getById(Long id, boolean includeDeletedUsers) throws SegueDatabaseException;

    /**
     * Get a user by email.
     *
     * @param email
     *            - local user email address.
     * @return A user object.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    RegisteredUser getByEmail(final String email) throws SegueDatabaseException;

    /**
     * Find users by a prototype.
     * 
     * @param prototype
     *            - a user prototype that can be used for matching fields.
     * @return list of users
     * @throws SegueDatabaseException
     *             if there is a database error.
     */
    List<RegisteredUser> findUsers(RegisteredUser prototype) throws SegueDatabaseException;

    /**
     * Bulk find users based on ids.
     * 
     * @param usersToLocate
     *            - user ids as a list.
     * @return a List of Registered Users.
     * @throws SegueDatabaseException
     *             - if there is a problem with the database.
     */
    List<RegisteredUser> findUsers(List<Long> usersToLocate) throws SegueDatabaseException;

    /**
     * Get a user by email verification token.
     *
     * @param token
     *            - password reset token
     * @return A user object.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    RegisteredUser getByEmailVerificationToken(final String token) throws SegueDatabaseException;

    /**
     * Update user object in the data store.
     * 
     * @param user
     *            - the user object to persist.
     * 
     * @return user which was saved.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    RegisteredUser createOrUpdateUser(RegisteredUser user) throws SegueDatabaseException;

    /**
     * Delete a user account by id.
     * 
     * @param userToDelete
     *            - the user account id to remove.
     * @throws SegueDatabaseException
     *             if an error occurs
     */
    void deleteUserAccount(final RegisteredUser userToDelete) throws SegueDatabaseException;

    /**
     * A method that will allow us to measure how active a user's account is.
     * 
     * @param user
     *            to update.
     * @throws SegueDatabaseException
     *             if an error occurs
     */
    void updateUserLastSeen(final RegisteredUser user) throws SegueDatabaseException;

    /**
     * A method that will allow us to measure how active a user's account is.
     * 
     * @param user
     *            to update.
     * @param date
     *            to use.
     * @throws SegueDatabaseException
     *             if an error occurs
     */
    void updateUserLastSeen(final RegisteredUser user, final Date date) throws SegueDatabaseException;

    /**
     * Increment the session token of a user object in the data store.
     *
     * @param user
     *            - the user object to update the session token of.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    void incrementSessionToken(RegisteredUser user) throws SegueDatabaseException;

    /**
     * Count all the users by role and return a map
     * @return map of user role to integers
     * @throws SegueDatabaseException
     *             - if there is a problem with the database.
     */
    Map<Role, Long> getRoleCount() throws SegueDatabaseException;

    /**
     * Count the users by role seen over the previous time interval
     * @param timeInterval time interval over which to count
     * @return map of counts for each role
     * @throws SegueDatabaseException
     *             - if there is a problem with the database.
     */
    Map<Role, Long> getRolesLastSeenOver(TimeInterval timeInterval) throws SegueDatabaseException;

    /**
     * Count users' reported genders
     * @return map of counts for each gender.
     * @throws SegueDatabaseException
     *             - if there is a problem with the database.
     */
    Map<Gender, Long> getGenderCount() throws SegueDatabaseException;

    /**
     * Count users' reported school information
     * @return map of counts for students who have provided or not provided school information
     * @throws SegueDatabaseException
     *             - if there is a problem with the database.
     */
    Map<SchoolInfoStatus, Long> getSchoolInfoStats() throws SegueDatabaseException;
}
