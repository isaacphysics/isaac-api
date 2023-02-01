/*
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
package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Maps;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.dos.LocationHistoryEvent;
import uk.ac.cam.cl.dtg.isaac.dos.LocationHistory;
import uk.ac.cam.cl.dtg.util.locations.Location;
import uk.ac.cam.cl.dtg.util.locations.LocationServerException;
import uk.ac.cam.cl.dtg.util.locations.PostCodeLocationResolver;
import uk.ac.cam.cl.dtg.util.locations.PostCodeRadius;

/**
 * LocationHistoryManager. This class is intended to be used to maintain a database of geocoded ip addresses such that
 * we can look up historically where a particular ip address was. This is based on the assumption that ip address
 * allocation change over time.
 *
 * @author sac92, ags46
 *
 */
public class LocationManager {
    private static final Logger log = LoggerFactory.getLogger(LocationManager.class);
    private static final int LOCATION_UPDATE_FREQUENCY_IN_DAYS = 30;
    private static final int NON_PERSISTENT_CACHE_TIME_IN_HOURS = 1;

    private final LocationHistory dao;
    private final PostCodeLocationResolver postCodeLocationResolver;
    private final Cache<String, Location> locationCache;
    private final Cache<String, Location> failedLocationCache;

    /**
     * @param dao
     *            - the location history data access object.
     * @param postCodeLocationResolver
     *            - the external postCode location resolver.
     */
    @Inject
    public LocationManager(final LocationHistory dao,
            final PostCodeLocationResolver postCodeLocationResolver) {
        this.dao = dao;
        this.postCodeLocationResolver = postCodeLocationResolver;

        // This cache is here to prevent lots of needless look ups to the database.
        locationCache = CacheBuilder.newBuilder().expireAfterWrite(NON_PERSISTENT_CACHE_TIME_IN_HOURS, TimeUnit.HOURS)
                .<String, Location> build();
        failedLocationCache = CacheBuilder.newBuilder().expireAfterWrite(NON_PERSISTENT_CACHE_TIME_IN_HOURS, TimeUnit.HOURS)
                .<String, Location> build();
    }

    /**
     * Get the latest location information held by our history.
     *
     * @param ipAddress
     *            that we are interested in.
     * @return latest location info for that ip address. or null if we have no data.
     * @throws SegueDatabaseException
     *             - if we cannot resolve the location from our database
     */
    public Location getLocationFromHistory(final String ipAddress) throws SegueDatabaseException {
        Validate.notBlank(ipAddress, "You must provide an ipAddress.");

        Location cachedLocation = locationCache.getIfPresent(ipAddress);
        if (cachedLocation != null) {
            return cachedLocation;
        }

        LocationHistoryEvent latestByIPAddress = dao.getLatestByIPAddress(ipAddress);
        if (null == latestByIPAddress) {
            return null;
        }

        Location locationInformation = latestByIPAddress.getLocationInformation();
        locationCache.put(ipAddress, locationInformation);

        return locationInformation;
    }

    /**
     * Get the latest location information held by our history.
     *
     * @param ipAddresses
     *            that we are interested in.
     * @return latest location info for that ip address. or null if we have no data.
     * @throws SegueDatabaseException
     *             - if we cannot resolve the location from our database
     */
    public Map<String, Location> getLocationsFromHistory(final Collection<String> ipAddresses)
            throws SegueDatabaseException {

        Map<String, LocationHistoryEvent> latestByIPAddresses = dao.getLatestByIPAddresses(ipAddresses);

        return this.convertToIPLocationMap(latestByIPAddresses);
    }

    /**
     * @param fromDate - lower bound for inclusion in the results.
     * @param toDate - upper bound for inclusion in the results.
     * @return get the last locations and ip addresses by date range.
     * @throws SegueDatabaseException if the database fails to retreive required information
     */
    public Map<String, Location> getLocationsByLastAccessDate(final Date fromDate, final Date toDate)
            throws SegueDatabaseException {
        return this.convertToIPLocationMap(dao.getLatestByIPAddresses(fromDate, toDate));
    }

    /**
     * @param toConvert the map to convert
     * @return map of ip address to location
     */
    private Map<String, Location> convertToIPLocationMap(final Map<String, LocationHistoryEvent> toConvert) {
        Map<String, Location> resultToReturn = Maps.newHashMap();
        for (Entry<String, LocationHistoryEvent> e : toConvert.entrySet()) {
            resultToReturn.put(e.getKey(), e.getValue().getLocationInformation());
            locationCache.put(e.getKey(), e.getValue().getLocationInformation());
        }

        return resultToReturn;
    }

    /**
     * @param postCodeAndUserIds
     *            - A map of postcodes to userids
     * @param targetPostCode
     *            - The post code we want to find users near to
     * @param radius
     *            - radius to search
     * @return - a list of userids who have schools in that radius
     * @throws LocationServerException
     *             - anm exception when the location service fails
     * @throws SegueDatabaseException
     *             - anm exception when the database service fails
     */
    public List<Long> getUsersWithinPostCodeDistanceOf(final Map<String, List<Long>> postCodeAndUserIds,
            final String targetPostCode, final PostCodeRadius radius) throws LocationServerException,
            SegueDatabaseException {
        return postCodeLocationResolver.filterPostcodesWithinProximityOfPostcode(postCodeAndUserIds,
                targetPostCode, radius);
    }

}
