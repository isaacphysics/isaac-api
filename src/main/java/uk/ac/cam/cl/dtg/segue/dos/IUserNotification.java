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

import java.util.Date;

/**
 * This interface represents a record of a specific notification having been shown to a user.
 * 
 */
public interface IUserNotification {

    /**
     * Represents the status of a notification.
     */
    public enum NotificationStatus {
        DISMISSED, POSTPONED, DISABLED
    }

    /**
     * @return the user id.
     */
    String getUserId();

    /**
     * @return the notification id
     */
    String getContentNotificationId();

    /**
     * @param userId
     *            the user id to set
     */
    void setUserId(String userId);

    /**
     * @param status
     *            the status to set
     */
    void setStatus(NotificationStatus status);

    /**
     * @return the status of this notification
     */
    NotificationStatus getStatus();

    /**
     * @param contentNotificationid
     *            the notification id
     */
    void setContentNotificationid(String contentNotificationid);

    /**
     * @return the date the record of the notification response was created.
     */
    Date getCreated();

    /**
     * @param created
     *            the date this was created.
     */
    void setCreated(Date created);

}
