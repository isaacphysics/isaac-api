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
package uk.ac.cam.cl.dtg.isaac.dos;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.Validate;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.util.locations.Location;
import uk.ac.cam.cl.dtg.util.locations.PostCode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;

/**
 * @author sac92
 *
 */
public class PgLocationHistory implements LocationHistory {
    private final PostgresSqlDb database;
    private final ObjectMapper locationSerializer;

    private static final Logger log = LoggerFactory.getLogger(PgLocationHistory.class);
    
    /**
     * PgLocationHistory.
     * 
     * @param database
     *            - Preconfigured PostGres instance.
     */
    @Inject
    public PgLocationHistory(final PostgresSqlDb database) {
        this.database = database;
        locationSerializer = new ObjectMapper();
        locationSerializer.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.isaac.dos.LocationHistory#getLatestByIPAddress(java. lang.String)
     */
    @Override
    public LocationHistoryEvent getLatestByIPAddress(final String ipAddress) throws SegueDatabaseException {
        Validate.notBlank(ipAddress);

        String query = "SELECT * FROM ip_location_history WHERE ip_address = ? AND is_current = ? ORDER BY last_lookup DESC";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, ipAddress);
            pst.setBoolean(2, true);

            try (ResultSet results = pst.executeQuery()) {

                while (results.next()) {
                    return buildPgLocationEntry(results);
                }

                // we must not have found anything.
                return null;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public PostCode getPostCode(final String postCode) throws SegueDatabaseException {
        if (null == postCode || postCode.isEmpty()) {
            return null;
        }

        String query = "SELECT postcode, lat, lon FROM uk_post_codes WHERE postcode = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, postCode);

            try (ResultSet results = pst.executeQuery()) {

                while (results.next()) {
                    return new PostCode(results.getString("postcode"), results.getDouble("lat"),
                            results.getDouble("lon"));
                }

                // we must not have found anything.
                return null;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public Map<String, LocationHistoryEvent> getLatestByIPAddresses(final Collection<String> ipAddresses)
            throws SegueDatabaseException {

        // This is a nasty hack to make a prepared statement using the sql IN operator.
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ipAddresses.size(); i++) {
            builder.append("?,");
        }
        String query = "SELECT * FROM ip_location_history WHERE ip_address IN (" +
                builder.deleteCharAt(builder.length() - 1).toString() + ") AND is_current = ? ORDER BY last_lookup DESC";

        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            int index = 1;
            for (String s : ipAddresses) {
                pst.setString(index++, s);
            }

            pst.setBoolean(index++, true);

            try (ResultSet results = pst.executeQuery()) {
                Map<String, LocationHistoryEvent> resultToReturn = Maps.newHashMap();

                while (results.next()) {
                    PgLocationEvent buildPgLocationEntry = buildPgLocationEntry(results);
                    resultToReturn.put(buildPgLocationEntry.getIpAddress(), buildPgLocationEntry);
                }
                return resultToReturn;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public List<LocationHistoryEvent> getAllByIPAddress(final String ipAddress) throws SegueDatabaseException {
        Validate.notBlank(ipAddress);

        String query = "SELECT * FROM ip_location_history WHERE ip_address = ? ORDER BY created ASC";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, ipAddress);

            try (ResultSet results = pst.executeQuery()) {
                List<LocationHistoryEvent> returnResult = Lists.newArrayList();
                while (results.next()) {
                    returnResult.add(buildPgLocationEntry(results));
                }
                return returnResult;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.isaac.dos.LocationHistory#storeLocationEvent(uk.ac.cam.cl.dtg.isaac.dos.LocationEvent)
     */
    @Override
    public LocationHistoryEvent storeLocationEvent(final String ipAddress, final Location location)
            throws JsonProcessingException, SegueDatabaseException {
        return this.createNewEvent(ipAddress, location);
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.isaac.dos.LocationHistory#storeLocationEvent(uk.ac.cam.cl.dtg.isaac.dos.LocationEvent)
     */
    @Override
    public void updateLocationEventDate(final Long id, final boolean isCurrent) throws SegueDatabaseException {
        Validate.notNull(id);

        String query = "UPDATE ip_location_history SET last_lookup = ?, is_current=? WHERE id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement ps = conn.prepareStatement(query);
        ) {
            ps.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
            ps.setBoolean(2, isCurrent);
            ps.setLong(3, id);

            ps.executeUpdate();

        } catch (SQLException se) {
            throw new SegueDatabaseException("Postgres exception", se);
        }

    }

    /**
     * Creates a brand new event.
     * 
     * @param ipAddress
     *            of interest
     * @param location
     *            geocoded
     * @return a copy of the event.
     * @throws SegueDatabaseException
     *             - if there is a db error.
     * @throws JsonProcessingException
     *             - if we can't parse / serialize the json
     */
    private LocationHistoryEvent createNewEvent(final String ipAddress, final Location location)
            throws SegueDatabaseException, JsonProcessingException {
        String query = "INSERT INTO ip_location_history(id, ip_address, location_information, created, last_lookup, is_current) "
                + "VALUES (DEFAULT, ?, ?, ?, ?, ?)";
        try (Connection conn = database.getDatabaseConnection();
            PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ) {
            Date creationDate = new Date();

            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(locationSerializer.writeValueAsString(location));

            pst.setString(1, ipAddress);
            pst.setObject(2, jsonObject);
            pst.setTimestamp(3, new java.sql.Timestamp(creationDate.getTime()));
            pst.setTimestamp(4, new java.sql.Timestamp(creationDate.getTime()));
            pst.setBoolean(5, true);

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save location event.");
            }

            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    return new PgLocationEvent(id, ipAddress, location, creationDate, creationDate);
                } else {
                    throw new SQLException("Creating location event failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * Create a PgLocationEvent from a results set.
     * 
     * Assumes there is a result to read.
     * 
     * @param results
     *            - the results to convert
     * @return a new PgEventBooking
     * @throws SQLException
     *             - if an error occurs.
     */
    private PgLocationEvent buildPgLocationEntry(final ResultSet results) throws SQLException {
        Location location;
        try {
            location = locationSerializer.readValue(results.getString("location_information"), Location.class);
        } catch (IOException e) {
            log.error("IOException while trying to convert location entry.", e);
            return null;
        }

        return new PgLocationEvent(results.getLong("id"), results.getString("ip_address"), location,
                results.getTimestamp("created"), results.getTimestamp("last_lookup"));
    }

    @Override
    public Map<String, LocationHistoryEvent> getLatestByIPAddresses(final Date fromDate, final Date toDate)
            throws SegueDatabaseException {
        String query = "SELECT * FROM ip_location_history WHERE last_lookup BETWEEN ? AND ?" +
                " AND is_current = TRUE ORDER BY last_lookup DESC";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setDate(1, new java.sql.Date(fromDate.getTime()));
            pst.setDate(2, new java.sql.Date(toDate.getTime()));

            try (ResultSet results = pst.executeQuery()) {
                Map<String, LocationHistoryEvent> resultToReturn = Maps.newHashMap();

                while (results.next()) {
                    PgLocationEvent buildPgLocationEntry = buildPgLocationEntry(results);
                    resultToReturn.put(buildPgLocationEntry.getIpAddress(), buildPgLocationEntry);
                }
                return resultToReturn;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.isaac.dos.LocationHistory#storePostCodes(java.util.List)
     */
    @Override
    public void storePostCodes(List<PostCode> foundPostCodes) throws SegueDatabaseException {
        String query = "INSERT INTO uk_post_codes(postcode, lat, lon) VALUES (?, ?, ?)";
        try (Connection conn = database.getDatabaseConnection()) {
            conn.setAutoCommit(false);

            for (PostCode postCode : foundPostCodes) {

                // Ignore post codes with invalid lat/lon
                if (postCode.getLat() == null || postCode.getLon() == null) {
                    continue;
                }

                try (PreparedStatement pst = conn.prepareStatement(query)) {
                    pst.setString(1, postCode.getPostCode());
                    pst.setDouble(2, postCode.getLat());
                    pst.setDouble(3, postCode.getLon());

                    if (pst.executeUpdate() == 0) {
                        throw new SegueDatabaseException("Unable to save location event.");
                    }
                }
            }
            conn.commit();
            conn.setAutoCommit(true);

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
}
