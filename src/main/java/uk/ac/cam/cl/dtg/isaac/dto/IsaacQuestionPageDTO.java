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
import uk.ac.cam.cl.dtg.isaac.dos.Difficulty;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;

/**
 * IsaacQuestion Page DTO.
 *
 */
@JsonContentType("isaacQuestionPage")
public class IsaacQuestionPageDTO extends SeguePageDTO {
  private Float passMark;
  private String supersededBy;
  private Difficulty difficulty;

  @JsonCreator
  public IsaacQuestionPageDTO(
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
      @JsonProperty("published") final Boolean published,
      @JsonProperty("tags") final Set<String> tags,
      @JsonProperty("deprecated") final Boolean deprecated,
      @JsonProperty("level") final Integer level,
      @JsonProperty("difficulty") final Difficulty difficulty,
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
  public IsaacQuestionPageDTO() {

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

  public Difficulty getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(final Difficulty difficulty) {
    this.difficulty = difficulty;
  }

}
