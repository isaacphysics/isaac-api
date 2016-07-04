/**
 * Copyright 2016 Ian Davies
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

import uk.ac.cam.cl.dtg.segue.dto.content.ChemicalFormulaDTO;

@DTOMapping(ChemicalFormulaDTO.class)
@JsonContentType("chemicalFormula")
public class ChemicalFormula extends Choice {
    private String mhchemExpression;

    /**
     * @return the mhchemExpression
     */
    public String getMhchemExpression() {
        return mhchemExpression;
    }

    /**
     * @param mhchemExpression the pythonExpression to set
     */
    public void setMhchemExpression(final String mhchemExpression) {
        this.mhchemExpression = mhchemExpression;
    }
}
