/*
 * Copyright 2018 Meurig Thomas
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
package uk.ac.cam.cl.dtg.segue.configuration.exceptionMappers;


import com.fasterxml.jackson.databind.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * ExceptionHandler for JsonMappingException which Jackson can throw before hitting a facade code.
 * Without this the exception would be returned to the user without being handled and formatted nicely.
 */
@Provider
public class JacksonExceptionMapper implements ExceptionMapper<JsonMappingException> {
    private static final Logger log = LoggerFactory.getLogger(JacksonExceptionMapper.class);

    @Context
    private HttpServletRequest request;

    @Override
    public Response toResponse(final JsonMappingException e) {
        log.error("{} for {} {} - ({})", e.getClass().getSimpleName(), e.getMessage(), request.getMethod(), request.getRequestURI());
        return SegueErrorResponse.getBadRequestResponse("Invalid JSON provided!");
    }
}
