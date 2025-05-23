/*
 * Copyright 2022 James Sharkey
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

import uk.ac.cam.cl.dtg.isaac.dto.IsaacPageFragmentDTO;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;

@DTOMapping(IsaacPageFragmentDTO.class)
@JsonContentType("isaacPageFragment")
public class IsaacPageFragment extends Content {
    private String summary;
    private String teacherNotes;

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

    /**
     * Gets the teacherNotes.
     *
     * @return the teacherNotes
     */
    public String getTeacherNotes() {
        return teacherNotes;
    }

    /**
     * Sets the teacherNotes.
     *
     * @param teacherNotes
     *            the teacherNotes to set
     */
    public void setTeacherNotes(final String teacherNotes) {
        this.teacherNotes = teacherNotes;
    }
}
