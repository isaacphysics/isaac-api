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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;

/**
 * Represents a postgres specific implementation of the AbstractEmailManager DAO interface.
 *
 * @author Alistair Stead
 *
 */
public class PgEmailPreferenceManager extends AbstractEmailPreferenceManager {
    private final PostgresSqlDb database;
    private static final Logger log = LoggerFactory.getLogger(PgEmailPreferenceManager.class);
    
    /**
     * Postgres email preferences.
     * @param database - the postgres database
     */
    @Inject
    public PgEmailPreferenceManager(final PostgresSqlDb database) {
    	this.database = database;
    }
    
	@Override
	public void saveEmailPreferences(final long userId, final List<IEmailPreference> 
					emailPreferences) throws SegueDatabaseException {
		
		for (IEmailPreference preference : emailPreferences) {
			Validate.isTrue(preference.getEmailType().isValidEmailPreference());
		}
		
		upsertEmailPreferenceRecords(emailPreferences);
	}
	

	/**
     * @param userId
     * @return
     * @throws SegueDatabaseException
     */
    @Override
    public List<IEmailPreference> getEmailPreferences(final long userId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select * FROM user_email_preferences WHERE user_id = ?");
            pst.setLong(1, userId);

            ResultSet results = pst.executeQuery();
            List<IEmailPreference> returnResult = Lists.newArrayList();
            Set<Integer> existingPreferences = new HashSet<Integer>();
            while (results.next()) {
            	int emailPreference = results.getInt("email_preference");
            	boolean emailPreferenceStatus = results.getBoolean("email_preference_status");
                returnResult.add(new PgEmailPreference(userId, EmailType.mapIntToPreference(emailPreference),
																								emailPreferenceStatus));
                existingPreferences.add(emailPreference);
            }
            
            //Add default email settings if they are not initialised
            EmailType [] emailPreferenceTypes = EmailType.values();
            
            for (int i = 0; i < emailPreferenceTypes.length; i++) {
            	if (!existingPreferences.contains(emailPreferenceTypes[i].mapEmailTypeToInt())
            					&& emailPreferenceTypes[i].isValidEmailPreference()) {
            		PgEmailPreference newPreference = new PgEmailPreference(userId, emailPreferenceTypes[i], true);
            		returnResult.add(newPreference);
            	}
            }
            
            
            return returnResult;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }



