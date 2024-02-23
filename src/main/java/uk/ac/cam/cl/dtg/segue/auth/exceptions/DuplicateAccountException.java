/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.auth.exceptions;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * DuplicateAccountException. This exception means that an account already exists with an key field.
 * 
 */
public class DuplicateAccountException extends SegueDatabaseException {
    private static final long serialVersionUID = 7415512495026915539L;
    public static final String DEFAULT_MESSAGE = "An account with that email address already exists.";

    /**
     * Create a new DuplicateAccountException.
     * 
     * @param message
     *            - message to add
     */
    public DuplicateAccountException(final String message) {
        super(message);
    }

    /**
     * Create a new DuplicateAccountException with the default message.
     */
    public DuplicateAccountException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Create a new DuplicateAccountException.
     * 
     * @param message
     *            - message to add
     * @param e
     *            - exception to add.
     */
    public DuplicateAccountException(final String message, final Exception e) {
        super(message, e);
    }

}
