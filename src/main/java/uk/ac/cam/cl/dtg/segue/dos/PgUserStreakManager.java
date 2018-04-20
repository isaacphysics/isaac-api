package uk.ac.cam.cl.dtg.segue.dos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import uk.ac.cam.cl.dtg.segue.api.userAlerts.IAlertListener;
import uk.ac.cam.cl.dtg.segue.api.userAlerts.UserAlertsWebSocket;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

/**
 * Created by du220 on 16/04/2018.
 */
public class PgUserStreakManager implements IUserStreaksManager {

    private final PostgresSqlDb database;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * PgUserStreakManager.
     *
     * @param database
     *            client for postgres.
     */
    public PgUserStreakManager(final PostgresSqlDb database) {
        this.database = database;
    }


    @Override
    public Map<String, Object> getCurrentStreakRecord(final RegisteredUserDTO user) {

        Map<String, Object> streakRecord = Maps.newHashMap();
        streakRecord.put("currentActivity", 0);
        streakRecord.put("currentStreak", 0);

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_streaks_current_progress(?) " +
                    "left join user_streaks(?) " +
                    "on user_streaks_current_progress.currentdate - user_streaks.enddate <= 1");

            pst.setLong(1, user.getId());
            pst.setLong(2, user.getId());
            ResultSet results = pst.executeQuery();

            if (results.next()) {
                streakRecord.put("currentActivity", results.getInt("currentprogress"));
                streakRecord.put("currentStreak", results.getInt("streaklength"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return streakRecord;
    }

    @Override
    public int getLongestStreak(final RegisteredUserDTO user) {

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_streaks(?) ORDER BY streaklength limit 1");

            pst.setLong(1, user.getId());
            ResultSet results = pst.executeQuery();

            if (results.next()) {
                return results.getInt("streaklength");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void notifyUserOfStreakChange(final RegisteredUserDTO user) {
        // FIXME - it is unlikely that this is the best location for this code!
        // It is better than in the already bloated facade method, however!
        if (null != UserAlertsWebSocket.connectedSockets && UserAlertsWebSocket.connectedSockets.containsKey(user.getId())) {

            try {
                IUserAlert alert = new PgUserAlert(null, user.getId(),
                        objectMapper.writeValueAsString(ImmutableMap.of("streakRecord", this.getCurrentStreakRecord(user))),
                        "progress", new Timestamp(System.currentTimeMillis()), null, null, null);

                for (IAlertListener listener : UserAlertsWebSocket.connectedSockets.get(user.getId())) {
                    listener.notifyAlert(alert);
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

}
