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
package uk.ac.cam.cl.dtg.isaac.dto.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

/**
 * DTO representing a segue page.
 *
 */
public class SeguePageDTO extends ContentDTO {
    private String summary;
    private Boolean deprecated;
    private String supersededBy;
    private String teacherNotes;
    private SidebarDTO sidebar;

    @JsonCreator
    public SeguePageDTO(@JsonProperty("id") String id,
                        @JsonProperty("title") String title, @JsonProperty("subtitle") String subtitle,
                        @JsonProperty("type") String type, @JsonProperty("author") String author,
                        @JsonProperty("encoding") String encoding, @JsonProperty("canonicalSourceFile") String canonicalSourceFile,
                        @JsonProperty("layout") String layout, @JsonProperty("children") List<ContentBaseDTO> children,
                        @JsonProperty("value") String value, @JsonProperty("attribution") String attribution,
                        @JsonProperty("relatedContent") List<ContentSummaryDTO> relatedContent,
                        @JsonProperty("published") Boolean published, @JsonProperty("deprecated") Boolean deprecated,
                        @JsonProperty("supersededBy") String supersededBy, @JsonProperty("tags") Set<String> tags,
                        @JsonProperty("teacherNotes") String teacherNotes, @JsonProperty("level") Integer level,
                        @JsonProperty("sidebar") SidebarDTO sidebar) {

        super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value,
                attribution, relatedContent, published, tags, level);

        this.deprecated = deprecated;
        this.supersededBy = supersededBy;
        this.teacherNotes = teacherNotes;
        this.sidebar = sidebar;
    }

    public SeguePageDTO() {
    }

    /**
     * Gets the summary.
     * 
     * @return the summary
     */
    public final String getSummary() {
        return summary;
    }

    /**
     * Sets the summary.
     * 
     * @param summary
     *            the summary to set
     */
    public final void setSummary(final String summary) {
        this.summary = summary;
    }

    @Override
    @JsonIgnore(false) // Override the parent class decorator!
    public String getCanonicalSourceFile() {
        return this.canonicalSourceFile;
    }

    public String getTeacherNotes() {
        return teacherNotes;
    }

    public void setTeacherNotes(final String teacherNotes) {
        this.teacherNotes = teacherNotes;
    }

    public Boolean getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getSupersededBy() {
        return supersededBy;
    }

    public void setSupersededBy(String supersededBy) {
        this.supersededBy = supersededBy;
    }

    public SidebarDTO getSidebar() {
        return sidebar;
    }

    public void setSidebar(final SidebarDTO sidebar) {
        this.sidebar = sidebar;
    }
}
