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
package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.dos.LocationHistoryEvent;
import uk.ac.cam.cl.dtg.segue.dos.LocationHistory;
import uk.ac.cam.cl.dtg.util.locations.ILocationResolver;
import uk.ac.cam.cl.dtg.util.locations.Location;
import uk.ac.cam.cl.dtg.util.locations.LocationServerException;

/**
 * LocationHistoryManager.
 * This class is intended to be used to maintain a database of geocoded ip addresses such 
 * that we can look up historically where a particular ip address was. This is based
 * on the assumption that ip address allocation change over time. 
 * 
 * @author sac92
 *
 */
public class LocationHistoryManager implements ILocationResolver {
	private static final Logger log = LoggerFactory.getLogger(LocationHistoryManager.class);
	private static final int LOCATION_UPDATE_FREQUENCY_IN_DAYS = 30;
	private static final int NON_PERSISTENT_CACHE_TIME_IN_HOURS = 1;
	
	private final LocationHistory dao;
	private final ILocationResolver locationResolver;
	private final Cache<String, Location> locationCache; 
	
	/**
	 * @param dao - the location history data access object.
	 * @param locationResolver - the external location resolver.
	 */
	@Inject
	public LocationHistoryManager(final LocationHistory dao, final ILocationResolver locationResolver) {
		this.dao = dao;
		this.locationResolver = locationResolver;

		// This cache is here to prevent lots of needless look ups to the database. 
		locationCache = CacheBuilder.newBuilder().expireAfterWrite(NON_PERSISTENT_CACHE_TIME_IN_HOURS, TimeUnit.HOURS)
				.<String, Location> build();
	}
	
	/**
	 * This method will keep our database of ip addresses to locations updated.
	 * @param ipAddress that should be looked up
	 * @throws SegueDatabaseException 
	 * @throws IOException 
	 */
	public void refreshLocation(final String ipAddress) throws SegueDatabaseException, IOException {
		// special case
		if (ipAddress == null || ipAddress.startsWith("localhost") || ipAddress.contains("0:0:0:0:0:0:0:1")
				|| ipAddress.contains("127.0.0.1")) {
			// do not record
			log.debug("Not geocoding ip address as it looks like localhost: " + ipAddress);
			return;
		}
		
		// if it is present in the local cache we have no need to hit the database
		if (locationCache.getIfPresent(ipAddress) != null) {
			return;
		}
		
		// do we have an existing location for this ip address.
		LocationHistoryEvent latestByIPAddress = dao.getLatestByIPAddress(ipAddress);
		Location locationToCache;
		try {
			if (latestByIPAddress != null) {
				locationToCache = latestByIPAddress.getLocationInformation();
				Calendar locationExpiry = Calendar.getInstance(); 
				locationExpiry.setTime(latestByIPAddress.getLastUpdated());
				locationExpiry.add(Calendar.DATE, LOCATION_UPDATE_FREQUENCY_IN_DAYS);
				
				if (new Date().after(locationExpiry.getTime())) {
					// lookup to see if ip location data is different. If so update it.
					Location locationInformation = locationResolver.resolveAllLocationInformation(ipAddress);
					
					if (locationInformation.equals(latestByIPAddress.getLocationInformation())) {
						dao.updateLocationEventDate(latestByIPAddress.getId(), true);
						log.debug("Ip address location is the same. Refreshing.");

					} else {
						dao.updateLocationEventDate(latestByIPAddress.getId(), false);
						locationToCache = dao.storeLocationEvent(ipAddress, locationInformation)
								.getLocationInformation();
						log.debug("Location Info Different. Updating to new value.");
					}
				}
			} else {
				Location locationInformation = locationResolver.resolveAllLocationInformation(ipAddress);
				locationToCache = dao.storeLocationEvent(ipAddress, locationInformation).getLocationInformation();
			}
			
			this.locationCache.put(ipAddress, locationToCache);
			
		} catch (LocationServerException e) {
			log.error(String.format("Unable to resolve location for ip address: %s. Skipping...", ipAddress), e);
		}
	}
	
	@Override
	public Location resolveAllLocationInformation(final String ipAddress) throws IOException,
			LocationServerException {
		return locationResolver.resolveAllLocationInformation(ipAddress);
		
	}

	@Override
	public Location resolveCountryOnly(final String ipAddress) throws IOException, LocationServerException {
		return locationResolver.resolveCountryOnly(ipAddress);
	}
	
	/**
	 * Get the latest location information held by our history.
	 * 
	 * @param ipAddress that we are interested in.
	 * @return latest location info for that ip address. or null if we have no data.
	 * @throws SegueDatabaseException - if we cannot resolve the location from our database
	 */
	public Location getLocationFromHistory(final String ipAddress) throws SegueDatabaseException {
		LocationHistoryEvent latestByIPAddress = dao.getLatestByIPAddress(ipAddress);
		if (null == latestByIPAddress) {
			return null;
		}
		
		return latestByIPAddress.getLocationInformation();
	}
}
