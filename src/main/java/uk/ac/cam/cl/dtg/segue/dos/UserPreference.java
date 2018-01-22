/*
 * Copyright 2017 James Sharkey
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
 *  A class to represent a general user preference, which has both a type (for example a colour preference) and a name
 *  (for example the specific colour 'red') associated with the boolean value. The preference object is immutable.
 */
public class UserPreference {

    private long userId;
    private String preferenceType;
    private String preferenceName;
    private boolean preferenceValue;

    /**
     * Create an immutable UserPreference object.
     * @param userId - the ID of the user
     * @param preferenceType - the type of preference
     * @param preferenceName - the name of the preference
     * @param preferenceValue - the boolean value of the preference
     */
    public UserPreference(final long userId, final String preferenceType, final String preferenceName, final boolean preferenceValue) {
        this.userId = userId;
        this.preferenceType = preferenceType;
        this.preferenceName = preferenceName;
        this.preferenceValue = preferenceValue;
    }

    /**
     * @return the ID of the user the preference belongs to
     */
    public long getUserId() {
        return userId;
    }

    /**
     * @return the type of the preference
     */
    public String getPreferenceType() {
        return preferenceType;
    }

    /**
     * @return the name of the perference
     */
    public String getPreferenceName() {
        return preferenceName;
    }

    /**
     * @return the value of the preference
     */
    public boolean getPreferenceValue() {
        return preferenceValue;
    }
}
