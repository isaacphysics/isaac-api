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
package uk.ac.cam.cl.dtg.segue.dos;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.locations.Location;

/**
 *
 */
public interface LocationHistory {
    /**
     * Get the latest location by an ip address.
     * 
     * @param ipAddress
     *            of interest
     * @return the location event record
     * @throws SegueDatabaseException
     *             - if there is a db error.
     */
    LocationHistoryEvent getLatestByIPAddress(final String ipAddress) throws SegueDatabaseException;

    /**
     * Get all locations by an ip address.
     * 
     * @param ipAddress
     *            of interest
     * @return the location event records
     * @throws SegueDatabaseException
     *             - if there is a db error.
     */
    List<LocationHistoryEvent> getAllByIPAddress(final String ipAddress) throws SegueDatabaseException;

    /**
     * Store location information about an ip address.
     * 
     * You should check to see if an existing record exists and run updateLocationEventDate on it before creating a new
     * one.
     * 
     * @param ipAddress
     *            of interest
     * @param location
     *            (geo-coded)
     * @return the newly created locationEvent Record.
     * @throws JsonProcessingException
     *             if we cannot convert the location into something useful.
     * @throws SegueDatabaseException
     *             - if there is a db error.
     */
    LocationHistoryEvent storeLocationEvent(final String ipAddress, final Location location)
            throws JsonProcessingException, SegueDatabaseException;

    /**
     * Update a location record's last updated date and isCurrent flag.
     * 
     * @param id
     *            the database id for the record of interest
     * @param isCurrent
     *            - whether or not this record should be treated as the current one.
     * @throws SegueDatabaseException
     *             - if there is a db error.
     */
    void updateLocationEventDate(final Long id, boolean isCurrent) throws SegueDatabaseException;

    /**
     * Utility method to allow requesting IP addresses in one single db request.
     * 
     * @param ipAddress
     *            - list of ip addresses of interest. - if null then all ips will be returned.
     * @return Map of Ip address to location history event.
     * @throws SegueDatabaseException
     *             - if we cannot talk to the db.
     */
    Map<String, LocationHistoryEvent> getLatestByIPAddresses(Collection<String> ipAddress)
            throws SegueDatabaseException;
}