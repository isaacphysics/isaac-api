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
    // Old fieldnames, to be removed
    private String urn;
    private String name;
    private String postcode;

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
     * @param urn
     *            - unique id
     * @param name
     *            - name of the school.
     * @param postcode
     *            -postcode of the school
     * @param dataSource
     *            -dataSource of this information
     * @param closed
     *            - whether the school is closed
     */
    public School(final String urn, final String name, final String postcode, final Boolean closed, final SchoolDataSource dataSource) {
        this.urn = urn;
        this.name = name;
        this.postcode = postcode;
        this.dataSource = dataSource;
        this.closed = closed;
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
     * Gets the urn.
     * 
     * @return the urn
     */
    public String getUrn() {
        return urn;
    }

    /**
     * Sets the urn.
     * 
     * @param urn
     *            the urn to set
     */
    public void setUrn(final String urn) {
        this.urn = urn;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     * 
     * @param name
     *            the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the postcode.
     * 
     * @return the postcode
     */
    public String getPostcode() {
        return postcode;
    }

    /**
     * Sets the postcode.
     * 
     * @param postcode
     *            the postcode to set
     */
    public void setPostcode(final String postcode) {
        this.postcode = postcode;
    }


    /**
     * Gets the school id.
     *
     * @return the school id
     */
    public String getSchoolId() {
        return schoolId;
    }

    /**
     * Sets the school id.
     *
     * @param schoolId
     *            the school id to set
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
        result = prime * result + ((urn == null) ? 0 : urn.hashCode());
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
        if (urn == null) {
            if (other.urn != null) {
                return false;
            }
        } else if (!urn.equals(other.urn)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "School [urn=" + urn + ", name=" + name + ", postcode=" + postcode + "]";
    }
}
