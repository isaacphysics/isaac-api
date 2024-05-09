/*
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;

import java.util.List;
import java.util.Set;

/**
 * IsaacQuestion Page DTO.
 *
 */
@JsonContentType("isaacQuestionPage")
public class IsaacQuestionPageDTO extends SeguePageDTO {
    protected Float passMark;
    protected Integer difficulty;

    @JsonCreator
    public IsaacQuestionPageDTO(@JsonProperty("id") String id,
            @JsonProperty("title") String title, @JsonProperty("subtitle") String subtitle,
            @JsonProperty("type") String type, @JsonProperty("author") String author,
            @JsonProperty("encoding") String encoding, @JsonProperty("canonicalSourceFile") String canonicalSourceFile,
            @JsonProperty("layout") String layout, @JsonProperty("children") List<ContentBaseDTO> children,
            @JsonProperty("value") String value, @JsonProperty("attribution") String attribution,
            @JsonProperty("relatedContent") List<ContentSummaryDTO> relatedContent,
            @JsonProperty("published") Boolean published, @JsonProperty("tags") Set<String> tags,
            @JsonProperty("deprecated") Boolean deprecated,
            @JsonProperty("level") Integer level, @JsonProperty("difficulty") Integer difficulty,
            @JsonProperty("passMark") Float passMark, @JsonProperty("supersededBy") String supersededBy) {

        super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value,
                attribution, relatedContent, published, deprecated, supersededBy, tags, level);

        this.passMark = passMark;
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

    public void setPassMark(Float passMark) {
        this.passMark = passMark;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }
}
