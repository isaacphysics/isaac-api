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
package uk.ac.cam.cl.dtg.segue.dao.associations;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBQuery.Query;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.AssociationGroup;
import uk.ac.cam.cl.dtg.segue.dos.AssociationToken;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembership;
import uk.ac.cam.cl.dtg.segue.dos.UserAssociation;

import com.google.inject.Inject;
import com.mongodb.DB;

/**
 * MongoAssociationDataManager.
 * 
 */
public class MongoAssociationDataManager implements IAssociationDataManager {
	private static final String ASSOCIATION_COLLECTION_NAME = "userAssociations";
	private static final String ASSOCIATION_TOKENS_COLLECTION_NAME = "userAssociationsTokens";
	private static final String GROUP_COLLECTION_NAME = "userGroups";
	private static final String GROUP_MEMBERSHIP_COLLECTION_NAME = "groupMemberships";

	private static final Logger log = LoggerFactory.getLogger(MongoAssociationDataManager.class);

	private final DB database;



	/**
	 * PostgresAssociationDataManager.
	 * 
	 * @param database
	 *            - preconfigured connection
	 */
	@Inject
	public MongoAssociationDataManager(final DB database) {
		this.database = database;

		// TODO: Ensure collection indices are created
	}

	@Override
	public AssociationToken saveAssociationToken(final AssociationToken token)
		throws SegueDatabaseException {
		Validate.notNull(token);

		JacksonDBCollection<AssociationToken, String> jacksonCollection = JacksonDBCollection.wrap(
				database.getCollection(ASSOCIATION_TOKENS_COLLECTION_NAME), AssociationToken.class,
				String.class);

		WriteResult<AssociationToken, String> result = jacksonCollection.save(token);
		if (result.getError() != null) {
			log.error("Error during database update " + result.getError());
			throw new SegueDatabaseException(
					"MongoDB encountered an exception while creating a new user account: "
							+ result.getError());
		}

		return result.getSavedObject();
	}

	@Override
	public void createAssociation(final AssociationToken token, final String userIdGrantingAccess)
		throws SegueDatabaseException {
		Validate.notNull(token);

		JacksonDBCollection<UserAssociation, String> associationCollection = JacksonDBCollection.wrap(
				database.getCollection(ASSOCIATION_COLLECTION_NAME), UserAssociation.class, String.class);

		WriteResult<UserAssociation, String> result = associationCollection.save(new UserAssociation(null,
				userIdGrantingAccess, token.getOwnerUserId(), new Date()));

		if (result.getError() != null) {
			throw new SegueDatabaseException("Unable to create association in the database.");
		}

	}

	@Override
	public void deleteAssociation(final String ownerUserId, final String userIdWithAccess)
		throws SegueDatabaseException {
		JacksonDBCollection<UserAssociation, String> associationCollection = JacksonDBCollection.wrap(
				database.getCollection(ASSOCIATION_COLLECTION_NAME), UserAssociation.class, String.class);

		Query query = DBQuery.and(DBQuery.is(Constants.ASSOCIATION_USER_GRANTING_ACCESS, ownerUserId),
				DBQuery.is(Constants.ASSOCIATION_USER_RECEIVING_ACCESS, userIdWithAccess));

		WriteResult<UserAssociation, String> result = associationCollection.remove(query);
		if (result.getError() != null) {
			log.error("Error during database update " + result.getError());
			throw new SegueDatabaseException(
					"MongoDB encountered an exception while deleting an association: " + result.getError());
		}
	}

	@Override
	public boolean hasValidAssociation(final String userIdRequestingAccess, final String ownerUserId) {
		JacksonDBCollection<UserAssociation, String> associationCollection = JacksonDBCollection.wrap(
				database.getCollection(ASSOCIATION_COLLECTION_NAME), UserAssociation.class, String.class);

		Query query = DBQuery.and(DBQuery.is(Constants.ASSOCIATION_USER_GRANTING_ACCESS, ownerUserId),
				DBQuery.is(Constants.ASSOCIATION_USER_RECEIVING_ACCESS, userIdRequestingAccess));

		UserAssociation userAssociation = associationCollection.findOne(query);

		if (null != userAssociation) {
			return true;
		}

		return false;
	}
	
	@Override
	public List<UserAssociation> getUserAssociations(final String userId) {
		Validate.notBlank(userId);
		
		JacksonDBCollection<UserAssociation, String> associationCollection = JacksonDBCollection.wrap(
				database.getCollection(ASSOCIATION_COLLECTION_NAME), UserAssociation.class, String.class);

		Query query = DBQuery.is(Constants.ASSOCIATION_USER_GRANTING_ACCESS, userId);
		
		DBCursor<UserAssociation> results = associationCollection.find(query);

		return results.toArray();
	}

	@Override
	public AssociationGroup createGroup(final AssociationGroup group) throws SegueDatabaseException {
		JacksonDBCollection<AssociationGroup, String> jacksonCollection = JacksonDBCollection.wrap(
				database.getCollection(GROUP_COLLECTION_NAME), AssociationGroup.class, String.class);

		WriteResult<AssociationGroup, String> result = jacksonCollection.save(group);
		if (result.getError() != null) {
			log.error("Error during database update " + result.getError());
			throw new SegueDatabaseException("MongoDB encountered an exception while creating a new group: "
					+ result.getError());
		}

		group.setId(result.getSavedId());
		return group;
	}

	@Override
	public AssociationToken lookupAssociationToken(final String tokenCode) {
		JacksonDBCollection<AssociationToken, String> jacksonCollection = JacksonDBCollection.wrap(
				database.getCollection(ASSOCIATION_TOKENS_COLLECTION_NAME), AssociationToken.class,
				String.class);

		return jacksonCollection.findOne(DBQuery.is(Constants.ASSOCIATION_TOKEN_FIELDNAME, tokenCode));
	}

	@Override
	public void addUserToGroup(final String userId, final String groupId) throws SegueDatabaseException {
		JacksonDBCollection<GroupMembership, String> groupMembershipCollection = JacksonDBCollection
				.wrap(database.getCollection(GROUP_MEMBERSHIP_COLLECTION_NAME), GroupMembership.class,
						String.class);

		WriteResult<GroupMembership, String> result = groupMembershipCollection.save(new GroupMembership(
				null, userId, groupId));

		if (result.getError() != null) {
			log.error("Error during database update " + result.getError());
			throw new SegueDatabaseException("MongoDB encountered an exception while creating a new groupt: "
					+ result.getError());
		}
	}

	@Override
	public boolean hasGroup(final String groupId) {
		JacksonDBCollection<AssociationGroup, String> groupCollection = JacksonDBCollection.wrap(
				database.getCollection(GROUP_COLLECTION_NAME), AssociationGroup.class, String.class);
		AssociationGroup group = groupCollection.findOneById(groupId);

		return group != null;
	}
}
