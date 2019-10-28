/**
 * Copyright 2019 Meurig Thomas
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


import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;

import java.util.List;

public class TestDTO {
    /**
     * DTO for holding choices and their expected values
     */
    public static final class TestCaseDTO {
        Choice choice;
        Boolean expected;
        Boolean actual;
        Content explanation;

        public void setChoice(Choice choice) {
            this.choice = choice;
        }
        public Choice getChoice() {
            return this.choice;
        }

        public void setExpected(Boolean expected) {
            this.expected = expected;
        }
        public Boolean getExpected() {
            return this.expected;
        }

        public void setActual(Boolean actual) {
            this.actual = actual;
        }
        public Boolean getActual() {
            return this.actual;
        }

        public void setExplanation(Content explanation) {
            this.explanation = explanation;
        }
        public Content getExplanation() {
            return this.explanation;
        }
    }

    List<Choice> choices;
    List<TestCaseDTO> tests;
    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }
    public List<Choice> getChoices() {
        return this.choices;
    }
    public void setCases(List<TestCaseDTO> tests) {
        this.tests = tests;
    }
    public List<TestCaseDTO> getCases() {
        return this.tests;
    }
}
