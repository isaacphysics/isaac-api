/**
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.isaac.api;

import uk.ac.cam.cl.dtg.segue.api.AbstractSegueFacade;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Class that all IsaacFacades can inherit from.
 */
public class AbstractIsaacFacade extends AbstractSegueFacade {

	/**
	 * AbstractIsaacFacade.
	 * @param properties - globally available properties.
	 * @param logManager - log manager to support logging user actions.
	 */
	public AbstractIsaacFacade(final PropertiesLoader properties, final ILogManager logManager) {
		super(properties, logManager);

	}
}
