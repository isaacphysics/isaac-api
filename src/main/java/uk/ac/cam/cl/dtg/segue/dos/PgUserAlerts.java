package uk.ac.cam.cl.dtg.segue.dos;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.segue.api.userAlerts.UserAlertsWebSocket;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.*;
import java.util.List;

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
    public List<IUserAlert> getUserAlerts(Long userId) throws SegueDatabaseException {
        String query = "SELECT * FROM user_alerts WHERE user_id = ? ORDER BY created ASC";
        try (Connection conn = db.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);

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
    public IUserAlert createAlert(Long userId, String message, String link) throws SegueDatabaseException {
        String query = "INSERT INTO user_alerts (user_id, message, link, created) VALUES (?, ?, ?, ?) RETURNING *";
        try (Connection conn = db.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);
            pst.setString(2, message);
            pst.setString(3, link);
            pst.setTimestamp(4, new Timestamp(System.currentTimeMillis()));

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
    public void recordAlertEvent(Long alertId, IUserAlert.AlertEvents eventType) throws SegueDatabaseException {
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
        }
        query += "= ?  WHERE id = ?";
        try (Connection conn = db.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pst.setLong(2, alertId);

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to update user notification.");
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
}
