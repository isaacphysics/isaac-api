/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.dao;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Content deserializer will try and use the map built up in the ContentMapper class to determine what subtype of
 * content needs to be created.
 * <br>
 * Currently this is dependent on the register map key being the exact same text as the json type property value stored
 * in the database.
 * <br>
 * This class with trim any string elements it sees. If it sees an empty string element it will be removed from the
 * list.
 *
 */
public class TrimWhitespaceListDeserializer extends JsonDeserializer<List<String>> {

  @Override
  public List<String> deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
      throws IOException {

    @SuppressWarnings("unchecked")
    List<String> listOfStringToTrim = jsonParser.readValueAs(ArrayList.class);

    int index = 0;
    for (String s : listOfStringToTrim) {
      if (!s.trim().isEmpty()) {
        listOfStringToTrim.set(index, s.trim());
      }
      index++;
    }

    listOfStringToTrim.removeAll(List.of(""));

    return listOfStringToTrim;
  }
}
