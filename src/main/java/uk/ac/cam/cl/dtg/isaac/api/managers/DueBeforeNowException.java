/**
 * Copyright 2021 Raspberry Pi Foundation
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
 * An exception to show a quiz assignment has been set with a due date before now.
 *
 * As this would give students no time to answer the quiz, it is considered illegal.
 */
public class DueBeforeNowException extends SegueDatabaseException {
    private static final long serialVersionUID = -2110572766294320630L;

    /**
     * DueBeforeNowException. If due date has already passed.
     */
    public DueBeforeNowException() {
        super("You cannot set a quiz with a due date in the past.");
    }
}
