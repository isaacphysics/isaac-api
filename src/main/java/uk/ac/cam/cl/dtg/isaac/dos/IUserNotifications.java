/**
 * Copyright 2015 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.isaac.dos.IUserNotification.NotificationStatus;

/**
 * This interface represents the DAO for recording user interactions with notifications and should be backed by some
 * database.
 */
public interface IUserNotifications {

    /**
     * @param userId - the user id to look up
     * @return the list of notification records for this user.
     * @throws SegueDatabaseException - if a database error has occurred.
     */
    List<IUserNotification> getUserNotifications(Long userId) throws SegueDatabaseException;

    /**
     * @param userId - the user id to save a record for.
     * @param notificationId to save
     * @param status to save
     * @throws SegueDatabaseException - if a database error has occurred.
     */
    void saveUserNotification(Long userId, String notificationId, NotificationStatus status)
            throws SegueDatabaseException;
}
