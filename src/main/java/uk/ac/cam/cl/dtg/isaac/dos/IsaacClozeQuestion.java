/**
 * Copyright 2021 Chris Purdy
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacClozeQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacClozeValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;


/**
 * Content DO for IsaacClozeQuestions.
 */
@DTOMapping(IsaacClozeQuestionDTO.class)
@JsonContentType("isaacClozeQuestion")
@ValidatesWith(IsaacClozeValidator.class)
public class IsaacClozeQuestion extends IsaacItemQuestion {

  private Boolean withReplacement;
  // Detailed feedback option not needed in the client so not in DTO:
  private Boolean detailedItemFeedback;

  public Boolean getWithReplacement() {
    return withReplacement;
  }

  public void setWithReplacement(final Boolean withReplacement) {
    this.withReplacement = withReplacement;
  }

  public Boolean getDetailedItemFeedback() {
    return detailedItemFeedback;
  }

  public void setDetailedItemFeedback(final Boolean detailedItemFeedback) {
    this.detailedItemFeedback = detailedItemFeedback;
  }
}