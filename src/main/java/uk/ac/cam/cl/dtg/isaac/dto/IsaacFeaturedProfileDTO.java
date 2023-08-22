/**
 * Copyright 2014 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ImageDTO;

import java.util.List;
import java.util.Set;

/**
 * DO for isaac featured profiles.
 *
 */
@JsonContentType("isaacFeaturedProfile")
public class IsaacFeaturedProfileDTO extends ContentDTO {
    private String emailAddress;
    private ImageDTO image;
    private String homepage;

    @JsonCreator
    public IsaacFeaturedProfileDTO(
            @JsonProperty("id") final String id,
            @JsonProperty("title") final String title,
            @JsonProperty("subtitle") final String subtitle,
            @JsonProperty("type") final String type,
            @JsonProperty("author") final String author,
            @JsonProperty("encoding") final String encoding,
            @JsonProperty("canonicalSourceFile") final String canonicalSourceFile,
            @JsonProperty("layout") final String layout,
            @JsonProperty("children") final List<ContentBaseDTO> children,
            @JsonProperty("value") final String value,
            @JsonProperty("attribution") final String attribution,
            @JsonProperty("relatedContent") final List<ContentSummaryDTO> relatedContent,
            @JsonProperty("version") final boolean published,
            @JsonProperty("tags") final Set<String> tags,
            @JsonProperty("deprecated") final Boolean deprecated,
            @JsonProperty("level") final Integer level,
            @JsonProperty("src") final String src,
            @JsonProperty("altText") final String altText,
            @JsonProperty("emailAddress") final String emailAddress,
            @JsonProperty("image") final ImageDTO image,
            @JsonProperty("homepage") final String homepage) {
        super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value,
                attribution, relatedContent, published, deprecated, tags, level);

        this.emailAddress = emailAddress;
        this.image = image;
    }

    /**
     * Default constructor required for Jackson.
     */
    public IsaacFeaturedProfileDTO() {

    }

    /**
     * Gets the emailAddress.
     * 
     * @return the emailAddress
     */
    public String getEmailAddress() {
        return emailAddress;
    }

    /**
     * Sets the emailAddress.
     * 
     * @param emailAddress
     *            the emailAddress to set
     */
    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    /**
     * Gets the image.
     * 
     * @return the image
     */
    public ImageDTO getImage() {
        return image;
    }

    /**
     * Sets the image.
     * 
     * @param image
     *            the image to set
     */
    public void setImage(final ImageDTO image) {
        this.image = image;
    }

    /**
     * Gets the homepage.
     * 
     * @return the homepage
     */
    public String getHomepage() {
        return homepage;
    }

    /**
     * Sets the homepage.
     * 
     * @param homepage
     *            the homepage to set
     */
    public void setHomepage(final String homepage) {
        this.homepage = homepage;
    }
}
