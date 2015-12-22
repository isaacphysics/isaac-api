/**
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dao.users;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.Validate;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;

/**
 * @author sac92
 *
 */
public class PgUsers implements IUserDataManager {
    private static final String MASTER_ID = "_id"; // MONGO DB ID
    //private static final Logger log = LoggerFactory.getLogger(PgUsers.class);
            
    private final PostgresSqlDb database;
    
    /**
     * PgUsers.
     * @param ds - the postgres datasource to use
     */
    @Inject
    public PgUsers(final PostgresSqlDb ds) {
        this.database = ds;
    }

    @Override
    public RegisteredUser registerNewUserWithProvider(final RegisteredUser user, final AuthenticationProvider provider,
            final String providerUserId) throws SegueDatabaseException {
        // create the users local account.
        RegisteredUser localUser = this.createOrUpdateUser(user);

        // link the provider account to the newly created account.
        this.linkAuthProviderToAccount(localUser, provider, providerUserId);

        return localUser;
    }

    @Override
    public boolean hasALinkedAccount(final RegisteredUser user) throws SegueDatabaseException {
        if (user.getId() == null) {
            return false;
        }
        
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT COUNT(*) AS TOTAL FROM linked_accounts WHERE user_id = ?");
            pst.setLong(1, user.getId());

            ResultSet results = pst.executeQuery();
            results.next();
            return results.getInt("TOTAL") != 0;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }     
    }

    @Override
    public List<AuthenticationProvider> getAuthenticationProvidersByUser(final RegisteredUser user)
            throws SegueDatabaseException {

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM linked_accounts WHERE user_id = ?");
            pst.setLong(1, user.getId());

            ResultSet results = pst.executeQuery();
            
            List<AuthenticationProvider> listOfResults = Lists.newArrayList();
            while (results.next()) {
                listOfResults.add(AuthenticationProvider.valueOf(results.getString("provider")));
            }

            return listOfResults;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }  
    }

    @Override
    public RegisteredUser getByLinkedAccount(final AuthenticationProvider provider, final String providerUserId)
            throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select * FROM linked_accounts WHERE provider = ? AND provider_user_id = ?");
            pst.setString(1, provider.name());
            pst.setString(2, providerUserId);

            ResultSet results = pst.executeQuery();
            
            if (!results.isBeforeFirst()) {
                return null;
            } else {
                results.next();                
            }
            
            return getById(results.getLong("user_id"));
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }    
    }

    @Override
    public boolean linkAuthProviderToAccount(final RegisteredUser user, final AuthenticationProvider provider,
            final String providerUserId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn
                    .prepareStatement(
                            "INSERT INTO linked_accounts(user_id, provider, provider_user_id)"
                            + " VALUES (?, ?, ?);",
                            Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, user.getId());
            pst.setString(2, provider.name());
            pst.setString(3, providerUserId);
            
            int affectedRows = pst.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating linked account record failed, no rows changed");
            }
            
            return true;
            
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public void unlinkAuthProviderFromUser(final RegisteredUser user, final AuthenticationProvider provider)
            throws SegueDatabaseException {
        List<AuthenticationProvider> authenticationProvidersByUser = getAuthenticationProvidersByUser(user);
        if (!authenticationProvidersByUser.contains(provider)) {
            throw new SegueDatabaseException(String.format(
                    "The delete request cannot be fulfilled as there is no %s provider registered for user (%s).",
                    provider, user.getEmail()));
        }
               
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("DELETE FROM linked_accounts WHERE provider = ? AND user_id = ?");
            pst.setString(1, provider.name());
            pst.setLong(2, user.getId());
            
            pst.execute();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public RegisteredUser getByLegacyId(final String id) throws SegueDatabaseException {
        // if the id is null then we won't find anyone so just return null.
        if (null == id) {
            return null;
        }
        
        // TODO Currently this uses the old mongo id for look ups.
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM users WHERE " + MASTER_ID + " = ?");
            pst.setString(1, id);

            ResultSet results = pst.executeQuery();
            
            if (!results.isBeforeFirst()) {
                return null;
            } 
            
            return this.findOneUser(results);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }        
    }
    
    /**
     * @param id the id of the user to find
     * @return a user object or null if none found.
     * @throws SegueDatabaseException
     */
    @Override
    public RegisteredUser getById(final Long id) throws SegueDatabaseException {
        if (null == id) {
            return null;
        }
        
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
            pst.setLong(1, id);

            ResultSet results = pst.executeQuery();
            
            return this.findOneUser(results);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }        
    }

    @Override
    public RegisteredUser getByEmail(final String email) throws SegueDatabaseException {
        Validate.notBlank(email);
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM users WHERE email ILIKE ?");
            pst.setString(1, email);

            ResultSet results = pst.executeQuery();
            
            return this.findOneUser(results);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }  
    }

    @Override
    public List<RegisteredUser> findUsers(final RegisteredUser prototype) throws SegueDatabaseException {
        Map<String, Object> fieldsOfInterest = Maps.newHashMap();
        
        // Interesting fields to use for prototypical search
        if (null != prototype.getId()) {
            fieldsOfInterest.put("id", prototype.getId());
        }
        if (null != prototype.getEmail()) {
            fieldsOfInterest.put("email", prototype.getEmail());
        }
        if (null != prototype.getFamilyName()) {
            fieldsOfInterest.put("family_name", prototype.getFamilyName());
        }
        if (null != prototype.getGivenName()) {
            fieldsOfInterest.put("given_name", prototype.getGivenName());
        }
        if (null != prototype.getSchoolId()) {
            fieldsOfInterest.put("school_id", prototype.getSchoolId());
        }
        if (null != prototype.getSchoolOther()) {
            fieldsOfInterest.put("school_other", prototype.getSchoolOther());
        }
        if (null != prototype.getRole()) {
            fieldsOfInterest.put("role", prototype.getRole().name());
        }
        
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            
            StringBuilder sb = new StringBuilder();
            if (fieldsOfInterest.entrySet().size() != 0) {
                sb.append(" WHERE ");
            }
            
            int index = 0;
            List<Object> orderToAdd = Lists.newArrayList();
            for (Entry<String, Object> e : fieldsOfInterest.entrySet()) {
                
                if (e.getValue() instanceof String) {
                    sb.append(e.getKey() + " ILIKE ?");
                } else {
                    sb.append(e.getKey() + " = ?");
                }

                orderToAdd.add(e.getValue());
                if (index + 1 < fieldsOfInterest.entrySet().size()) {
                    sb.append(" AND ");
                }
                index++;
            }
            
            pst = conn.prepareStatement("SELECT * FROM users" + sb.toString());
            index = 1;
            for (Object value : orderToAdd) {
                if (value instanceof String) {
                    pst.setString(index, (String) value);
                }
                if (value instanceof Integer) {
                    pst.setInt(index, (Integer) value);
                }
                if (value instanceof Long) {
                    pst.setLong(index, (Long) value);
                }
                index++;
            }
            
            ResultSet results = pst.executeQuery();

            return this.findAllUsers(results);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }  
        
    }

    @Override
    public List<RegisteredUser> findUsers(final List<Long> usersToLocate) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            
            StringBuilder inParams = new StringBuilder();
            inParams.append("?");
            for (int i = 1; i < usersToLocate.size(); i++) {
                inParams.append(",?");
            }
            
            pst = conn.prepareStatement(String.format("SELECT * FROM users WHERE id IN (%s)",
                    inParams.toString()));

            int index = 1;
            for (Long userId : usersToLocate) {
                pst.setLong(index, userId);
                index++;
            }

            ResultSet results = pst.executeQuery();
            
            return this.findAllUsers(results);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }  
    }

    @Override
    public RegisteredUser getByResetToken(final String token) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM users WHERE reset_token = ?");
            pst.setString(1, token);

            ResultSet results = pst.executeQuery();
            
            return this.findOneUser(results);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public RegisteredUser getByEmailVerificationToken(final String token) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM users WHERE email_verification_token = ?");
            pst.setString(1, token);

            ResultSet results = pst.executeQuery();
            
            return this.findOneUser(results);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public RegisteredUser createOrUpdateUser(final RegisteredUser user) throws SegueDatabaseException {

        // determine if it is a create or update
        RegisteredUser u = this.getById(user.getId());
        
        if (null == u) {
            // create a new one
            u = this.createUser(user);
        } else {
            // update
            u = this.updateUser(user);
        }
        
        return u;
    }

    @Override
    public void deleteUserAccount(final RegisteredUser userToDelete) throws SegueDatabaseException {
        if (null == userToDelete) {
            throw new SegueDatabaseException("Unable to locate the user requested to delete.");
        }

        try (Connection conn = database.getDatabaseConnection()) {
            try {
                conn.setAutoCommit(false);
                PreparedStatement deleteLinkedAccounts;
                deleteLinkedAccounts = conn.prepareStatement("DELETE FROM linked_accounts WHERE user_id = ?");
                deleteLinkedAccounts.setLong(1, userToDelete.getId());
                deleteLinkedAccounts.execute();
                
                PreparedStatement deleteUserAccount;
                deleteUserAccount = conn.prepareStatement("DELETE FROM users WHERE id = ?");
                deleteUserAccount.setLong(1, userToDelete.getId());
                deleteUserAccount.execute();
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e1) {
            throw new SegueDatabaseException("Postgres exception", e1);
        } 
    }

    @Override
    public void updateUserLastSeen(final RegisteredUser user) throws SegueDatabaseException {
        this.updateUserLastSeen(user, new Date());
    }

    @Override
    public void updateUserLastSeen(final RegisteredUser user, final Date date) throws SegueDatabaseException {
        Validate.notNull(user);
        
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("UPDATE users SET last_seen = ? WHERE id = ?");
            pst.setTimestamp(1, new java.sql.Timestamp(date.getTime()));
            pst.setLong(2, user.getId());
            pst.execute();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
    
    /**
     * createUser.
     * @param userToCreate - a user object to persist
     * @return a register user as just created.
     * @throws SegueDatabaseException
     */
    private RegisteredUser createUser(final RegisteredUser userToCreate) throws SegueDatabaseException {    
        // make sure student is default role if none set
        if (null == userToCreate.getRole()) {
        	userToCreate.setRole(Role.STUDENT);
        }
        
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn
                    .prepareStatement(
                            "INSERT INTO users(family_name, given_name, email, role, "
                            + "date_of_birth, gender, registration_date, school_id, "
                            + "school_other, last_updated, email_verification_status, "
                            + "last_seen, default_level, password, secure_salt, reset_token, "
                            + "reset_expiry, email_verification_token, email_verification_token_expiry) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
                            Statement.RETURN_GENERATED_KEYS);
            // TODO: Change this to annotations or something to rely exclusively on the pojo.
            setValueHelper(pst, 1, userToCreate.getFamilyName());
            setValueHelper(pst, 2, userToCreate.getGivenName());
            setValueHelper(pst, 3, userToCreate.getEmail());
            setValueHelper(pst, 4, userToCreate.getRole());
            setValueHelper(pst, 5, userToCreate.getDateOfBirth());
            setValueHelper(pst, 6, userToCreate.getGender());
            setValueHelper(pst, 7, userToCreate.getRegistrationDate());
            setValueHelper(pst, 8, userToCreate.getSchoolId());
            setValueHelper(pst, 9, userToCreate.getSchoolOther());
            setValueHelper(pst, 10, userToCreate.getLastUpdated());
            setValueHelper(pst, 11,  userToCreate.getEmailVerificationStatus());
            setValueHelper(pst, 12, userToCreate.getLastSeen());
            setValueHelper(pst, 13, userToCreate.getDefaultLevel());
            setValueHelper(pst, 14, userToCreate.getPassword());
            setValueHelper(pst, 15, userToCreate.getSecureSalt());
            setValueHelper(pst, 16, userToCreate.getResetToken());
            setValueHelper(pst, 17, userToCreate.getResetExpiry());
            setValueHelper(pst, 18, userToCreate.getEmailVerificationToken());
            setValueHelper(pst, 19, userToCreate.getEmailVerificationTokenExpiry());
            
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save user.");
            }

            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    userToCreate.setId(id);
                    return userToCreate;
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
    
    /**
     * @param userToCreate - user object to save.
     * @return the user as from the database
     * @throws SegueDatabaseException
     */
    private RegisteredUser updateUser(final RegisteredUser userToCreate) throws SegueDatabaseException {
        RegisteredUser existingUserRecord = this.getById(userToCreate.getId());
        if (null == existingUserRecord) {
            throw new SegueDatabaseException("The user you have tried to update does not exist.");
        }
        
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn
                    .prepareStatement(
                            "UPDATE users SET family_name = ?, given_name = ?, email = ?, role = ?, "
                            + "date_of_birth = ?, gender = ?, registration_date = ?, school_id = ?, "
                            + "school_other = ?, last_updated = ?, email_verification_status = ?, "
                            + "last_seen = ?, default_level = ?, password = ?, secure_salt = ?, reset_token = ?, "
                            + "reset_expiry = ?, email_verification_token = ?, email_verification_token_expiry = ? "
                            + "WHERE id = ?;");
            
            setValueHelper(pst, 1, userToCreate.getFamilyName());
            setValueHelper(pst, 2, userToCreate.getGivenName());
            setValueHelper(pst, 3, userToCreate.getEmail());
            setValueHelper(pst, 4, userToCreate.getRole());
            setValueHelper(pst, 5, userToCreate.getDateOfBirth());
            setValueHelper(pst, 6, userToCreate.getGender());
            setValueHelper(pst, 7, userToCreate.getRegistrationDate());
            setValueHelper(pst, 8, userToCreate.getSchoolId());
            setValueHelper(pst, 9, userToCreate.getSchoolOther());
            setValueHelper(pst, 10, userToCreate.getLastUpdated());
            setValueHelper(pst, 11,  userToCreate.getEmailVerificationStatus());
            setValueHelper(pst, 12, userToCreate.getLastSeen());
            setValueHelper(pst, 13, userToCreate.getDefaultLevel());
            setValueHelper(pst, 14, userToCreate.getPassword());
            setValueHelper(pst, 15, userToCreate.getSecureSalt());
            setValueHelper(pst, 16, userToCreate.getResetToken());
            setValueHelper(pst, 17, userToCreate.getResetExpiry());
            setValueHelper(pst, 18, userToCreate.getEmailVerificationToken());
            setValueHelper(pst, 19, userToCreate.getEmailVerificationTokenExpiry());
            setValueHelper(pst, 20, userToCreate.getId());
            
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save user.");
            }

            return this.getById(existingUserRecord.getId());
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
        
        
    }
    
    /**
     * Create a pgEventBooking from a results set.
     * 
     * Assumes there is a result to read.
     * 
     * @param results
     *            - the results to convert
     * @return a new PgEventBooking
     * @throws SQLException
     *             - if an error occurs.
     */
    private RegisteredUser buildRegisteredUser(final ResultSet results) throws SQLException {
        if (null == results) {
            return null;
        }
        
        RegisteredUser u = new RegisteredUser();
        u.setId(results.getLong("id"));
        u.setFamilyName(results.getString("family_name"));
        u.setGivenName(results.getString("given_name"));
        u.setEmail(results.getString("email"));
        u.setRole(results.getString("role") != null ? Role.valueOf(results.getString("role")) : null);
        u.setDateOfBirth(results.getDate("date_of_birth"));
        u.setGender(results.getString("gender") != null ? Gender.valueOf(results.getString("gender")) : null);
        u.setRegistrationDate(results.getTimestamp("registration_date"));
        
        u.setSchoolId(results.getLong("school_id"));
        if (results.wasNull()) {
            u.setSchoolId(null);
        }
        
        u.setSchoolOther(results.getString("school_other"));
        u.setLastUpdated(results.getTimestamp("last_updated"));
        u.setLastSeen(results.getTimestamp("last_seen"));
        u.setDefaultLevel(results.getInt("default_level"));
        u.setPassword(results.getString("password"));
        u.setSecureSalt(results.getString("secure_salt"));
        u.setResetToken(results.getString("reset_token"));
        u.setResetExpiry(results.getTimestamp("reset_expiry"));
        u.setEmailVerificationToken(results.getString("email_verification_token"));
        u.setEmailVerificationStatus(results.getString("email_verification_status") != null ? EmailVerificationStatus
                .valueOf(results.getString("email_verification_status")) : null);
        u.setEmailVerificationTokenExpiry(results.getTimestamp("email_verification_token_expiry"));
        
        return u;
    }

    /**
     * findOne helper method to ensure that only one result matches the search criteria.
     * 
     * @param results
     *            - from a jdbc database search
     * @return a single user that matches the search criteria or null of no matches found.
     * @throws SQLException
     *             - if a db error occurs
     * @throws SegueDatabaseException
     *             - if more than one result is returned
     */
    private RegisteredUser findOneUser(final ResultSet results) throws SQLException, SegueDatabaseException {
        // are there any results
        if (!results.isBeforeFirst()) {
            return null;
        } 
        
        List<RegisteredUser> listOfResults = Lists.newArrayList();
        while (results.next()) {
            listOfResults.add(buildRegisteredUser(results));
        }

        if (listOfResults.size() > 1) {
            throw new SegueDatabaseException("Ambiguous result, expected single result and found more than one"
                    + listOfResults);
        }

        return listOfResults.get(0);
    }
    
    /**
     * findOne helper method to ensure that only one result matches the search criteria.
     * 
     * @param results
     *            - from a jdbc database search
     * @return a single user that matches the search criteria or null of no matches found.
     * @throws SQLException
     *             - if a db error occurs
     * @throws SegueDatabaseException
     *             - if more than one result is returned
     */
    private List<RegisteredUser> findAllUsers(final ResultSet results) throws SQLException, SegueDatabaseException {
        List<RegisteredUser> listOfResults = Lists.newArrayList();
        while (results.next()) {
            listOfResults.add(buildRegisteredUser(results));
        }

        return listOfResults;
    }
    
    /**
     * Helper that picks the correct pst method based on the value provided.
     * 
     * @param pst - prepared statement - already initialised
     * @param index - index of the value to be replaced in the pst
     * @param value - value
     * @throws SQLException 
     */
    private void setValueHelper(final PreparedStatement pst, final int index, final Object value) throws SQLException {
        if (null == value) {
            pst.setNull(index, java.sql.Types.NULL);
            return;
        }
        
        if (value.getClass().isEnum()) {
            pst.setString(index, ((Enum<?>) value).name());
        }
        
        if (value instanceof String) {
            pst.setString(index, (String) value);
        }
        
        if (value instanceof Integer) {
            pst.setInt(index, (Integer) value);
        }
        
        if (value instanceof Long) {
            pst.setLong(index, (Long) value);
        }
        
        if (value instanceof Date) {
            pst.setTimestamp(index, new java.sql.Timestamp(((Date) value).getTime()));
        }
    }
}