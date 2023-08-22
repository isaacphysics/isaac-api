/**
 * Copyright 2019 Andrea Franceschini
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

/**
 * Glossary Term object The glossary term object is a specialized form of content and allows the storage of data
 * relating to glossary terms.
 *
 */
public class GlossaryTermDTO extends ContentDTO {
    private ContentDTO explanation;
    private boolean autoId;
    private String examBoard;

    @JsonCreator
    public GlossaryTermDTO(@JsonProperty("explanation") final ContentDTO explanation, @JsonProperty("examBoard") final String examBoard) {
        this.explanation = explanation;
        this.examBoard = examBoard != null ? examBoard : "";
    }

    public ContentDTO getExplanation() {
        return this.explanation;
    }

    public void setExplanation(final ContentDTO explanation) {
        this.explanation = explanation;
    }

    public String getExamBoard() {
        return this.examBoard;
    }

    public void setExamBoard(final String examBoard) {
        this.examBoard = examBoard;
    }

    @JsonIgnore
    public final boolean getAutoId() {
        return autoId;
    }

    public final void setAutoId(final boolean autoId) {
        this.autoId = autoId;
    }
}
