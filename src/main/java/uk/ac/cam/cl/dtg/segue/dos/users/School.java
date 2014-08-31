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
	@JsonIgnore
	private String establishmentNumber;
	private String name;
	private String postcode;

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
	 * @param establishmentNumber
	 *            - unique id for the establishment.
	 * @param name
	 *            - name of the school.
	 * @param postcode
	 *            -postcode of the school
	 */
	public School(final String urn, final String establishmentNumber, final String name,
			final String postcode) {
		this.urn = urn;
		this.establishmentNumber = establishmentNumber;
		this.name = name;
		this.postcode = postcode;
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
	 * Gets the establishmentNumber.
	 * @return the establishmentNumber
	 */
	@JsonIgnore
	public String getEstablishmentNumber() {
		return establishmentNumber;
	}

	/**
	 * Sets the establishmentNumber.
	 * @param establishmentNumber the establishmentNumber to set
	 */
	public void setEstablishmentNumber(final String establishmentNumber) {
		this.establishmentNumber = establishmentNumber;
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
}
