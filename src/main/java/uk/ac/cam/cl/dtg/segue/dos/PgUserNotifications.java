/**
 * Copyright 2015 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.IUserNotification.NotificationStatus;

/**
 * Represents a postgres specific implementation of the UserNotifications DAO interface.
 *
 */
public class PgUserNotifications implements IUserNotifications {
    private final PostgresSqlDb database;

    // private static final Logger log = LoggerFactory.getLogger(PgUserNotifications.class);

    /**
     * PgUserNotifications.
     * 
     * @param database
     *            - the postgres client.
     */
    @Inject
    public PgUserNotifications(final PostgresSqlDb database) {
        this.database = database;
    }

    @Override
    public List<IUserNotification> getUserNotifications(final Long userId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_notifications WHERE user_id = ? ORDER BY created ASC");
            pst.setLong(1, userId);

            ResultSet results = pst.executeQuery();
            List<IUserNotification> returnResult = Lists.newArrayList();
            while (results.next()) {
                returnResult.add(buildPgUserNotifications(results));
            }
            return returnResult;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * @param userId
     *            - user
     * @param contentId
     *            - notification content
     * @return userNotification object.
     * @throws SegueDatabaseException
     *             - if there is a problem looking up the user notification record requested.
     */
    public IUserNotification getNotification(final Long userId, final String contentId) throws SegueDatabaseException {
        IUserNotification notification = this.getNotificationRecord(userId, contentId);

        if (null == notification) {
            throw new ResourceNotFoundException("Unable to locate the notification record requested");
        }

        return notification;
    }

    /**
     * @param notification
     * @throws SegueDatabaseException
     */
    @Override
    public void saveUserNotification(final Long userId, final String notificationId, final NotificationStatus status)
            throws SegueDatabaseException {
        IUserNotification notification = new PgUserNotification(userId, notificationId, status, new Date());

        if (this.getNotificationRecord(userId, notificationId) == null) {
            insertNewNotificationRecord(notification);
        } else {
            updateNotificationRecord(notification);
        }
    }

    /**
     * @param result - the sql result set to be extracted.
     * @return a UserNotification object.
     * @throws SQLException - if bad things happen
     */
    private IUserNotification buildPgUserNotifications(final ResultSet result) throws SQLException {
        return new PgUserNotification(result.getLong("user_id"), result.getString("notification_id"),
                NotificationStatus.valueOf(result.getString("status")), result.getTimestamp("created"));
    }

    /**
     * @param notification - the notification to insert.
     * @throws SegueDatabaseException - if it fails.
     */
    private void insertNewNotificationRecord(final IUserNotification notification) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {

            pst = conn
                    .prepareStatement("INSERT INTO user_notifications "
                            + "(user_id, notification_id, status, created) "
                            + "VALUES (?, ?, ?, ?)");

            pst.setLong(1, notification.getUserId());
            pst.setString(2, notification.getContentNotificationId());
            pst.setString(3, notification.getStatus().name());
            pst.setTimestamp(4, new java.sql.Timestamp(notification.getCreated().getTime()));

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save user notification.");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * @param notification - the notification to update.
     * @throws SegueDatabaseException - if it fails.
     */
    private void updateNotificationRecord(final IUserNotification notification) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {

            pst = conn
                    .prepareStatement("UPDATE user_notifications "
                            + "SET status = ?, created=? "
                            + "WHERE user_id = ? AND notification_id = ?");

            pst.setString(1, notification.getStatus().name());
            pst.setTimestamp(2, new java.sql.Timestamp(notification.getCreated().getTime()));
            pst.setLong(3, notification.getUserId());
            pst.setString(4, notification.getContentNotificationId());

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to update user notification.");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * @param userId - user id
     * @param contentId - the notification id
     * @return the notification record or null.
     * @throws SegueDatabaseException - if bad things happen
     */
    private IUserNotification getNotificationRecord(final Long userId, final String contentId)
            throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_notifications WHERE user_id = ? AND notification_id = ?");
            pst.setLong(1, userId);
            pst.setString(2, contentId);

            ResultSet results = pst.executeQuery();
            List<IUserNotification> listOfResults = Lists.newArrayList();
            while (results.next()) {
                listOfResults.add(buildPgUserNotifications(results));
            }

            if (listOfResults.size() == 0) {
                return null;
            }

            // don't need to check other cases as the keys being looked up are composite in the database definition.
            return listOfResults.get(0);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
}
