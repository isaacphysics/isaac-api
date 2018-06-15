package uk.ac.cam.cl.dtg.segue.dao.userBadges;

import uk.ac.cam.cl.dtg.segue.api.managers.ITransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.ITransaction;
import uk.ac.cam.cl.dtg.segue.dos.UserBadge;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by du220 on 27/04/2018.
 */
public interface IUserBadgePersistenceManager {

    /**
     * Gets the current state of a user badge from the database
     *
     * @param user owner of badge record
     * @param badgeName enum of badge to be updated
     * @param transaction object which carries database transaction across multiple functions
     * @return a user badge object
     * @throws SegueDatabaseException
     */
    UserBadge getBadge(RegisteredUserDTO user, UserBadgeManager.Badge badgeName, ITransaction transaction) throws SegueDatabaseException;

    /**
     * Updates the state of a user badge to the database
     *
     * @param badge a user badge object
     * @param transaction object which carries database transaction across multiple functions
     * @throws SegueDatabaseException
     */
    public void updateBadge(UserBadge badge, ITransaction transaction) throws SegueDatabaseException;

}
