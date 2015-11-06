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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import com.google.api.client.util.Lists;
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
			
			if (null != getEmailPreference(userId, preference.getEmailType())) {
				updateEmailPreferenceRecord(preference);
			} else {
				insertNewEmailPreferenceRecord(preference);
			}
		}
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
            while (results.next()) {
            	int emailPreference = results.getInt("email_preference");
            	boolean emailPreferenceStatus = results.getBoolean("email_preference_status");
                returnResult.add(new PgEmailPreference(userId, EmailType.mapIntToPreference(emailPreference),
																								emailPreferenceStatus));
            }
            return returnResult;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }



	@Override
	public Map<String, List<IEmailPreference>> getEmailPreferences(
					final List<Long> userIds) throws SegueDatabaseException {
		
		HashMap<String, List<IEmailPreference>> returnMap = new HashMap<String, List<IEmailPreference>>();
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select * FROM user_email_preferences "
            						   + "WHERE user_id in (?) ORDER BY user_id ASC");
            pst.setString(1, userIds.toString());

            ResultSet results = pst.executeQuery();

            while (results.next()) {
            	long userId = results.getLong("user_id");
            	int emailPreference = results.getInt("email_preference");
            	boolean emailPreferenceStatus = results.getBoolean("email_preference_status");
        		List<IEmailPreference> values = null;
        		if (returnMap.containsKey(userId) && returnMap.get(userId) != null) {
        			values = returnMap.get(userId); 
        		} else {
        			values = Lists.newArrayList();
        		}

        		values.add(new PgEmailPreference(userId, 
        						EmailType.mapIntToPreference(emailPreference), emailPreferenceStatus));
            }
            
            return returnMap;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
	}
	
    /**
     * @param emailPreference - the user email preference to insert.
     * @throws SegueDatabaseException - if it fails.
     */
    private void insertNewEmailPreferenceRecord(final IEmailPreference emailPreference) 
    				throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {

            pst = conn
                    .prepareStatement("INSERT INTO user_email_preferences "
                            + "(user_id, email_preference, email_preference_status) "
                            + "VALUES (?, ?, ?)");

            pst.setLong(1, emailPreference.getUserId());
            pst.setInt(2, emailPreference.getEmailType().mapEmailTypeToInt());
            pst.setBoolean(3, emailPreference.getEmailPreferenceStatus());

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save user email preference.");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
    
    /**
     * @param emailPreference - the user email preference to update
     */
    private void updateEmailPreferenceRecord(final IEmailPreference emailPreference) 
    					throws SegueDatabaseException {
    	PreparedStatement pst;
    	try (Connection conn = database.getDatabaseConnection()) {
    		pst = conn.prepareStatement("UPDATE user_email_preferences " 
    						+ "SET email_preference_status=?"
    						+ "WHERE user_id=? AND email_preference=?");
    		pst.setBoolean(1,  emailPreference.getEmailPreferenceStatus());
    		pst.setLong(2, emailPreference.getUserId());
    		pst.setInt(3,  emailPreference.getEmailType().mapEmailTypeToInt());
    		
    		if (pst.executeUpdate() != 1) {
    			throw new SegueDatabaseException("Unable to save user email preference.");
    		}
    		
    	} catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
    	}
    }
    
    /**
     * @param emailPreference - the user email preference to delete
     */
    private void deleteEmailPreferenceRecord(final IEmailPreference emailPreference) 
    					throws SegueDatabaseException {
    	PreparedStatement pst;
    	try (Connection conn = database.getDatabaseConnection()) {
    		pst = conn.prepareStatement("DELETE user_email_preferences " 
    						+ "WHERE user_id=? AND email_preference=?");
    		pst.setLong(2, emailPreference.getUserId());
    		pst.setInt(3,  emailPreference.getEmailType().mapEmailTypeToInt());
    		
    		if (pst.executeUpdate() != 1) {
    			throw new SegueDatabaseException("Unable to save user email preference.");
    		}
    		
    	} catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
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
        		return null;
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

	/* (non-Javadoc)
	 * @see uk.ac.cam.cl.dtg.segue.dos.AbstractEmailPreferenceManager#mapToEmailPreferencePair(java.util.List)
	 */
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
