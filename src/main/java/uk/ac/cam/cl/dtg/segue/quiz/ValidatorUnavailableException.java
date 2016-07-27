/**
 * Copyright 2016 James Sharkey
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

package uk.ac.cam.cl.dtg.segue.quiz;

/**
 * An exception which indicates that the question validator is unavailable.
 *
 * @author James Sharkey
 *
 */
public class ValidatorUnavailableException extends Exception {
    private static final long serialVersionUID = -5088917052521699195L;

    /**
     * Creates a ValidatorUnavailableException.
     *
     * @param message
     *            - to store with the exception.
     */
    public ValidatorUnavailableException(final String message) {
        super(message);
    }
}
