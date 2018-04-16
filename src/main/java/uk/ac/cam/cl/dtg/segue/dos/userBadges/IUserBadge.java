package uk.ac.cam.cl.dtg.segue.dos.userBadges;

/**
 * Created by du220 on 13/04/2018.
 */
public interface IUserBadge {

    int getLevel();
    void initialiseState();
    void updateState(Object event);
}
