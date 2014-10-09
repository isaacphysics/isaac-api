/**
 * Copyright 2014 Nick Rogers
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DO representing a Facebook User.
 *
 */
public class FacebookUser {
	private String id;
	private String firstName;
	private String email;
	private String gender;
	private String lastName;
	private String link;
	private String locale;
	private String name;
	private Integer timezone;
	private String updatedTime;
	private boolean verified;

	/**
	 * 
	 * @param id -
	 * @param firstName - 
	 * @param email - 
	 * @param gender -
	 * @param lastName -
	 * @param link - 
	 * @param locale - 
	 * @param name - 
	 * @param timezone -
	 * @param updatedTime -
	 * @param verified -
	 */
	@JsonCreator
	public FacebookUser(@JsonProperty("id") final String id,
			@JsonProperty("first_name") final String firstName,
			@JsonProperty("email") final String email,
			@JsonProperty("gender") final String gender,
			@JsonProperty("last_name") final String lastName,
			@JsonProperty("link") final String link,
			@JsonProperty("locale") final String locale,
			@JsonProperty("name") final String name,
			@JsonProperty("timezone") final Integer timezone,
			@JsonProperty("updated_time") final String updatedTime,
			@JsonProperty("verified") final boolean verified) {
		this.id = id;
		this.firstName = firstName;
		this.email = email;
		this.gender = gender;
		this.lastName = lastName;
		this.locale = locale;
		this.name = name;
		this.timezone = timezone;
		this.updatedTime = updatedTime;
		this.verified = verified;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @param firstName
	 *            the firstName to set
	 */
	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	/**
	 * @return the email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email
	 *            the email to set
	 */
	public void setEmail(final String email) {
		this.email = email;
	}

	/**
	 * @return the gender
	 */
	public String getGender() {
		return gender;
	}

	/**
	 * @param gender
	 *            the gender to set
	 */
	public void setGender(final String gender) {
		this.gender = gender;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @param lastName
	 *            the lastName to set
	 */
	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	/**
	 * @return the link
	 */
	public String getLink() {
		return link;
	}

	/**
	 * @param link
	 *            the link to set
	 */
	public void setLink(final String link) {
		this.link = link;
	}

	/**
	 * @return the locale
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * @param locale
	 *            the locale to set
	 */
	public void setLocale(final String locale) {
		this.locale = locale;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @return the timezone
	 */
	public Integer getTimezone() {
		return timezone;
	}

	/**
	 * @param timezone
	 *            the timezone to set
	 */
	public void setTimezone(final Integer timezone) {
		this.timezone = timezone;
	}

	/**
	 * @return the updatedTime
	 */
	public String getUpdatedTime() {
		return updatedTime;
	}

	/**
	 * @param updatedTime
	 *            the updatedTime to set
	 */
	public void setUpdatedTime(final String updatedTime) {
		this.updatedTime = updatedTime;
	}

	/**
	 * @return the verified
	 */
	public boolean isVerified() {
		return verified;
	}

	/**
	 * @param verified
	 *            the verified to set
	 */
	public void setVerified(final boolean verified) {
		this.verified = verified;
	}
}
