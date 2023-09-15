/*
 * Copyright 2022 Chris Purdy
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

import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacReorderQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacReorderValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;


/**
 * Content DO for IsaacReorderQuestions.
 *
 */
@DTOMapping(IsaacReorderQuestionDTO.class)
@JsonContentType("isaacReorderQuestion")
@ValidatesWith(IsaacReorderValidator.class)
public class IsaacReorderQuestion extends IsaacItemQuestion {
}