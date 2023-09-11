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
import uk.ac.cam.cl.dtg.isaac.dto.IsaacPodDTO;

/**
 * DO for isaac featured profiles.
 *
 */
@DTOMapping(IsaacPodDTO.class)
@JsonContentType("isaacPod")
public class IsaacPod extends Content {
  private Image image;
  private String url;

  @JsonCreator
  public IsaacPod(
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
      @JsonProperty("url") final String url) {
    super(id, title, subtitle, type, author, encoding,
        canonicalSourceFile, layout, children, value, attribution,
        relatedContent, published, deprecated, tags, level);

    this.url = url;
    this.image = image;
  }

  /**
   * Default constructor required for Jackson.
   */
  public IsaacPod() {

  }

  /**
   * Gets the image.
   * @return the image
   */
  public Image getImage() {
    return image;
  }

  /**
   * Sets the image.
   * @param image the image to set
   */
  public void setImage(final Image image) {
    this.image = image;
  }

  /**
   * Gets the url.
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the url.
   * @param url the url to set
   */
  public void setUrl(final String url) {
    this.url = url;
  }
}
