/**
 * Copyright 2015 Alistair Stead
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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.IEmailPreference.EmailPreference;

/**
 * Interface class for email preferences.
 *
 * @author Alistair Stead
 *
 */
public interface IEmailPreferences {
	
	/**
     * @param userId - the user id to look up
     * @return the list of email preferences for this user.
     * @throws SegueDatabaseException - if a database error has occurred.
	 */
	List<IEmailPreference> getEmailPreferences(final String userId) throws SegueDatabaseException;
	
	/**
     * @param userId - the user id to look up
     * @param emailPreference - the preference we want to look up
     * @return the list of email preferences for this user.
     * @throws SegueDatabaseException - if a database error has occurred.
	 */
	IEmailPreference getEmailPreference(final String userId, 
									final EmailPreference emailPreference) throws SegueDatabaseException;
	
	/**
     * @param userIds - a list of user ids to look up
     * @return a map of user ids and email preferences.
     * @throws SegueDatabaseException - if a database error has occurred.
	 */
	Map<String, List<IEmailPreference>> getEmailPreferences(final List<String> userIds) throws SegueDatabaseException;
	
    /**
     * @param userId - the user id to save a record for.
     * @param emailPreference - a user email preference
     * @param emailPreferenceStatus - the status of the preference 
     * @throws SegueDatabaseException - if a database error has occurred.
     */
    void saveEmailPreference(String userId, EmailPreference emailPreference, boolean emailPreferenceStatus)
            throws SegueDatabaseException;

}
