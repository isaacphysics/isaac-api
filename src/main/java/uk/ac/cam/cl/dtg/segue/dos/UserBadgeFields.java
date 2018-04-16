package uk.ac.cam.cl.dtg.segue.dos;

import uk.ac.cam.cl.dtg.segue.dos.userBadges.IUserBadge;

/**
 * Created by du220 on 13/04/2018.
 */
public abstract class UserBadgeFields implements IUserBadge {

    public enum Badge {
        MECHANICS,
        DYNAMICS
    }

    public Long userId;
    public Badge badgeName;
    public Object state;

    public UserBadgeFields() {

    }

}
