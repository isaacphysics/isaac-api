/*
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

import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacSymbolicValidator;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.ArrayList;
import java.util.List;

/**
 * Isaac Symbolic Question DO.
 *
 */
@DTOMapping(IsaacSymbolicQuestionDTO.class)
@JsonContentType("isaacSymbolicQuestion")
@ValidatesWith(IsaacSymbolicValidator.class)
public class IsaacSymbolicQuestion extends IsaacQuestionBase {
    private String formulaSeed;
    private List<String> availableSymbols;

    public final String getFormulaSeed() {
        return formulaSeed;
    }

    public void setFormulaSeed(String formulaSeed) {
        this.formulaSeed = formulaSeed;
    }

    public final List<String> getAvailableSymbols() {
        if (null == availableSymbols) {
            return new ArrayList<>();
        }

        return availableSymbols;
    }

    public void setAvailableSymbols(List<String> availableSymbols) {
        this.availableSymbols = availableSymbols;
    }

}
