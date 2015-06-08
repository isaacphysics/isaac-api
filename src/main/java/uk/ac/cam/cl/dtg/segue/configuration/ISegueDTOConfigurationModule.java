/**
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.configuration;

import java.util.List;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;

/**
 * Interface for configuration modules that will work nicely with Segue.
 *
 */
public interface ISegueDTOConfigurationModule {

    /**
     * This method should provide a map of 'type' identifiers to Classes which extend the Segue Content DTO.
     * 
     * The DTOs registered using this method should match the content objects stored in the content object datastore.
     * 
     * Note: It is expected that the 'type' key should be exactly the same as any type declared in json files that might
     * need to be deserialized.
     * 
     * @return a map of string type identifiers to classes that extend Content.
     */
    List<Class<? extends Content>> getContentDataTransferObjectMap();

}
