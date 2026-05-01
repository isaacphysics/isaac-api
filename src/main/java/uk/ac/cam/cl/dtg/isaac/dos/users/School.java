/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dos.users;

/**
 * School information POJO.
 * 
 */
public class School {
    private String schoolId;
    private String countryCode;
    private String schoolName;
    private String town;
    private String postalCode;
    private Boolean excluded;
    private Boolean closed;

    /**
     * Enum to represent where this school object was created.
     */
    public enum SchoolDataSource {
        GOVERNMENT_UK, GOVERNMENT_IE, GOVERNMENT_SCT, GOVERNMENT_SCT_IND, GOVERNMENT_WLS, GOVERNMENT_NIR, USER_ENTERED;

        @Override
        public String toString() {
            return this.name();
        }
    }

    private SchoolDataSource dataSource;

    /**
     * Default Constructor for mappers.
     */
    public School() {

    }

    /**
     * Full constructor.
     *
     * @param schoolId
     *            - unique school ID
     * @param countryCode
     *            - country code for the school
     * @param schoolName
     *            - name of the school
     * @param town
     *            - name of the town where the school is located
     * @param postalCode
     *            - postal code of the school
     * @param excluded
     *            - whether the school is excluded when searching for schools by name
     * @param closed
     *            - whether the school is closed
     * @param dataSource
     *            - data source of this information
     */
    public School(final String schoolId, final String countryCode, final String schoolName, final String town,
                  final String postalCode, final Boolean excluded, final Boolean closed, final SchoolDataSource dataSource) {
        this.schoolId = schoolId;
        this.countryCode = countryCode;
        this.schoolName = schoolName;
        this.town = town;
        this.postalCode = postalCode;
        this.excluded = excluded;
        this.closed = closed;
        this.dataSource = dataSource;
    }

    /**
     * Gets the school ID.
     *
     * @return the school ID
     */
    public String getSchoolId() {
        return schoolId;
    }

    /**
     * Sets the school ID.
     *
     * @param schoolId
     *            the school ID to set
     */
    public void setSchoolId(final String schoolId) {
        this.schoolId = schoolId;
    }

    /**
     * Gets the country code.
     *
     * @return the country code
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * Sets the country code.
     *
     * @param countryCode
     *            the country code to set
     */
    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    /**
     * Gets the school name.
     *
     * @return the school name
     */
    public String getSchoolName() {
        return schoolName;
    }

    /**
     * Sets the school name.
     *
     * @param schoolName
     *            the school name to set
     */
    public void setSchoolName(final String schoolName) {
        this.schoolName = schoolName;
    }

    /**
     * Gets the town.
     *
     * @return the town
     */
    public String getTown() {
        return town;
    }

    /**
     * Sets the town.
     *
     * @param town
     *            the town to set
     */
    public void setTown(final String town) {
        this.town = town;
    }

    /**
     * Gets the postal code.
     *
     * @return the postal code
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * Sets the postal code.
     *
     * @param postalCode
     *            the postal code to set
     */
    public void setPostalCode(final String postalCode) {
        this.postalCode = postalCode;
    }

    /**
     * Gets the excluded status.
     *
     * @return whether the school is excluded when searching for schools by name
     */
    public Boolean getExcluded() {
        return excluded;
    }

    /**
     * Sets the excluded status.
     *
     * @param excluded
     *            whether the school should be excluded when searching for schools by name
     */
    public void setExcluded(final Boolean excluded) {
        this.excluded = excluded;
    }

    /**
     * Gets the closed status.
     *
     * @return whether the school is closed
     */
    public Boolean getClosed() {
        return closed;
    }

    /**
     * Sets the closed status.
     *
     * @param closed
     *            whether the school is closed
     */
    public void setClosed(final Boolean closed) {
        this.closed = closed;
    }

    /**
     * Gets the data source for the school information.
     * 
     * @return the data source
     */
    public SchoolDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets the data source for the school information.
     * 
     * @param dataSource
     *            the data source to set
     */
    public void setDataSource(final SchoolDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((schoolId == null) ? 0 : schoolId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof School)) {
            return false;
        }
        School other = (School) obj;
        if (schoolId == null) {
            if (other.schoolId != null) {
                return false;
            }
        } else if (!schoolId.equals(other.schoolId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "School [id=" + schoolId + ", name=" + schoolName + ", postcode=" + postalCode + "]";
    }
}
