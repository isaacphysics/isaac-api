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

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.time.StopWatch;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.spi.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows us to log the performance of all requests.
 *
 */
@Provider
@ServerInterceptor
public class PerformanceMonitor implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitor.class);

    @Context
    private HttpRequest request;

    public static final long WARNING_THRESHOLD = 5000;
    public static final long ERROR_THRESHOLD = 20000;

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
            log.debug(String.format("Request: %s %s took %dms", requestContext.getMethod(), request.getUri()
                    .getRequestUri().toURL().toString(), timeInMs));
        } else if (timeInMs > WARNING_THRESHOLD && timeInMs < ERROR_THRESHOLD) {
            log.warn(String.format("Performance Warning: Request: %s %s took %dms and exceeded threshold of %d",
                    requestContext.getMethod(), request.getUri().getRequestUri().toURL().toString(), timeInMs,
                    WARNING_THRESHOLD));
        } else {
            log.error(String.format("Performance Alert: Request: %s %s took %dms and exceeded threshold of %d",
                    requestContext.getMethod(), request.getUri().getRequestUri().toURL().toString(), timeInMs,
                    ERROR_THRESHOLD));
        }
    }

}
