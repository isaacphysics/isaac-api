/*
 * Copyright 2016 Alistair Stead
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
package uk.ac.cam.cl.dtg.util.locations;

/**
 * A class to hold the structure of a postcode.
 *
 * @author Alistair Stead
 *
 */
public record PostCode(String postCode, Double lat, Double lon) {

    /**
     * A class to hold the structure of a postcode.
     * 
     * @param postCode
     *            - the string version of the postcode
     * @param lat
     *            - the latitude
     * @param lon
     *            - the longitude
     */
    public PostCode(final String postCode, final Double lat, final Double lon) {
        // Strip whitespace to make comparison easier
        if (postCode != null) {
            this.postCode = postCode.replace(" ", "");
        } else {
            this.postCode = null;
        }

        this.lat = lat;
        this.lon = lon;
    }
}
