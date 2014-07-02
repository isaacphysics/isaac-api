package uk.ac.cam.cl.dtg.segue.dao;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dto.users.User;

/**
 * Interface for managing and persisting user specific data in segue. 
 * @author Stephen Cummins
 */
public interface IUserDataManager {

	/**
	 * Register a user in the local data repository.
	 * 
	 * @param user - the user object to persist
	 * @param provider - the provider that has authenticated the user.
	 * @param providerUserId - the provider specific unique user id.
	 * @return the local users id.
	 */
	String register(final User user, final AuthenticationProvider provider,
			final String providerUserId);

	/**
	 * 
	 * @param provider - the provider that has authenticated the user.
	 * @param providerUserId - the provider specific unique user id.
	 * @return a full populated user object based on the provider authentication information given.
	 */
	User getByLinkedAccount(final AuthenticationProvider provider,
			final String providerUserId);

	
	/**
	 * Get a user by local Id.
	 * @param id - local user id.
	 * @return A user object.
	 */
	User getById(final String id);
}
