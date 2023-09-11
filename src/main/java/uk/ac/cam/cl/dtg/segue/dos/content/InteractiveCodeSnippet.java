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

package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.isaac.dos.content.CodeSnippet;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.dto.content.InteractiveCodeSnippetDTO;

/**
 * Interactive code snippet is a code snippet that can be run and edited.
 *
 */
@DTOMapping(InteractiveCodeSnippetDTO.class)
@JsonContentType("interactiveCodeSnippet")
public class InteractiveCodeSnippet extends CodeSnippet {

  private String setupCode;
  private String testCode;
  private String expectedResult;
  private Boolean wrapCodeInMain;

  /**
   * Default constructor, required for mappers.
   */
  public InteractiveCodeSnippet() {

  }

  public String getSetupCode() {
    return setupCode;
  }

  public void setSetupCode(final String setupCode) {
    this.setupCode = setupCode;
  }

  public String getTestCode() {
    return testCode;
  }

  public void setTestCode(final String testCode) {
    this.testCode = testCode;
  }

  public String getExpectedResult() {
    return expectedResult;
  }

  public void setExpectedResult(final String expectedResult) {
    this.expectedResult = expectedResult;
  }

  public Boolean getWrapCodeInMain() {
    return wrapCodeInMain;
  }

  public void setWrapCodeInMain(final Boolean wrapCodeInMain) {
    this.wrapCodeInMain = wrapCodeInMain;
  }
}
