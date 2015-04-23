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
package uk.ac.cam.cl.dtg.util.locations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

/**
 * @author sac92
 *
 */
public class IPInfoDBLocationResolver implements ILocationResolver {
	// Constants as expected from the ipInfoDBResponse
	private static final String COUNTRY_CODE = "countryCode";
	private static final String COUNTRY_NAME = "countryName";
	private static final String REGION_NAME = "regionName";
	private static final String CITY_NAME = "cityName";
	private static final String ZIP_CODE = "zipCode";
	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";
	private static final String STATUS_CODE = "statusCode";
	private static final String ERROR_STATUS_CODE = "ERROR";

	private static final String STATUS_MESSAGE = "statusMessage";
	
	private final String urlBase = "http://api.ipinfodb.com/v3/ip-city/";
	private final String apiAuthKey;

	/**
	 * IPInfoDBLocationResolver.
	 * @param apiAuthKey
	 */
	@Inject
	public IPInfoDBLocationResolver(final String apiAuthKey) {
		this.apiAuthKey = apiAuthKey;
		
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.cl.dtg.util.locations.ILocationResolver#getLocationInformation(java.lang.String)
	 */
	@Override
	public Location getLocationInformation(final String ipAddress) throws IOException {
		StringBuilder sb = new StringBuilder(urlBase);
		sb.append(String.format("?key=%s&ip=%s&format=json", apiAuthKey, ipAddress));
        
		URL ipInfoDBServiceURL = new URL(sb.toString());
        URLConnection ipInfoDBService = ipInfoDBServiceURL.openConnection();
        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                ipInfoDBService.getInputStream()));
        String inputLine;
        StringBuilder jsonResponseBuilder = new StringBuilder();
        
        
        while ((inputLine = in.readLine()) != null) {
        	jsonResponseBuilder.append(inputLine);
        }
            
        in.close();
        
        String jsonResponse = jsonResponseBuilder.toString();
        
		return this.convertJsonToLocation(jsonResponse);
	}
	
	/**
	 * 
	 * @param json response form IPInfoDB
	 * @return A location object with as much detail as we can gather
	 * @throws JsonParseException 
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	private Location convertJsonToLocation(final String json) throws JsonParseException,
			JsonMappingException, IOException, NumberFormatException {
		ObjectMapper objectMapper = new ObjectMapper();
		
		@SuppressWarnings("unchecked")
		HashMap<String, String> response = objectMapper.readValue(json, HashMap.class);
		
		if (response == null || response.isEmpty()) {
			throw new IOException("The response from the IPInfoDBLocationResolver was null");
		}
		
		if (response.get(STATUS_CODE) != null && response.get(STATUS_CODE).equals(ERROR_STATUS_CODE)) {
			throw new IOException(
					String.format(
							"Unable to complete ip address to location lookup, "
							+ "server responded with the following message: %s",
							response.get(STATUS_MESSAGE)));
		}
		
		Address partialAddress = new Address(null, null, response.get(CITY_NAME), null, response.get(ZIP_CODE),
				response.get(COUNTRY_NAME));
		String latString = response.get(LATITUDE);
		String lonString = response.get(LONGITUDE);
		
		Double lat;
		Double lon;
		if (null == latString || null == lonString) {
			lat = null;
			lon = null;
		} else {
			lat = Double.parseDouble(latString);
			lon = Double.parseDouble(lonString);
		}
			
		Location result = new Location(partialAddress, lat, lon);
		
		return result;
	}
}
