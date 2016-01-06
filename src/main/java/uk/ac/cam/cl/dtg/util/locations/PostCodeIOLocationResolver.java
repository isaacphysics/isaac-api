/**
 * Copyright 2016 Alistair Stead
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.common.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class to allow postcode-related searches using external service.
 *
 * @author Alistair Stead
 *
 */
public class PostCodeIOLocationResolver implements PostCodeLocationResolver {
    private static final Logger log = LoggerFactory.getLogger(PostCodeIOLocationResolver.class);

    private final String url = "http://api.postcodes.io/postcodes";

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.util.locations.PostCodeLocationResolver#filterPostcodesWithinProximityOfPostcode(
     * java.util.HashMap, java.lang.String, int)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Long> filterPostcodesWithinProximityOfPostcode(
            final HashMap<String, ArrayList<Long>> postCodeAndUserIds, final String targetPostCode,
            final int distanceInMiles)
            throws LocationServerException {

        if (null == postCodeAndUserIds) {
            throw new LocationServerException("Map of postcodes cannot be null");
        }

        LinkedList<Long> resultingUserIds = new LinkedList<Long>();

        StringBuilder sb = new StringBuilder();
        sb.append("{ \"postcodes\" : [");
        for (String key : postCodeAndUserIds.keySet()) {
            sb.append("\"");
            sb.append(key);
            sb.append("\"");
            sb.append(", ");
        }

        // add the target postcode, so we can do it in one request
        sb.append("\"");
        sb.append(targetPostCode);
        sb.append("\"] }");

        String requestJson = sb.toString();

        HashMap<String, Object> response = new HashMap<String, Object>();

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);


        StringEntity requestEntity;
        try {
            requestEntity = new StringEntity(requestJson);
            httppost.addHeader("Content-Type", "application/json");
            httppost.setEntity(requestEntity);
            HttpResponse httpresponse = httpclient.execute(httppost);
            HttpEntity entity = httpresponse.getEntity();
            String jsonResponse = EntityUtils.toString(entity);
            ObjectMapper objectMapper = new ObjectMapper();

            response = objectMapper.readValue(jsonResponse, HashMap.class);
        } catch (UnsupportedEncodingException | JsonParseException | JsonMappingException e) {
            String error = "Unable to parse postcode location response " + e.getMessage();
            log.error(error);
            throw new LocationServerException(error);
        } catch (IOException e) {
            String error = "Unable to read postcode location response " + e.getMessage();
            log.error(error);
            throw new LocationServerException(error);
        }

        // Calculate distances from target postcode
        int responseCode = (int) response.get("status");
        if (responseCode == HttpResponseStatus.OK.getCode()) {
            ArrayList<HashMap<String, Object>> responseResult = (ArrayList<HashMap<String, Object>>) response
                    .get("result");
            Double targetLat = null, targetLon = null;

            // First find target lat and lon
            Iterator<HashMap<String, Object>> it = responseResult.iterator();
            while (it.hasNext()) {
                Map<String, Object> responseResultItem = it.next();
                String queryPostcode = (String) responseResultItem.get("query");
                if (queryPostcode.equals(targetPostCode)) {
                    HashMap<String, Object> result = (HashMap<String, Object>) responseResultItem.get("result");
                    if (result != null) {
                        targetLat = (Double) result.get("latitude");
                        targetLon = (Double) result.get("longitude");
                        responseResult.remove(responseResultItem);
                        break;
                    }
                }
            }

            if (null == targetLat || null == targetLon) {
                throw new LocationServerException(
                        "Location service failed to return valid lat/lon for target postcode");
            }

            // Iterate and filter other postcodes by distance
            it = responseResult.iterator();
            while (it.hasNext()) {
                Map<String, Object> item = it.next();
                HashMap<String, Object> postCodeDetails = (HashMap<String, Object>) item.get("result");

                if (postCodeDetails != null) {
                    Double sourceLat = (Double) postCodeDetails.get("latitude");
                    Double sourceLon = (Double) postCodeDetails.get("longitude");
                    if (sourceLat != null && sourceLon != null) {
                        double distInMiles = getLatLonDistanceInMiles(targetLat, targetLon, sourceLat,
                                sourceLon);

                        String postCodeQuery = (String) item.get("query");

                        if (distInMiles <= distanceInMiles && postCodeAndUserIds.containsKey(postCodeQuery)) {
                            // Add this to a list, with user ids
                            resultingUserIds.addAll(postCodeAndUserIds.get(postCodeQuery));
                        }
                    }
                }

            }

        }
        return resultingUserIds;
    }
    

    /**
     * @param lat1
     *            - latitude 1
     * @param lon1
     *            - longitude 1
     * @param lat2
     *            - latitude 2
     * @param lon2
     *            - longitude 2
     * @return - distance in miles
     */
    private double getLatLonDistanceInMiles(final double lat1, final double lon1, final double lat2,
            final double lon2) {
        // borrowed from http://www.movable-type.co.uk/scripts/latlong.html
        int R = 6371000;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double d = R * c;

        // convert from metres to miles
        d = (d / 1000) * 0.621371;

        return d;
    }




}
