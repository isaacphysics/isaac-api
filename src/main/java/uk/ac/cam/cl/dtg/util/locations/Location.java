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
package uk.ac.cam.cl.dtg.util.locations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author sac92
 *
 */
public class Location {
    private Address address;
    private Double latitude;
    private Double longitude;

    /**
     * 
     * @param address
     *            includes postal address information.
     * @param latitude
     *            - as a decimal value
     * @param longitude
     *            - as a decimal value
     */
    @JsonCreator
    public Location(@JsonProperty("address") final Address address, @JsonProperty("latitude") final Double latitude,
            @JsonProperty("longitude") final Double longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Gets the address.
     * 
     * @return the address
     */
    public Address getAddress() {
        return address;
    }

    /**
     * Sets the address.
     *
     * @param address the location to set
     */
    public void setAddress(final Address address) {
        this.address = address;
    }

    /**
     * Gets the latitude.
     * 
     * @return the latitude
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * Gets the longitude.
     * 
     * @return the longitude
     */
    public Double getLongitude() {
        return longitude;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + ((latitude == null) ? 0 : latitude.hashCode());
        result = prime * result + ((longitude == null) ? 0 : longitude.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Location)) {
            return false;
        }
        Location other = (Location) obj;
        if (address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!address.equals(other.address)) {
            return false;
        }
        if (latitude == null) {
            if (other.latitude != null) {
                return false;
            }
        } else if (!latitude.equals(other.latitude)) {
            return false;
        }
        if (longitude == null) {
            if (other.longitude != null) {
                return false;
            }
        } else if (!longitude.equals(other.longitude)) {
            return false;
        }
        return true;
    }
}
