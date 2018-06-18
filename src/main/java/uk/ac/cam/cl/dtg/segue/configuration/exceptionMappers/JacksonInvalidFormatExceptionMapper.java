/*
 * Copyright 2018 Meurig Thomas
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
package uk.ac.cam.cl.dtg.segue.configuration.exceptionMappers;


import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created by mlt47 on 06/04/2018.
 * ExceptionHandler for InvalidFormatExceptions which Jackson can throw before hitting a facade code.
 * Without this the exception would be returned to the user without being handled and formatted nicely.
 */
@Provider
public class JacksonInvalidFormatExceptionMapper implements ExceptionMapper<InvalidFormatException> {
    private static final Logger log = LoggerFactory.getLogger(JacksonInvalidFormatExceptionMapper.class);

    @Context
    private HttpServletRequest request;

    @Override
    public Response toResponse(final InvalidFormatException e) {
        String message = String.format("%s on %s request to %s", e.getClass().getSimpleName(), request.getMethod(),
                 request.getRequestURI());
        log.error(message);
        return SegueErrorResponse.getBadRequestResponse("Invalid Format");
    }
}
