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
package uk.ac.cam.cl.dtg.segue.api.monitors;

import org.apache.commons.lang3.time.StopWatch;
import org.jboss.resteasy.spi.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics.REQUEST_LATENCY_HISTOGRAM;

/**
 * Allows us to log the performance of all requests.
 *
 */
@Provider
public class PerformanceMonitor implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitor.class);

    @Context
    private HttpRequest request;

    public static final long WARNING_THRESHOLD = 3000;
    public static final long ERROR_THRESHOLD = 10000;
    private static final long NUMBER_OF_MILLISECONDS_IN_A_SECOND = 1000;
    private static final String NO_MATCHING_ENDPOINT = "NO_MATCHING_ENDPOINT";

    /**
     * PerformanceMonitor.
     */
    public PerformanceMonitor() {

    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        StopWatch timer = new StopWatch();
        timer.start();
        request.setAttribute("timer", timer);
    }

    @Override
    public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
            throws IOException {
        StopWatch timer = (StopWatch) request.getAttribute("timer");
        request.removeAttribute("timer");
        
        if (null == timer) {
            // no timer started don't continue
            return;
        }
        
        timer.stop();
        long timeInMs = timer.getTime();

        if (timeInMs < WARNING_THRESHOLD) {
            log.debug(String.format("Request: %s %s took %dms",
                    requestContext.getMethod(), request.getUri().getPath(), timeInMs));
        } else if (timeInMs < ERROR_THRESHOLD) {
            log.warn(String.format("Performance Warning: Request: %s %s took %dms and exceeded threshold of %d",
                    requestContext.getMethod(), request.getUri().getPath(), timeInMs, WARNING_THRESHOLD));
        } else {
            log.error(String.format("Performance Alert: Request: %s %s took %dms and exceeded threshold of %d",
                    requestContext.getMethod(), request.getUri().getPath(), timeInMs, ERROR_THRESHOLD));
        }

        // Record for metrics
        REQUEST_LATENCY_HISTOGRAM
                .labels(requestContext.getMethod(), pathWithoutPathParamValues(request.getUri()))
                .observe((double)timeInMs / NUMBER_OF_MILLISECONDS_IN_A_SECOND);
    }

    private String pathWithoutPathParamValues(UriInfo uri) {
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
