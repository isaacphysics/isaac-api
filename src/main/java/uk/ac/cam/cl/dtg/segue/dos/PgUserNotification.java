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
 *
 */
public class PgUserNotification implements UserNotification {
    private Long userId;
    private String contentNotificationId;
    private NotificationStatus status;
    private Date created;

    /**
     * 
     */
    public PgUserNotification() {

    }

    /**
     * @param userId - of the user who has responded
     * @param contentNotificationid - notification id.
     * @param status - status of the notification
     * @param created - date the entry was made.
     */
    public PgUserNotification(final Long userId, final String contentNotificationid, final NotificationStatus status,
            final Date created) {
        this.userId = userId;
        this.contentNotificationId = contentNotificationid;
        this.status = status;
        this.setCreated(created);
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.segue.dos.UserNotification#getUserId()
     */
    @Override
    public Long getUserId() {
        return userId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.segue.dos.UserNotification#getContentNotificationId()
     */
    @Override
    public String getContentNotificationId() {
        return contentNotificationId;
    }

    /**
     * Sets the contentNotificationid.
     * 
     * @param contentNotificationId
     *            the contentNotificationid to set
     */
    @Override
    public void setContentNotificationid(final String contentNotificationId) {
        this.contentNotificationId = contentNotificationId;
    }

    /**
     * Gets the status.
     * 
     * @return the status
     */
    @Override
    public NotificationStatus getStatus() {
        return status;
    }

    /**
     * Sets the status.
     * 
     * @param status
     *            the status to set
     */
    @Override
    public void setStatus(final NotificationStatus status) {
        this.status = status;
    }

    /**
     * Sets the userId.
     * 
     * @param userId
     *            the userId to set
     */
    @Override
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    /**
     * Gets the created.
     * @return the created
     */
    @Override
    public Date getCreated() {
        return created;
    }

    /**
     * Sets the created.
     * @param created the created to set
     */
    @Override
    public void setCreated(final Date created) {
        this.created = created;
    }
}
