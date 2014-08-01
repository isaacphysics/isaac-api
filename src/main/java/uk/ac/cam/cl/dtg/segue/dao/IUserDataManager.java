package uk.ac.cam.cl.dtg.segue.dao;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dos.users.User;
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
	 */
	String register(final User user, final AuthenticationProvider provider,
			final String providerUserId);
	
	/**
	 * Determine whether the user has at least one linked account. 
	 * @param user with a valid id.
	 * @return true if we can find at least one linked account, false if we can't.
	 */
	boolean hasALinkedAccount(User user);
	
	/**
	 * Find a user by their linked account information.
	 * 
	 * @param provider
	 *            - the provider that has authenticated the user.
	 * @param providerUserId
	 *            - the provider specific unique user id.
	 * @return a full populated user object based on the provider authentication
	 *         information given.
	 */
	User getByLinkedAccount(final AuthenticationProvider provider,
			final String providerUserId);

	/**
	 * Get a user by local Id.
	 * 
	 * @param id
	 *            - local user id.
	 * @return A user object.
	 */
	User getById(final String id);

	/**
	 * Get a user by email.
	 * 
	 * @param email
	 *            - local user id.
	 * @return A user object.
	 */
	User getByEmail(final String email);

	/**
	 * Update user object in the data store.
	 * 
	 * @param user
	 *            - the user object to persist.
	 *            
	 * @return user which was saved.
	 */
	User updateUser(User user);

	/**
	 * Update a particular field on a user object.
	 * 
	 * @param user
	 *            - the user object containing the users local id.
	 * @param questionPageId
	 *            - the high level id of the question page. This may be used for
	 *            determining whether a page of questions has been completed.
	 * @param fullQuestionId
	 *            - the full id of the question.
	 * @param questionAttempt
	 *            - the question attempt object recording the users result.
	 */
	void registerQuestionAttempt(final User user, final String questionPageId,
			final String fullQuestionId, final QuestionValidationResponseDTO questionAttempt);

}
