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
import java.util.List;

public class QuizFeedbackDTO {
    public static class Mark {
        public Integer questionPartsCorrect;
        public Integer questionPartsIncorrect;
        public Integer questionPartsNotAttempted;
        public Integer questionPartsTotal;

        public Mark() {
            this.questionPartsCorrect = 0;
            this.questionPartsIncorrect = 0;
            this.questionPartsNotAttempted = 0;
            this.questionPartsTotal = 0;
        }
    }

    public static class SectionMark {
        private String sectionId;
        private String sectionName;
        private Mark mark;

        public SectionMark(String sectionId, String sectionName, Mark mark) {
            this.sectionId = sectionId;
            this.sectionName = sectionName;
            this.mark = mark;
        }

        public String getSectionId() {
            return sectionId;
        }

        public String getSectionName() {
            return sectionName;
        }

        public Mark getMark() {
            return mark;
        }
    }

    @Nullable
    private Mark overallMark;

    @Nullable
    private List<SectionMark> sectionMarks;

    public QuizFeedbackDTO(Mark overallMark, List<SectionMark> sectionMarks) {
        this.overallMark = overallMark;
        this.sectionMarks = sectionMarks;
    }

    @Nullable
    public Mark getOverallMark() {
        return overallMark;
    }

    @Nullable
    public List<SectionMark> getSectionMarks() {
        return sectionMarks;
    }
}
