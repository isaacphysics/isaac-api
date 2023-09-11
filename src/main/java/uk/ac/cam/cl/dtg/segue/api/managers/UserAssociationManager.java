/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api.managers;

import com.google.inject.Inject;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.AssociationToken;
import uk.ac.cam.cl.dtg.isaac.dos.UserAssociation;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.IAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.associations.InvalidUserAssociationTokenException;
import uk.ac.cam.cl.dtg.segue.dao.associations.UserGroupNotFoundException;

/**
 * UserAssociationManager Responsible for managing user associations, groups and permissions for one user to grant data
 * view rights to another.
 */
public class UserAssociationManager {
  private static final Logger log = LoggerFactory.getLogger(UserAssociationManager.class);
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int TOKEN_LENGTH = 6;
  private static final int BITS_IN_INTEGER = 32;
  private static final int RANDOM_BITS_TO_EXTRACT = 5;
  private static final int BIT_SHIFT_MASK = 0x1f;

  private final IAssociationDataManager associationDatabase;
  private final GroupManager userGroupManager;
  private final UserAccountManager userManager;

  /**
   * UserAssociationManager.
   *
   * @param associationDatabase
   *            - IAssociationDataManager providing access to the database.
   * @param userManager
   *            - UserAccountManager for checking permissions to access other user objects
   * @param userGroupManager
   *            - IAssociationDataManager providing access to the database.
   */
  @Inject
  public UserAssociationManager(
      final IAssociationDataManager associationDatabase,
      final UserAccountManager userManager,
      final GroupManager userGroupManager) {
    this.associationDatabase = associationDatabase;
    this.userManager = userManager;
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
                                                   final Long associatedGroupId)
      throws SegueDatabaseException, UserGroupNotFoundException {
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
   * <br>
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

    char[] authToken = new char[TOKEN_LENGTH];

    int index = 0;  // Where we are in the token.
    int shift = 0;  // Where we are in the random 32 bit integer.
    int randomBits = SECURE_RANDOM.nextInt();

    // Use 5 bit ints extracted from randomBits, to generate tokenLength random characters from sample space.
    while (index < TOKEN_LENGTH) {
      if (shift >= BITS_IN_INTEGER
          / RANDOM_BITS_TO_EXTRACT) {  // If we've expired the 32/5 values in this random int, get a new one, reset shift.
        randomBits = SECURE_RANDOM.nextInt();
        shift = 0;
      }
      int chr =
          (randomBits >> (RANDOM_BITS_TO_EXTRACT * shift)) & BIT_SHIFT_MASK;  // Extract next 5 bit int from randomBits.
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
   * <br>
   * I.e. Who can currently view a given user's data.
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
   * Get all those user's whose data I can see.
   *
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
   * Revoke all permissions granted by data owner.
   *
   * @param ownerUser
   *            - the user who owns the data
   * @throws SegueDatabaseException
   *             - If there is a database issue whilst fulfilling the request.
   */
  public void revokeAllAssociationsByOwnerUser(final RegisteredUserDTO ownerUser)
      throws SegueDatabaseException {
    Validate.notNull(ownerUser);

    associationDatabase.deleteAssociationsByOwner(ownerUser.getId());
  }

  /**
   * Revoke all permissions granted to a data recipient.
   *
   * @param recipientUser
   *            - the user who owns the data
   * @throws SegueDatabaseException
   *             - If there is a database issue whilst fulfilling the request.
   */
  public void revokeAllAssociationsByRecipientUser(final RegisteredUserDTO recipientUser)
      throws SegueDatabaseException {
    Validate.notNull(recipientUser);

    associationDatabase.deleteAssociationsByRecipient(recipientUser.getId());
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
   * <br>
   * Users always have permission to view their own data. Students never have permission to view another users data.
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
          || Role.ADMIN.equals(currentUser.getRole())
          || !Role.STUDENT.equals(currentUser.getRole()) && this.associationDatabase.hasValidAssociation(
          currentUser.getId(), userRequested.getId());
    } catch (SegueDatabaseException e) {
      log.error("Database Error: Unable to determine whether a user has permission to view another users data.",
          e);
      return false;
    }
  }

  /**
   * Overloaded method to handle different user representation object.
   * @param currentUser
   *            - requesting permission
   * @param userRequested
   *            - the owner of the data to view.
   * @return true if yes false if no.
   */
  public boolean hasPermission(final RegisteredUserDTO currentUser, final RegisteredUserDTO userRequested) {
    return this.hasPermission(currentUser, userManager.convertToUserSummaryObject(userRequested));
  }

  /**
   * Check if one user has teacher-level permission to view another user's data.
   * <br>
   * Users always have permission to view their own data. Students never have permission to view another users data,
   * and tutors do not have teacher-level permissions.
   *
   * @param currentUser
   *            - requesting permission
   * @param userRequested
   *            - the owner of the data to view.
   * @return true if yes false if no.
   */
  public boolean hasTeacherPermission(final RegisteredUserDTO currentUser, final UserSummaryDTO userRequested) {
    try {
      return currentUser.getId().equals(userRequested.getId())
          || Role.ADMIN.equals(currentUser.getRole())
          || !Role.STUDENT.equals(currentUser.getRole()) && !Role.TUTOR.equals(currentUser.getRole())
          && this.associationDatabase.hasValidAssociation(currentUser.getId(), userRequested.getId());
    } catch (SegueDatabaseException e) {
      log.error("Database Error: Unable to determine whether a user has permission to view another users data.",
          e);
      return false;
    }
  }

  /**
   * Overloaded method to handle different user representation object.
   * @param currentUser
   *            - requesting permission
   * @param userRequested
   *            - the owner of the data to view.
   * @return true if yes or false if no.
   */
  public boolean hasTeacherPermission(final RegisteredUserDTO currentUser, final RegisteredUserDTO userRequested) {
    return this.hasTeacherPermission(currentUser, userManager.convertToUserSummaryObject(userRequested));
  }

  /**.
   * Filter a list of records on whether a user ID has an association with the current user
   * @param currentUser the user which might have been granted access.
   * @param records a list of objects containing an ID.
   * @param userIdKey a function which takes the record and returns the user ID.
   * @param <T> the type of the object containing an ID.
   * @return a filtered list of type List{@literal <T>}.
   * @throws SegueDatabaseException if it was not able to get the user's associations form the database.
   */
  public <T> List<T> filterUnassociatedRecords(
      final RegisteredUserDTO currentUser,
      final List<T> records,
      final Function<T, Long> userIdKey
  ) throws SegueDatabaseException {
    // Get current user's associated IDs
    Set<Long> associations = this.getAssociationsForOthers(currentUser).stream()
        .map(UserAssociation::getUserIdGrantingPermission)
        .collect(Collectors.toSet());
    // Add own ID to associations
    associations.add(currentUser.getId());

    return records.stream()
        .filter(item -> associations.contains(userIdKey.apply(item)))
        .collect(Collectors.toList());
  }

  /**
   * A special case of the generic filterUnassociatedRecords for when the records are a list of user IDs.
   * @param currentUser the user which might have been granted access.
   * @param userIds a list of user IDs.
   * @return a list of user ID which has granted the current user to view their data.
   * @throws SegueDatabaseException if it was not able to get the user's associations form the database.
   */
  public List<Long> filterUnassociatedRecords(
      final RegisteredUserDTO currentUser, final List<Long> userIds
  ) throws SegueDatabaseException {
    return this.filterUnassociatedRecords(currentUser, userIds, Function.identity());
  }
}
