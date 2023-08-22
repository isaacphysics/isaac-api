/*
 * Copyright 2022 James Sharkey
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
package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;

@JsonContentType("isaacPageFragment")
public class IsaacPageFragmentDTO extends ContentDTO {
    private String summary;

    @Override
    @JsonIgnore(false) // Override the parent class decorator!
    public String getCanonicalSourceFile() {
        return super.getCanonicalSourceFile();
    }

    /**
     * Gets the summary.
     *
     * @return the summary
     */
    public final String getSummary() {
        return summary;
    }

    /**
     * Sets the summary.
     *
     * @param summary
     *            the summary to set
     */
    public final void setSummary(final String summary) {
        this.summary = summary;
    }
}
