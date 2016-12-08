/**
 * Copyright 2014 Stephen Cummins
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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * School information POJO.
 * 
 */
public class School {
    private String urn;
    private String name;
    private String postcode;

    /**
     * Enum to represent where this school object was created.
     */
    public enum SchoolDataSource {
        GOVERNMENT_UK, GOVERNMENT_IE, GOVERNMENT_SCO, GOVERNMENT_WAL, USER_ENTERED;

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
     */
    public School(final String urn, final String name, final String postcode, final SchoolDataSource dataSource) {
        this.urn = urn;
        this.name = name;
        this.postcode = postcode;
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
     * Gets the verifiedSchool.
     * 
     * @return the verifiedSchool
     */
    public SchoolDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Sets the dataSource.
     * 
     * @param dataSource
     *            the dataSource to set
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
