/**
 * Copyright 2022 James Sharkey
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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.jboss.resteasy.spi.DefaultOptionsMethodException;

/**
 * Exception mapper for OPTIONS requests to bypass UnhandledExceptionMapper.
 * <br>
 * For simple CORS OPTIONS requests, setting chainPreflight=false in web.xml is enough to
 * prevent Jetty from passing the OPTIONS request down to RESTEasy and thus our application.
 * <br>
 * However, for "non-simple" OPTIONS requests (those that do not match an allowed origin or
 * have unexpected or missing headers) the chainPreflight setting is ignored, and they are
 * passed down to our application. Since we have no @OPTIONS methods, RESTEasy throws its
 * DefaultOptionsMethodException (which is more useful than a Method NotAllowedException).
 * The issue https://github.com/eclipse/jetty.project/issues/7894 provided some useful
 * context to explain this processing, but the source code in
 * {@link org.eclipse.jetty.ee10.servlets.CrossOriginFilter#doFilter} is also quite clear.
 * <br>
 * We'd like to wrap all unhandled exceptions and log them nicely, hence the handler class
 * UnhandledExceptionMapper. Unfortunately, the DefaultOptionsMethodException is just an
 * unhandled exception and so would be treated as a 500 Internal Server Error if it reached
 * that mapper. Returning 500 to a CORS request and missing the CORS headers breaks CORS.
 * <br>
 * Since the DefaultOptionsMethodException contains the (complete and valid) CORS response
 * required, we can instead catch that exception specifically and return the attached response.
 * This is all this class does.
 */
public class OptionsMethodExceptionMapper implements ExceptionMapper<DefaultOptionsMethodException> {

  @Override
  public Response toResponse(final DefaultOptionsMethodException optionsMethodException) {
    return optionsMethodException.getResponse();
  }
}
