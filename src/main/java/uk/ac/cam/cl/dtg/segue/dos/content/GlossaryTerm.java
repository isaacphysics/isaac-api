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
package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.segue.dto.content.GlossaryTermDTO;

/**
 * Glossary Term object The glossary term object is a specialized form of content and allows the storage of data
 * relating to glossary terms.
 *
 */
@DTOMapping(GlossaryTermDTO.class)
@JsonContentType("glossaryTerm")
public class GlossaryTerm extends Content {
    protected Content explanation;

    /**
     * Default constructor, required for mappers.
     */
    public GlossaryTerm() {

    }

    /**
     * Gets the explanation.
     *
     * @return the explanation
     */
    public final Content getExplanation() {
        return explanation;
    }

    /**
     * Sets the explanation.
     *
     * @param explanation
     *            the explanation to set
     */
    public final void setExplanation(final Content explanation) {
        this.explanation = explanation;
    }
}
