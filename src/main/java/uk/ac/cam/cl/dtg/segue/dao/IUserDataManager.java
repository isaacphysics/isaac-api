package uk.ac.cam.cl.dtg.segue.dao;

import java.util.List;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dto.users.User;

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
	 * Update user object in the data store.
	 * 
	 * @param user - the user object to persist.
	 */
	void updateUser(User user);

	/**
	 * Add item to user's field list.
	 * 
	 * @param user - user to update
	 * @param key - field to update
	 * @param value - value to replace field with.
	 */
	void addItemToListField(User user, String key, List value);

	/**
	 * Update a particular field on a user object.
	 * 
	 * @param user - the user object containing the users local id.
	 * @param field - the name of the field that we want to update.
	 * @param mapKey - The key to add to the map.
	 * @param value - the object to use as the value for the field. Must be serializable.
	 */
	void addItemToMapField(User user, String field, String mapKey, Object value);
}
