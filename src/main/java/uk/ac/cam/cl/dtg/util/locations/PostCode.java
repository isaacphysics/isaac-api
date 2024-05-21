/**
 * Copyright 2016 Alistair Stead
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

package uk.ac.cam.cl.dtg.util.locations;

/**
 * A class to hold the structure of a postcode.
 *
 * @author Alistair Stead
 *
 */
public class PostCode {

  @SuppressWarnings("java:S1700")
  private final String postCode;
  private final Double lat;
  private final Double lon;

  /**
   * A class to hold the structure of a postcode.
   *
   * @param postCode the string version of the postcode
   * @param lat      the latitude
   * @param lon      the longitude
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

  public String getPostCode() {
    return postCode;
  }

  /**
   * Get the Latitude.
   *
   * @return the latitude as a double
   */
  public Double getLat() {
    return lat;
  }

  /**
   * Get the Longitude.
   *
   * @return the longitude as a double
   */
  public Double getLon() {
    return lon;
  }

}
