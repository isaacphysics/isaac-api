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

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.LocationHistory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;

/**
 * Class to allow postcode-related searches using external service.
 *
 * @author Alistair Stead
 *
 */
public class PostCodeIOLocationResolver implements PostCodeLocationResolver {
    private static final Logger log = LoggerFactory.getLogger(PostCodeIOLocationResolver.class);

    private final String url = "http://api.postcodes.io/postcodes";
    private final int POSTCODEIO_MAX_REQUESTS = 100;
    
    private final LocationHistory locationHistory;

    /**
     * PostCode resolver that uses queries postcodes from the local database and external postcodes.io database.
     * 
     * @param locationHistory
     *            - the location history so we can access the database of existing post codes
     */
    @Inject
    public PostCodeIOLocationResolver(final LocationHistory locationHistory) {
        this.locationHistory = locationHistory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.util.locations.PostCodeLocationResolver#filterPostcodesWithinProximityOfPostcode(
     * java.util.HashMap, java.lang.String, int)
     */
    @Override
    public List<Long> filterPostcodesWithinProximityOfPostcode(final Map<String, List<Long>> postCodeIDMap,
            final String targetPostCode, final PostCodeRadius postCodeRadius)
            throws LocationServerException,
            SegueDatabaseException {

        if (null == postCodeIDMap) {
            throw new LocationServerException("Map of postcodes cannot be null");
        }

        final Map<String, List<Long>> cleanPostCodeIDMap = Maps.newHashMap();
        for (String key : postCodeIDMap.keySet()) {
            List<Long> val = postCodeIDMap.get(key);
            if (key != null) {
                cleanPostCodeIDMap.put(key.replace(" ", ""), val);
            }
        }

        LinkedList<Long> resultingUserIds = new LinkedList<Long>();

        // first do a database lookup, then fallback on the service
        List<PostCode> knownPostCodes = Lists.newArrayList();
        List<String> unknownPostCodes = Lists.newArrayList();
        for (String postCode : cleanPostCodeIDMap.keySet()) {
            PostCode result = this.locationHistory.getPostCode(postCode);
            if (null == result) {
                unknownPostCodes.add(postCode);
            } else {
                knownPostCodes.add(result);
            }
        }

        // add the target postcode, so we can do it in one request
        PostCode targetPostCodeObject = this.locationHistory.getPostCode(targetPostCode);
        
        if (null == targetPostCodeObject) {
            List<String> targetPostCodeList = Lists.newArrayList();
            targetPostCodeList.add(targetPostCode);
            List<PostCode> results = submitPostCodeRequest(targetPostCodeList);
            if (results != null && results.size() == 1) {
                targetPostCodeObject = results.get(0);
            } else {
                throw new LocationServerException(
                        "Location service failed to return valid lat/lon for target postcode");
            } 
        }

        List<PostCode> foundPostCodes = carryOutExternalPostCodeServiceRequest(unknownPostCodes);

        // Store new postcodes back to the database
        this.locationHistory.storePostCodes(foundPostCodes);

        knownPostCodes.addAll(foundPostCodes);
        
        for (PostCode postCode : knownPostCodes) {
            
            if (null == postCode.getLat() || null == postCode.getLon()) {
                continue;
            }
            
            double distInMiles = getLatLonDistanceInMiles(targetPostCodeObject.getLat(),
                    targetPostCodeObject.getLon(), postCode.getLat(),
                    postCode.getLon());
            
            if (distInMiles <= postCodeRadius.getDistance()
                    && cleanPostCodeIDMap.containsKey(postCode.getPostCode())) {
                // Add this to a list, with user ids
                resultingUserIds.addAll(cleanPostCodeIDMap.get(postCode.getPostCode()));
            }
            
        }
        
        return resultingUserIds;
    }
    
    /**
     * Method to ensure that only 100 (the max) post codes are queried using the external service at once.
     * 
     * @param unknownPostCodes
     *            - a list of post codes not exceeding 100 in length
     * @return - a list of post code objects
     * @throws LocationServerException
     *             - if there was an issue with the service
     */
    private List<PostCode> carryOutExternalPostCodeServiceRequest(final List<String> unknownPostCodes)
            throws LocationServerException {

        log.info(String.format("Carrying out external postcode service request with %d unknown postcodes",
                unknownPostCodes.size()));

        if (unknownPostCodes.size() > 100) {
            List<PostCode> completeResults = Lists.newArrayList();
            for (int i = 0; i < unknownPostCodes.size(); i += 100) {
                List<String> subList = unknownPostCodes.subList(i, Math.min(i + 100, unknownPostCodes.size()));
                List<PostCode> results = submitPostCodeRequest(subList);
                completeResults.addAll(results);
            }
            return completeResults;
        } else {
            return submitPostCodeRequest(unknownPostCodes);
        }

    }

    /**
     * @param unknownPostCodes
     *            - a list of postcodes not exceeding maxRequests in length
     * @return - the results
     * @throws LocationServerException
     *             - if there was an issue with the service
     */
    @SuppressWarnings("unchecked")
    private List<PostCode> submitPostCodeRequest(final List<String> unknownPostCodes)
            throws LocationServerException {

        if (unknownPostCodes.size() > POSTCODEIO_MAX_REQUESTS) {
            throw new IllegalArgumentException(String.format("Number of postcodes cannot be bigger than %d!",
                    POSTCODEIO_MAX_REQUESTS));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{ \"postcodes\" : [");
        for (int i = 0; i < unknownPostCodes.size(); i++) {
            sb.append("\"");
            sb.append(unknownPostCodes.get(i));
            sb.append("\"");
            if (i < unknownPostCodes.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("] }");

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

        List<PostCode> returnList = Lists.newArrayList();
        int responseCode = (int) response.get("status");
        if (responseCode == HttpResponseStatus.OK.getCode()) {
            ArrayList<HashMap<String, Object>> responseResult = (ArrayList<HashMap<String, Object>>) response
                    .get("result");

            Iterator<HashMap<String, Object>> it = responseResult.iterator();
            while (it.hasNext()) {
                Map<String, Object> item = it.next();
                HashMap<String, Object> postCodeDetails = (HashMap<String, Object>) item.get("result");

                if (postCodeDetails != null) {
                    Double sourceLat = (Double) postCodeDetails.get("latitude");
                    Double sourceLon = (Double) postCodeDetails.get("longitude");
                    PostCode postcode = new PostCode((String) item.get("query"), sourceLat,
                            sourceLon);
                    returnList.add(postcode);
                }

            }
        }

        return returnList;
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
