/*
 * Copyright 2015 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao.users;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Gender;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserAuthenticationSettings;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserContext;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.DuplicateAccountException;
import uk.ac.cam.cl.dtg.segue.dao.AbstractPgDataManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * @author Stephen Cummins
 *
 */
public class PgUsers extends AbstractPgDataManager implements IUserDataManager {
    private static final String POSTGRES_EXCEPTION_MESSAGE = "Postgres exception";
    private static final String JSONB_PROCESSING_ERROR_MESSAGE = "Postgres JSONb processing exception";

    private final PostgresSqlDb database;
    private final ObjectMapper jsonMapper;

    /**
     * PgUsers.
     * @param ds - the postgres datasource to use
     * @param jsonMapper - a mapper for converting to and from JSON for postgres' jsonb type
     */
    @Inject
    public PgUsers(final PostgresSqlDb ds, final ObjectMapper jsonMapper) {
        this.database = ds;
        this.jsonMapper = jsonMapper;
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

        String query = "SELECT COUNT(*) AS TOTAL FROM linked_accounts WHERE user_id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, user.getId());

            try (ResultSet results = pst.executeQuery()) {
                results.next();
                return results.getInt("TOTAL") != 0;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        }     
    }

    @Override
    public List<AuthenticationProvider> getAuthenticationProvidersByUser(RegisteredUser user) throws SegueDatabaseException {
        String query = "SELECT * FROM linked_accounts WHERE user_id = ?;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, user.getId());

            List<AuthenticationProvider> authenticationProviders = Lists.newArrayList();
            try (ResultSet queryResults = pst.executeQuery()) {
                while (queryResults.next()) {
                    authenticationProviders.add(AuthenticationProvider.valueOf(queryResults.getString("provider")));
                }
                return authenticationProviders;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        }
    }

    @Override
    public UserAuthenticationSettings getUserAuthenticationSettings(Long userId) throws SegueDatabaseException {

        String query = "SELECT users.id, password IS NOT NULL AS has_segue_account, user_totp.shared_secret IS NOT NULL AS mfa_status, array_agg(provider) AS linked_accounts " +
                "FROM (users LEFT OUTER JOIN user_credentials ON user_credentials.user_id=users.id) " +
                "LEFT OUTER JOIN linked_accounts ON users.id=linked_accounts.user_id " +
                "LEFT OUTER JOIN user_totp ON users.id=user_totp.user_id WHERE users.id=? GROUP BY users.id, user_credentials.user_id, mfa_status;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);

            try (ResultSet results = pst.executeQuery()) {
                if (!results.isBeforeFirst()) {
                    return null;
                } else {
                    results.next();
                }

                String[] providers = (String[]) results.getArray("linked_accounts").getArray();
                List<AuthenticationProvider> providersList = Lists.newArrayList();
                for (String provider : providers) {
                    // the way the join works means that if a user has no linked accounts a single element comes back as null
                    if (provider != null) {
                        providersList.add(AuthenticationProvider.valueOf(provider));
                    }
                }

                return new UserAuthenticationSettings(userId, providersList, results.getBoolean("has_segue_account"), results.getBoolean("mfa_status"));
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        }
    }

    @Override
    public RegisteredUser getByLinkedAccount(final AuthenticationProvider provider, final String providerUserId)
            throws SegueDatabaseException {
        String query = "SELECT * FROM linked_accounts WHERE provider = ? AND provider_user_id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, provider.name());
            pst.setString(2, providerUserId);

            try (ResultSet results = pst.executeQuery()) {
                if (!results.isBeforeFirst()) {
                    return null;
                } else {
                    results.next();
                }
                return getById(results.getLong("user_id"));
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        }    
    }

    @Override
    public boolean linkAuthProviderToAccount(final RegisteredUser user, final AuthenticationProvider provider,
            final String providerUserId) throws SegueDatabaseException {
        String query = "INSERT INTO linked_accounts(user_id, provider, provider_user_id) VALUES (?, ?, ?);";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ) {
            pst.setLong(1, user.getId());
            pst.setString(2, provider.name());
            pst.setString(3, providerUserId);
            
            int affectedRows = pst.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating linked account record failed, no rows changed");
            }
            
            return true;
            
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
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

        String query = "DELETE FROM linked_accounts WHERE provider = ? AND user_id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, provider.name());
            pst.setLong(2, user.getId());
            
