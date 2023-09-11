/**
 * Copyright 2014 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacWildcardDTO;

/**
 * IsaacWildcard Represents gameboard advertising space.
 */
@JsonContentType("isaacWildcard")
@DTOMapping(IsaacWildcardDTO.class)
public class IsaacWildcard extends Content {
  private String description;
  private String url;

  @JsonCreator
  public IsaacWildcard(
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
      @JsonProperty("published") final boolean published,
      @JsonProperty("deprecated") final Boolean deprecated,
      @JsonProperty("tags") final Set<String> tags,
      @JsonProperty("level") final Integer level,
      @JsonProperty("description") final String description,
      @JsonProperty("url") final String url) {
    super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value, attribution,
        relatedContent, published, deprecated, tags, level);

    this.description = description;
    this.url = url;
  }

  /**
   * Default constructor required for Jackson.
   */
  public IsaacWildcard() {

  }

  /**
   * getDescription.
   *
   * @return the description.
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description
   *            of the wildcard
   */
  public void setDescription(final String description) {
    this.description = description;
  }

  /**
   * getUrl.
   *
   * @return url
   */
  public String getUrl() {
    // It appears as though sometimes urls are provided with trailing spaces in git...
    // I do not know why...
    if (url != null) {
      return url.trim();
    } else {
      return null;
    }
  }

  /**
   * @param url
   *            - navigation url
   */
  public void setUrl(final String url) {
    this.url = url;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("IsaacWildcard [description=");
    builder.append(description);
    builder.append(", url=");
    builder.append(url);
    builder.append(", title=");
    builder.append(getTitle());
    builder.append(", subtitle=");
    builder.append(getSubtitle());
    builder.append(", author=");
    builder.append(getAuthor());
    builder.append(", encoding=");
    builder.append(getEncoding());
    builder.append(", layout=");
    builder.append(getLayout());
    builder.append(", children=");
    builder.append(getChildren());
    builder.append(", value=");
    builder.append(getValue());
    builder.append(", attribution=");
    builder.append(getAttribution());
    builder.append(", relatedContent=");
    builder.append(getRelatedContent());
    builder.append(", published=");
    builder.append(getPublished());
    builder.append(", level=");
    builder.append(getLevel());
    builder.append(", id=");
    builder.append(getId());
    builder.append(", type=");
    builder.append(getType());
    builder.append(", tags=");
    builder.append(getTags());
    builder.append(", canonicalSourceFile=");
    builder.append(getCanonicalSourceFile());
    builder.append(", version=");
    builder.append(getVersion());
    builder.append("]");
    return builder.toString();
  }
}
