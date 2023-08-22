/**
 * Copyright 2014 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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

    @JsonCreator
    public SeguePageDTO(
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
            @JsonProperty("deprecated") final Boolean deprecated,
            @JsonProperty("tags") final Set<String> tags,
            @JsonProperty("level") final Integer level) {

        super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value,
                attribution, relatedContent, published, deprecated, tags, level);

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
        return super.getCanonicalSourceFile();
    }
}
