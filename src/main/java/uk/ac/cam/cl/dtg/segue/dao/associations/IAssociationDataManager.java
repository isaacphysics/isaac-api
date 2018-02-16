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

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.AssociationToken;
import uk.ac.cam.cl.dtg.segue.dos.UserAssociation;

/**
 * Interface for data manager classes that deal with user association data.
 *
 */
public interface IAssociationDataManager {

    /**
     * Generates a token with no group.
     * 
     * @param token
     *            - the token to save
     * @return an AssociationToken.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    AssociationToken saveAssociationToken(final AssociationToken token) throws SegueDatabaseException;

    /**
     * Looksup an Association Token.
     * 
     * @param tokenCode
     *            the token code
     * @return an AssociationToken containing owner information and group id if set.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    AssociationToken lookupAssociationToken(final String tokenCode) throws SegueDatabaseException;

    /**
     * getAssociationTokenByGroupId.
     * 
     * @param groupId
     *            - id of the group to check.
     * @return token if the group has a token already otherwise null.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    AssociationToken getAssociationTokenByGroupId(Long groupId) throws SegueDatabaseException;

    /**
     * Deletes the token record but leaves associations intact.
     * 
     * @param token
     *            - token to delete from the database.
     * @throws SegueDatabaseException
     *             - Database problem.
     */
    void deleteToken(String token) throws SegueDatabaseException;

    /**
     * Creates an association based on a token.
     * 
     * @param token
     *            - containing information about the user to grant access to and the group the userIdGrantingAccess
     *            should go into.
     * @param userIdGrantingAccess
     *            - This user is the user granting access to their data.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    void createAssociation(AssociationToken token, Long userIdGrantingAccess) throws SegueDatabaseException;

    /**
     * Creates an association based on a token.
     *
     * @param userIdReceivingAccess
     *            - the user to grant access to.
     * @param userIdGrantingAccess
     *            - This user is the user granting access to their data.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    void createAssociation(final Long userIdReceivingAccess, final Long userIdGrantingAccess)
            throws SegueDatabaseException;

    /**
     * Revoke permission to access personal data.
     * 
     * @param ownerUserId
     *            - the owner of the data.
     * @param userIdWithAccess
     *            - user who should no longer have access
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    void deleteAssociation(final Long ownerUserId, final Long userIdWithAccess) throws SegueDatabaseException;

    /**
     * Determines whether the user has a valid association already.
     * 
     * @param userIdRequestingAccess
     *            - User who wishes to access someone elses' data.
     * @param ownerUserId
     *            - the owner of the data being accessed.
     * @return true if the userIdRequestingAccess has permission, false if not.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    boolean hasValidAssociation(Long userIdRequestingAccess, Long ownerUserId) throws SegueDatabaseException;

    /**
     * Get a list of user associations for a given user.
     * 
     * I.e. Who can currently view a users data.
     * 
     * @param userId
     *            - User to find the associations for.
     * @return the list of user associations.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    List<UserAssociation> getUserAssociations(Long userId) throws SegueDatabaseException;

    /**
     * Get a list of user associations that provide grant access for a given user.
     * 
     * I.e. Who can I currently see data for.
     * 
     * @param userId
     *            - User to find the associations for.
     * @return the list of user associations.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    List<UserAssociation> getUsersThatICanSee(Long userId) throws SegueDatabaseException;
}
