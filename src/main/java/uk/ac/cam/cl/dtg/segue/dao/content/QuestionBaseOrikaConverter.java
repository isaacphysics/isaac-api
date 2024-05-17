/*
 * Copyright 2024 James Sharkey
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
package uk.ac.cam.cl.dtg.segue.dao.content;


import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;

/**
 *  Class to force Orika to map IsaacQuestionBase to DTO correctly in a generic manner.
 *
 *  It is necessary since InlineRegions have a "List<IsaacQuestionBase> inlineQuestions", which are not
 *  of type ContentBase and so do not correctly use the recursive conversion. This converter matches the
 *  type for those objects and so Orika will choose to use it when it needs to convert them. All this class
 *  then has to do is use the base converter as normal to actually convert the questions from DO to DTO.
 *
 */
public class QuestionBaseOrikaConverter extends AbstractPolymorphicConverter<IsaacQuestionBase, IsaacQuestionBaseDTO> {

    private final ContentBaseOrikaConverter baseOrikaConverter;

    public QuestionBaseOrikaConverter(final ContentBaseOrikaConverter baseOrikaConverter) {
        this.baseOrikaConverter = baseOrikaConverter;
    }

    @Override
    public IsaacQuestionBaseDTO convert(IsaacQuestionBase isaacQuestionBase, Type<? extends IsaacQuestionBaseDTO> type, MappingContext mappingContext) {
        // We don't do anything clever here; the ContentBase converter is clever, we just defer to it:
        return (IsaacQuestionBaseDTO) baseOrikaConverter.convert(isaacQuestionBase, type, mappingContext);
    }
}
