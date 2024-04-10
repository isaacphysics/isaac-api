/**
 * Copyright 2017 Stephen Cummins
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

package uk.ac.cam.cl.dtg.isaac.dos.users;

import java.time.Instant;

/**
 * LocalUserCredential.
 * Pojo representing credentials as stored in the database.
 */
public class LocalUserCredential {
  private Long userId;
  private String password;

  private String secureSalt;
  private String securityScheme;

  private String resetToken;
  private Instant resetExpiry;

  private Instant created;
  private Instant lastUpdated;

  public LocalUserCredential() {

  }

  public LocalUserCredential(final Long userId, final String password, final String secureSalt,
                             final String securityScheme) {
    this.userId = userId;
    this.password = password;
    this.secureSalt = secureSalt;
    this.securityScheme = securityScheme;
  }


  public Long getUserId() {
    return userId;
  }

  public void setUserId(final Long userId) {
    this.userId = userId;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getSecureSalt() {
    return secureSalt;
  }

  public void setSecureSalt(final String secureSalt) {
    this.secureSalt = secureSalt;
  }

  public String getSecurityScheme() {
    return securityScheme;
  }

  public void setSecurityScheme(final String securityScheme) {
    this.securityScheme = securityScheme;
  }

  public String getResetToken() {
    return resetToken;
  }

  public void setResetToken(final String resetToken) {
    this.resetToken = resetToken;
  }

  public Instant getResetExpiry() {
    return resetExpiry;
  }

  public void setResetExpiry(final Instant resetExpiry) {
    this.resetExpiry = resetExpiry;
  }

  public Instant getCreated() {
    return created;
  }

  public void setCreated(final Instant created) {
    this.created = created;
  }

  public Instant getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(final Instant lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
}
