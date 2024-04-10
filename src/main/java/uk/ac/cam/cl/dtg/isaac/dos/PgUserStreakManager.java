package uk.ac.cam.cl.dtg.isaac.dos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.useralerts.UserAlertsWebSocket;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

public class PgUserStreakManager implements IUserStreaksManager {
  private static final Logger log = LoggerFactory.getLogger(PgUserStreakManager.class);
  private static final String DATABASE_ERROR_MESSAGE = "Database error";
  private static final String CURRENT_ACTIVITY = "currentActivity";
  private static final String CURRENT_STREAK = "currentStreak";

  private final PostgresSqlDb database;
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * PgUserStreakManager.
   *
   * @param database client for postgres.
   */
  @Inject
  public PgUserStreakManager(final PostgresSqlDb database) {
    this.database = database;
  }

  @Override
  public Map<String, Object> getCurrentStreakRecord(final RegisteredUserDTO user) {
    String query = "SELECT * FROM user_streaks_current_progress(?) LEFT JOIN user_streaks(?)"
        + " ON user_streaks_current_progress.currentdate - user_streaks.enddate <= 1"
        + " AND user_streaks.startdate <= user_streaks_current_progress.currentdate";

    return calculateCurrentStreakRecord(user, query);
  }

  @Override
  public int getLongestStreak(final RegisteredUserDTO user) {
    String query = "SELECT * FROM user_streaks(?) ORDER BY streaklength DESC LIMIT 1";

    return calculateLongestStreak(user, query);
  }

  @Override
  public Map<String, Object> getCurrentWeeklyStreakRecord(final RegisteredUserDTO user) {
    String query = "SELECT * FROM user_streaks_weekly_current_progress(?) LEFT JOIN user_streaks_weekly(?)"
        + " ON user_streaks_weekly_current_progress.currentweek - user_streaks_weekly.enddate <= 7"
        + " AND user_streaks_weekly.startdate <= user_streaks_weekly_current_progress.currentweek";

    return calculateCurrentStreakRecord(user, query);
  }

  private Map<String, Object> calculateCurrentStreakRecord(RegisteredUserDTO user, String query) {
    Map<String, Object> streakRecord = new HashMap<>();
    streakRecord.put(CURRENT_ACTIVITY, 0);
    streakRecord.put(CURRENT_STREAK, 0);

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(1, user.getId());
      pst.setLong(2, user.getId());

      try (ResultSet results = pst.executeQuery()) {
        if (results.next()) {
          streakRecord.put(CURRENT_ACTIVITY, results.getInt("currentprogress"));
          streakRecord.put(CURRENT_STREAK, results.getInt("streaklength"));
        }
      }
    } catch (SQLException e) {
      log.error(DATABASE_ERROR_MESSAGE, e);
    }
    return streakRecord;
  }

  @Override
  public int getLongestWeeklyStreak(final RegisteredUserDTO user) {
    String query = "SELECT * FROM user_streaks_weekly(?) ORDER BY streaklength DESC LIMIT 1";

    return calculateLongestStreak(user, query);
  }

  private int calculateLongestStreak(RegisteredUserDTO user, String query) {
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(1, user.getId());

      try (ResultSet results = pst.executeQuery()) {
        if (results.next()) {
          return results.getInt("streaklength");
        }
      }
    } catch (SQLException e) {
      log.error(DATABASE_ERROR_MESSAGE, e);
    }
    return 0;
  }

  @Override
  public void notifyUserOfStreakChange(final RegisteredUserDTO user) {
    long userId = user.getId();
    try {
      IUserAlert alert = new PgUserAlert(null, userId,
          objectMapper.writeValueAsString(Map.of("dailyStreakRecord", this.getCurrentStreakRecord(user),
              "weeklyStreakRecord", this.getCurrentWeeklyStreakRecord(user))),
          "progress", new Timestamp(System.currentTimeMillis()), null, null, null);

      UserAlertsWebSocket.notifyUserOfAlert(userId, alert);
    } catch (JsonProcessingException e) {
      log.error(String.format("Unable to serialize user streak change JSON for user %s: %s",
          user.getId(), e.getMessage()));
    }
  }
}
