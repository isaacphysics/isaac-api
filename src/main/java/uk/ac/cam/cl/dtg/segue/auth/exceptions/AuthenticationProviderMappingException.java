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
package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception which indicates a failure in mapping a string to a known security provider.
 * 
 * @author Stephen Cummins
 * 
 */
public class AuthenticationProviderMappingException extends Exception {
    private static final long serialVersionUID = -284452434569344L;

    /**
     * Creates an AuthenticationProviderMappingException.
     * 
     * @param message
     *            - to store with the exception.
     */
    public AuthenticationProviderMappingException(final String message) {
        super(message);
    }
}
