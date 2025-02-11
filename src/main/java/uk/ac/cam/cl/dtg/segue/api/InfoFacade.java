/*
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.api;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Info Facade
 * 
 * This facade is intended to provide access to meta data about the current running system.
 * 
 */
@Path("/info")
@Tag(name = "/info")
public class InfoFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(InfoFacade.class);
    private final SegueJobService segueJobService;

    /**
     * @param properties
     *            - to allow access to system properties.
     * @param logManager
     *            - for logging events using the logging api.
     */
    @Inject
    public InfoFacade(final AbstractConfigLoader properties, final SegueJobService segueJobService,
                      final ILogManager logManager) {
        super(properties, logManager);
        this.segueJobService = segueJobService;
    }

    /**
     * Gets the current version of the segue application.
     * 
     * @return segue version as a string wrapped in a response.
     */
    @GET
    @Path("/segue_version")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the currently running API build version.")
    public final Response getSegueAppVersion() {
        ImmutableMap<String, String> result = new ImmutableMap.Builder<String, String>().put("segueVersion",
                Objects.requireNonNullElse(SegueGuiceConfigurationModule.getSegueVersion(), "unknown")).build();

        return Response.ok(result).build();
    }

    /**
     * Gets the current mode that the segue application is running in.
     * 
     * @param request
     *            - for cache control purposes.
     * @return segue mode as a string wrapped in a response. e.g {segueMode:DEV}
     */
    @GET
    @Path("/segue_environment")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get the mode that the API is currently running in: DEV or PROD.")
    public final Response getSegueEnvironment(@Context final Request request) {
        String environment = this.getProperties().getProperty(SEGUE_APP_ENVIRONMENT);

        EntityTag etag = new EntityTag(environment.hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag, NUMBER_SECONDS_IN_THIRTY_DAYS);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        ImmutableMap<String, String> result = ImmutableMap.of("segueEnvironment", environment);

        return Response.ok(result).cacheControl(this.getCacheControl(NUMBER_SECONDS_IN_THIRTY_DAYS, true)).tag(etag)
                .build();
    }

    /**
     * This method checks the status of the symbolic checker live dependency.
     *
     * @return json success true or false
     */
    @GET
    @Path("/symbolic_checker/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check whether the symbolic question checker is running.")
    public Response pingEqualityChecker() {

        return pingUrlForStatus("http://" + this.getProperties().getProperty(Constants.EQUALITY_CHECKER_HOST)
                + ":" + this.getProperties().getProperty(Constants.EQUALITY_CHECKER_PORT) +  "/");
    }

    /**
     * This method checks the status of the chemistry checker live dependency.
     *
     * @return json success true or false
     */
    @GET
    @Path("/chemistry_checker/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check whether the chemistry question checker is running.")
    public Response pingChemistryChecker() {

        return pingUrlForStatus("http://" + this.getProperties().getProperty(Constants.CHEMISTRY_CHECKER_HOST)
                + ":" + this.getProperties().getProperty(Constants.CHEMISTRY_CHECKER_PORT) +  "/");
    }

    /**
     * This method checks the status of the ETL live dependency.
     *
     * @return json success true or false
     */
    @GET
    @Path("/etl/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check whether the content indexer is running.")
    public Response pingETLServer() {

        return pingUrlForStatus("http://" + getProperties().getProperty("ETL_HOSTNAME") + ":"
                + getProperties().getProperty("ETL_PORT") + "/isaac-api/api/etl/ping");
    }

    /**
     * This method checks the status of the elasticsearch live dependency.
     *
     * @return json success true or false
     */
    @GET
    @Path("/elasticsearch/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check whether elasticsearch is running.")
    public Response pingElasticSearch() {

        return pingUrlForStatus("http://" + getProperties().getProperty("SEARCH_CLUSTER_ADDRESS") + ":"
                    + getProperties().getProperty("SEARCH_CLUSTER_INFO_PORT") + "/_cat/health");
    }

    /**
     * This method checks the status of the Quartz job service.
     *
     * @return json success true or false
     */
    @GET
    @Path("/quartz/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check whether Quartz job scheduler is currently running.")
    public Response pingQuartzScheduler() {
        if (segueJobService.wasStarted() && !segueJobService.isShutdown()) {
            return Response.ok(ImmutableMap.of("success", true)).build();
        } else {
            return Response.ok(ImmutableMap.of("success", false)).build();
        }
    }

    /**
     *  Test a HTTP URL for a 200 status code.
     *
     * @param url - the url to test.
     * @return a Response containing "success" and true/false for the status.
     */
    private Response pingUrlForStatus(final String url) {

        HttpResponse<String> httpResponse = null;
        try {
            HttpClient httpClient = java.net.http.HttpClient.newHttpClient();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET().build();

            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        } catch (IOException | InterruptedException e) {
            log.warn(String.format("Error when pinging for status: %s", e));
        }

        // FIXME: should we inspect the response body?
        if (httpResponse != null && httpResponse.statusCode() == 200) {
            return Response.ok(ImmutableMap.of("success", true)).build();
        } else {
            return Response.ok(ImmutableMap.of("success", false)).build();
        }
    }
}
