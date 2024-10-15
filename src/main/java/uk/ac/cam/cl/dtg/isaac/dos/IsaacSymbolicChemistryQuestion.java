/*
 * Copyright 2016 Ian Davies
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
package uk.ac.cam.cl.dtg.isaac.dos;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicChemistryQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacSymbolicChemistryValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

/**
 * Created by ipd21 on 11/05/2016.
 */
@DTOMapping(IsaacSymbolicChemistryQuestionDTO.class)
@JsonContentType("isaacSymbolicChemistryQuestion")
@ValidatesWith(IsaacSymbolicChemistryValidator.class)
public class IsaacSymbolicChemistryQuestion extends IsaacSymbolicQuestion {
    @JsonProperty("isNuclear")
    private boolean isNuclear;
    private boolean allowPermutations;

    /**
     * @return whether the question is a nuclear question or not
     */
    public boolean isNuclear() {
        return isNuclear;
    }

    /**
     * @param nuclear set whether the question is a nuclear question or not
     */
    public void setNuclear(boolean nuclear) {
        isNuclear = nuclear;
    }

    /**
     * @return whether the question allows compound permutations e.g. C10H22 == CH3(CH2)8CH3
     */
    public boolean getAllowPermutations() { return allowPermutations; }

    /**
     * @param allowPermutations set whether the question allows compound permutations e.g. C10H22 == CH3(CH2)8CH3
     */
    public void setAllowPermutations(boolean allowPermutations) {
        this.allowPermutations = allowPermutations;
    }
}
