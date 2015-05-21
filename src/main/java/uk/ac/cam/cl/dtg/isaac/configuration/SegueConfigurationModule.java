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
package uk.ac.cam.cl.dtg.isaac.configuration;

import java.util.List;
import java.util.Set;

import org.reflections.Reflections;

import com.google.api.client.util.Lists;

import uk.ac.cam.cl.dtg.segue.configuration.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;

/**
 * Segue Configuration module for Isaac. 
 * 
 * This is used to register isaac specific DOs and DTOs with Segue.
 */
public class SegueConfigurationModule implements ISegueDTOConfigurationModule {

	@SuppressWarnings("unchecked")
	@Override
	public List<Class<? extends Content>> getContentDataTransferObjectMap() {
		List<Class<? extends Content>> supplementaryContentDOs = Lists.newArrayList();
		
		// We need to different content objects here for the
		// auto-mapping to work
		
		Reflections reflections = new Reflections("uk.ac.cam.cl.dtg.isaac");
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(JsonContentType.class);
	     
		for (Class<?> classToAdd : annotated) {
			if (Content.class.isAssignableFrom(classToAdd)) {
				supplementaryContentDOs.add((Class<Content>) classToAdd);
			}
		}

		return supplementaryContentDOs;
	}
}
