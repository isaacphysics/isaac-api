/**
 * Copyright 2018 Meurig Thomas
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

import uk.ac.cam.cl.dtg.segue.dto.content.FreeTextRuleDTO;

@DTOMapping(FreeTextRuleDTO.class)
@JsonContentType("freeTextRule")
public class FreeTextRule extends Choice {
    private boolean caseInsensitive;
    private boolean allowsAnyOrder;
    private boolean allowsExtraWords;
    private boolean allowsMisspelling;

    public void setCaseInsensitive(final boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }
    public boolean isCaseInsensitive() {
        return this.caseInsensitive;
    }
    public void setAllowsAnyOrder(final boolean allowsAnyOrder) {
        this.allowsAnyOrder = allowsAnyOrder;
    }
    public boolean getAllowsAnyOrder() {
        return this.allowsAnyOrder;
    }
    public void setAllowsExtraWords(final boolean allowsExtraWords) {
        this.allowsExtraWords = allowsExtraWords;
    }
    public boolean getAllowsExtraWords() {
        return this.allowsExtraWords;
    }
    public void setAllowsMisspelling(final boolean allowsMisspelling) {
        this.allowsMisspelling = allowsMisspelling;
    }
    public boolean getAllowsMisspelling() {
        return this.allowsMisspelling;
    }
}