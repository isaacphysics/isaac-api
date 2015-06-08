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
package uk.ac.cam.cl.dtg.segue.dao;

/**
 * SegueDatabaseException. This is a general exception that can be used to indicate a problem completing a database
 * operation.
 * 
 */
public class SegueDatabaseException extends Exception {
    private static final long serialVersionUID = -5212563770896174988L;

    /**
     * Create a new SegueDatabaseException.
     * 
     * @param message
     *            - message to add
     */
    public SegueDatabaseException(final String message) {
        super(message);
    }

    /**
     * Create a new SegueDatabaseException.
     * 
     * @param message
     *            - message to add
     * @param e
     *            - exception to add.
     */
    public SegueDatabaseException(final String message, final Exception e) {
        super(message, e);
    }

}
