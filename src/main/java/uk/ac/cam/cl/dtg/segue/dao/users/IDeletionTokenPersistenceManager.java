package uk.ac.cam.cl.dtg.segue.dao.users;

import uk.ac.cam.cl.dtg.isaac.dos.users.AccountDeletionToken;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * Interface for long-term storage of AccountDeletionToken objects.
 */
public interface IDeletionTokenPersistenceManager {

    /**
     * Get the account deletion token for a user account, if it exists.
     *
     * @param userId - the user in question.
     * @return the account deletion token, or null if one does not exist.
     * @throws SegueDatabaseException on database failure.
     */
    AccountDeletionToken getAccountDeletionToken(Long userId) throws SegueDatabaseException;

    /**
     * Save an account deletion token to permanent storage.
     *
     * @param token - the token to save.
     * @return the token. This may or may not have additional metadata added compared to the 'token' param.
     * @throws SegueDatabaseException on database failure.
     */
    AccountDeletionToken saveAccountDeletionToken(AccountDeletionToken token) throws SegueDatabaseException;
}