	@Override
	public Map<Long, Map<EmailType, Boolean>> getEmailPreferences(
			final List<RegisteredUserDTO> users) throws SegueDatabaseException {
		
		Map<Long, Map<EmailType, Boolean>> returnMap = Maps.newHashMap(); 
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            StringBuilder sb = new StringBuilder();
            sb.append("Select * FROM user_email_preferences WHERE user_id in (");
            
            for (int i = 0; i < users.size(); i++) {
            	sb.append("?" + (i < users.size() - 1 ? ", " : ""));
            }
            
            sb.append(") ORDER BY user_id ASC, email_preference ASC");
            
            pst = conn.prepareStatement(sb.toString());

            for (int i = 1; i <= users.size(); i++) {
            	pst.setLong(i, users.get(i - 1).getId());
            }

            ResultSet results = pst.executeQuery();
            
            // get preferences for all users
            while (results.next()) {
            	long userId = results.getLong("user_id");
            	int emailPreference = results.getInt("email_preference");
            	boolean emailPreferenceStatus = results.getBoolean("email_preference_status");
            	Map<EmailType, Boolean> values = null;
        		if (returnMap.containsKey(userId) && returnMap.get(userId) != null) {
        			values = returnMap.get(userId); 
        		} else {
        			values = new HashMap<EmailType, Boolean>();
        			returnMap.put(userId, values);
        		}
        		EmailType emailType = EmailType.mapIntToPreference(emailPreference);
        		values.put(emailType, emailPreferenceStatus);
            }
            
            // set defaults for those email preferences that have not been found
            for (int i = 0; i < users.size(); i++) {
            	long key = users.get(i).getId();
            	if (returnMap.containsKey(key)) {
            		Map<EmailType, Boolean> existingPreferences = returnMap.get(key);
            		
            		for (EmailType emailType : EmailType.values()) {
            			if (emailType.isValidEmailPreference() 
            							&& !existingPreferences.containsKey(emailType)) {
            				existingPreferences.put(emailType, true);
            			}
            		}
            	}
            }
            
            return returnMap;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
	}
	
       
    /**
     * @param emailPreferences 
     * 					- the user email preference to update
     * @throws SegueDatabaseException
     * 					- when there is a database error
     */
    private void upsertEmailPreferenceRecords(final List<IEmailPreference> emailPreferences) 
    					throws SegueDatabaseException {
    	PreparedStatement pst;
    	try (Connection conn = database.getDatabaseConnection()) {
    		conn.setAutoCommit(false);
    		for(IEmailPreference preference : emailPreferences) {
	    		pst = conn.prepareStatement("WITH upsert AS (UPDATE user_email_preferences "
	    				+ "SET email_preference_status=? WHERE user_id=? AND email_preference=? RETURNING *) "
	    				+ "INSERT INTO user_email_preferences (user_id, email_preference, email_preference_status) "
	    				+ "SELECT ?, ?, ? WHERE NOT EXISTS (SELECT * FROM upsert);");
	    		pst.setBoolean(1,  preference.getEmailPreferenceStatus());
	    		pst.setLong(2, preference.getUserId());
	    		pst.setInt(3,  preference.getEmailType().mapEmailTypeToInt());
	    		pst.setLong(4, preference.getUserId());
	    		pst.setInt(5,  preference.getEmailType().mapEmailTypeToInt());
	    		pst.setBoolean(6,  preference.getEmailPreferenceStatus());
	    		
	
	            pst.executeUpdate();
    		}
            conn.commit();
            conn.setAutoCommit(true);
    	} catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception on upsert ", e);
    	}
    }    

    @Override
	public IEmailPreference getEmailPreference(final long userId,
									final EmailType emailType) throws SegueDatabaseException {
    	PreparedStatement pst;
    	try (Connection conn = database.getDatabaseConnection()) {
    		pst = conn.prepareStatement("SELECT * FROM user_email_preferences WHERE user_id=? AND email_preference=?");
    		pst.setLong(1, userId);
    		pst.setInt(2, emailType.mapEmailTypeToInt());
    		pst.setMaxRows(1);
    		ResultSet results = pst.executeQuery();
    		if (results.next()) {
    			boolean emailPreferenceStatus = results.getBoolean("email_preference_status");
        		return new PgEmailPreference(userId, emailType, emailPreferenceStatus);
    		} else {
    			//set defaults for those email preferences that have not been found
        		return new PgEmailPreference(userId, emailType, true);
    		}
    	} catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
    	}
	}

	@Override
	public List<IEmailPreference> mapToEmailPreferenceList(final long userId,
					final Map<String, Boolean> preferencePairs) {
		List<IEmailPreference> returnObject = null;
		try {
			returnObject = new ArrayList<IEmailPreference>();
			Set<String> keys = preferencePairs.keySet();
			for (String key : keys) {
				Boolean preference = preferencePairs.get(key);
				EmailType emailType = EmailType.valueOf(key);
				returnObject.add(new PgEmailPreference(userId, emailType, preference));
			}
		} catch (IllegalArgumentException e) {
			log.error("IllegalArgumentException - email preference could not be mapped to an email type");
		}
		return returnObject;
	}

	@Override
	public Map<String, Boolean> mapToEmailPreferencePair(final List<IEmailPreference> emailPreferenceList) {
		Map<String, Boolean> returnObject = null;
		returnObject = new HashMap<String, Boolean>();
		for (IEmailPreference preference : emailPreferenceList) {
			returnObject.put(preference.getEmailType().toString(), preference.getEmailPreferenceStatus());
		}
		return returnObject;
	}

	
}
