package uk.ac.cam.cl.dtg.isaac.quiz;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 *  Encapsulates common functionality required for making external validator requests.
 */
public abstract class AbstractExternalValidator {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AbstractExternalValidator() {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build();
        objectMapper = new ObjectMapper();
    }

    /**
     * Make a JSON HTTP POST request to an external validator, and provide the response JSON as a HashMap.
     *
     * @param externalValidatorUrl - the URL of an external validator to POST to.
     * @param requestBody          - the JSON request body as a Map
     * @return the response JSON, as a HashMap
     * @throws IOException - on failure to communicate with the external validator
     */
    HashMap<String, Object> getResponseFromExternalValidator(final String externalValidatorUrl,
                                                             final Map<String, String> requestBody) throws IOException {
        // This is ridiculous. All we want to do is pass some JSON to a REST endpoint and get some JSON back.
        StringWriter sw = new StringWriter();
        JsonGenerator g = new JsonFactory().createGenerator(sw);
        objectMapper.writeValue(g, requestBody);
        g.close();
        String requestString = sw.toString();

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(externalValidatorUrl))
                    .timeout(Duration.ofMillis(3000))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestString))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")  // JSON _will_ be String -> Object.
            HashMap<String, Object> response = objectMapper.readValue(httpResponse.body(), HashMap.class);

            return response;
        } catch (final InterruptedException e) {
            throw new IOException(e);
        }
    }
}
