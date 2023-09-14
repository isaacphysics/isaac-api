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

package uk.ac.cam.cl.dtg.isaac.dto.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Content Class (Data Transfer Object) This class represents a majority of content types within the Content Management
 * system. It is generalised to encourage reuse as much as is appropriate. This object should be kept as being easily
 * serializable to enable it to be exposed via web views.
 *
 */
public class ContentDTO extends ContentBaseDTO {
  private String title;
  private String subtitle;
  private String author;
  private String encoding;
  private String layout;
  // this is the actual list of children content objects.
  private List<ContentBaseDTO> children;
  private String value;
  private String attribution;
  private List<ContentSummaryDTO> relatedContent;
  private Boolean published;
  private Boolean deprecated;
  private Integer level;
  private Boolean expandable;

  @JsonCreator
  public ContentDTO(@JsonProperty("id") final String id,
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
                    @JsonProperty("published") final Boolean published,
                    @JsonProperty("deprecated") final Boolean deprecated,
                    @JsonProperty("tags") final Set<String> tags,
                    @JsonProperty("level") final Integer level) {
    this.setId(id);
    this.title = title;
    this.subtitle = subtitle;
    this.setType(type != null ? type : "string");
    this.author = author;
    this.encoding = encoding;
    this.setCanonicalSourceFile(canonicalSourceFile);
    this.layout = layout;
    this.value = value;
    this.attribution = attribution;
    this.relatedContent = relatedContent;
    this.published = published;
    this.deprecated = deprecated;
    this.children = children;
    this.setTags(tags);
    this.level = level;

    // useful for when we want to augment this POJO
    if (null == this.children) {
      this.children = new ArrayList<ContentBaseDTO>();
    }

    if (null == this.getTags()) {
      this.setTags(new HashSet<String>());
    }
  }

  /**
   * Basic constructor to allow communication of a simple value.
   *
   * @param value
   *            - value of the content to create.
   */
  public ContentDTO(final String value) {
    this.value = value;
    this.setType("content");
    this.encoding = "markdown";

    // useful for when we want to augment this POJO
    if (null == this.children) {
      this.children = Lists.newArrayList();
    }

    if (null == this.getTags()) {
      this.setTags(Sets.newHashSet());
    }
  }

  /**
   * Default constructor required for Jackson.
   */
  public ContentDTO() {
    // useful for when we want to augment this POJO
    this.children = Lists.newArrayList();
    this.setTags(Sets.newHashSet());
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public void setSubtitle(final String subtitle) {
    this.subtitle = subtitle;
  }

  @JsonIgnore
  public String getAuthor() {
    return author;
  }

  public void setAuthor(final String author) {
    this.author = author;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(final String encoding) {
    this.encoding = encoding;
  }

  public String getLayout() {
    return layout;
  }

  public void setLayout(final String layout) {
    this.layout = layout;
  }

  public String getValue() {
    return value;
  }

  public void setValue(final String value) {
    this.value = value;
  }

  public String getAttribution() {
    return attribution;
  }

  public void setAttribution(final String attribution) {
    this.attribution = attribution;
  }

  public List<ContentSummaryDTO> getRelatedContent() {
    return relatedContent;
  }

  public void setRelatedContent(final List<ContentSummaryDTO> relatedContent) {
    this.relatedContent = relatedContent;
  }

  public List<ContentBaseDTO> getChildren() {
    return this.children;
  }

  public void setChildren(final List<ContentBaseDTO> children) {
    this.children = children;
  }

  /**
   * Gets the published.
   *
   * @return the published
   */
  public Boolean getPublished() {
    return published;
  }

  /**
   * Sets the published.
   *
   * @param published
   *            the published to set
   */
  public void setPublished(final Boolean published) {
    this.published = published;
  }

  public Boolean getDeprecated() {
    return deprecated;
  }

  public void setDeprecated(final Boolean deprecated) {
    this.deprecated = deprecated;
  }

  public Integer getLevel() {
    return level;
  }

  public void setLevel(final Integer level) {
    this.level = level;
  }

  public Boolean getExpandable() {
    return this.expandable;
  }

  public void setExpandable(final Boolean expandable) {
    this.expandable = expandable;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof ContentDTO)) {
      return false;
    }

    ContentDTO c = (ContentDTO) o;
    boolean result = true;

    if (this.getId() != null) {
      result = result && this.getId().equals(c.getId());
    }
    if (this.title != null) {
      result = result && this.title.equals(c.getTitle());
    }
    if (this.value != null) {
      result = result && this.value.equals(c.getValue());
    }
    if (this.getCanonicalSourceFile() != null) {
      result = result && this.getCanonicalSourceFile().equals(c.getCanonicalSourceFile());
    }

    return result;
  }

  @Override
  public int hashCode() {
    int hashCode = 0;

    if (this.getId() != null) {
      hashCode = hashCode + this.getId().hashCode();
    }
    if (this.title != null) {
      hashCode = hashCode + this.title.hashCode();
    }
    if (this.value != null) {
      hashCode = hashCode + this.value.hashCode();
    }

    return hashCode;
  }

  @Override
  public String toString() {
    return super.toString() + " Title: " + this.title;
  }

}
