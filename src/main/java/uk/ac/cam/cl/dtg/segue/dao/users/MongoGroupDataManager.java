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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.mongojack.DBQuery.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembership;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;

/**
 * MongoAssociationDataManager.
 * 
 */
public class MongoGroupDataManager implements IUserGroupDataManager {
	private static final String GROUP_COLLECTION_NAME = "userGroups";
	private static final String GROUP_MEMBERSHIP_COLLECTION_NAME = "groupMemberships";
	private static final String GROUP_NAME_FIELD = "groupName";
	
	private static final Logger log = LoggerFactory.getLogger(MongoGroupDataManager.class);

	private final DB database;
	private final JacksonDBCollection<UserGroup, String> groupCollection;
	private final JacksonDBCollection<GroupMembership, String> groupMembershipCollection;

	/**
	 * PostgresAssociationDataManager.
	 * 
	 * @param database
	 *            - preconfigured connection
	 */
	@Inject
	public MongoGroupDataManager(final DB database) {
		this.database = database;
		
		groupCollection = JacksonDBCollection.wrap(this.database.getCollection(GROUP_COLLECTION_NAME),
				UserGroup.class, String.class);
		groupMembershipCollection = JacksonDBCollection
				.wrap(this.database.getCollection(GROUP_MEMBERSHIP_COLLECTION_NAME), GroupMembership.class,
						String.class);
	}

	@Override
	public UserGroup createGroup(final UserGroup group) throws SegueDatabaseException {
		WriteResult<UserGroup, String> result = groupCollection.save(group);
		if (result.getError() != null) {
			log.error("Error during database update " + result.getError());
			throw new SegueDatabaseException("MongoDB encountered an exception while creating a new group: "
					+ result.getError());
		}

		group.setId(result.getSavedId());
		return group;
	}

	@Override
	public UserGroup editGroup(final UserGroup group) throws SegueDatabaseException {
		WriteResult<UserGroup, String> result = groupCollection.updateById(group.getId(), group);
		if (result.getError() != null) {
			log.error("Error during database update " + result.getError());
			throw new SegueDatabaseException("MongoDB encountered an exception while editing a new group: "
					+ result.getError());
		}

		return group;
	}
	
	@Override
	public void addUserToGroup(final String userId, final String groupId) throws SegueDatabaseException {
		WriteResult<GroupMembership, String> result = groupMembershipCollection.save(new GroupMembership(
				null, groupId, userId));

		if (result.getError() != null) {
			log.error("Error during database update " + result.getError());
			throw new SegueDatabaseException("MongoDB encountered an exception while creating a new group: "
					+ result.getError());
		}
	}

	@Override
	public void removeUserFromGroup(final String userId, final String groupId) throws SegueDatabaseException {
		Query query = DBQuery.and(DBQuery.is(Constants.GROUP_FK, groupId),
				DBQuery.is(Constants.USER_ID_FKEY_FIELDNAME, userId));

		WriteResult<GroupMembership, String> result = groupMembershipCollection.remove(query);

		if (result.getError() != null) {
			log.error("Error during database update " + result.getError());
			throw new SegueDatabaseException(
					"MongoDB encountered an exception while removing a user from a group: "
							+ result.getError());
		}
	}

	@Override
	public List<UserGroup> getGroupsByOwner(final String ownerUserId) {
		Query query = DBQuery.is(Constants.OWNER_USER_ID_FKEY_FIELDNAME, ownerUserId);

		DBCursor<UserGroup> result = groupCollection.find(query).sort(new BasicDBObject(GROUP_NAME_FIELD , 1));

		return result.toArray();
	}
	
	@Override
	public UserGroup findById(final String groupId) {
		return groupCollection.findOneById(groupId);
	}
	
	@Override
	public void deleteGroup(final String groupId) throws SegueDatabaseException {
		Query query = DBQuery.is(Constants.GROUP_FK, groupId);

		WriteResult<GroupMembership, String> cascadeDeleteResult = groupMembershipCollection.remove(query);
		
		if (cascadeDeleteResult.getError() != null) {
			throw new SegueDatabaseException(
					"Unable to delete group from database - failed on deleting membership information.");
		}

		WriteResult<UserGroup, String> result = groupCollection.removeById(groupId);
		
		if (result.getError() != null) {
			throw new SegueDatabaseException("Unable to delete group from database");
		}
	}

	@Override
	public List<String> getGroupMemberIds(final String groupId) {
		Query query = DBQuery.is(Constants.GROUP_FK, groupId);
		
		DBCursor<GroupMembership> results = groupMembershipCollection.find(query);
		
		List<String> userIds = Lists.newArrayList();
		for (GroupMembership gm : results.toArray()) {
			userIds.add(gm.getUserId());
		}
		
		return userIds;
	}

	@Override
	public Collection<UserGroup> getGroupMembershipList(final String userId) throws SegueDatabaseException {
		Query query = DBQuery.is(Constants.USER_ID_FKEY_FIELDNAME, userId);
		
		DBCursor<GroupMembership> results = groupMembershipCollection.find(query);
		
		Set<UserGroup> groups = Sets.newHashSet();
		for (GroupMembership gm : results.toArray()) {
			UserGroup group = this.findById(gm.getGroupId());
			if (group != null) {
				groups.add(group);	
			}
		}
		
		return groups;
	}
}
