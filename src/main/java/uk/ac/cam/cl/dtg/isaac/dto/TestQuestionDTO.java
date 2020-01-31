/*
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

import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceQuestionDTO;

import java.util.List;

/**
 * DTO for constructing a test question and the answer cases we expect
 */
public class TestQuestionDTO extends ChoiceQuestionDTO {
    List<TestCaseDTO> testCases;

    public void setTestCases(List<TestCaseDTO> tests) {
        this.testCases = tests;
    }
    public List<TestCaseDTO> getTestCases() {
        return this.testCases;
    }
}
