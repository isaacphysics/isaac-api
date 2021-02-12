/*
 * Copyright 2021 Raspberry Pi Foundation
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

import javax.annotation.Nullable;
import java.util.Map;

public class QuizFeedbackDTO {
    public static class Mark {
        public Integer correct;
        public Integer incorrect;
        public Integer notAttempted;

        public Mark() {
            this.correct = 0;
            this.incorrect = 0;
            this.notAttempted = 0;
        }
    }

    @Nullable
    private Boolean complete;

    @Nullable
    private Mark overallMark;

    @Nullable
    private Map<String, Mark> sectionMarks;

    public QuizFeedbackDTO(Mark overallMark, Map<String, Mark> sectionMarks) {
        this.overallMark = overallMark;
        this.sectionMarks = sectionMarks;
        this.complete = true;
    }

    public QuizFeedbackDTO() {
        this.complete = false;
    }

    @Nullable
    public Mark getOverallMark() {
        return overallMark;
    }

    @Nullable
    public Map<String, Mark> getSectionMarks() {
        return sectionMarks;
    }

    @Nullable
    public Boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }
}
