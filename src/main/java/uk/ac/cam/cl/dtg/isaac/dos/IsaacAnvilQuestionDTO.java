/**
 * Copyright 2015 Stephen Cummins
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

import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.AnvilAppDTO;

/**
 * @author sac92
 *
 */
public class IsaacAnvilQuestionDTO extends IsaacQuestionBaseDTO {
    private AnvilAppDTO anvilApp;

    /**
     * IsaacAnvilQuestionDTO.
     */
    public IsaacAnvilQuestionDTO() {

    }

    /**
     * Gets the anvilApp.
     * 
     * @return the anvilApp
     */
    public AnvilAppDTO getAnvilApp() {
        return anvilApp;
    }

    /**
     * Sets the anvilApp.
     * 
     * @param anvilApp
     *            the anvilApp to set
     */
    public void setAnvilApp(final AnvilAppDTO anvilApp) {
        this.anvilApp = anvilApp;
    }
}
