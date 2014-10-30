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
package uk.ac.cam.cl.dtg.segue.api.managers;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.GroupNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.associations.IAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.associations.UserAssociationException;
import uk.ac.cam.cl.dtg.segue.dos.AssociationGroup;
import uk.ac.cam.cl.dtg.segue.dos.AssociationToken;
import uk.ac.cam.cl.dtg.segue.dos.UserAssociation;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import com.google.inject.Inject;

/**
 * UserAssociationManager Responsible for managing user associations, groups and
 * permissions for one user to grant data view rights to another.
 */
public class UserAssociationManager {
	private final IAssociationDataManager database;

	private final int tokenLength = 5;
	
	private static final Logger log = LoggerFactory.getLogger(UserAssociationManager.class);

	/**
	 * UserAssociationManager.
	 * @param database
	 *            - IAssociationDataManager providing access to the database.
	 */
	@Inject
	public UserAssociationManager(final IAssociationDataManager database) {
		this.database = database;
	}

	/**
	 * generate token that other users can use to grant access to their data.
	 * 
	 * @param registeredUser
	 *            - the user who will ultimately receive access to someone
	 *            else's data.
	 * @param associatedGroupId
	 *            - Group id to add user to
	 * @return AssociationToken - that allows another user to grant permission
	 *         to the owner of the token.
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 * @throws GroupNotFoundException - if the group specified does not exist. 
	 */
	public AssociationToken generateToken(final RegisteredUserDTO registeredUser,
			final String associatedGroupId) throws SegueDatabaseException, GroupNotFoundException {
		Validate.notNull(registeredUser);
		
		if (associatedGroupId != null && !database.hasGroup(associatedGroupId)) {
			throw new GroupNotFoundException("Group not found: " + associatedGroupId);
		}
		
		// create some kind of random token
		String token = new String(Base64.encodeBase64(UUID.randomUUID().toString().getBytes())).replace("=",
				"").substring(0, tokenLength);

		AssociationToken associationToken = new AssociationToken(token, registeredUser.getDbId(),
				associatedGroupId);
		
		return database.saveAssociationToken(associationToken);
	}

	/**
	 * createAssociationGroup.
	 * 
	 * @param groupName
	 *            - name describing the group.
	 * @param groupOwner
	 *            - the user who wishes to grant permissions to another.
	 * @return AssociationGroup
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public AssociationGroup createAssociationGroup(final String groupName, final RegisteredUserDTO groupOwner)
		throws SegueDatabaseException {
		Validate.notBlank(groupName);
		Validate.notNull(groupOwner);

		AssociationGroup group = new AssociationGroup(null, groupName, groupOwner.getDbId(), new Date());

		return database.createGroup(group);
	}
	
	/**
	 * getAssociations.
	 * @param user to find associations for.
	 * @return List of all of their associations.
	 */
	public List<UserAssociation> getAssociations(final RegisteredUserDTO user) {
		return database.getUserAssociations(user.getDbId());
	}

	/**
	 * createAssociationWithToken.
	 * 
	 * @param token
	 *            - The token which links to a user (receiving permission) and
	 *            possibly a group.
	 * @param userGrantingPermission
	 *            - the user who wishes to grant permissions to another.
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 * @throws UserAssociationException
	 *             - if we cannot create the association because it is invalid.
	 */
	public void createAssociationWithToken(final String token, final RegisteredUserDTO userGrantingPermission)
		throws SegueDatabaseException, UserAssociationException {
		Validate.notBlank(token);
		Validate.notNull(userGrantingPermission);

		AssociationToken lookedupToken = database.lookupAssociationToken(token);

		if (database.hasValidAssociation(lookedupToken.getOwnerUserId(), userGrantingPermission.getDbId())) {
			throw new UserAssociationException("Association already exists.");
		}

		database.createAssociation(lookedupToken, userGrantingPermission.getDbId());

		if (lookedupToken.getGroupId() != null) {
			database.addUserToGroup(userGrantingPermission.getDbId(), lookedupToken.getGroupId());
			log.info(String.format("Adding User: %s to Group: %s", userGrantingPermission.getDbId(),
					lookedupToken.getGroupId()));
		}
	}

	/**
	 * Revoke user access.
	 * 
	 * @param ownerUser
	 *            - the user who owns the data
	 * @param userToRevoke
	 *            - the user to revoke access too.
	 * @throws SegueDatabaseException
	 *             - If there is a database issue whilst fulfilling the request.
	 */
	public void revokeAssociation(final RegisteredUserDTO ownerUser, final RegisteredUserDTO userToRevoke)
		throws SegueDatabaseException {
		Validate.notNull(ownerUser);
		Validate.notNull(userToRevoke);
		
		database.deleteAssociation(ownerUser.getDbId(), userToRevoke.getDbId());
	}
}
