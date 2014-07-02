package uk.ac.cam.cl.dtg.segue.dao;

import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import com.google.inject.Inject;
import com.mongodb.DB;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dto.users.LinkedAccount;
import uk.ac.cam.cl.dtg.segue.dto.users.User;

/**
 * This class is responsible for managing and persisting user data.
 *
 */
public class UserDataManager implements IUserDataManager {

	private final DB database;
	private static final String USER_COLLECTION_NAME = "users";
	private static final String LINKED_ACCOUNT_COLLECTION_NAME = "linkedAccounts";

	@Inject
	public UserDataManager(DB database) {
		this.database = database;
	}

	@Override
	public String register(User user, AuthenticationProvider provider,
			String providerId) {
		JacksonDBCollection<User, String> jc = JacksonDBCollection.wrap(
				database.getCollection(USER_COLLECTION_NAME), User.class,
				String.class);

		// ensure userId is empty as if this is a registration then it should
		// get a new id.
		user.setDbId(null);
		WriteResult<User, String> r = jc.save(user);

		User localUser = r.getSavedObject();
		String localUserId = r.getDbObject().get("_id").toString();

		// link the provider account to the newly created account.
		this.linkAuthProviderToAccount(localUser, provider, providerId);

		return localUserId;
	}

	@Override
	public User getById(String id) throws IllegalArgumentException {
		if (null == id) {
			return null;
		}

		JacksonDBCollection<User, String> jc = JacksonDBCollection.wrap(
				database.getCollection(USER_COLLECTION_NAME), User.class,
				String.class);

		// Do database query using plain mongodb so we only have to read from
		// the database once.
		User user = jc.findOneById(id);

		return user;
	}

	/**
	 * This method expects the linked account object to not have a local user id
	 * set but to have a provider and provider id.
	 * 
	 * @param account
	 * @return the local user details for the user specified. Or null if we don't know about them. 
	 */
	@Override
	public User getByLinkedAccount(AuthenticationProvider provider,
			String providerUserId) {
		if (null == provider || null == providerUserId) {
			return null;
		}

		JacksonDBCollection<LinkedAccount, String> jc = JacksonDBCollection
				.wrap(database.getCollection(LINKED_ACCOUNT_COLLECTION_NAME),
						LinkedAccount.class, String.class);

		LinkedAccount linkAccount = jc.findOne(DBQuery.and(
				DBQuery.is("provider", provider),
				DBQuery.is("providerUserId", providerUserId)));

		if (null == linkAccount) {
			return null;
		}

		return this.getById(linkAccount.getLocalUserId());
	}

	public boolean linkAuthProviderToAccount(User user,
			AuthenticationProvider provider, String providerId) {
		JacksonDBCollection<LinkedAccount, String> jc = JacksonDBCollection
				.wrap(database.getCollection(LINKED_ACCOUNT_COLLECTION_NAME),
						LinkedAccount.class, String.class);

		WriteResult<LinkedAccount, String> r = jc.save(new LinkedAccount(null,
				user.getDbId(), provider, providerId));

		if (r.getError() == null) {
			return true;
		} else {
			return false;
		}
	}

}
