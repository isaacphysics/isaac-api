package uk.ac.cam.cl.dtg.segue.dos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(PgUserStreakManager.class);

    private final PostgresSqlDb database;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * PgUserStreakManager.
     *
     * @param database
     *            client for postgres.
     */
    @Inject
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
            pst = conn.prepareStatement("SELECT * FROM"
                    + " user_streaks_current_progress(?) LEFT JOIN user_streaks(?)"
                    + " ON user_streaks_current_progress.currentdate - user_streaks.enddate <= 1"
                    + " AND user_streaks.startdate <= user_streaks_current_progress.currentdate");

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
            pst = conn.prepareStatement("SELECT * FROM user_streaks(?) ORDER BY streaklength DESC LIMIT 1");

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
    public Map<String, Object> getCurrentWeeklyStreakRecord(final RegisteredUserDTO user) {

        Map<String, Object> streakRecord = Maps.newHashMap();
        streakRecord.put("currentActivity", 0);
        streakRecord.put("currentStreak", 0);

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM"
                    + " user_streaks_weekly_current_progress(?) LEFT JOIN user_streaks_weekly(?)"
                    + " ON user_streaks_weekly_current_progress.currentweek - user_streaks_weekly.enddate <= 7"
                    + " AND user_streaks_weekly.startdate <= user_streaks_weekly_current_progress.currentweek");

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
    public int getLongestWeeklyStreak(final RegisteredUserDTO user) {

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_streaks_weekly(?) ORDER BY streaklength DESC LIMIT 1");

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
        long userId = user.getId();
        try {
            IUserAlert alert = new PgUserAlert(null, userId,
                    objectMapper.writeValueAsString(ImmutableMap.of("dailyStreakRecord", this.getCurrentStreakRecord(user), "weeklyStreakRecord", this.getCurrentWeeklyStreakRecord(user))),
                    "progress", new Timestamp(System.currentTimeMillis()), null, null, null);

            UserAlertsWebSocket.notifyUserOfAlert(userId, alert);
        } catch (JsonProcessingException e) {
            log.error(String.format("Unable to serialize user streak change JSON for user %s: %s",
                    user.getId(), e.getMessage()));
        }
    }

}
