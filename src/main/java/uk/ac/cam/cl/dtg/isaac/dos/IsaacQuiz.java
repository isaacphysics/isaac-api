/*
 * Copyright 2021 Raspberry Pi Foundation
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;

/**
 * DO for isaac quiz.
 */
@DTOMapping(IsaacQuizDTO.class)
@JsonContentType("isaacQuiz")
public class IsaacQuiz extends SeguePage {
  @Deprecated
  private boolean visibleToStudents;
  private List<String> hiddenFromRoles;
  private Content rubric;

  @JsonCreator
  public IsaacQuiz(
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
      @JsonProperty("visibleToStudents") final boolean visibleToStudents,
      @JsonProperty("hiddenFromRoles") final List<String> hiddenFromRoles,
      @JsonProperty("rubric") final Content rubric) {
    super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value, attribution,
        relatedContent, published, deprecated, tags, level);

    this.visibleToStudents = visibleToStudents;
    this.hiddenFromRoles = hiddenFromRoles;
    this.rubric = rubric;
  }

  /**
   * Default constructor required for Jackson.
   */
  public IsaacQuiz() {

  }

  @Deprecated
  public boolean getVisibleToStudents() {
    return visibleToStudents;
  }

  @Deprecated
  public void setVisibleToStudents(final boolean visibleToStudents) {
    this.visibleToStudents = visibleToStudents;
  }

  public Content getRubric() {
    return rubric;
  }

  public void setRubric(final Content rubric) {
    this.rubric = rubric;
  }

  public List<String> getHiddenFromRoles() {
    return hiddenFromRoles;
  }

  public void setHiddenFromRoles(final List<String> hiddenFromRoles) {
    this.hiddenFromRoles = hiddenFromRoles;
  }
}
