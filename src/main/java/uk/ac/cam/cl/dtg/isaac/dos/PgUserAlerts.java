package uk.ac.cam.cl.dtg.isaac.dos;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import uk.ac.cam.cl.dtg.segue.api.userAlerts.UserAlertsWebSocket;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

public class PgUserAlerts implements IUserAlerts {

  private final PostgresSqlDb db;

  @Inject
  public PgUserAlerts(final PostgresSqlDb db) {
    this.db = db;
  }

  private PgUserAlert buildPgUserAlert(final ResultSet result) throws SQLException {
    return new PgUserAlert(
        result.getLong("id"),
        result.getLong("user_id"),
        result.getString("message"),
        result.getString("link"),
        result.getTimestamp("created"),
        result.getTimestamp("seen"),
        result.getTimestamp("clicked"),
        result.getTimestamp("dismissed"));
  }

  @Override
  public List<IUserAlert> getUserAlerts(final Long userId) throws SegueDatabaseException {
    String query = "SELECT * FROM user_alerts WHERE user_id = ? ORDER BY created ASC";
    try (Connection conn = db.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_ALERTS_USER_ID, userId);

      try (ResultSet results = pst.executeQuery()) {
        List<IUserAlert> returnResult = Lists.newArrayList();
        while (results.next()) {
          returnResult.add(buildPgUserAlert(results));
        }
        return returnResult;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public IUserAlert createAlert(final Long userId, final String message, final String link)
      throws SegueDatabaseException {
    String query = "INSERT INTO user_alerts (user_id, message, link, created) VALUES (?, ?, ?, ?) RETURNING *";
    try (Connection conn = db.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_CREATE_ALERT_USER_ID, userId);
      pst.setString(FIELD_CREATE_ALERT_MESSAGE, message);
      pst.setString(FIELD_CREATE_ALERT_LINK, link);
      pst.setTimestamp(FIELD_CREATE_ALERT_CREATED, new Timestamp(System.currentTimeMillis()));

      try (ResultSet results = pst.executeQuery()) {
        results.next();

        IUserAlert alert = buildPgUserAlert(results);
        UserAlertsWebSocket.notifyUserOfAlert(userId, alert);

        return alert;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public void recordAlertEvent(final Long alertId, final IUserAlert.AlertEvents eventType)
      throws SegueDatabaseException {
    String query = "UPDATE user_alerts SET ";
    switch (eventType) {
      case SEEN:
        query += "seen";
        break;
      case CLICKED:
        query += "clicked";
        break;
      case DISMISSED:
        query += "dismissed";
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + eventType);
    }
    query += "= ?  WHERE id = ?";
    try (Connection conn = db.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setTimestamp(FIELD_RECORD_EVENT_TIMESTAMP, new Timestamp(System.currentTimeMillis()));
      pst.setLong(FIELD_RECORD_EVENT_ALERT_ID, alertId);

      if (pst.executeUpdate() == 0) {
        throw new SegueDatabaseException("Unable to update user notification.");
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  // Field Constants
  // getUserAlerts
  private static final int FIELD_GET_ALERTS_USER_ID = 1;

  // createAlert
  private static final int FIELD_CREATE_ALERT_USER_ID = 1;
  private static final int FIELD_CREATE_ALERT_MESSAGE = 2;
  private static final int FIELD_CREATE_ALERT_LINK = 3;
  private static final int FIELD_CREATE_ALERT_CREATED = 4;

  // FIELD_CREATE_ALERT_CREATED
  private static final int FIELD_RECORD_EVENT_TIMESTAMP = 1;
  private static final int FIELD_RECORD_EVENT_ALERT_ID = 2;
}
