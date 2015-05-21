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

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.bson.types.ObjectId;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.mongojack.DBQuery.Query;
import org.mongojack.internal.MongoJackModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoException;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AccountAlreadyLinkedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.DuplicateAccountException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.database.MongoDb;
import uk.ac.cam.cl.dtg.segue.dos.QuestionAttemptUserRecord;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.LinkedAccount;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;

/**
 * This class is responsible for managing and persisting user data.
 * 
 * @author Stephen Cummins
 */
public class MongoUserDataManager implements IUserDataManager {

	private static final Logger log = LoggerFactory
			.getLogger(MongoUserDataManager.class);

	private final MongoDb database;
	private final ContentMapper contentMapper;
	
	private static final String USER_COLLECTION_NAME = "users";
	private static final String LINKED_ACCOUNT_COLLECTION_NAME = "linkedAccounts";
	private static final String QUESTION_ATTEMPTS_COLLECTION_NAME = "questionAttempts";

	private static Boolean initialised = false;
	
	/**
	 * Creates a new user data maanger object.
	 * 
	 * @param database
	 *            - the database reference used for persistence.
	 * @param contentMapper - The preconfigured content mapper for pojo mapping.
	 */
	@Inject
	public MongoUserDataManager(final MongoDb database, final ContentMapper contentMapper) {
		this.database = database;
		this.contentMapper = contentMapper;
		
		if (!initialised) {
			initialiseDataManager();	
		}
	}

	@Override
	public final RegisteredUser createOrUpdateUser(final RegisteredUser user) throws SegueDatabaseException {
		JacksonDBCollection<RegisteredUser, String> jc = JacksonDBCollection.wrap(
				database.getDB().getCollection(USER_COLLECTION_NAME), RegisteredUser.class,
				String.class);

		try {
			WriteResult<RegisteredUser, String> r = jc.save(user);

			if (r.getError() != null) {
				log.error("Error during database update " + r.getError());
				throw new SegueDatabaseException(
						"MongoDB encountered an exception while creating a new user account: " + r.getError());
			}
			
			return r.getSavedObject();	
		} catch (MongoException.DuplicateKey e) {
			throw new DuplicateAccountException("A user with a duplicate key exists in the database.", e);
		} catch (MongoException e) {
			String errorMessage = "MongoDB encountered an exception while attempting to create a user account.";
			log.error(errorMessage, e);
			throw new SegueDatabaseException(errorMessage, e);
		}
	}
	
	@Override
	public void deleteUserAccount(final String id) throws SegueDatabaseException {
		Validate.notBlank(id, "The id field must not be blank.");
		
		JacksonDBCollection<RegisteredUser, String> userCollection = JacksonDBCollection.wrap(
				database.getDB().getCollection(USER_COLLECTION_NAME), RegisteredUser.class,
				String.class);
			
		try {
			WriteResult<RegisteredUser, String> r = userCollection.removeById(id);
			
			if (r.getError() != null) {
				log.error("Error during database update " + r.getError());
				throw new SegueDatabaseException(
						"MongoDB encountered an exception while deleting a user account: " + r.getError());
			}
			
			// tidy up
			this.cleanupOrphanedLinkedAccounts(id);
			
		} catch (MongoException e) {
			String errorMessage = "MongoDB encountered an exception while attempting to delete a user account.";
			log.error(errorMessage, e);
			throw new SegueDatabaseException(errorMessage, e);
		}
	}
	
	@Override
	public final String registerNewUserWithProvider(final RegisteredUser user, final AuthenticationProvider provider,
			final String providerUserId) throws SegueDatabaseException {
		Validate.notNull(user);
		Validate.notNull(provider);
		Validate.notNull(providerUserId);

		// create the users local account.
		RegisteredUser localUser = this.createOrUpdateUser(user);
		String localUserId = localUser.getDbId().toString();
		
		// link the provider account to the newly created account.
		this.linkAuthProviderToAccount(localUser, provider, providerUserId);

		return localUserId;
	}

