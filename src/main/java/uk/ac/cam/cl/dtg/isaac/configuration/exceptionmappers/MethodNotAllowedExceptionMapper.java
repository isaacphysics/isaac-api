/**
 * Copyright 2017 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.configuration.exceptionmappers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;

@Provider
public class MethodNotAllowedExceptionMapper implements ExceptionMapper<jakarta.ws.rs.NotAllowedException> {
  private static final Logger log = LoggerFactory.getLogger(MethodNotAllowedExceptionMapper.class);

  @Context
  private HttpServletRequest request;

  @Override
  public Response toResponse(final jakarta.ws.rs.NotAllowedException e) {
    String message = String.format("Request %s %s is not allowed", request.getMethod(), request.getRequestURI());
    log.error(message);
    return SegueErrorResponse.getMethodNotAllowedReponse(message);
  }
}
