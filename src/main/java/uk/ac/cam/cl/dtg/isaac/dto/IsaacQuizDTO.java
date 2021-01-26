/**
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
package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;

import java.util.List;
import java.util.Set;

/**
 * DO for isaac featured profiles.
 *
 */
@JsonContentType("isaacQuiz")
public class IsaacQuizDTO extends ContentDTO {
	private boolean visibleToStudents;

	@JsonCreator
	public IsaacQuizDTO(
			@JsonProperty("id") String id,
            @JsonProperty("title") String title,
			@JsonProperty("subtitle") String subtitle,
			@JsonProperty("type") String type,
			@JsonProperty("author") String author,
			@JsonProperty("encoding") String encoding,
			@JsonProperty("canonicalSourceFile") String canonicalSourceFile,
			@JsonProperty("layout") String layout,
			@JsonProperty("children") List<ContentBaseDTO> children,
			@JsonProperty("value") String value,
			@JsonProperty("attribution") String attribution,
			@JsonProperty("relatedContent") List<ContentSummaryDTO> relatedContent,
			@JsonProperty("version") boolean published,
			@JsonProperty("tags") Set<String> tags,
			@JsonProperty("level") Integer level,
			@JsonProperty("visibleToStudents") boolean visibleToStudents) {
		super(id, title, subtitle, type, author, encoding,
				canonicalSourceFile, layout, children, value, attribution,
				relatedContent, published, tags, level);

		this.visibleToStudents = visibleToStudents;
	}

	/**
	 * Default constructor required for Jackson.
	 */
	public IsaacQuizDTO() {

	}

	public boolean getVisibleToStudents() {
		return visibleToStudents;
	}

	public void setVisibleToStudents(boolean visibleToStudents) {
		this.visibleToStudents = visibleToStudents;
	}
}
