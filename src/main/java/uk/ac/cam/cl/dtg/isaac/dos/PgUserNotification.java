/**
 * Copyright 2015 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dos;

import java.time.Instant;

/**
 *
 */
public class PgUserNotification implements IUserNotification {
  private Long userId;
  private String contentNotificationId;
  private NotificationStatus status;
  private Instant created;

  /**
   *
   */
  public PgUserNotification() {

  }

  /**
   * @param userId - of the user who has responded
   * @param contentNotificationId - notification id.
   * @param status - status of the notification
   * @param created - date the entry was made.
   */
  public PgUserNotification(final Long userId, final String contentNotificationId, final NotificationStatus status,
                            final Instant created) {
    this.userId = userId;
    this.contentNotificationId = contentNotificationId;
    this.status = status;
    this.created = created;
  }

  /*
   * (non-Javadoc)
   *
   * @see uk.ac.cam.cl.dtg.isaac.dos.UserNotification#getUserId()
   */
  @Override
  public Long getUserId() {
    return userId;
  }

  /*
   * (non-Javadoc)
   *
   * @see uk.ac.cam.cl.dtg.isaac.dos.UserNotification#getContentNotificationId()
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
  public void setContentNotificationId(final String contentNotificationId) {
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
   *
   * @return the created
   */
  @Override
  public Instant getCreated() {
    return created;
  }

  /**
   * Sets the created.
   *
   * @param created the created to set
   */
  @Override
  public void setCreated(final Instant created) {
    this.created = created;
  }
}
