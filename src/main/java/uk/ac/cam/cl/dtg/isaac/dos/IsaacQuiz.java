/*
 * Copyright 2021 Raspberry Pi Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;

import java.util.List;
import java.util.Set;

/**
 * DO for isaac quiz.
 *
 */
@DTOMapping(IsaacQuizDTO.class)
@JsonContentType("isaacQuiz")
public class IsaacQuiz extends SeguePage {
    private List<String> hiddenFromRoles;
    private Content rubric;

    @JsonCreator
    public IsaacQuiz(
            @JsonProperty("id") String id, @JsonProperty("title") String title,
            @JsonProperty("subtitle") String subtitle,
            @JsonProperty("type") String type,
            @JsonProperty("author") String author,
            @JsonProperty("encoding") String encoding,
            @JsonProperty("canonicalSourceFile") String canonicalSourceFile,
            @JsonProperty("layout") String layout,
            @JsonProperty("children") List<ContentBase> children,
            @JsonProperty("value") String value,
            @JsonProperty("attribution") String attribution,
            @JsonProperty("relatedContent") List<String> relatedContent,
            @JsonProperty("version") boolean published,
            @JsonProperty("deprecated") Boolean deprecated,
            @JsonProperty("supersededBy") String supersededBy,
            @JsonProperty("tags") Set<String> tags,
            @JsonProperty("permissions") String permissions,
            @JsonProperty("notes") String notes,
            @JsonProperty("teacherNotes") String teacherNotes,
            @JsonProperty("level") Integer level,
            @JsonProperty("hiddenFromRoles") List<String> hiddenFromRoles,
            @JsonProperty("rubric") Content rubric) {
        super(id, title, subtitle, type, author, encoding,
                canonicalSourceFile, layout, children, value, attribution,
                relatedContent, published, deprecated, supersededBy, tags, permissions, notes,
                teacherNotes, level, null);

        this.hiddenFromRoles = hiddenFromRoles;
        this.rubric = rubric;
    }

    /**
     * Default constructor required for Jackson.
     */
    public IsaacQuiz() {

    }

    public Content getRubric() {
        return rubric;
    }

    public void setRubric(Content rubric){
        this.rubric = rubric;
    }

    public List<String> getHiddenFromRoles() {
        return hiddenFromRoles;
    }

    public void setHiddenFromRoles(List<String> hiddenFromRoles) {
        this.hiddenFromRoles = hiddenFromRoles;
    }
}
