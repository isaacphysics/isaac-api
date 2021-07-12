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
package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacRegexStringMatchQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacRegexStringMatchValidator;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

/**
 * DO for isaacStringMatchQuestion.
 *
 */
@DTOMapping(IsaacRegexStringMatchQuestionDTO.class)
@JsonContentType("isaacRegexStringMatchQuestion")
@ValidatesWith(IsaacRegexStringMatchValidator.class)
public class IsaacRegexStringMatchQuestion extends IsaacQuestionBase {
    private Boolean multiLineEntry;

    public Boolean getMultiLineEntry() {
        return multiLineEntry;
    }

    public void setMultiLineEntry(final Boolean multiLineEntry) {
        this.multiLineEntry = multiLineEntry;
    }
}