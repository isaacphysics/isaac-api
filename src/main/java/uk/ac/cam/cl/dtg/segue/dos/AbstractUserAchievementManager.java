/*
 * Copyright 2017 Dan Underwood
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

package uk.ac.cam.cl.dtg.segue.dos;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.List;
import java.util.Map;

/**
 *  Abstract class for managing general user achievements.
 *
 *  @author Dan Underwood
 */
public abstract class AbstractUserAchievementManager {

    /**
     * Get all achievements of one type for a specific user.
     * @param userId - the ID of the user interested in
     * @return a list of the UserAchievement objects
     * @throws SegueDatabaseException - if a database error occurs
     */
    public abstract List<UserAchievement> getUserAchievements(final long userId)
            throws SegueDatabaseException;

}
