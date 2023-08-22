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
package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DO for isaac featured profiles.
 *
 */
@JsonContentType("isaacQuiz")
public class IsaacQuizDTO extends SeguePageDTO implements EmailService.HasTitleOrId {
    @Deprecated
    private boolean visibleToStudents;
    private List<String> hiddenFromRoles;
    private QuizFeedbackMode defaultFeedbackMode;
    private ContentDTO rubric;

    // Properties for sending feedback
    private Integer total;
    private Map<String, Integer> sectionTotals;
    private QuizFeedbackDTO individualFeedback;
    private List<QuizUserFeedbackDTO> userFeedback;

    @JsonCreator
    public IsaacQuizDTO(
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
            @JsonProperty("version") final boolean published,
            @JsonProperty("deprecated") final Boolean deprecated,
            @JsonProperty("tags") final Set<String> tags,
            @JsonProperty("level") final Integer level,
            @JsonProperty("visibleToStudents") final boolean visibleToStudents,
            @JsonProperty("hiddenFromRoles") final List<String> hiddenFromRoles,
            @JsonProperty("defaultFeedbackMode") final QuizFeedbackMode defaultFeedbackMode,
            @JsonProperty("rubric") final ContentDTO rubric) {
        super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value, attribution,
                relatedContent, published, deprecated, tags, level);

        this.visibleToStudents = visibleToStudents;
        this.hiddenFromRoles = hiddenFromRoles;
        this.defaultFeedbackMode = defaultFeedbackMode;
        this.rubric = rubric;
    }

    /**
     * Default constructor required for Jackson.
     */
    public IsaacQuizDTO() {

    }

    @Deprecated
    public boolean getVisibleToStudents() {
        return visibleToStudents;
    }

    @Deprecated
    public void setVisibleToStudents(final boolean visibleToStudents) {
        this.visibleToStudents = visibleToStudents;
    }

    public List<String> getHiddenFromRoles() {
        return hiddenFromRoles;
    }

    public void setHiddenFromRoles(final List<String> hiddenFromRoles) {
        this.hiddenFromRoles = hiddenFromRoles;
    }

    @Nullable
    public ContentDTO getRubric() {
        return rubric;
    }

    @Nullable
    public void setRubric(final ContentDTO rubric) {
        this.rubric = rubric;
    }

    @Nullable
    public QuizFeedbackMode getDefaultFeedbackMode() {
        return defaultFeedbackMode;
    }

    public void setDefaultFeedbackMode(final QuizFeedbackMode defaultFeedbackMode) {
        this.defaultFeedbackMode = defaultFeedbackMode;
    }

    @Nullable
    public Integer getTotal() {
        return total;
    }

    public void setTotal(final Integer total) {
        this.total = total;
    }

    @Nullable
    public Map<String, Integer> getSectionTotals() {
        return sectionTotals;
    }

    public void setSectionTotals(final Map<String, Integer> sectionTotals) {
        this.sectionTotals = sectionTotals;
    }

    @Nullable
    public QuizFeedbackDTO getIndividualFeedback() {
        return individualFeedback;
    }

    public void setIndividualFeedback(final QuizFeedbackDTO individualFeedback) {
        this.individualFeedback = individualFeedback;
    }
}
