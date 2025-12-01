/*
 * Copyright 2022 James Sharkey
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
package uk.ac.cam.cl.dtg.isaac.configuration.exceptionMappers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.net.URISyntaxException;

/**
 *  RESTEasy ExceptionMapper to turn unhandled exceptions into Response objects in JSON format.
 */
@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger log = LoggerFactory.getLogger(UnhandledExceptionMapper.class);
    private static final String UNEXPECTED_ERROR = "An unexpected error occurred and has been logged!";

    @Context
    private HttpServletRequest request;

    @Override
    public Response toResponse(final Exception e) {

        // Some exceptions should map to specific HTTP response codes.
        // Since we are catching /all/ exceptions here, we have to do these ourselves (but this lets them be JSON too).
        if (e.getCause() instanceof URISyntaxException || e instanceof BadRequestException) {
            // 400: This happens on malformed or malicious URLs being provided.
            log.warn("{}: {}", e.getClass().getSimpleName(), e.getMessage());
            return SegueErrorResponse.getBadRequestResponse("Malformed request!");

        } else if (e instanceof NotFoundException) {
            // 404: This happens if an endpoint does not match, e.g. after endpoint scanning.
            log.warn("Endpoint for {} {} not found", request.getMethod(), request.getRequestURI());
            return SegueErrorResponse.getResourceNotFoundResponse("Endpoint not found.");

        } else if (e instanceof NotAllowedException) {
            // 405: This happens on e.g. a POST to a GET endpoint.
            log.warn("Request {} {} is not allowed", request.getMethod(), request.getRequestURI());
            String message = String.format("Method %s not supported.", request.getMethod());
            return SegueErrorResponse.getMethodNotAllowedResponse(message, (NotAllowedException) e);

        } else if (e instanceof NotSupportedException) {
            // 415: This happens on invalid or missing Content-Type.
            log.warn("Content Type {} for {} {} is not allowed", request.getContentType(), request.getMethod(), request.getRequestURI());
            return SegueErrorResponse.getUnsupportedContentTypeResponse(request.getContentType());
        }

        // Otherwise a completely generic error message that leaks little information:
        log.error("{} for {} {} - ({})", e.getClass().getSimpleName(), e.getMessage(), request.getMethod(), request.getRequestURI());
        if (e instanceof NullPointerException) {
            log.error("NPE stack trace:", e);  // Help diagnose bugs in our code.
        }
        return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR).toResponse();
    }
}