	@Override
	public RegisteredUser getById(final String id) throws SegueDatabaseException {
		if (null == id) {
			return null;
		}

		// Since we are attaching our own auto mapper we have to do MongoJack configure on it. 
		ObjectMapper objectMapper = contentMapper.getContentObjectMapper();
		MongoJackModule.configure(objectMapper);
				
		JacksonDBCollection<RegisteredUser, String> jc = JacksonDBCollection.wrap(
				database.getDB().getCollection(USER_COLLECTION_NAME), RegisteredUser.class,
				String.class, objectMapper);
		try {
			// Do database query using plain mongodb so we only have to read from
			// the database once.
			RegisteredUser user = jc.findOneById(id);
			
			return user;
		} catch (MongoException e) {
			String errorMessage = "MongoDB encountered an exception while attempting to find a user account by id.";
			log.error(errorMessage, e);
			throw new SegueDatabaseException(errorMessage, e);
		} catch (IllegalArgumentException e) {
			String errorMessage = "The id provided was not found or was illegal.";
			log.error(errorMessage, e);
			throw new SegueDatabaseException(errorMessage, e);
		}
	}

	@Override
	public RegisteredUser getByEmail(final String email) throws SegueDatabaseException {
		if (null == email) {
			return null;
		}

		JacksonDBCollection<RegisteredUser, String> jc = JacksonDBCollection.wrap(
				database.getDB().getCollection(USER_COLLECTION_NAME), RegisteredUser.class,
				String.class);
		try {
			// Do database query using plain mongodb so we only have to read from
			// the database once.
			RegisteredUser user = jc.findOne(new BasicDBObject(
					Constants.LOCAL_AUTH_EMAIL_FIELDNAME, email.trim()));

			return user;
		} catch (MongoException e) {
			String errorMessage = "MongoDB encountered an exception "
					+ "while attempting to find a user account by email address.";
			log.error(errorMessage, e);
			throw new SegueDatabaseException(errorMessage, e);
		}
	}
	
	@Override 
	public List<RegisteredUser> findUsers(final RegisteredUser prototype) throws SegueDatabaseException {
		if (null == prototype) {
			return null;
		}
		
		JacksonDBCollection<RegisteredUser, String> jc = JacksonDBCollection.wrap(
				database.getDB().getCollection(USER_COLLECTION_NAME), RegisteredUser.class,
				String.class);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		MongoJackModule.configure(mapper);
		
		BasicDBObject query = new BasicDBObject(mapper.convertValue(
				prototype, HashMap.class));
		
		DBCursor<RegisteredUser> users = jc.find(query).sort(DBSort.asc("familyName"));
		
		return users.toArray();		
	}
	
	@Override 
	public List<RegisteredUser> findUsers(final List<String> usersToLocate) throws SegueDatabaseException {
		Validate.notNull(usersToLocate);
		// have to have another instance of this as not thread-safe with own mapper.
		JacksonDBCollection<RegisteredUser, String> jc = JacksonDBCollection.wrap(
				database.getDB().getCollection(USER_COLLECTION_NAME), RegisteredUser.class,
				String.class);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		MongoJackModule.configure(mapper);
		
		Query query = null;
		List<Query> idsToFind = Lists.newArrayList();
		
		for (String userId : usersToLocate) {
			idsToFind.add(DBQuery.is("_id", userId));	
		}
		
		query = DBQuery.or(idsToFind.toArray(new Query[usersToLocate.size()]));

		DBCursor<RegisteredUser> users = jc.find(query);
		
		return users.toArray();		
	}

	@Override
	public RegisteredUser getByResetToken(final String token) throws SegueDatabaseException {
		if (null == token) {
			return null;
		}

		JacksonDBCollection<RegisteredUser, String> jc = JacksonDBCollection.wrap(
				database.getDB().getCollection(USER_COLLECTION_NAME), RegisteredUser.class,
				String.class);
		try {
			// Do database query using plain mongodb so we only have to read from
			// the database once.
			RegisteredUser user = jc.findOne(new BasicDBObject(
					Constants.LOCAL_AUTH_RESET_TOKEN_FIELDNAME, token.trim()));

			return user;
		} catch (MongoException e) {
			String errorMessage = "MongoDB encountered an exception "
					+ "while attempting to find a user account by email address.";
			log.error(errorMessage, e);
			throw new SegueDatabaseException(errorMessage, e);
		}
	}

