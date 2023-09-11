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
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;

/**
 * IsaacQuestion Page DO.
 *
 */
@DTOMapping(IsaacQuestionPageDTO.class)
@JsonContentType("isaacQuestionPage")
public class IsaacQuestionPage extends SeguePage {
  private Float passMark;
  private String supersededBy;
  private Integer difficulty;

  @JsonCreator
  public IsaacQuestionPage(
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
      @JsonProperty("difficulty") final Integer difficulty,
      @JsonProperty("passMark") final Float passMark,
      @JsonProperty("supersededBy") final String supersededBy) {
    super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value,
        attribution, relatedContent, published, deprecated, tags, level);

    this.passMark = passMark;
    this.supersededBy = supersededBy;
    this.difficulty = difficulty;
  }

  /**
   * Default constructor required for Jackson.
   */
  public IsaacQuestionPage() {
  }

  public Float getPassMark() {
    return passMark;
  }

  public void setPassMark(final Float passMark) {
    this.passMark = passMark;
  }

  public String getSupersededBy() {
    return supersededBy;
  }

  public void setSupersededBy(final String supersededBy) {
    this.supersededBy = supersededBy;
  }

  public Integer getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(final Integer difficulty) {
    this.difficulty = difficulty;
  }
}
