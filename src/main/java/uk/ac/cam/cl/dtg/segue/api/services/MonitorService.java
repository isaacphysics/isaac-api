package uk.ac.cam.cl.dtg.segue.api.services;

import com.google.inject.Inject;

import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

public class MonitorService {
    private static final String NO_MATCHING_ENDPOINT = "NO_MATCHING_ENDPOINT";

    @Inject // We don't require anything to be injected yet, but we'll adopt this pattern for unit-testing's sake
    private MonitorService() {}

    public String getPathWithoutPathParamValues(UriInfo uri) {
        List<String> matchingUris = uri.getMatchedURIs(); // Ordered so that current resource URI is first

        if (matchingUris.isEmpty()) {
            return NO_MATCHING_ENDPOINT;
        }

        String mostSpecificMatchingUri = "/" + matchingUris.get(0);
        // Replace any path param values with its curly-braced, path param identifier
        for (Map.Entry<String, List<String>> pathParams : uri.getPathParameters().entrySet()) {
            for (String paramValue : pathParams.getValue()) {
                mostSpecificMatchingUri =
                        mostSpecificMatchingUri.replace(paramValue, "{" + pathParams.getKey() + "}");
            }
        }
        return mostSpecificMatchingUri;
    }
}
