/*
 * Copyright 2018 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dto.users;

import java.util.Date;

/**
 * Admin view of a User Summary object, which contains additional information (e.g. last login date).
 */
public class UserSummaryForAdminUsersDTO extends UserSummaryWithEmailAddressDTO {
    private Date lastUpdated;
    private Date lastSeen;
    private Date registrationDate;
    private String schoolId;
    private String schoolOther;

    /**
     * UserSummaryDTO.
     */
    public UserSummaryForAdminUsersDTO() {

    }

    /**
     * Gets the lastUpdated.
     *
     * @return the lastUpdated
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Sets the lastUpdated.
     *
     * @param lastUpdated
     *            the lastUpdated to set
     */
    public void setLastUpdated(final Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Gets the lastSeen.
     *
     * @return the lastSeen
     */
    public Date getLastSeen() {
        return lastSeen;
    }

    /**
     * Sets the lastSeen.
     *
     * @param lastSeen
     *            the lastSeen to set
     */
    public void setLastSeen(final Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    /**
     * Gets the registrationDate.
     *
     * @return the registrationDate
     */
    public Date getRegistrationDate() {
        return registrationDate;
    }

    /**
     * Sets the registrationDate.
     *
     * @param registrationDate
     *            the registrationDate to set
     */
    public void setRegistrationDate(final Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    /**
     * Gets the schoolId.
     *
     * @return the schoolId
     */
    public String getSchoolId() {
        return schoolId;
    }

    /**
     * Sets the schoolId.
     *
     * @param schoolId
     *            the schoolId to set
     */
    public void setSchoolId(final String schoolId) {
        this.schoolId = schoolId;
    }

    /**
     * Gets the schoolOther.
     *
     * @return the schoolOther
     */
    public String getSchoolOther() {
        return schoolOther;
    }

    /**
     * Sets the schoolOther.
     *
     * @param schoolOther
     *            the schoolOther to set
     */
    public void setSchoolOther(final String schoolOther) {
        this.schoolOther = schoolOther;
    }

    @Override
    public String toString() {
        return "lastUpdated=" + "UserSummaryForAdminUsersDTO{" + lastUpdated
                + ", lastSeen=" + lastSeen
                + ", registrationDate=" + registrationDate
                + ", schoolId=" + schoolId
                + ", schoolOther=" + schoolOther
                + '}';
    }
}
