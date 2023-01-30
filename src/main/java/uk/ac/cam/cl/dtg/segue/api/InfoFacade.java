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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_THIRTY_DAYS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_APP_ENVIRONMENT;

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

    private final GitContentManager contentManager;
    private final SegueJobService segueJobService;

    /**
     * @param properties
     *            - to allow access to system properties.
     * @param contentManager
     *            - So that metadata about content can be accessed.
     * @param logManager
     *            - for logging events using the logging api.
     */
    @Inject
    public InfoFacade(final PropertiesLoader properties, final GitContentManager contentManager,
                      final SegueJobService segueJobService,
                      final ILogManager logManager) {
        super(properties, logManager);
        this.contentManager = contentManager;
        this.segueJobService = segueJobService;
    }

    /**
     * Gets the current version of the segue application.
     * 
     * @return segue version as a string wrapped in a response.
     */
    @GET
    @Path("segue_version")
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
    @Path("segue_environment")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get the mode that the API is currently running in: DEV or PROD.")
    public final Response getSegueEnvironment(@Context final Request request) {
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag, NUMBER_SECONDS_IN_THIRTY_DAYS);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        ImmutableMap<String, String> result = new ImmutableMap.Builder<String, String>().put("segueEnvironment",
                this.getProperties().getProperty(SEGUE_APP_ENVIRONMENT)).build();

        return Response.ok(result).cacheControl(this.getCacheControl(NUMBER_SECONDS_IN_THIRTY_DAYS, true)).tag(etag)
                .build();
    }

    /**
     * This method return a json response containing version related information.
     * 
     * @return a version info as json response
     */
    @GET
    @Path("content_versions/live_version")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the current content version commit SHA.")
    public final Response getLiveVersionInfo() {
        ImmutableMap<String, String> result = new ImmutableMap.Builder<String, String>().put("liveVersion",
                this.contentManager.getCurrentContentSHA()).build();

        return Response.ok(result).build();
    }

    /**
     * This method return a json response containing version related information.
     * 
     * @return a version info as json response
     */
    @GET
    @Path("content_versions/cached")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all currently indexed content commit SHAs.")
    public final Response getCachedVersions() {

        ImmutableMap<String, Collection<String>> result = new ImmutableMap.Builder<String, Collection<String>>().put(
                "cachedVersions", this.contentManager.getCachedContentSHAList()).build();

        return Response.ok(result).build();
    }

    /**
     * This method checks the status of the symbolic checker live dependency.
     *
     * @return json success true or false
     */
    @GET
    @Path("symbolic_checker/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check whether the symbolic question checker is running.")
    public Response pingEqualityChecker() {

        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://" + this.getProperties().getProperty(Constants.EQUALITY_CHECKER_HOST)
                                      + ":" + this.getProperties().getProperty(Constants.EQUALITY_CHECKER_PORT) +  "/");

        HttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 200) {
            return Response.ok(ImmutableMap.of("success", true)).build();
        } else {
            return Response.ok(ImmutableMap.of("success", false)).build();
        }

    }

    /**
     * This method checks the status of the chemistry checker live dependency.
     *
     * @return json success true or false
     */
    @GET
    @Path("chemistry_checker/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check whether the chemistry question checker is running.")
    public Response pingChemistryChecker() {

        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://" + this.getProperties().getProperty(Constants.CHEMISTRY_CHECKER_HOST)
                                      + ":" + this.getProperties().getProperty(Constants.CHEMISTRY_CHECKER_PORT) +  "/");

        HttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 200) {
            return Response.ok(ImmutableMap.of("success", true)).build();
        } else {
            return Response.ok(ImmutableMap.of("success", false)).build();
        }

    }

    /**
     * This method checks the status of the ETL live dependency.
     *
     * @return json success true or false
     */
    @GET
    @Path("etl/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check whether the content indexer is running.")
    public Response pingETLServer() {
        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://" + getProperties().getProperty("ETL_HOSTNAME") + ":"
                + getProperties().getProperty("ETL_PORT") + "/isaac-api/api/etl/ping");

        HttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (IOException e) {
            log.warn("Error when checking status of ETL server: " + e.toString());
        }

        if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 200) {
            return Response.ok(ImmutableMap.of("success", true)).build();
        } else {
            return Response.ok(ImmutableMap.of("success", false)).build();
        }

    }

    /**
     * This method checks the status of the elasticsearch live dependency.
     *
     * @return json success true or false
     */
    @GET
    @Path("elasticsearch/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check whether elasticsearch is running.")
    public Response pingElasticSearch() {

        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet("http://" + getProperties().getProperty("SEARCH_CLUSTER_ADDRESS") + ":"
                + getProperties().getProperty("SEARCH_CLUSTER_INFO_PORT") + "/_cat/health");

        HttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpGet);
        } catch (IOException e) {
            log.warn("Error when checking status of elasticsearch: " + e.toString());
        }

        // FIXME - this assumes a 200 means all is ok.
        // It's likely that a real problem with clustering would also lead to a 200!
        if (httpResponse != null && httpResponse.getStatusLine().getStatusCode() == 200) {
            return Response.ok(ImmutableMap.of("success", true)).build();
        } else {
            return Response.ok(ImmutableMap.of("success", false)).build();
        }

    }

    /**
     * This method checks the status of the Quartz job service.
     *
     * @return json success true or false
     */
    @GET
    @Path("quartz/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check whether Quartz job scheduler is running.")
    public Response pingQuartzScheduler() {
        if (segueJobService.isStarted()) {
            return Response.ok(ImmutableMap.of("success", true)).build();
        } else {
            return Response.ok(ImmutableMap.of("success", false)).build();
        }
    }
}
