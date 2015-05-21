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
package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacFeaturedProfileDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.Image;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;

/**
 * DO for isaac featured profiles.
 *
 */
@DTOMapping(IsaacFeaturedProfileDTO.class)
@JsonContentType("isaacFeaturedProfile")
public class IsaacFeaturedProfile extends Content {

	protected String emailAddress;
	protected Image image;
	protected String homepage;

	@JsonCreator
	public IsaacFeaturedProfile(@JsonProperty("_id") String _id,
			@JsonProperty("id") String id, @JsonProperty("title") String title,
			@JsonProperty("subtitle") String subtitle,
			@JsonProperty("type") String type,
			@JsonProperty("author") String author,
			@JsonProperty("encoding") String encoding,
			@JsonProperty("canonicalSourceFile") String canonicalSourceFile,
			@JsonProperty("layout") String layout,
			@JsonProperty("children") List<ContentBase> children,
			@JsonProperty("value") String value,
			@JsonProperty("attribution") String attribution,
			@JsonProperty("relatedContent") List<String> relatedContent,
			@JsonProperty("version") boolean published,
			@JsonProperty("tags") Set<String> tags,
			@JsonProperty("level") Integer level,
			@JsonProperty("emailAddress") String emailAddress,
			@JsonProperty("image") Image image,
			@JsonProperty("homepage") String homepage) {
		super(_id, id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level);

		this.emailAddress = emailAddress;
		this.image = image;
		this.homepage = homepage;
	}

	/**
	 * Default constructor required for Jackson
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
