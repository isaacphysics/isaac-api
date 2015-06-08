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

import org.elasticsearch.common.lang3.Validate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

/**
 * Concrete implemention of a geocoder using the third party IPInfoDB service.
 * 
 * http://www.ipinfodb.com/
 * 
 */
public class IPInfoDBLocationResolver implements ILocationResolver {
    /**
     * Enum mapping IPInfoDB properties to something we can use.
     */
    private enum IPInfoDBLocationResponseProperties {
        COUNTRY_CODE("countryCode"), COUNTRY_NAME("countryName"), CITY_NAME("cityName"), ZIP_CODE("zipCode"), LATITUDE(
                "latitude"), LONGITUDE("longitude"), 
                STATUS_CODE("statusCode"), ERROR_STATUS_CODE("ERROR"), STATUS_MESSAGE(
                "statusMessage");

        private String valueAsString;

        /**
         * @param value
         *            of the IPInfoDB response
         */
        private IPInfoDBLocationResponseProperties(final String value) {
            valueAsString = value;
        }

        @Override
        public String toString() {
            return valueAsString;
        }
    }

    private final String urlBase = "http://api.ipinfodb.com/v3/";
    private final String urlFull = "ip-city/";
    private final String urlMinimal = "ip-country/";
    private final String apiAuthKey;

    /**
     * IPInfoDBLocationResolver.
     * 
     * @param apiAuthKey
     *            - required for use of ipinfodb.
     */
    @Inject
    public IPInfoDBLocationResolver(final String apiAuthKey) {
        Validate.notBlank(apiAuthKey, "The API key must not be blank or null");
        this.apiAuthKey = apiAuthKey;
    }

    @Override
    public Location resolveAllLocationInformation(final String ipAddress) throws IOException, LocationServerException {
        StringBuilder sb = new StringBuilder(urlBase);
        sb.append(String.format("%s?key=%s&ip=%s&format=json", urlFull, apiAuthKey, ipAddress));

        URL ipInfoDBServiceURL = new URL(sb.toString());

        return this.convertJsonToLocation(resolveFromServer(ipInfoDBServiceURL));
    }

    @Override
    public Location resolveCountryOnly(final String ipAddress) throws IOException, LocationServerException {
        StringBuilder sb = new StringBuilder(urlBase);
        sb.append(String.format("%s?key=%s&ip=%s&format=json", urlMinimal, apiAuthKey, ipAddress));

        URL ipInfoDBServiceURL = new URL(sb.toString());

        return this.convertJsonToLocation(resolveFromServer(ipInfoDBServiceURL));
    }

    /**
     * Resolve location from third party service.
     * 
     * @param url
     *            - fully qualified api request url.
     * @return the json response as a string.
     * @throws IOException
     *             - if there is an error contacting the server.
     */
    private String resolveFromServer(final URL url) throws IOException {
        URLConnection ipInfoDBService = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(ipInfoDBService.getInputStream()));
        String inputLine;
        StringBuilder jsonResponseBuilder = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            jsonResponseBuilder.append(inputLine);
        }

        in.close();

        return jsonResponseBuilder.toString();
    }

    /**
     * 
     * @param json
     *            response form IPInfoDB
     * @return A location object with as much detail as we can gather
     * @throws IOException
     *             if we cannot read the response from the server.
     * @throws LocationServerException
     *             - if the server returns a problem.
     */
    private Location convertJsonToLocation(final String json) throws IOException, LocationServerException {
        ObjectMapper objectMapper = new ObjectMapper();

        @SuppressWarnings("unchecked")
        HashMap<String, String> response = objectMapper.readValue(json, HashMap.class);

        if (response == null || response.isEmpty()) {
            throw new IOException("The response from the IPInfoDBLocationResolver was null");
        }

        if (response.get(IPInfoDBLocationResponseProperties.STATUS_CODE + "") != null
                && response.get(IPInfoDBLocationResponseProperties.STATUS_CODE + "").equals(
                        IPInfoDBLocationResponseProperties.ERROR_STATUS_CODE + "")) {
            throw new LocationServerException(String.format("Unable to complete ip address to location lookup, "
                    + "server responded with the following message: %s",
                    response.get(IPInfoDBLocationResponseProperties.STATUS_MESSAGE + "")));
        }

        Address partialAddress = new Address(null, null,
                response.get(IPInfoDBLocationResponseProperties.CITY_NAME + ""), null,
                response.get(IPInfoDBLocationResponseProperties.ZIP_CODE + ""),
                response.get(IPInfoDBLocationResponseProperties.COUNTRY_NAME + ""));
        String latString = response.get(IPInfoDBLocationResponseProperties.LATITUDE + "");
        String lonString = response.get(IPInfoDBLocationResponseProperties.LONGITUDE + "");

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
