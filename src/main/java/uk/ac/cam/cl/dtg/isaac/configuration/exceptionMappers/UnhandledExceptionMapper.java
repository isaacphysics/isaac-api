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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;

/**
 *  RESTEasy ExceptionMapper to turn unhandled exceptions into useful Response objects.
 */
@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger log = LoggerFactory.getLogger(UnhandledExceptionMapper.class);

    @Context
    private HttpServletRequest request;

    @Override
    public Response toResponse(final Exception e) {
        String message = String.format("%s on %s %s", e.getClass().getSimpleName(), request.getMethod(), request.getRequestURI());
        log.error(message, e);
        return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "An unhandled error occurred!", e).toResponse();
    }
}