	@Override
	public void registerQuestionAttempt(final String userId,
			final String questionPageId, final String fullQuestionId,
			final QuestionValidationResponse questionAttempt) throws SegueDatabaseException {
		// Since we are attaching our own auto mapper we have to do MongoJack configure on it. 
		ObjectMapper objectMapper = contentMapper.getContentObjectMapper();
		MongoJackModule.configure(objectMapper);
		
		JacksonDBCollection<QuestionAttemptUserRecord, String> jc = JacksonDBCollection.wrap(
				database.getDB().getCollection(QUESTION_ATTEMPTS_COLLECTION_NAME), QuestionAttemptUserRecord.class,
				String.class, objectMapper);

		try {
			BasicDBObject query = new BasicDBObject(
					Constants.USER_ID_FKEY_FIELDNAME, userId);
			
			DBCursor<QuestionAttemptUserRecord> questionAttemptRecord = jc.find(query);
			String questionRecordId = null;
			if (questionAttemptRecord.size() > 1) {
				// multiple records returned so this is ambiguous.
				throw new SegueDatabaseException(
						"Expected to only find one QuestionAttempt for the user, found "
								+ questionAttemptRecord.size());
			} else if (questionAttemptRecord.size() == 0) {
				// create a new QuestionAttemptUserRecord
				WriteResult<QuestionAttemptUserRecord, String> save = jc.save(new QuestionAttemptUserRecord(
						null, userId));
				
				questionRecordId = save.getSavedId();
				
				if (save.getError() != null) {
					log.error("Error during database update " + save.getError());
					throw new SegueDatabaseException(
							"Error occurred whilst trying to create new QuestionAttemptUserRecord: "
									+ save.getError());
				}
			} else {
				// set the question Record Id so that we can modify it for the update. 
				questionRecordId = questionAttemptRecord.toArray().get(0).getId();
			}

			WriteResult<QuestionAttemptUserRecord, String> r = jc.updateById(
					questionRecordId,
					DBUpdate.push(Constants.QUESTION_ATTEMPTS_FIELDNAME + "."
							+ questionPageId + "." + fullQuestionId,
							questionAttempt));
			
			if (r.getError() != null) {
				log.error("Error during database update " + r.getError());
				throw new SegueDatabaseException(
						"Error occurred whilst trying to update the users QuestionAttemptUserRecord: "
								+ r.getError());
			}
			
		} catch (MongoException e) {
			log.error("MongoDB Database Exception. ", e);
		}
	}
	
	@Override
	public QuestionAttemptUserRecord getQuestionAttempts(final String userId)
		throws SegueDatabaseException {
		
		// Since we are attaching our own auto mapper we have to do MongoJack configure on it. 
		ObjectMapper objectMapper = contentMapper.getContentObjectMapper();
		MongoJackModule.configure(objectMapper);
		
		JacksonDBCollection<QuestionAttemptUserRecord, String> jc = JacksonDBCollection.wrap(
				database.getDB().getCollection(QUESTION_ATTEMPTS_COLLECTION_NAME), QuestionAttemptUserRecord.class,
				String.class, objectMapper);

		try {
			BasicDBObject query = new BasicDBObject(
					Constants.USER_ID_FKEY_FIELDNAME, userId);
			
			DBCursor<QuestionAttemptUserRecord> questionAttemptRecord = jc.find(query);
			
			if (questionAttemptRecord.size() > 1) {
				throw new SegueDatabaseException(String.format(
						"Expected to only find one QuestionAttempt for the user (%s), found %s", userId,
						questionAttemptRecord.size()));
			} else if (questionAttemptRecord.size() == 0) {
				return new QuestionAttemptUserRecord();
			}
			
			return questionAttemptRecord.toArray().get(0);
		} catch (MongoException e) {
			log.error("MongoDB Database Exception. ", e);
			throw new SegueDatabaseException("Database error.", e);
		}
	}

	@Override
	public final RegisteredUser getByLinkedAccount(final AuthenticationProvider provider,
			final String providerUserId) throws SegueDatabaseException {
		if (null == provider || null == providerUserId) {
			return null;
		}

		JacksonDBCollection<LinkedAccount, String> jc = JacksonDBCollection
				.wrap(database.getDB().getCollection(LINKED_ACCOUNT_COLLECTION_NAME),
						LinkedAccount.class, String.class);

		LinkedAccount linkAccount = jc.findOne(DBQuery.and(DBQuery.is(
				Constants.LINKED_ACCOUNT_PROVIDER_FIELDNAME, provider), DBQuery
				.is(Constants.LINKED_ACCOUNT_PROVIDER_USER_ID_FIELDNAME,
						providerUserId)));

		if (null == linkAccount) {
			return null;
		}
		
		RegisteredUser registeredUser = this.getById(linkAccount.getLocalUserId());
		
		if (null == registeredUser) {
			log.info("Deleting linked accounts and trying to search again.");
			// this means that a user has been deleted and the link record remains.
			// we should delete the link record as well as any others partaining to this user.
			this.cleanupOrphanedLinkedAccounts(linkAccount.getLocalUserId());
			// Try to find the account again.
			registeredUser = getByLinkedAccount(provider, providerUserId);
		}
		
		return registeredUser;
	}

