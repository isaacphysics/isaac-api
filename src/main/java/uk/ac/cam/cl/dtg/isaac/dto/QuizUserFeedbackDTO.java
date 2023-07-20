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

import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;

import jakarta.annotation.Nullable;
import java.util.Objects;

public class QuizUserFeedbackDTO {
    private UserSummaryDTO user;
    private QuizFeedbackDTO feedback;

    public QuizUserFeedbackDTO(UserSummaryDTO user, @Nullable QuizFeedbackDTO feedback) {
        this.user = user;
        this.feedback = feedback;
    }

    public UserSummaryDTO getUser() {
        return user;
    }

    @Nullable
    public QuizFeedbackDTO getFeedback() {
        return feedback;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizUserFeedbackDTO that = (QuizUserFeedbackDTO) o;
        return Objects.equals(user, that.user) &&
            Objects.equals(feedback, that.feedback);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, feedback);
    }
}
