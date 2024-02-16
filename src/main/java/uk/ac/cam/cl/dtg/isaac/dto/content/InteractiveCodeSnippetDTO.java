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

package uk.ac.cam.cl.dtg.isaac.dto.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Interactive code snippet is a code snippet that can be run and edited.
 *
 */
public class InteractiveCodeSnippetDTO extends CodeSnippetDTO {

  private String setupCode;
  private String testCode;
  private String expectedResult;
  private Boolean wrapCodeInMain;

  @JsonCreator
  public InteractiveCodeSnippetDTO(@JsonProperty("language") final String language,
                                   @JsonProperty("code") final String code,
                                   @JsonProperty("disableHighlighting") final Boolean disableHighlighting,
                                   @JsonProperty("url") final String url,
                                   @JsonProperty("setupCode") final String setupCode,
                                   @JsonProperty("testCode") final String testCode,
                                   @JsonProperty("expectedResult") final String expectedResult,
                                   @JsonProperty("wrapCodeInMain") final Boolean wrapCodeInMain) {
    super(language, code, disableHighlighting, url);
    this.setupCode = setupCode;
    this.testCode = testCode;
    this.expectedResult = expectedResult;
    this.wrapCodeInMain = wrapCodeInMain;
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

