/**
 * Copyright 2021 Raspberry Pi Foundation
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
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;

/**
 * IsaacQuiz Page DO.
 *
 */
@DTOMapping(IsaacQuizSectionDTO.class)
@JsonContentType("isaacQuizSection")
public class IsaacQuizSection extends Content {

  @JsonCreator
  public IsaacQuizSection(
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
      @JsonProperty("level") final Integer level) {
    super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value, attribution,
        relatedContent, published, deprecated, tags, level);
  }

  /**
   * Default constructor required for Jackson.
   */
  public IsaacQuizSection() {
  }
}
