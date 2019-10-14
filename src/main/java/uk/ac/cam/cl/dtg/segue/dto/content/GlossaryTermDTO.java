/**
 * Copyright 2019 Andrea Franceschini
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Glossary Term object The glossary term object is a specialized form of content and allows the storage of data
 * relating to glossary terms.
 *
 */
public class GlossaryTermDTO extends ContentDTO {
    protected ContentDTO explanation;
    protected boolean autoId;

    @JsonCreator
    public GlossaryTermDTO(@JsonProperty("explanation") ContentDTO explanation) {
        this.explanation = explanation;
    }

    public ContentDTO getExplanation() {
        return this.explanation;
    }

    public void setExplanation(ContentDTO explanation) {
        this.explanation = explanation;
    }

    @JsonIgnore
    public final boolean getAutoId() { return autoId; }
    public final void setAutoId(final boolean autoId) { this.autoId = autoId; }
}
