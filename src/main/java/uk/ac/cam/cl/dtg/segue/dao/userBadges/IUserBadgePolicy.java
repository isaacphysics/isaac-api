package uk.ac.cam.cl.dtg.segue.dao.userBadges;

import com.fasterxml.jackson.databind.JsonNode;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.ITransaction;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 *  Interface defining structure of a user badge policy
 *  Specifies retrieval of badge level, initialisation of partial state, and update of partial state
 *
 * Created by du220 on 13/04/2018.
 */
public interface IUserBadgePolicy {

    /**
     * Retrieves the current state of a badge
     *
     * @param state an object defining the partial state from which a level can be extracted
     * @return an integer defining the level of badge achieved
     */
    int getLevel(JsonNode state);

    /**
     * Initialises a partial state for a particular badge
     *
     * @param user the user for which state should be calculated
     * @param transaction object which carries database transaction across multiple functions
     * @return an object describing the current partial state aggregated from the current user activity record
     */
    JsonNode initialiseState(RegisteredUserDTO user, ITransaction transaction);

    /**
     * Updates the partial state based on an event trigger
     *
     * @param user the user for which state should be updated
     * @param state the current state
     * @param event the triggering event description (should be unique)
     * @return an updated partial state
     */
    JsonNode updateState(RegisteredUserDTO user, JsonNode state, String event);
}
