/**
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.api.client.util.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * This DTO represents high level information about a piece of content
 * 
 * This should be a light weight object used for presenting search results etc.
 * 
 */
public class ContentSummaryDTO {
    private String id;
    private String title;
    private String summary;
    private String type;
    private String level;
    private List<String> tags;
    private String url;
    private Boolean correct;
    private List<String> questionPartIds;

    /**
     * Private constructor required for Dozer.
     */
    public ContentSummaryDTO() {
        tags = Lists.newArrayList();
        questionPartIds = Lists.newArrayList();
    }

    /**
     * Gets the id.
     * 
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     * 
     * @param id
     *            the id to set
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Gets the title.
     * 
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     * 
     * @param title
     *            the title to set
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Gets the summary.
     * 
     * @return the summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Sets the summary.
     * 
     * @param summary
     *            the summary to set
     */
    public void setSummary(final String summary) {
        this.summary = summary;
    }

    /**
     * Gets the type.
     * 
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     * 
     * @param type
     *            the type to set
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Gets the tags.
     * 
     * @return the tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Sets the tags.
     * 
     * @param tags
     *            the tags to set
     */
    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    /**
     * Gets the level.
     * 
     * @return the level
     */
    public String getLevel() {
        return level;
    }

    /**
     * Sets the level.
     * 
     * @param level
     *            the level to set
     */
    public void setLevel(final String level) {
        this.level = level;
    }

    /**
     * Gets the url.
     * 
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the url.
     * 
     * @param url
     *            the url to set
     */
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * Gets whether the question is completed correctly.
     *
     * @return correct
     */
    public Boolean getCorrect() {
        return this.correct;
    }

    /**
     * Sets whether the question has been completed correctly.
     *
     * @param correct
     *            the value to set completion
     */
    public void setCorrect(final Boolean correct) {
        this.correct = correct;
    }

    /**
     * Gets a list of the question part IDs
     *
     * @return list of question part IDs for any questions in this content
     */
    @JsonIgnore
    public List<String> getQuestionPartIds() {
        return this.questionPartIds;
    }

    /**
     * Sets a list of question part IDs
     *
     * @param questionPartIds list of question part IDs for any questions in this content
     */
    @JsonIgnore
    public void setQuestionPartIds(List<String> questionPartIds) {
        this.questionPartIds = questionPartIds;
    }
}