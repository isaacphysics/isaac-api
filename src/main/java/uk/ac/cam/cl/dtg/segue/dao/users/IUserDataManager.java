/**
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

import java.util.List;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.QuestionAttemptUserRecord;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;

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
	String registerNewUserWithProvider(final RegisteredUser user, final AuthenticationProvider provider,
			final String providerUserId) throws SegueDatabaseException;
	
	/**
	 * Determine whether the user has at least one linked account. 
	 * @param user with a valid id.
	 * @return true if we can find at least one linked account, false if we can't.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	boolean hasALinkedAccount(RegisteredUser user) throws SegueDatabaseException;
	
	/**
	 * GetAllLinked Accounts by user.
	 * 
	 * @param user - the user DO to search for.
	 * @return List of authentication providers or an empty list.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	List<AuthenticationProvider> getAuthenticationProvidersByUser(final RegisteredUser user) throws SegueDatabaseException;
	
	/**
	 * Find a user by their linked account information.
	 * 
	 * @param provider
	 *            - the provider that has authenticated the user.
	 * @param providerUserId
	 *            - the provider specific unique user id.
	 * @return a full populated user object based on the provider authentication
	 *         information given.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	RegisteredUser getByLinkedAccount(final AuthenticationProvider provider,
			final String providerUserId) throws SegueDatabaseException;
	
	/**
	 * Creates a link record, connecting a local user to an external provider
	 * for authentication purposes.
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
	boolean linkAuthProviderToAccount(final RegisteredUser user,
			final AuthenticationProvider provider, final String providerUserId) throws SegueDatabaseException;
	
	/**
	 * Unlink providerFromUser.
	 * 
	 * This will delete the entry in the linkedAccounts table and prevent a user
	 * from authenticating using that linked account in the future.
	 * 
	 * Note: It is best practice to make sure the user can login with some other
	 * means before doing this.
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
	 * Get a user by local Id.
	 * 
	 * @param id
	 *            - local user id.
	 * @return A user object.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	RegisteredUser getById(final String id) throws SegueDatabaseException;

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
	 * Get a user by password reset token.
	 *
	 * @param token
	 *            - password reset token
	 * @return A user object.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	RegisteredUser getByResetToken(final String token) throws SegueDatabaseException;

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
	 * Update a particular field on a user object.
	 * 
	 * @param userId
	 *            - the user id to try and find.
	 * @param questionPageId
	 *            - the high level id of the question page. This may be used for
	 *            determining whether a page of questions has been completed.
	 * @param fullQuestionId
	 *            - the full id of the question.
	 * @param questionAttempt
	 *            - the question attempt object recording the users result.
	 * @throws SegueDatabaseException - if there is an error during the database operation.            
	 */
	void registerQuestionAttempt(final String userId, final String questionPageId,
			final String fullQuestionId, final QuestionValidationResponseDTO questionAttempt)
		throws SegueDatabaseException;

	/**
	 * Get a users question attempts.
	 * 
	 * @param userId
	 *            - the id of the user to search for.
	 * @return the questionAttempts map or an empty map if the user has not yet
	 *         registered any attempts.
	 * @throws SegueDatabaseException - If there is a database error.
	 */
	QuestionAttemptUserRecord getQuestionAttempts(final String userId)
		throws SegueDatabaseException;
}
