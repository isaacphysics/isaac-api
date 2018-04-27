package uk.ac.cam.cl.dtg.segue.dao.userBadges;

import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * Created by du220 on 13/04/2018.
 */
public interface IUserBadgePolicy {

    /**
     *
     * @param state
     * @return
     */
    int getLevel(Object state);

    /**
     *
     * @param user
     * @return
     */
    Object initialiseState(RegisteredUserDTO user);

    /**
     *
     * @param state
     * @param event
     * @return
     */
    Object updateState(Object state, Object event);
}
