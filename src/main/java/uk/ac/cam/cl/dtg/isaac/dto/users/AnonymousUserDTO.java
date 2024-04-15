/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.isaac.dto.users;

import java.time.Instant;

/**
 * Data Transfer Object to represent an anonymous user of the system.
 *
 */
public class AnonymousUserDTO extends AbstractSegueUserDTO {
  private String sessionId;
  private Instant dateCreated;
  private Instant lastUpdated;


  /**
   * Default constructor required for Jackson.
   */
  public AnonymousUserDTO() {
  }

  /**
   * Full constructor for the AnonymousUser object.
   *
   * @param sessionId
   *            - Our session Unique ID
   */
  public AnonymousUserDTO(final String sessionId) {
    this.sessionId = sessionId;
  }

  /**
   * Full constructor for the AnonymousUser object.
   *
   * @param sessionId
   *            - Our session Unique ID
   * @param dateCreated - date the user object was created
   * @param lastUpdated - last time it was updated.
   */
  public AnonymousUserDTO(final String sessionId, final Instant dateCreated, final Instant lastUpdated) {
    this.sessionId = sessionId;
    this.dateCreated = dateCreated;
    this.lastUpdated = lastUpdated;
  }

  /**
   * Gets the sessionId.
   *
   * @return the sessionId
   */
  public String getSessionId() {
    return sessionId;
  }

  /**
   * Sets the sessionId.
   *
   * @param sessionId
   *            the sessionId to set
   */
  public void setSessionId(final String sessionId) {
    this.sessionId = sessionId;
  }

  /**
   * Gets the dateCreated.
   *
   * @return the dateCreated
   */
  public Instant getDateCreated() {
    return dateCreated;
  }

  /**
   * Sets the dateCreated.
   *
   * @param dateCreated
   *            the dateCreated to set
   */
  public void setDateCreated(final Instant dateCreated) {
    this.dateCreated = dateCreated;
  }


  /**
   * get date this object was last updated.
   *
   * @return update date
   */
  public Instant getLastUpdated() {
    return lastUpdated;
  }

  /**
   * set the last update date.
   *
   * @param lastUpdated last update date
   */
  public void setLastUpdated(final Instant lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
}