	@Override
	public boolean hasALinkedAccount(final RegisteredUser user) throws SegueDatabaseException {
		JacksonDBCollection<LinkedAccount, String> jc = JacksonDBCollection
				.wrap(database.getDB().getCollection(LINKED_ACCOUNT_COLLECTION_NAME),
						LinkedAccount.class, String.class);

		BasicDBObject query = new BasicDBObject(
				Constants.LINKED_ACCOUNT_LOCAL_USER_ID_FIELDNAME, user
				.getDbId());
		try {
			DBCursor<LinkedAccount> linkAccounts = jc.find(query);
			
			if (linkAccounts.size() > 0) {
				return true;
			}
			
			return false;
		} catch (MongoException e) {
			String errorMessage = "MongoDB encountered an exception "
					+ "while attempting to find a user's linked accounts";
			log.error(errorMessage, e);
			throw new SegueDatabaseException(errorMessage, e);
		}
	}

	@Override
	public List<AuthenticationProvider> getAuthenticationProvidersByUser(final RegisteredUser user)
		throws SegueDatabaseException {
		Validate.notNull(user);
		Validate.notEmpty(user.getDbId());
		
		JacksonDBCollection<LinkedAccount, String> jc = JacksonDBCollection
				.wrap(database.getDB().getCollection(LINKED_ACCOUNT_COLLECTION_NAME),
						LinkedAccount.class, String.class);
		try {
			BasicDBObject query = new BasicDBObject(
					Constants.LINKED_ACCOUNT_LOCAL_USER_ID_FIELDNAME, user.getDbId());
			DBCursor<LinkedAccount> linkAccounts = jc.find(query);
			
			List<AuthenticationProvider> providersToReturn = Lists.newArrayList();
			for (LinkedAccount accountLinkRecord : linkAccounts) {
				providersToReturn.add(accountLinkRecord.getProvider());
			}
			
			return providersToReturn;
		} catch (MongoException e) {
			String errorMessage = "MongoDB encountered an exception "
					+ "while attempting to find a user's linked accounts";
			log.error(errorMessage, e);
			throw new SegueDatabaseException(errorMessage, e);
		}
	}
	
	@Override
	public void unlinkAuthProviderFromUser(final RegisteredUser user, final AuthenticationProvider provider)
		throws SegueDatabaseException {
		Validate.notNull(user);
		Validate.notNull(user.getDbId());
		Validate.notNull(provider);
		
		JacksonDBCollection<LinkedAccount, String> jc = JacksonDBCollection
				.wrap(database.getDB().getCollection(LINKED_ACCOUNT_COLLECTION_NAME),
						LinkedAccount.class, String.class);
		
		DBQuery.Query linkAccountToDeleteQuery = DBQuery.and(DBQuery.is(
				Constants.LINKED_ACCOUNT_PROVIDER_FIELDNAME, provider), DBQuery
				.is(Constants.LINKED_ACCOUNT_LOCAL_USER_ID_FIELDNAME,
						user.getDbId()));
		
		LinkedAccount linkAccountToDelete = jc.findOne(linkAccountToDeleteQuery);
		
		if (null == linkAccountToDelete) {
			throw new SegueDatabaseException("Unable to locate linkedAccount for deletion.");
		}
		
		jc.removeById(linkAccountToDelete.getId());
	}
	
