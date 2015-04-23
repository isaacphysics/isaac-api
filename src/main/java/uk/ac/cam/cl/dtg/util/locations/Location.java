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
	 * @param address includes postal address information. 
	 * @param latitude - as a decimal value
	 * @param longitude - as a decimal value
	 */
	public Location(final Address address, final Double latitude, final Double longitude) {
		this.address = address;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * Gets the address.
	 * @return the address
	 */
	public Address getAddress() {
		return address;
	}

	/**
	 * Gets the latitude.
	 * @return the latitude
	 */
	public Double getLatitude() {
		return latitude;
	}

	/**
	 * Gets the longitude.
	 * @return the longitude
	 */
	public Double getLongitude() {
		return longitude;
	}	
}
