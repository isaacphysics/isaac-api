package uk.ac.cam.cl.dtg.segue.dos;

import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Map;

/**
 * Created by du220 on 16/04/2018.
 */
public interface IUserStreaksManager {

    /**
     * This method will get the current streak and current streak progress for a registered user.
     *
     * @param user
     *            - the registered user
     * @return the current streak map object
     */
    Map<String, Object> getCurrentStreakRecord(RegisteredUserDTO user);

    /**
     * This method will get the longest streak a registered user has achieved.
     *
     * @param user
     *            - the registered user
     * @return the length of the longest streak
     */
    int getLongestStreak(RegisteredUserDTO user);

    /**
     * This method will get the current weekly streak and current weekly streak progress for a registered user.
     *
     * @param user
     *            - the registered user
     * @return the current streak map object
     */
    Map<String, Object> getCurrentWeeklyStreakRecord(RegisteredUserDTO user);

    /**
     * This method will get the longest weekly streak a registered user has achieved.
     *
     * @param user
     *            - the registered user
     * @return the length of the longest streak
     */
    int getLongestWeeklyStreak(RegisteredUserDTO user);

    /**
     * This method will notify a registered user that their streak has changed.
     *
     * @param user
     *            - the registered user to notify
     */
    void notifyUserOfStreakChange(RegisteredUserDTO user);
}
