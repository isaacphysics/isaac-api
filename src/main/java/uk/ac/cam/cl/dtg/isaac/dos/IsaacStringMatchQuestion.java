/**
 * Copyright 2017 James Sharkey
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
import uk.ac.cam.cl.dtg.isaac.dto.IsaacStringMatchQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacStringMatchValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

/**
 * DO for isaacStringMatchQuestion.
 */
@DTOMapping(IsaacStringMatchQuestionDTO.class)
@JsonContentType("isaacStringMatchQuestion")
@ValidatesWith(IsaacStringMatchValidator.class)
public class IsaacStringMatchQuestion extends IsaacQuestionBase {
  private Boolean multiLineEntry;
  private Boolean preserveLeadingWhitespace;
  private Boolean preserveTrailingWhitespace;

  public Boolean getMultiLineEntry() {
    return multiLineEntry;
  }

  public void setMultiLineEntry(final Boolean multiLineEntry) {
    this.multiLineEntry = multiLineEntry;
  }

  public Boolean getPreserveTrailingWhitespace() {
    return preserveTrailingWhitespace;
  }

  public void setPreserveTrailingWhitespace(final Boolean preserveTrailingWhitespace) {
    this.preserveTrailingWhitespace = preserveTrailingWhitespace;
  }

  public Boolean getPreserveLeadingWhitespace() {
    return preserveLeadingWhitespace;
  }

  public void setPreserveLeadingWhitespace(final Boolean preserveLeadingWhitespace) {
    this.preserveLeadingWhitespace = preserveLeadingWhitespace;
  }
}