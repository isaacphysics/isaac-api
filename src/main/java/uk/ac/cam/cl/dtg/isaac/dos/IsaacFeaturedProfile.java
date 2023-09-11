/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.isaac.dos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.Image;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacFeaturedProfileDTO;

/**
 * DO for isaac featured profiles.
 *
 */
@DTOMapping(IsaacFeaturedProfileDTO.class)
@JsonContentType("isaacFeaturedProfile")
public class IsaacFeaturedProfile extends Content {

  private String emailAddress;
  private Image image;
  private String homepage;

  @JsonCreator
  public IsaacFeaturedProfile(
      @JsonProperty("id") final String id,
      @JsonProperty("title") final String title,
      @JsonProperty("subtitle") final String subtitle,
      @JsonProperty("type") final String type,
      @JsonProperty("author") final String author,
      @JsonProperty("encoding") final String encoding,
      @JsonProperty("canonicalSourceFile") final String canonicalSourceFile,
      @JsonProperty("layout") final String layout,
      @JsonProperty("children") final List<ContentBase> children,
      @JsonProperty("value") final String value,
      @JsonProperty("attribution") final String attribution,
      @JsonProperty("relatedContent") final List<String> relatedContent,
      @JsonProperty("version") final boolean published,
      @JsonProperty("deprecated") final Boolean deprecated,
      @JsonProperty("tags") final Set<String> tags,
      @JsonProperty("level") final Integer level,
      @JsonProperty("emailAddress") final String emailAddress,
      @JsonProperty("image") final Image image,
      @JsonProperty("homepage") final String homepage) {
    super(id, title, subtitle, type, author, encoding,
        canonicalSourceFile, layout, children, value, attribution,
        relatedContent, published, deprecated, tags, level);

    this.emailAddress = emailAddress;
    this.image = image;
    this.homepage = homepage;
  }

  /**
   * Default constructor required for Jackson.
   */
  public IsaacFeaturedProfile() {

  }

  /**
   * Gets the e-mail address.
   * @return the email
   */
  public String getEmailAddress() {
    return emailAddress;
  }

  /**
   * Sets the email address.
   * @param emailAddress to set
   */
  public void setEmailAddress(final String emailAddress) {
    this.emailAddress = emailAddress;
  }

  /**
   * Get the profile image.
   * @return the image
   */
  public Image getImage() {
    return image;
  }

  /**
   * Set the image for the profile.
   * @param image the image to set
   */
  public void setImage(final Image image) {
    this.image = image;
  }

  /**
   * Gets the homepage.
   * @return the homepage
   */
  public String getHomepage() {
    return homepage;
  }

  /**
   * Sets the homepage.
   * @param homepage the homepage to set
   */
  public void setHomepage(final String homepage) {
    this.homepage = homepage;
  }
}
