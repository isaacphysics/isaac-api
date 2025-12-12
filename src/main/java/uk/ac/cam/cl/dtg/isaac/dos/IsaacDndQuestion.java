/*
 * Copyright 2021 Chris Purdy
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

import org.apache.commons.lang3.BooleanUtils;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacDndQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacDndValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Content DO for IsaacDndQuestions.
 *
 */
@DTOMapping(IsaacDndQuestionDTO.class)
@JsonContentType("isaacDndQuestion")
@ValidatesWith(IsaacDndValidator.class)
public class IsaacDndQuestion extends IsaacItemQuestion {

    private Boolean withReplacement;
    // Detailed feedback option not needed in the client so not in DTO:
    private Boolean detailedItemFeedback;

    public List<DndChoice> getDndChoices() {
        return this.choices.stream().map(c -> (DndChoice) c).collect(Collectors.toList());
    }

    public Boolean getWithReplacement() {
        return withReplacement;
    }

    public void setWithReplacement(final Boolean withReplacement) {
        this.withReplacement = withReplacement;
    }

    public boolean getDetailedItemFeedback() {
        return BooleanUtils.isTrue(detailedItemFeedback);
    }

    public void setDetailedItemFeedback(final Boolean detailedItemFeedback) {
        this.detailedItemFeedback = detailedItemFeedback;
    }
}