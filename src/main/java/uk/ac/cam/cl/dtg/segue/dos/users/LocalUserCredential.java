/*
 * Copyright 2017 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dos.users;

import java.util.Date;

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
    private Date resetExpiry;

    private Date created;
    private Date lastUpdated;

    public LocalUserCredential() {

    }

    public LocalUserCredential(Long userId, String password, String secureSalt, String securityScheme) {
        this.userId = userId;
        this.password = password;
        this.secureSalt = secureSalt;
        this.securityScheme = securityScheme;
    }


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSecureSalt() {
        return secureSalt;
    }

    public void setSecureSalt(String secureSalt) {
        this.secureSalt = secureSalt;
    }

    public String getSecurityScheme() {
        return securityScheme;
    }

    public void setSecurityScheme(String securityScheme) {
        this.securityScheme = securityScheme;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public Date getResetExpiry() {
        return resetExpiry;
    }

    public void setResetExpiry(Date resetExpiry) {
        this.resetExpiry = resetExpiry;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
