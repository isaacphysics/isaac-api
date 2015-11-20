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

import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * Interface class for email preferences.
 *
 * @author Alistair Stead
 *
 */
public abstract class AbstractEmailPreferenceManager {
	
	/**
     * @param userId - the user id to look up
     * @return the list of email preferences for this user.
     * @throws SegueDatabaseException - if a database error has occurred.
	 */
	public abstract List<IEmailPreference> getEmailPreferences(final long userId) throws SegueDatabaseException;
	
	/**
     * @param userId - the user id to look up
     * @param emailPreference - the preference we want to look up
     * @return the list of email preferences for this user.
     * @throws SegueDatabaseException - if a database error has occurred.
	 */
	public abstract IEmailPreference getEmailPreference(final long userId, 
									final EmailType emailPreference) throws SegueDatabaseException;
	
	/**
     * @param userIds - a list of user ids to look up
     * @return a map of user ids and email preferences.
     * @throws SegueDatabaseException - if a database error has occurred.
	 */
	public abstract Map<Long, Map<EmailType, Boolean>> getEmailPreferences(final List<RegisteredUserDTO> users) 
									throws SegueDatabaseException;
	
    /**
     * @param userId - the user id to save a record for.
     * @param emailPreferences - the email preferences of the user
     * @throws SegueDatabaseException - if a database error has occurred.
     */
	public abstract void saveEmailPreferences(final long userId, final List<IEmailPreference> 
									emailPreferences) throws SegueDatabaseException;

	/**
	 * @param userId - the id of the user
	 * @param preferencePairs - pairs of preferences from the user facade
	 * @return a list of email preferences
	 */
	public abstract List<IEmailPreference> mapToEmailPreferenceList(long userId, Map<String, Boolean> preferencePairs);

	/**
	 * @param emailPreferenceList - list of email preferences
	 * @return map of email preference pairs
	 */
	public abstract Map<String, Boolean> mapToEmailPreferencePair(List<IEmailPreference> emailPreferenceList);

}
