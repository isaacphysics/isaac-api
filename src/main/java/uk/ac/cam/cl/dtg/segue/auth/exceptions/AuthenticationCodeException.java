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
 * This exception is used for when we are unable to get an authentication code.
 * 
 * e.g. if the user denies access to our app.
 * 
 */
public class AuthenticationCodeException extends Exception {
    private static final long serialVersionUID = -5464852652296975735L;

    /**
     * Creates an AuthenticationCode Exception.
     * 
     * @param msg
     *            - message to include with the exception.
     */
    public AuthenticationCodeException(final String msg) {
        super(msg);
    }

}
