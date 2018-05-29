/**
 * Copyright 2017 Meurig Thomas
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

import uk.ac.cam.cl.dtg.isaac.api.Constants.FastTrackConceptState;

/**
 * Construct to hold the title of a FastTrack concept with the value of the best level achieved for that concept.
 */
public class QuestionPartConceptDTO {
    private String title;
    private FastTrackConceptState bestLevel;

    /**
     * Generic constructor.
     */
    public QuestionPartConceptDTO() {}

    /**
     * Constructor which sets title.
     *
     * @param title
     *            of the FastTrack concept.
     */
    public QuestionPartConceptDTO(String title) {
        this.title = title;
    }

    /**
     * Get the title of the concept.
     *
     * @return the the title of the concept.
     */
    public final String getTitle() {
        return this.title;
    }

    /**
     * Set the concept's title.
     *
     * @param title
     *            to set for the concept.
     */
    public final void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Get the best achieved level for questions of this concept type.
     *
     * @return the best level.
     */
    public final FastTrackConceptState getBestLevel() {
        return this.bestLevel;
    }

    /**
     * Set the best achieved level for questions of this concept type.
     *
     * @param bestLevel
     *            to set for this concept.
     */
    public final void setBestLevel(FastTrackConceptState bestLevel) {
        this.bestLevel = bestLevel;
    }

}
