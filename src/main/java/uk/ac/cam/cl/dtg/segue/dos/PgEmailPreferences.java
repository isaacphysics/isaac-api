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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.IEmailPreference.EmailPreference;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

/**
 * Represents a postgres specific implementation of the UserEmailPreferences DAO interface.
 *
 * @author Alistair Stead
 *
 */
public class PgEmailPreferences implements IEmailPreferences {
    private final PostgresSqlDb database;
    
    /**
     * Postgres email preferences.
     * @param database - the postgres database
     */
    @Inject
    public PgEmailPreferences(final PostgresSqlDb database) {
    	this.database = database;
    }
    
	@Override
	public void saveEmailPreference(final String userId, final EmailPreference emailPreference, 
						final boolean emailPreferenceStatus) throws SegueDatabaseException {
		PgEmailPreference preference = new PgEmailPreference(userId, emailPreference, emailPreferenceStatus);
		if (null == getEmailPreference(userId, emailPreference)) {
			updateEmailPreferenceRecord(preference);
		} else {
			insertNewEmailPreferenceRecord(preference);
		}
	}
	

	/**
     * @param userId
     * @return
     * @throws SegueDatabaseException
     */
    @Override
    public List<IEmailPreference> getEmailPreferences(final String userId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select * FROM user_email_preferences WHERE user_id = ? ORDER BY created ASC");
            pst.setString(1, userId);

            ResultSet results = pst.executeQuery();
            List<IEmailPreference> returnResult = Lists.newArrayList();
            while (results.next()) {
            	int emailPreference = results.getInt("email_preference");
            	boolean emailPreferenceStatus = results.getBoolean("email_preference_status");
                returnResult.add(new PgEmailPreference(userId, EmailPreference.mapIntToPreference(emailPreference),
																								emailPreferenceStatus));
            }
            return returnResult;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }



	@Override
	public Map<String, List<IEmailPreference>> getEmailPreferences(
					final List<String> userIds) throws SegueDatabaseException {
		
		HashMap<String, List<IEmailPreference>> returnMap = new HashMap<String, List<IEmailPreference>>();
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select * FROM user_email_preferences "
            						   + "WHERE user_id in (?) ORDER BY user_id ASC");
            pst.setString(1, userIds.toString());

            ResultSet results = pst.executeQuery();

            while (results.next()) {
            	String userId = results.getString("user_id");
            	int emailPreference = results.getInt("email_preference");
            	boolean emailPreferenceStatus = results.getBoolean("email_preference_status");
        		List<IEmailPreference> values = null;
        		if (returnMap.containsKey(userId) && returnMap.get(userId) != null) {
        			values = returnMap.get(userId); 
        		} else {
        			values = Lists.newArrayList();
        		}
        		values.add(new PgEmailPreference(userId, 
        						EmailPreference.mapIntToPreference(emailPreference), emailPreferenceStatus));
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

            pst.setString(1, emailPreference.getUserId());
            pst.setInt(2, emailPreference.getEmailPreference().mapPreferenceToInt());
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
    						+ "SET email_preference_status='?'"
    						+ "WHERE user_id='?' AND email_preference='?'");
    		pst.setBoolean(1,  emailPreference.getEmailPreferenceStatus());
    		pst.setString(2, emailPreference.getUserId());
    		pst.setInt(3,  emailPreference.getEmailPreference().mapPreferenceToInt());
    		
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
    						+ "WHERE user_id='?' AND email_preference='?'");
    		pst.setString(2, emailPreference.getUserId());
    		pst.setInt(3,  emailPreference.getEmailPreference().mapPreferenceToInt());
    		
    		if (pst.executeUpdate() != 1) {
    			throw new SegueDatabaseException("Unable to save user email preference.");
    		}
    		
    	} catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
    	}
    }
    

    @Override
	public IEmailPreference getEmailPreference(final String userId,
			final EmailPreference emailPreference) throws SegueDatabaseException {
    	PreparedStatement pst;
    	try (Connection conn = database.getDatabaseConnection()) {
    		pst = conn.prepareStatement("SELECT email_preference_status FROM user_email_preferences " 
    						+ "WHERE user_id='?' AND email_preference='?'");
    		pst.setString(2, userId);
    		pst.setInt(3,  emailPreference.mapPreferenceToInt());
    		
    		ResultSet results = pst.executeQuery();
    		if (results.first()) {
    			boolean emailPreferenceStatus = results.getBoolean("email_preference_status");
        		return new PgEmailPreference(userId, emailPreference, emailPreferenceStatus);
    		} else {
        		return null;
    		}
    	} catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
    	}
	}

	
}
