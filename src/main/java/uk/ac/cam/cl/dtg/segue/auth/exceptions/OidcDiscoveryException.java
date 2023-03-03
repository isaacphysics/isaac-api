/*
 * Copyright 2023 Matthew Trew
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

package uk.ac.cam.cl.dtg.segue.auth.exceptions;


public class OidcDiscoveryException extends Exception {
    private static final long serialVersionUID = -5142779374259438086L;

    /**
     * Default Constructor.
     */
    public OidcDiscoveryException() {
        super();
    }

    /**
     * Constructor with message.
     *
     * @param message
     *            - to explain exception.
     */
    public OidcDiscoveryException(final String message) {
        super(message);
    }

    /**
     * @param message
     *            - explaining the exception
     * @param cause
     *            - if there is a root cause.
     */
    public OidcDiscoveryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}