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

import java.security.SecureRandom;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.InvalidUserAssociationTokenException;
import uk.ac.cam.cl.dtg.segue.dao.associations.UserGroupNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.associations.IAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dos.AssociationToken;
import uk.ac.cam.cl.dtg.segue.dos.UserAssociation;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

import com.google.inject.Inject;

/**
 * UserAssociationManager Responsible for managing user associations, groups and permissions for one user to grant data
 * view rights to another.
 */
public class UserAssociationManager {
    private static final Logger log = LoggerFactory.getLogger(UserAssociationManager.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int tokenLength = 6;
    
    private final IAssociationDataManager associationDatabase;
    private final GroupManager userGroupManager;

    /**
     * UserAssociationManager.
     * 
     * @param associationDatabase
     *            - IAssociationDataManager providing access to the database.
     * @param userGroupManager
     *            - IAssociationDataManager providing access to the database.
     */
    @Inject
    public UserAssociationManager(final IAssociationDataManager associationDatabase, 
            final GroupManager userGroupManager) {
        this.associationDatabase = associationDatabase;
        this.userGroupManager = userGroupManager;
        log.debug("Creating an instance of the UserAssociationManager.");
    }

    /**
     * generate and save a token that other users can use to grant access to their data.
     * 
     * @param registeredUser
     *            - the user who will ultimately receive access to someone else's data.
     * @param associatedGroupId
     *            - Group id to add user to
     * @return AssociationToken - that allows another user to grant permission to the owner of the token.
     * @throws SegueDatabaseException
     *             - If an error occurred while interacting with the database.
     * @throws UserGroupNotFoundException
     *             - if the group specified does not exist.
     */
    public AssociationToken generateAssociationToken(final RegisteredUserDTO registeredUser,
            final Long associatedGroupId) throws SegueDatabaseException, UserGroupNotFoundException {
        Validate.notNull(registeredUser);

        if (associatedGroupId != null) {
            if (!userGroupManager.isValidGroup(associatedGroupId)) {
                throw new UserGroupNotFoundException("Group not found: " + associatedGroupId);
            }

            AssociationToken groupToken = this.associationDatabase.getAssociationTokenByGroupId(associatedGroupId);
            if (groupToken != null) {
                return groupToken;
            }
        }

        // create some kind of random token and remove ambiguous characters.
        String token = newToken();

        AssociationToken associationToken = new AssociationToken(token, registeredUser.getId(),
                associatedGroupId);

        return associationDatabase.saveAssociationToken(associationToken);
    }

    /**
     * Generate a new string value for a token.
     *
     * There are 30 non-ambiguous uppercase alphanumeric characters. We use a 5 bit encoding system to generate the
     * number for which character to choose, skipping in the 1/16 chance it is outside the allowed range. Loop until we
     * generate all tokenLength required characters.
     *
     * @return String - the authentication token in string form.
     *
     */
    private static String newToken() {
        // Allow the following character to appear in tokens, removing ambiguous ones:
        String tokenCharMap = "ABCDEFGHJKLMNPQRTUVWXYZ2346789";

        char[] authToken = new char[tokenLength];

        int index = 0;  // Where we are in the token.
        int shift = 0;  // Where we are in the random 32 bit integer.
        int randomBits = secureRandom.nextInt();

        // Use 5 bit ints extracted from randomBits, to generate tokenLength random characters from sample space.
        while (index < tokenLength) {
            if (shift >= 32/5) {  // If we've expired the 32/5 values in this random int, get a new one, reset shift.
                randomBits = secureRandom.nextInt();
                shift = 0;
            }
            int chr = (randomBits >> (5*shift)) & 0x1f;  // Extract next 5 bit int from randomBits.
            shift++;  // Ensure we don't reuse any of randomBits.
            if (chr < tokenCharMap.length()) {
                // If we're in the valid range, use that character and advance in authToken, else try again.
                authToken[index] = tokenCharMap.charAt(chr);
                index++;
            }
        }
        return String.valueOf(authToken);
    }

    /**
     * Cleans up old tokens - for use when associated group is deleted.
     * 
     * @param groupId
     *            - to use as search term.
     * @throws InvalidUserAssociationTokenException
     *             - if we can't find the token.
     * @throws SegueDatabaseException
     *             - if a database error occurs.
     */
    public void deleteAssociationTokenByGroupId(final Long groupId) throws InvalidUserAssociationTokenException,
            SegueDatabaseException {
        AssociationToken associationTokenByGroupId = associationDatabase.getAssociationTokenByGroupId(groupId);
        if (null == associationTokenByGroupId) {
            throw new InvalidUserAssociationTokenException("The group token provided does not exist or is invalid.");
        }

        associationDatabase.deleteToken(associationTokenByGroupId.getToken());
    }

    /**
     * lookupTokenDetails - get a token from the database.
     * 
     * @param userMakingRequest
     *            - the user making the request for auditing purposes.
     * @param token
     *            - the token to look up.
     * @return AssociationToken - So that you can identify the owner user.
     * @throws InvalidUserAssociationTokenException - if thhe association is invalid.
     * @throws SegueDatabaseException - if there is a database error 
     */
    public AssociationToken lookupTokenDetails(final RegisteredUserDTO userMakingRequest, final String token)
            throws InvalidUserAssociationTokenException, SegueDatabaseException {
        Validate.notNull(userMakingRequest);
        Validate.notBlank(token);
        AssociationToken lookedupToken = associationDatabase.lookupAssociationToken(token);

        if (null == lookedupToken) {
            throw new InvalidUserAssociationTokenException("The group token provided does not exist or is invalid.");
        }

        return lookedupToken;
    }

    /**
     * get Associations.
     * 
     * This method will get all users who
     * 
     * @param user
     *            to find associations for.
     * @return List of all of their associations.
     * @throws SegueDatabaseException - if there is a database error
     */
    public List<UserAssociation> getAssociations(final RegisteredUserDTO user) throws SegueDatabaseException {
        return associationDatabase.getUserAssociations(user.getId());
    }

    /**
     * @param user
     *            - who may have access granted.
     * @return List of all associations
     * @throws SegueDatabaseException - if there is a database error 
     */
    public List<UserAssociation> getAssociationsForOthers(final RegisteredUserDTO user) throws SegueDatabaseException {
        return associationDatabase.getUsersThatICanSee(user.getId());
    }

    /**
     * createAssociationWithToken.
     * 
     * @param token
     *            - The token which links to a user (receiving permission) and possibly a group.
     * @param userGrantingPermission
     *            - the user who wishes to grant permissions to another.
     * @return The association token object
     * @throws SegueDatabaseException
     *             - If an error occurred while interacting with the database.
     * @throws InvalidUserAssociationTokenException
     *             - If the token provided is invalid.
     */
    public AssociationToken createAssociationWithToken(final String token, final RegisteredUserDTO userGrantingPermission)
            throws SegueDatabaseException, InvalidUserAssociationTokenException {
        Validate.notBlank(token);
        Validate.notNull(userGrantingPermission);

        AssociationToken lookedupToken = associationDatabase.lookupAssociationToken(token);

        if (null == lookedupToken) {
            throw new InvalidUserAssociationTokenException("The group token provided does not exist or is invalid.");
        }

        // add owner association
        if (!associationDatabase
                .hasValidAssociation(lookedupToken.getOwnerUserId(), userGrantingPermission.getId())) {
            associationDatabase.createAssociation(lookedupToken, userGrantingPermission.getId());
        }

        UserGroupDTO group = userGroupManager.getGroupById(lookedupToken.getGroupId());

        if (lookedupToken.getGroupId() != null) {
            userGroupManager.addUserToGroup(group, userGrantingPermission);
            log.debug(String.format("Adding User: %s to Group: %s", userGrantingPermission.getId(),
                    lookedupToken.getGroupId()));

            // add additional manager associations
            for (Long additionalManagerId : group.getAdditionalManagersUserIds()) {
                if (!associationDatabase
                        .hasValidAssociation(additionalManagerId, userGrantingPermission.getId())) {
                    associationDatabase.createAssociation(additionalManagerId, userGrantingPermission.getId());
                    // don't create a new association just do the group assignment as they have already granted permission.
                }
            }
        }
        return lookedupToken;
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

        associationDatabase.deleteAssociation(ownerUser.getId(), userToRevoke.getId());
    }

    /**
     * This method will accept a User summary object and will strip out any data that is restricted by authorisation
     * settings.
     * 
     * @param currentUser
     *            - user requesting access
     * @param userRequested
     *            - the users to be accessed.
     * @return updated user with data removed and access flags set.
     */
    public UserSummaryDTO enforceAuthorisationPrivacy(final RegisteredUserDTO currentUser,
            final UserSummaryDTO userRequested) {
        if (this.hasPermission(currentUser, userRequested)) {
            userRequested.setAuthorisedFullAccess(true);
        } else {
            userRequested.setAuthorisedFullAccess(false);
        }
        return userRequested;
    }

    /**
     * This method will accept a list of User objects and will strip out any data that is restricted by authorisation
     * settings.
     * 
     * @param currentUser
     *            - user requesting access
     * @param dataRequested
     *            - the list of users being accessed.
     * @return updated collection of users with data removed and access flags set.
     */
    public List<UserSummaryDTO> enforceAuthorisationPrivacy(final RegisteredUserDTO currentUser,
            final List<UserSummaryDTO> dataRequested) {
        // verify permission of currentUser to access dataRequested.

        // for those without permission obfuscate the date
        for (UserSummaryDTO user : dataRequested) {
            this.enforceAuthorisationPrivacy(currentUser, user);
        }
        return dataRequested;
    }

    /**
     * Check if one user has permission to view another user's data.
     * 
     * Users always have permission to view their own data.
     * 
     * @param currentUser
     *            - requesting permission
     * @param userRequested
     *            - the owner of the data to view.
     * @return true if yes false if no.
     */
    public boolean hasPermission(final RegisteredUserDTO currentUser, final UserSummaryDTO userRequested) {
        try {
            return currentUser.getId().equals(userRequested.getId())
                    || this.associationDatabase.hasValidAssociation(currentUser.getId(), userRequested.getId())
                    || Role.ADMIN.equals(currentUser.getRole());
        } catch (SegueDatabaseException e) {
            log.error("Database Error: Unable to determine whether a user has permission to view another users data.",
                    e);
            return false;
        }
    }
}
