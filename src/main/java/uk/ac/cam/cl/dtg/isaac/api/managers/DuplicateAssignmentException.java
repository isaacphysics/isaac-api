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
package uk.ac.cam.cl.dtg.isaac.api.managers;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * DuplicateAssignmentException.
 * @author sac92
 */
public class DuplicateAssignmentException extends SegueDatabaseException {
	private static final long serialVersionUID = -1086026219080089421L;

	/**
	 * DuplicateAssignmentException.
	 * If an assignment already exists.
	 * @param message - to provide for the exception
	 */
	public DuplicateAssignmentException(final String message) {
		super(message);
	}
}
