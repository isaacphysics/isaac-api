/*
 * Copyright 2017 James Sharkey
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
package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.StringChoiceDTO;

/**
 * String Choice allows marking as case-insensitive for more relaxed checking.
 *
 */
@DTOMapping(StringChoiceDTO.class)
@JsonContentType("stringChoice")
public class StringChoice extends Choice {
    private boolean caseInsensitive;

    public StringChoice() {
        
    }

    /**
     * @return Whether this string choice should allow any case to match.
     */
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    /**
     * Whether to match strictly to case or not.
     *
     * @param caseInsensitive Whether to ignore the case when checking.
     */
    public void setCaseInsensitive(final boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

}
