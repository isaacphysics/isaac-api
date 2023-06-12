/*
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.api;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.net.URI;
import java.net.URISyntaxException;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Segue Default Api Facade.
 * 
 * This class specifically caters for the Rutherford physics server and is expected to provide extended functionality to
 * the Segue api for use only on the Rutherford site.
 * 
 */
@Path("/")
@Tag(name = "/")
public class SegueDefaultFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(SegueDefaultFacade.class);

    /**
     * Constructor that allows pre-configuration of the segue api.
     * 
     * @param properties
     *            - the fully configured properties loader for the api.
     * @param logManager
     *            - An instance of the log manager used for recording usage of the CMS.

     */
    @Inject
    public SegueDefaultFacade(final AbstractConfigLoader properties, final ILogManager logManager) {
        super(properties, logManager);
    }
    
    /**
     * Redirect to swagger ui.
     * @param request - context
     * @return a redirect to a page listing the available endpoints.
     * @throws URISyntaxException - should never happen as hard coded.
     */
    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    @Cache
    @Deprecated
    @Operation(summary = "Redirect to Swagger.")
    public Response redirectToSwagger(@Context final HttpServletRequest request) throws URISyntaxException {
        String hostname = getProperties().getProperty(HOST_NAME);
        String proxyPath = getProperties().getProperty(PROXY_PATH);
        StringBuilder uri = new StringBuilder();

        if (proxyPath.equals("")) {
            uri.append("https://");
            uri.append(hostname);
            uri.append("/api-docs/");
        } else {
            uri.append(hostname);
            uri.append("/api-docs/");
        }
        
        log.info("Redirecting to swagger.");
        
        return Response.temporaryRedirect(new URI(uri.toString())).build();
    }
}
