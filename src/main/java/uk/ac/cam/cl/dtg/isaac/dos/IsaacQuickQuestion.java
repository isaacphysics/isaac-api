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
package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuickQuestionDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;

/**
 * Quick Question DO.
 */
@DTOMapping(IsaacQuickQuestionDTO.class)
@JsonContentType("isaacQuestion")
public class IsaacQuickQuestion extends IsaacQuestionBase {
    private Boolean showConfidence;

    /**
     * Gets the showConfidence
     *
     * @return the showConfidence
     */
    public final Boolean getShowConfidence() {
        return showConfidence;
    }

    /**
     * Sets the showConfidence
     *
     * @param showConfidence
     *              the showConfidence to set
     */
    public final void setShowConfidence(final Boolean showConfidence) {
        this.showConfidence = showConfidence;
    }
}
