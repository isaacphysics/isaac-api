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
 *
 */
public class Address {
	private String addressLine1;
	private String addressLine2;
	private String town;
	private String county;
	private String postalCode;
	private String country;
	
	/**
	 * Address.
	 */
	public Address() {
		
	}
	
	/**
	 * @param addressLine1 - as a string
	 * @param addressLine2 -  as a string
	 * @param town - as a string
	 * @param county - as a string
	 * @param postalCode - as a string
	 * @param country - as a string
	 */
	public Address(final String addressLine1, final String addressLine2, final String town,
			final String county, final String postalCode, final String country) {
		this.addressLine1 = addressLine1;
		this.addressLine2 = addressLine2;
		this.town = town;
		this.county = county;
		this.postalCode = postalCode;
		this.country = country;
	}

	/**
	 * Gets the addressLine1.
	 * @return the addressLine1
	 */
	public String getAddressLine1() {
		return addressLine1;
	}
	/**
	 * Sets the addressLine1.
	 * @param addressLine1 the addressLine1 to set
	 */
	public void setAddressLine1(final String addressLine1) {
		this.addressLine1 = addressLine1;
	}
	/**
	 * Gets the addressLine2.
	 * @return the addressLine2
	 */
	public String getAddressLine2() {
		return addressLine2;
	}
	/**
	 * Sets the addressLine2.
	 * @param addressLine2 the addressLine2 to set
	 */
	public void setAddressLine2(final String addressLine2) {
		this.addressLine2 = addressLine2;
	}
	/**
	 * Gets the town.
	 * @return the town
	 */
	public String getTown() {
		return town;
	}
	/**
	 * Sets the town.
	 * @param town the town to set
	 */
	public void setTown(final String town) {
		this.town = town;
	}
	/**
	 * Gets the county.
	 * @return the county
	 */
	public String getCounty() {
		return county;
	}
	/**
	 * Sets the county.
	 * @param county the county to set
	 */
	public void setCounty(final String county) {
		this.county = county;
	}
	/**
	 * Gets the postalCode.
	 * @return the postalCode
	 */
	public String getPostalCode() {
		return postalCode;
	}
	/**
	 * Sets the postalCode.
	 * @param postalCode the postalCode to set
	 */
	public void setPostalCode(final String postalCode) {
		this.postalCode = postalCode;
	}

	/**
	 * Gets the country.
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * Sets the country.
	 * @param country the country to set
	 */
	public void setCountry(final String country) {
		this.country = country;
	}
	
}
