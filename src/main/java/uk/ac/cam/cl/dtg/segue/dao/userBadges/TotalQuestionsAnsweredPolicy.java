package uk.ac.cam.cl.dtg.segue.dao.userBadges;

import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * Created by du220 on 27/04/2018.
 */
public class TotalQuestionsAnsweredPolicy implements IUserBadgePolicy {

    @Override
    public int getLevel(Object state) {
        return 0;
    }

    @Override
    public Object initialiseState(RegisteredUserDTO user) {
        return null;
    }

    @Override
    public Object updateState(Object state, Object event) {
        return null;
    }
}
