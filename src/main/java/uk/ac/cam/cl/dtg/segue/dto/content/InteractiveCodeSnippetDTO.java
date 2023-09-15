/*
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
package uk.ac.cam.cl.dtg.segue.dto.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.isaac.dto.content.CodeSnippetDTO;

/**
 * Interactive code snippet is a code snippet that can be run and edited
 *
 */
public class InteractiveCodeSnippetDTO extends CodeSnippetDTO {

    protected String setupCode;
    protected String testCode;
    protected String expectedResult;
    protected Boolean wrapCodeInMain;
    protected String dataUrl;

    @JsonCreator
    public InteractiveCodeSnippetDTO(@JsonProperty("language") String language, @JsonProperty("code") String code,
                                     @JsonProperty("disableHighlighting") Boolean disableHighlighting, @JsonProperty("url") String url,
                                     @JsonProperty("setupCode") String setupCode, @JsonProperty("testCode") String testCode,
                                     @JsonProperty("expectedResult") String expectedResult, @JsonProperty("wrapCodeInMain") Boolean wrapCodeInMain) {
        super(language, code, disableHighlighting, url);
        this.setupCode = setupCode;
        this.testCode = testCode;
        this.expectedResult = expectedResult;
        this.wrapCodeInMain = wrapCodeInMain;
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

    public String getDataUrl() {
        return dataUrl;
    }

    public void setDataUrl(final String dataUrl) {
        this.dataUrl = dataUrl;
    }
}

