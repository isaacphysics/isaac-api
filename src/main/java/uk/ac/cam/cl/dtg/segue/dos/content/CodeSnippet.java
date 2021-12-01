/**
 * Copyright 2021 Ben Hanson
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

import uk.ac.cam.cl.dtg.segue.dto.content.CodeSnippetDTO;

/**
 * Code snippet object
 *
 */
@DTOMapping(CodeSnippetDTO.class)
@JsonContentType("codeSnippet")
public class CodeSnippet extends Content {
    protected String language;
    protected String code;
    protected boolean disableHighlighting;
    protected String url;

    /**
     * Default constructor, required for mappers.
     */
    public CodeSnippet() {

    }

    /**
     * Gets the language.
     *
     * @return the language
     */
    public final String getLanguage() {
        return language;
    }

    /**
     * Sets the language.
     *
     * @param language
     *            the language to set
     */
    public final void setLanguage(final String language) {
        this.language = language;
    }

    public final String getCode() {
        return this.code;
    }

    public final void setCode(final String code) {
        this.code = code;
    }

    public final boolean getDisableHighlighting() {
        return disableHighlighting;
    }

    public final void setDisableHighlighting(final boolean disableHighlighting) {
        this.disableHighlighting = disableHighlighting;
    }

    public final String getUrl() {
        return this.url;
    }

    public final void setUrl(final String url) {
        this.url = url;
    }
}
