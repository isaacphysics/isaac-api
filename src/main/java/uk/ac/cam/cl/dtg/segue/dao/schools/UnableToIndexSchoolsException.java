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
package uk.ac.cam.cl.dtg.segue.dao.schools;

/**
 * An exception that is thrown when there is a problem building the index for a list of schools.
 * 
 */
public class UnableToIndexSchoolsException extends Exception {
    private static final long serialVersionUID = 3524664884686464375L;

    /**
     * Creates an UnableToIndexSchoolsException.
     * 
     * @param message
     *            - addition information to help identify the exception.
     */
    public UnableToIndexSchoolsException(final String message) {
        super(message);
    }

    /**
     * Creates an UnableToIndexSchoolsException.
     * 
     * @param message
     *            - addition information to help identify the exception.
     * @param e
     *            - addition exception objects.
     */
    public UnableToIndexSchoolsException(final String message, final Exception e) {
        super(message, e);
    }
}
