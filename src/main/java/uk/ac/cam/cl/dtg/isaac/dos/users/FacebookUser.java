/**
 * Copyright 2014 Nick Rogers
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

package uk.ac.cam.cl.dtg.isaac.dos.users;

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

  @JsonCreator
  public FacebookUser(@JsonProperty("id") final String id, @JsonProperty("first_name") final String firstName,
                      @JsonProperty("email") final String email, @JsonProperty("gender") final String gender,
                      @JsonProperty("last_name") final String lastName, @JsonProperty("link") final String link,
                      @JsonProperty("locale") final String locale, @JsonProperty("name") final String name,
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

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(final String firstName) {
    this.firstName = firstName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(final String gender) {
    this.gender = gender;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(final String lastName) {
    this.lastName = lastName;
  }

  public String getLink() {
    return link;
  }

  public void setLink(final String link) {
    this.link = link;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(final String locale) {
    this.locale = locale;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Integer getTimezone() {
    return timezone;
  }

  public void setTimezone(final Integer timezone) {
    this.timezone = timezone;
  }

  public String getUpdatedTime() {
    return updatedTime;
  }

  public void setUpdatedTime(final String updatedTime) {
    this.updatedTime = updatedTime;
  }

  public boolean isVerified() {
    return verified;
  }

  public void setVerified(final boolean verified) {
    this.verified = verified;
  }
}
