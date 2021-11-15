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
package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ImageDTO;

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
    public IsaacFeaturedProfileDTO( @JsonProperty("id") String id,
            @JsonProperty("title") String title, @JsonProperty("subtitle") String subtitle,
            @JsonProperty("type") String type, @JsonProperty("author") String author,
            @JsonProperty("encoding") String encoding, @JsonProperty("canonicalSourceFile") String canonicalSourceFile,
            @JsonProperty("layout") String layout, @JsonProperty("children") List<ContentBaseDTO> children,
            @JsonProperty("value") String value, @JsonProperty("attribution") String attribution,
            @JsonProperty("relatedContent") List<ContentSummaryDTO> relatedContent,
            @JsonProperty("version") boolean published, @JsonProperty("tags") Set<String> tags,
            @JsonProperty("deprecated") Boolean deprecated,
            @JsonProperty("level") Integer level, @JsonProperty("src") String src,
            @JsonProperty("altText") String altText, @JsonProperty("emailAddress") String emailAddress,
            @JsonProperty("image") ImageDTO image, @JsonProperty("homepage") String homepage) {
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