	@Override
	public boolean linkAuthProviderToAccount(final RegisteredUser user,
			final AuthenticationProvider provider, final String providerUserId) throws SegueDatabaseException {
		JacksonDBCollection<LinkedAccount, String> jc = JacksonDBCollection
				.wrap(database.getDB().getCollection(LINKED_ACCOUNT_COLLECTION_NAME),
						LinkedAccount.class, String.class);
		try {			
			DBQuery.Query existingLinkAccount = DBQuery.and(
					DBQuery.is(Constants.LINKED_ACCOUNT_PROVIDER_USER_ID_FIELDNAME, providerUserId),
					DBQuery.is(Constants.LINKED_ACCOUNT_PROVIDER_FIELDNAME, provider));
			
			LinkedAccount account = jc.findOne(existingLinkAccount);
			
			if (account != null && this.getById(account.getLocalUserId()) == null) {
				this.cleanupOrphanedLinkedAccounts(account.getLocalUserId());
			}
			
			WriteResult<LinkedAccount, String> r = jc.save(new LinkedAccount(null,
					user.getDbId(), provider, providerUserId));

			return null == r.getError();
		} catch (MongoException.DuplicateKey e) {
			throw new AccountAlreadyLinkedException(String.format(
					"This account (%s) has already been linked to a local user for the provider (%s) ",
					user.getEmail(), provider));
		} catch (MongoException e) {
			String errorMessage = "MongoDB encountered an exception "
					+ "while attempting to link an auth provider to a user account.";
			log.error(errorMessage, e);
			throw new SegueDatabaseException(errorMessage, e);
		} 

	}

	@Override
	public void updateUserLastSeen(final String userId) throws SegueDatabaseException {
		this.updateUserLastSeen(userId, new Date());
	}
	
	@Override
	public void updateUserLastSeen(final String userId, final Date date) throws SegueDatabaseException {
		Validate.notBlank(userId);
		// Since we are attaching our own auto mapper we have to do MongoJack configure on it. 
		ObjectMapper objectMapper = contentMapper.getContentObjectMapper();
		MongoJackModule.configure(objectMapper);
				
		JacksonDBCollection<BasicDBObject, String> jc = JacksonDBCollection.wrap(
				database.getDB().getCollection(USER_COLLECTION_NAME), BasicDBObject.class,
				String.class, objectMapper);

		Query q = DBQuery.is("_id", new ObjectId(userId));
		
		BasicDBObject update = new BasicDBObject("$set", new BasicDBObject(
				Constants.USER_LAST_SEEN_FIELDNAME, date));
		
		try {
			WriteResult<BasicDBObject, String> result = jc.update(q, update);

			if (result.getError() != null) {
				throw new SegueDatabaseException("Error response when updating last access date.");
			}
		} catch (MongoException e) {
			String errorMessage = "MongoDB encountered an exception "
					+ "while attempting to link an auth provider to a user account.";
			log.error(errorMessage, e);
			throw new SegueDatabaseException(errorMessage, e);
		}
	}	
	
	/**
	 * This method ensures that the collection is setup correctly and has all of
	 * the required indices.
	 */
	private void initialiseDataManager() {
		log.info("Initializing Mongo DB user collection indices.");
		DB conn = database.getDB();
		conn.getCollection(USER_COLLECTION_NAME).ensureIndex(
				new BasicDBObject(Constants.LOCAL_AUTH_EMAIL_FIELDNAME, 1),
				Constants.LOCAL_AUTH_EMAIL_FIELDNAME, true);

		BasicDBObject linkedAccountIndex = new BasicDBObject();
		linkedAccountIndex.put(Constants.LINKED_ACCOUNT_PROVIDER_USER_ID_FIELDNAME, 1);
		linkedAccountIndex.put(Constants.LINKED_ACCOUNT_PROVIDER_FIELDNAME, 1);
		
		conn.getCollection(LINKED_ACCOUNT_COLLECTION_NAME).ensureIndex(linkedAccountIndex,
				"LinkedAccountIndex", true);
		initialised = true;
	}
	
	/**
	 * This helper method will attempt to remove all linked accounts for a given user id.
	 * It is expected that this will only be used when a user is deleted.
	 * 
	 * @param userId - all linked accounts with this user id will be deleted.
	 */
	private void cleanupOrphanedLinkedAccounts(final String userId) {
		Validate.notBlank(userId);
		
		// verify that the user does not exist
		try {
			if (this.getById(userId) != null) {
				log.error("Unable to clean up accounts as the user is still here.");
				return;
			}
		} catch (SegueDatabaseException e) {
			log.error("Database error during clean up activity.");
		}
		
		JacksonDBCollection<LinkedAccount, String> jc = JacksonDBCollection
				.wrap(database.getDB().getCollection(LINKED_ACCOUNT_COLLECTION_NAME),
						LinkedAccount.class, String.class);
		
		DBQuery.Query linkAccountToDeleteQuery = DBQuery.is(
				Constants.LINKED_ACCOUNT_LOCAL_USER_ID_FIELDNAME, userId);
		
		jc.remove(linkAccountToDeleteQuery);
	}
}
