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
package uk.ac.cam.cl.dtg.isaac.dos;

import com.fasterxml.jackson.core.JsonProcessingException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.locations.Location;
import uk.ac.cam.cl.dtg.util.locations.PostCode;

import java.util.List;

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
     * @param postCode
     *            - a given postcode
     * @return - a postcode object
     * @throws SegueDatabaseException
     *             - if something goes wrong with the database.
     */
    PostCode getPostCode(final String postCode) throws SegueDatabaseException;

    /**
     * @param postCodes
     *            - a list of given postcodes
     * @throws SegueDatabaseException
     *             - if something goes wrong with the database.
     */
    void storePostCodes(List<PostCode> postCodes) throws SegueDatabaseException;
}