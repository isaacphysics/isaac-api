/**
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

import com.google.inject.name.Named;
import io.swagger.annotations.Api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Info Facade
 * 
 * This facade is intended to provide access to meta data about the current running system.
 * 
 */
@Path("/info")
@Api(value = "/info")
public class InfoFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(InfoFacade.class);

    private final IContentManager contentManager;
    private final String contentIndex;

    /**
     * @param properties
     *            - to allow access to system properties.
     * @param contentManager
     *            - So that metadata about content can be accessed.
     * @param logManager
     *            - for logging events using the logging api.
     */
    @Inject
    public InfoFacade(final PropertiesLoader properties, final IContentManager contentManager, @Named(CONTENT_INDEX) final String contentIndex,
                      final ILogManager logManager) {
        super(properties, logManager);
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
    }

    /**
     * This method returns all versions as an immutable map version_list.
     * 
     * @param limit
     *            parameter if not null will set the limit of the number entries to return the default is the latest 10
     *            (indices starting at 0).
     * 
     * @return a Response containing an immutable map version_list: [x..y..]
     */
    @GET
    @Path("content_versions")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getVersionsList(@QueryParam("limit") final String limit) {
        // try to parse the integer
        Integer limitAsInt = null;

        try {
            if (null == limit) {
                limitAsInt = DEFAULT_RESULTS_LIMIT;
            } else {
                limitAsInt = Integer.parseInt(limit);
            }
        } catch (NumberFormatException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                    "The limit requested is not a valid number.");
            log.debug(error.getErrorMessage());
            return error.toResponse();
        }

        List<String> allVersions = this.contentManager.listAvailableVersions();
        List<String> limitedVersions;
        try {
            limitedVersions = new ArrayList<String>(allVersions.subList(0, limitAsInt));
        } catch (IndexOutOfBoundsException e) {
            // they have requested a stupid limit so just give them what we have
            // got.
            limitedVersions = allVersions;
            log.debug("Bad index requested for version number." + " Using maximum index instead.");
        } catch (IllegalArgumentException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid limit specified: " + limit,
                    e);
            log.debug(error.getErrorMessage(), e);
            return error.toResponse();
        }

        ImmutableMap<String, Collection<String>> result = new ImmutableMap.Builder<String, Collection<String>>().put(
                "version_list", limitedVersions).build();

        return Response.ok(result).build();
    }

    /**
     * Gets the current version of the segue application.
     * 
     * @return segue version as a string wrapped in a response.
     */
    @GET
    @Path("segue_version")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getSegueAppVersion() {
        ImmutableMap<String, String> result = new ImmutableMap.Builder<String, String>().put("segueVersion",
                SegueGuiceConfigurationModule.getSegueVersion()).build();

        return Response.ok(result).build();
    }

    /**
     * Gets the current version of the segue application.
     * 
     * @param request
     *            for caching
     * @return segue version as a string wrapped in a response.
     */
    @GET
    @Path("log_event_types")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getLogEventTypes(@Context final Request request) {
        ImmutableMap<String, Collection<String>> result;
        try {
            result = new ImmutableMap.Builder<String, Collection<String>>().put("results",
                    getLogManager().getAllEventTypes()).build();
        } catch (SegueDatabaseException e) {
            log.error("Database error has occurred", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "A database error has occurred.").toResponse();
        }

        EntityTag etag = new EntityTag(result.toString().hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag, NUMBER_SECONDS_IN_ONE_DAY);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        return Response.ok(result).tag(etag).cacheControl(this.getCacheControl(NUMBER_SECONDS_IN_ONE_DAY, false))
                .build();
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
    public final Response getCachedVersions() {

        ImmutableMap<String, Collection<String>> result = new ImmutableMap.Builder<String, Collection<String>>().put(
                "cachedVersions", this.contentManager.getCachedVersionList()).build();

        return Response.ok(result).build();
    }

    @GET
    @Path("symbolic_checker/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pingEqualityChecker(@Context final HttpServletRequest request) {

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

    @GET
    @Path("chemistry_checker/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pingChemistryChecker(@Context final HttpServletRequest request) {

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

    @GET
    @Path("etl/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pingETLServer(@Context final HttpServletRequest request) {

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
}