            pst.execute();
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        }
    }

    @Override
    public RegisteredUser getById(final Long id) throws SegueDatabaseException {
        return getById(id, false);
    }

    @Override
    public RegisteredUser getById(final Long id, final boolean includeDeletedUsers) throws SegueDatabaseException {
        if (null == id) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM users WHERE id = ?");
        if (!includeDeletedUsers) {
            sb.append(" AND NOT deleted");
        }

        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(sb.toString());
        ) {
            pst.setLong(1, id);

            try (ResultSet results = pst.executeQuery()) {
                return this.findOneUser(results);
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException(JSONB_PROCESSING_ERROR_MESSAGE, e);
        }
    }

    @Override
    public RegisteredUser getByEmail(final String email) throws SegueDatabaseException {
        Validate.notBlank(email);
        String query = "SELECT * FROM users WHERE lower(email)=lower(?) AND NOT deleted";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, email);

            try (ResultSet results = pst.executeQuery()) {
                return this.findOneUser(results);
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException(JSONB_PROCESSING_ERROR_MESSAGE, e);
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
        if (null != prototype.getEmailVerificationStatus()) {
            fieldsOfInterest.put("email_verification_status", prototype.getEmailVerificationStatus().name());
        }

        // Build optional WHERE clause:
        StringBuilder sb = new StringBuilder();
        sb.append(" WHERE NOT deleted");
        if (fieldsOfInterest.entrySet().size() != 0) {
            sb.append(" AND ");
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

        String query = "SELECT * FROM users" + sb.toString() + " ORDER BY family_name, given_name";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
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
            
            try (ResultSet results = pst.executeQuery()) {
                return this.findAllUsers(results);
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException(JSONB_PROCESSING_ERROR_MESSAGE, e);
        }
    }

    @Override
    public List<RegisteredUser> findUsers(final List<Long> usersToLocate) throws SegueDatabaseException {
        String query = "SELECT * FROM users WHERE id = ANY(?) AND NOT deleted ORDER BY family_name, given_name";

        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            Array idArray = conn.createArrayOf("INTEGER", usersToLocate.toArray());
            pst.setArray(1, idArray);

            try (ResultSet results = pst.executeQuery()) {
                return this.findAllUsers(results);
            } finally {
                idArray.free();
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException(JSONB_PROCESSING_ERROR_MESSAGE, e);
        }
    }

    @Override
    public Map<Role, Long> getRoleCount() throws SegueDatabaseException {
        String query = "SELECT role, count(1) FROM users WHERE NOT deleted GROUP BY role;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
             ResultSet results = pst.executeQuery();
        ) {
            Map<Role, Long> resultToReturn = Maps.newHashMap();

            while (results.next()) {
                resultToReturn.put(Role.valueOf(results.getString("role")), results.getLong("count"));
            }

            return resultToReturn;
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        }
    }

    @Override
    public Map<Gender, Long> getGenderCount() throws SegueDatabaseException {
        String query = "SELECT gender, count(1) FROM users WHERE NOT deleted GROUP BY gender;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
             ResultSet results = pst.executeQuery();
        ) {
            Map<Gender, Long> resultToReturn = Maps.newHashMap();

            while (results.next()) {
                String genderString = results.getString("gender");
                Gender gender = genderString != null ? Gender.valueOf(genderString) : Gender.UNKNOWN;
                resultToReturn.put(gender, results.getLong("count"));
            }

            return resultToReturn;
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        }
    }

    @Override
    public Map<Role, Long> getRolesLastSeenOver(TimeInterval timeInterval) throws SegueDatabaseException {
        String query = "SELECT role, count(1) FROM users WHERE NOT deleted AND last_seen >= now() - ? GROUP BY role";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setObject(1, timeInterval.getPGInterval());

            try (ResultSet results = pst.executeQuery()) {
                Map<Role, Long> resultsToReturn = Maps.newHashMap();
                while (results.next()) {
                    resultsToReturn.put(Role.valueOf(results.getString("role")), results.getLong("count"));
                }
                return resultsToReturn;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        }
    }

    @Override
    public Map<SchoolInfoStatus, Long> getSchoolInfoStats() throws SegueDatabaseException {
        String query = "SELECT school_id IS NOT NULL AS has_school_id,  school_other IS NOT NULL AS has_school_other," +
                " count(1) FROM users WHERE NOT deleted GROUP BY has_school_id, has_school_other;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
             ResultSet results = pst.executeQuery();
        ) {
            Map<SchoolInfoStatus, Long> resultsToReturn = Maps.newHashMap();
            while (results.next()) {
                boolean hasSchoolId = results.getBoolean("has_school_id");
                boolean hasSchoolOther = results.getBoolean("has_school_other");
                SchoolInfoStatus recordType = SchoolInfoStatus.get(hasSchoolId, hasSchoolOther);
                resultsToReturn.put(recordType, results.getLong("count"));
            }
            return resultsToReturn;
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
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

        // FIXME: try-with-resources!
        try (Connection conn = database.getDatabaseConnection()) {
            try {
                conn.setAutoCommit(false);

                // Hash all PII in user object
                removePIIFromUserDO(userToDelete);
                // Ensure the last updated time is that of deletion
                userToDelete.setLastUpdated(new Date());
                // save it using this connection with auto commit turned off
                this.updateUser(conn, userToDelete);

                // Replace all linked providers with a uid account provider IDs to prevent clashes if the user creates a new account.
                String deleteLinkedAccountsQuery = "UPDATE linked_accounts SET provider_user_id = ? WHERE user_id = ?";
                try (PreparedStatement deleteLinkedAccounts = conn.prepareStatement(deleteLinkedAccountsQuery)) {
                    deleteLinkedAccounts.setString(1, UUID.randomUUID().toString());
                    deleteLinkedAccounts.setLong(2, userToDelete.getId());
                    deleteLinkedAccounts.execute();
                }

                // Remove password and password reset values. Leave secure_salt and last_updated untouched.
                // This is safe even in cases where user has no local credentials.
                String deleteUserCredentialsQuery = "UPDATE user_credentials SET reset_token=NULL, reset_expiry=NULL,"
                        + "password = ? WHERE user_id = ?";
                try (PreparedStatement deleteUserCredentials = conn.prepareStatement(deleteUserCredentialsQuery)) {
                    // In SegueLocalAuthenticator we use "LOCKED@..." as a prefix for deliberately-invalid password strings.
                    deleteUserCredentials.setString(1, "DELETED@" + UUID.randomUUID());
                    deleteUserCredentials.setLong(2, userToDelete.getId());
                    deleteUserCredentials.execute();
                }

                // Mark the user as deleted.
                String markUserDeletedQuery = "UPDATE users SET deleted=TRUE, last_updated=? WHERE id = ?";
                try (PreparedStatement markUserAsDeleted = conn.prepareStatement(markUserDeletedQuery)) {
                    markUserAsDeleted.setTimestamp(1, new Timestamp(new Date().getTime()));
                    markUserAsDeleted.setLong(2, userToDelete.getId());
                    markUserAsDeleted.execute();
                }

                conn.commit();
            } catch (SQLException | JsonProcessingException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e1) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e1);
        } catch (JsonProcessingException e1) {
            throw new SegueDatabaseException(JSONB_PROCESSING_ERROR_MESSAGE, e1);
        }
    }

    @Override
    public void mergeUserAccounts(final RegisteredUser target, final RegisteredUser source) throws SegueDatabaseException {
        if (null == target) {
            throw new SegueDatabaseException("Merge users target is null");
        } else if (null == source) {
            throw new SegueDatabaseException("Merge users source is null");
        }

        try (Connection conn = database.getDatabaseConnection()) {
            try {
                conn.setAutoCommit(false);

                try (PreparedStatement mergeUsers = conn.prepareStatement("SELECT mergeuser(?, ?)")) {
                    mergeUsers.setLong(1, target.getId());
                    mergeUsers.setLong(2, source.getId());
                    mergeUsers.execute();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e1) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e1);
        }
    }

    @Override
    public void updateUserLastSeen(final RegisteredUser user) throws SegueDatabaseException {
        this.updateUserLastSeen(user, new Date());
    }

    @Override
    public void updateUserLastSeen(final RegisteredUser user, final Date date) throws SegueDatabaseException {
        Objects.requireNonNull(user);

        String query = "UPDATE users SET last_seen = ? WHERE id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setTimestamp(1, new java.sql.Timestamp(date.getTime()));
            pst.setLong(2, user.getId());
            pst.execute();
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        }
    }

    @Override
    public void incrementSessionToken(RegisteredUser user) throws SegueDatabaseException {
        Objects.requireNonNull(user);

        String query = "UPDATE users SET session_token = session_token + 1 WHERE id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, user.getId());
            pst.execute();
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        }
    }

    /**
     * createUser.
     * @param userToCreate - a user object to persist
     * @return a register user as just created.
     * @throws SegueDatabaseException - If there is a db error
     */
    private RegisteredUser createUser(final RegisteredUser userToCreate) throws SegueDatabaseException {    
        // make sure student is default role if none set
        if (null == userToCreate.getRole()) {
            userToCreate.setRole(Role.STUDENT);
        }
        
        // make sure NOT_VERIFIED is default email verification status if none set
        if (null == userToCreate.getEmailVerificationStatus()) {
            userToCreate.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
        }

        String query = "INSERT INTO users(family_name, given_name, email, role, date_of_birth, gender,"
                + " registration_date, school_id, school_other, last_updated, email_verification_status, last_seen,"
                + " email_verification_token, email_to_verify, registered_contexts, registered_contexts_last_confirmed,"
                + " country_code, teacher_account_pending)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                + " ON CONFLICT DO NOTHING;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ) {
            List<String> userContextsJsonb = Lists.newArrayList();
            if (userToCreate.getRegisteredContexts() != null) {
                for (UserContext registeredContext : userToCreate.getRegisteredContexts()) {
                    userContextsJsonb.add(jsonMapper.writeValueAsString(registeredContext));
                }
            }
            Array userContexts = conn.createArrayOf("jsonb", userContextsJsonb.toArray());

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
            setValueHelper(pst, 11, userToCreate.getEmailVerificationStatus());
            setValueHelper(pst, 12, userToCreate.getLastSeen());
            setValueHelper(pst, 13, userToCreate.getEmailVerificationToken());
            setValueHelper(pst, 14, userToCreate.getEmailToVerify());
            pst.setArray(15, userContexts);
            setValueHelper(pst, 16, userToCreate.getRegisteredContextsLastConfirmed());
            setValueHelper(pst, 17, userToCreate.getCountryCode());
            setValueHelper(pst, 18, userToCreate.getTeacherAccountPending());

            if (pst.executeUpdate() == 0) {
                throw new DuplicateAccountException();
            }

            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    userToCreate.setId(id);
                    return userToCreate;
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            } finally {
                userContexts.free();
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException(JSONB_PROCESSING_ERROR_MESSAGE, e);
        }
    }

    /**
     * Update a user in the database.
     *
     * @param userToCreate - user object to save.
     * @return the user as from the database
     * @throws SegueDatabaseException - if there is a database problem
     */
    private RegisteredUser updateUser(final RegisteredUser userToCreate) throws SegueDatabaseException {
        RegisteredUser existingUserRecord = this.getById(userToCreate.getId());
        if (null == existingUserRecord) {
            throw new SegueDatabaseException("The user you have tried to update does not exist.");
        }

        try (Connection conn = database.getDatabaseConnection()) {
            return this.updateUser(conn, userToCreate);
        } catch (SQLException e) {
            throw new SegueDatabaseException(POSTGRES_EXCEPTION_MESSAGE, e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException(JSONB_PROCESSING_ERROR_MESSAGE, e);
        }
    }

    /**
     * Helper method that enables a connection configured for transactions to be passed in.
     *
     * @param conn  - A pre-created sql connection object - ideal if you want to pre configure auto commit to be turned off.
     * @param userToCreate - user object to save.
     * @return the user as from the database
     * @throws SQLException - if there is a database problem
     */
    private RegisteredUser updateUser(Connection conn, final RegisteredUser userToCreate) throws SegueDatabaseException, SQLException, JsonProcessingException {
        RegisteredUser existingUserRecord = this.getById(userToCreate.getId());
        if (null == existingUserRecord) {
            throw new SegueDatabaseException("The user you have tried to update does not exist.");
        }

        String query = "UPDATE users SET family_name = ?, given_name = ?, email = ?, role = ?, date_of_birth = ?,"
                + " gender = ?, registration_date = ?, school_id = ?, school_other = ?, last_updated = ?,"
                + " email_verification_status = ?, last_seen = ?, email_verification_token = ?, email_to_verify = ?,"
                + " registered_contexts = ?, registered_contexts_last_confirmed = ?, country_code = ?, teacher_account_pending = ? WHERE id = ?;";

        List<String> userContextsJsonb = Lists.newArrayList();
        if (userToCreate.getRegisteredContexts() != null) {
            for (UserContext registeredContext : userToCreate.getRegisteredContexts()) {
                userContextsJsonb.add(jsonMapper.writeValueAsString(registeredContext));
            }
        }
        Array userContexts = conn.createArrayOf("jsonb", userContextsJsonb.toArray());

        try (PreparedStatement pst = conn.prepareStatement(query)) {
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
            setValueHelper(pst, 11, userToCreate.getEmailVerificationStatus());
            setValueHelper(pst, 12, userToCreate.getLastSeen());
            setValueHelper(pst, 13, userToCreate.getEmailVerificationToken());
            setValueHelper(pst, 14, userToCreate.getEmailToVerify());
            pst.setArray(15, userContexts);
            setValueHelper(pst, 16, userToCreate.getRegisteredContextsLastConfirmed());
            setValueHelper(pst, 17, userToCreate.getCountryCode());
            setValueHelper(pst, 18, userToCreate.getTeacherAccountPending());
            setValueHelper(pst, 19, userToCreate.getId());


            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save user.");
            }

            return this.getById(existingUserRecord.getId());
        } finally {
            userContexts.free();
        }
    }
    
    /**
     * Build a {@link RegisteredUser} from a Postgres {@link ResultSet}.
     * 
     * @param results The results to convert
     * @return A RegisteredUser reflecting the results.
     * @throws SQLException If an error occurs.
     */
    private RegisteredUser buildRegisteredUser(final ResultSet results) throws SQLException, JsonProcessingException {
        if (null == results) {
            return null;
        }
        
        RegisteredUser u = new RegisteredUser();
        u.setId(results.getLong("id"));
        u.setFamilyName(results.getString("family_name"));
        u.setGivenName(results.getString("given_name"));
        u.setEmail(results.getString("email"));
        u.setRole(results.getString("role") != null ? Role.valueOf(results.getString("role")) : null);
        java.sql.Date dateOfBirth = results.getDate("date_of_birth");
        // So, DOB is a date in the database.
        // Because Java is Java, and this code goes back to 1996, the date is interpreted as a datetime at midnight
        // in the local timezone. This is obviously insane, but there you go.
        // It works on the servers because they are sensibly set to a UTC timezone. However, it is broken on local
        // machines where the timezone is London time.
        // The front-end expects a timestamp in UTC, so this line uses four deprecated functions to make one. The joy!
        if (null != dateOfBirth) {
            Date utcDate = new Date(
                    Date.UTC(dateOfBirth.getYear(), dateOfBirth.getMonth(), dateOfBirth.getDate(), 0, 0, 0));
            u.setDateOfBirth(utcDate);
        }
        u.setGender(results.getString("gender") != null ? Gender.valueOf(results.getString("gender")) : null);
        u.setRegistrationDate(results.getTimestamp("registration_date"));
        
        u.setSchoolId(results.getString("school_id"));
        if (results.wasNull()) {
            u.setSchoolId(null);
        }
        
        u.setSchoolOther(results.getString("school_other"));
        Array registeredContextsArray = results.getArray("registered_contexts");
        if (registeredContextsArray != null) {
            List<UserContext> userContexts = Lists.newArrayList();
            for (String registeredContextJson : (String[]) registeredContextsArray.getArray()) {
                userContexts.add(jsonMapper.readValue(registeredContextJson, UserContext.class));
            }
            u.setRegisteredContexts(userContexts);
        }
        u.setRegisteredContextsLastConfirmed(results.getTimestamp("registered_contexts_last_confirmed"));
        u.setLastUpdated(results.getTimestamp("last_updated"));
        u.setLastSeen(results.getTimestamp("last_seen"));
        u.setEmailToVerify(results.getString("email_to_verify"));
        u.setEmailVerificationToken(results.getString("email_verification_token"));
        u.setEmailVerificationStatus(results.getString("email_verification_status") != null ? EmailVerificationStatus
                .valueOf(results.getString("email_verification_status")) : null);
        u.setSessionToken(results.getInt("session_token"));

        u.setCountryCode(results.getString("country_code"));
        u.setTeacherAccountPending(results.getBoolean("teacher_account_pending"));

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
    private RegisteredUser findOneUser(final ResultSet results) throws SQLException, SegueDatabaseException, JsonProcessingException {
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
     */
    private List<RegisteredUser> findAllUsers(final ResultSet results) throws SQLException, JsonProcessingException {
        List<RegisteredUser> listOfResults = Lists.newArrayList();
        while (results.next()) {
            listOfResults.add(buildRegisteredUser(results));
        }

        return listOfResults;
    }

    /**
     * Helper function to remove PII and set tombstone flag for a Registered User.
     * Note: This function mutates the object that it was provided.
     *
     * This method performs the same action as the script
     *     src/main/resources/db_scripts/scheduled/archive-users-transaction.sql
     * and changes here should be reflected there.
     */
    private static void removePIIFromUserDO(RegisteredUser user) {
        user.setFamilyName(null);
        user.setGivenName(null);
        user.setEmail(UUID.randomUUID().toString());
        user.setEmailVerificationToken(null);
        user.setEmailToVerify(null);
        user.setSchoolOther(null); // Risk this contains something identifying!

        if (user.getDateOfBirth() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(user.getDateOfBirth());
            calendar.set(Calendar.DATE, 1);
            user.setDateOfBirth(calendar.getTime());
        }

    }
}