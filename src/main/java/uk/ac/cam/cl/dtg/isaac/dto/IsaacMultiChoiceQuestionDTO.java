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
package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

/**
 * Content DO for isaacMultiChoiceQuestions.
 *
 */
@JsonContentType("isaacMultiChoiceQuestion")
public class IsaacMultiChoiceQuestionDTO extends IsaacQuestionBaseDTO {

    /**
     * Gets the choices.
     * Unignores getting the choices as they are required
     * to be presented in multi-choice questions
     *
     * @return the choices
     */
    @Override
    @JsonIgnore(false)
    public List<ChoiceDTO> getChoices() {
        return super.getChoices();
    }

}
