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
package uk.ac.cam.cl.dtg.segue.dao;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.ILocationHistory;
import uk.ac.cam.cl.dtg.isaac.dos.LocationHistoryEvent;
import uk.ac.cam.cl.dtg.util.locations.IPLocationResolver;
import uk.ac.cam.cl.dtg.util.locations.Location;
import uk.ac.cam.cl.dtg.util.locations.LocationServerException;
import uk.ac.cam.cl.dtg.util.locations.PostCodeLocationResolver;
import uk.ac.cam.cl.dtg.util.locations.PostCodeRadius;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This manager governs access to IP and other location lookup services.
 *
 */
public class LocationManager {
    private static final Logger log = LoggerFactory.getLogger(LocationManager.class);
    private static final int LOCATION_UPDATE_FREQUENCY_IN_DAYS = 30;
    private static final int NON_PERSISTENT_CACHE_TIME_IN_HOURS = 1;

    private final ILocationHistory dao;
    private final IPLocationResolver ipLocationResolver;
    private final PostCodeLocationResolver postCodeLocationResolver;
    private final Cache<String, Boolean> locationUpdatedRecentlyCache;

    /**
     * @param dao
     *            - the location history data access object.
     * @param ipLocationResolver
     *            - the external ip location resolver.
     * @param postCodeLocationResolver
     *            - the external postCode location resolver.
     */
    @Inject
    public LocationManager(final ILocationHistory dao, final IPLocationResolver ipLocationResolver,
                           final PostCodeLocationResolver postCodeLocationResolver) {
        this.dao = dao;
        this.ipLocationResolver = ipLocationResolver;
        this.postCodeLocationResolver = postCodeLocationResolver;

        // This cache is here to prevent lots of needless look-ups to the database.
        locationUpdatedRecentlyCache = CacheBuilder.newBuilder().expireAfterWrite(NON_PERSISTENT_CACHE_TIME_IN_HOURS, TimeUnit.HOURS).build();
    }

    /**
     * This method will keep our database of ip addresses to locations updated.
     * 
     * @param ipAddress
     *            that should be looked up
     * @throws SegueDatabaseException
     *             - if we cannot resolve the location from our database
     * @throws IOException
     *             - if there is an IO error
     */
    public void refreshLocation(final String ipAddress) throws SegueDatabaseException, IOException {
        // if the IP is missing or present in our cache, no need to look up again
        if (ipAddress == null || ipAddress.isEmpty() || locationUpdatedRecentlyCache.getIfPresent(ipAddress) != null) {
            return;
        }

        // do not attempt to record localhost IP addresses:
        if (ipAddress.equals("0:0:0:0:0:0:0:1") || ipAddress.equals("127.0.0.1")) {
            log.debug("Not geocoding ip address as it looks like localhost: {}", ipAddress);
            this.locationUpdatedRecentlyCache.put(ipAddress, false);
            return;
        }

        // if it is a private subnet, lookup will fail, so skip lookup and cache this failure
        if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.")
                || (ipAddress.startsWith("172.") && ipAddress.matches("^172\\.(?:1[6-9]|2[0-9]|3[01])\\..*"))) {
            log.debug("Not geocoding ip address as it looks like a private subnet: {}", ipAddress);
            this.locationUpdatedRecentlyCache.put(ipAddress, false);
            return;
        }

        // do we have an existing location for this ip address?
        LocationHistoryEvent latestByIPAddress = dao.getLatestByIPAddress(ipAddress);
        try {
            if (latestByIPAddress != null) {
                Calendar locationExpiry = Calendar.getInstance();
                locationExpiry.setTime(latestByIPAddress.getLastUpdated());
                locationExpiry.add(Calendar.DATE, LOCATION_UPDATE_FREQUENCY_IN_DAYS);

                if (new Date().after(locationExpiry.getTime())) {
                    // check if ip location data has changed, and if so update it, else mark it still valid
                    Location locationInformation = ipLocationResolver.resolveAllLocationInformation(ipAddress);

                    if (locationInformation.equals(latestByIPAddress.getLocationInformation())) {
                        dao.updateLocationEventDate(latestByIPAddress.getId(), true);
                        log.debug("Location for IP '{}' unchanged. Marking as current.", ipAddress);

                    } else {
                        dao.updateLocationEventDate(latestByIPAddress.getId(), false);
                        dao.storeLocationEvent(ipAddress, locationInformation);
                        log.debug("Location for IP '{}' changed. Updating to new value.", ipAddress);
                    }
                }
            } else {
                Location locationInformation = ipLocationResolver.resolveAllLocationInformation(ipAddress);
                dao.storeLocationEvent(ipAddress, locationInformation);
                log.debug("Recording location for IP '{}'.", ipAddress);
            }

            this.locationUpdatedRecentlyCache.put(ipAddress, true);

        } catch (final LocationServerException e) {
            log.error("Unable to resolve location for IP address: '{}'. {} ", ipAddress, e.getMessage());
            // add to failed cache so we don't repeat failed lookups immediately
            this.locationUpdatedRecentlyCache.put(ipAddress, false);
        }
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
