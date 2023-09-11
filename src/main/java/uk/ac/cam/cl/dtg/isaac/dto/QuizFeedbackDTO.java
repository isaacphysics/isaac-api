/**
 * Copyright 2021 Raspberry Pi Foundation
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

package uk.ac.cam.cl.dtg.isaac.dto;

import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

public class QuizFeedbackDTO {
  public static class Mark {
    private Integer correct;
    private Integer incorrect;
    private Integer notAttempted;

    public Mark() {
      this.correct = 0;
      this.incorrect = 0;
      this.notAttempted = 0;
    }

    public static Mark notAttempted(final Integer notAttempted) {
      Mark mark = new Mark();
      mark.notAttempted = notAttempted;
      return mark;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Mark mark = (Mark) o;
      return Objects.equals(getCorrect(), mark.getCorrect())
          && Objects.equals(incorrect, mark.incorrect)
          && Objects.equals(notAttempted, mark.notAttempted);
    }

    @Override
    public int hashCode() {
      return Objects.hash(correct, incorrect, notAttempted);
    }

    public Integer getCorrect() {
      return correct;
    }

    public void setCorrect(final Integer correct) {
      this.correct = correct;
    }

    public Integer getIncorrect() {
      return incorrect;
    }

    public void setIncorrect(final Integer incorrect) {
      this.incorrect = incorrect;
    }

    public Integer getNotAttempted() {
      return notAttempted;
    }

    public void setNotAttempted(final Integer notAttempted) {
      this.notAttempted = notAttempted;
    }
  }

  @Nullable
  private Boolean complete;

  @Nullable
  private Mark overallMark;

  @Nullable
  private Map<String, Mark> sectionMarks;

  @Nullable
  private Map<String, Mark> questionMarks;

  public QuizFeedbackDTO(final Mark overallMark, final Map<String, Mark> sectionMarks,
                         final Map<String, Mark> questionMarks) {
    this.overallMark = overallMark;
    this.sectionMarks = sectionMarks;
    this.questionMarks = questionMarks;
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
  public Map<String, Mark> getQuestionMarks() {
    return questionMarks;
  }

  @Nullable
  public Boolean isComplete() {
    return complete;
  }

  public void setComplete(final boolean complete) {
    this.complete = complete;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuizFeedbackDTO that = (QuizFeedbackDTO) o;
    return Objects.equals(complete, that.complete)
        && Objects.equals(overallMark, that.overallMark)
        && Objects.equals(sectionMarks, that.sectionMarks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(complete, overallMark, sectionMarks);
  }
}
