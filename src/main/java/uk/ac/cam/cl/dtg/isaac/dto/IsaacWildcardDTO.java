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

package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;
import uk.ac.cam.cl.dtg.isaac.dos.content.Image;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;

/**
 * ********************************************
 * <br>
 * Note: This class is currently not used.!! TODO: make sure mapping is completed.
 * ********************************************
 */
@JsonContentType("isaacWildcard")
public class IsaacWildcardDTO extends ContentDTO {
  private String description;
  private String url;

  @JsonCreator
  public IsaacWildcardDTO(
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
      @JsonProperty("image") final Image image,
      @JsonProperty("description") final String description,
      @JsonProperty("url") final String url) {
    super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value,
        attribution, relatedContent, published, deprecated, tags, level);

    this.description = description;
    this.url = url;
  }

  /**
   * Default constructor required for Jackson.
   */
  public IsaacWildcardDTO() {

  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }
}
