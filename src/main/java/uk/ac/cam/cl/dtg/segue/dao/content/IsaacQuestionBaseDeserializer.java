/*
 * Copyright 2014 Stephen Cummins
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;

import java.io.IOException;

/**
 * QuestionValidationResponse deserializer
 *
 * This class requires the primary content base deserializer as a constructor argument.
 *
 * It is to allow subclasses of the choices object to be detected correctly.
 */
public class IsaacQuestionBaseDeserializer extends JsonDeserializer<IsaacQuestionBase> {
    private ContentBaseDeserializer contentBaseDeserializer;

    /**
     * Create a QuestionValidationResponse deserializer.
     *
     * @param contentDeserializer
     *            -
     */
    public IsaacQuestionBaseDeserializer(final ContentBaseDeserializer contentDeserializer) {
        this.contentBaseDeserializer = contentDeserializer;
    }

    @Override
    public IsaacQuestionBase deserialize(final JsonParser jsonParser,
                                                  final DeserializationContext deserializationContext) throws IOException {
        return (IsaacQuestionBase) this.contentBaseDeserializer.deserialize(jsonParser, deserializationContext);

    }
}