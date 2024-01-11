/*
 * Copyright 2024 Meurig Thomas
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
package uk.ac.cam.cl.dtg.isaac.tutor;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class TutorExternalService {

    private final String hostname;
    private final String port;
    private final String externalTutorUrl;


    /**
     * Make a JSON HTTP request to an external service, and provide the response JSON as a HashMap.
     *
     * @param externalServiceUrl - the URL of an external service.
     * @param requestBody - the JSON request body as a Map
     * @return the response JSON, as a HashMap
     * @throws IOException - on failure to communicate with the external validator
     */
    // TODO - this is largely duplicated in IValidator. Refactor to a common location.
    private HashMap<String, Object> getResponseFromExternalService(final String externalServiceUrl,
                                                                   final String method,
                                                                   final Map<String, String> requestBody) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        JsonGenerator g = new JsonFactory().createGenerator(sw);
        mapper.writeValue(g, requestBody);
        g.close();
        String requestString = sw.toString();

        HttpClient httpClient = new DefaultHttpClient();
        HttpRequestBase httpRequest;
        if ("POST".equals(method)) {
            HttpPost httpPost = new HttpPost(externalServiceUrl);
            httpPost.setEntity(new StringEntity(requestString, "UTF-8"));
            httpRequest = httpPost;
        } else {
            httpRequest = new HttpGet(externalServiceUrl);
        }

        httpRequest.addHeader("Content-Type", "application/json");

        HttpResponse httpResponse = httpClient.execute(httpRequest);
        HttpEntity responseEntity = httpResponse.getEntity();
        String responseString = EntityUtils.toString(responseEntity);
        HashMap<String, Object> response = mapper.readValue(responseString, HashMap.class);

        return response;
    }


    public TutorExternalService(final String hostname, final String port) {
        this.hostname = hostname;
        this.port = port;
        this.externalTutorUrl = "http://" + this.hostname + ":" + this.port;
    }

    public Map<String, Object> createNewThread() throws IOException {
        return getResponseFromExternalService(this.externalTutorUrl + "/threads", "POST", null);
    }

}
