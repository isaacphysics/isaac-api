package uk.ac.cam.cl.dtg.segue.dos;

import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Map;

/**
 * Created by du220 on 16/04/2018.
 */
public interface IUserStreaksManager {

    Map<String, Object> getCurrentStreakRecord(RegisteredUserDTO user);
    int getHighestStreak(RegisteredUserDTO user);
}
