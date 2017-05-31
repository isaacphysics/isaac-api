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

/**
 *  A class to represent a general user achievement, which has both a type (for example a badge achievement) and a name.
 *  The achievement object is immutable.
 */
public class UserAchievement {

    private String achievementName;
    private Integer achievementAmount;

    /**
     * Create an immutable UserAchievement object.
     * @param achievementName - the name of the achievement
     */
    public UserAchievement(final String achievementName, final Integer achievementAmount) {
        this.achievementName = achievementName;
        this.achievementAmount = achievementAmount;
    }


    /**
     * @return the name of the achievement
     */
    public String getAchievementName() {
        return this.achievementName;
    }


    /**
     * @return the threshold amount of the achievement
     */
    public Integer getAchievementAmount() {
        return this.achievementAmount;
    }

}
