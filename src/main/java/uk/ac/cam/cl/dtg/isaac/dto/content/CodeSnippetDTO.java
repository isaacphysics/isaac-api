/**
 * Copyright 2021 Ben Hanson
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
 * Code snippet is a special type of content.
 *
 */
public class CodeSnippetDTO extends ContentDTO {
  private String language;
  private String code;
  private boolean disableHighlighting;
  private String url;

  @JsonCreator
  public CodeSnippetDTO(@JsonProperty("language") final String language, @JsonProperty("code") final String code,
                        @JsonProperty("disableHighlighting") final Boolean disableHighlighting,
                        @JsonProperty("url") final String url) {
    this.language = language;
    this.code = code;
    this.disableHighlighting = disableHighlighting;
    this.url = url;
  }

  public String getLanguage() {
    return this.language;
  }

  public void setLanguage(final String language) {
    this.language = language;
  }

  public String getCode() {
    return this.code;
  }

  public void setCode(final String code) {
    this.code = code;
  }

  public final boolean getDisableHighlighting() {
    return disableHighlighting;
  }

  public final void setDisableHighlighting(final boolean disableHighlighting) {
    this.disableHighlighting = disableHighlighting;
  }

  public String getUrl() {
    return this.url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }
}

