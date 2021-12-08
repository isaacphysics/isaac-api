/**
 * Copyright 2021 Chris Purdy
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

import uk.ac.cam.cl.dtg.segue.dto.content.InteractiveCodeSnippetDTO;

/**
 * Interactive code snippet is a code snippet that can be run and edited
 *
 */
@DTOMapping(InteractiveCodeSnippetDTO.class)
@JsonContentType("interactiveCodeSnippet")
public class InteractiveCodeSnippet extends CodeSnippet {

    protected String setupCode;
    protected String testCode;
    protected String expectedResult;
    protected Boolean wrapCodeInMain;

    /**
     * Default constructor, required for mappers.
     */
    public InteractiveCodeSnippet() {

    }

    public String getSetupCode() {
        return setupCode;
    }

    public void setSetupCode(String setupCode) {
        this.setupCode = setupCode;
    }

    public String getTestCode() {
        return testCode;
    }

    public void setTestCode(String testCode) {
        this.testCode = testCode;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public Boolean getWrapCodeInMain() {
        return wrapCodeInMain;
    }

    public void setWrapCodeInMain(Boolean wrapCodeInMain) {
        this.wrapCodeInMain = wrapCodeInMain;
    }
}
