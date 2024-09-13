/*
 * Copyright 2019 Andrea Franceschini
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics.CACHE_METRICS_COLLECTOR;

/**
 * Glossary Facade
 *
 * This facade is intended to provide access to glossary terms.
 *
 */
@Path("/glossary")
@Tag(name = "/glossary")
public class GlossaryFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(GlossaryFacade.class);

    private final GitContentManager contentManager;
    private final Cache<String, ResultsWrapper<ContentDTO>> termCache;

    /**
     * @param properties     - to allow access to system properties.
     * @param contentManager - so that metadata about content can be accessed.
     * @param logManager     - for logging events using the logging api.
     */
    @Inject
    public GlossaryFacade(final AbstractConfigLoader properties, final GitContentManager contentManager,
                          final ILogManager logManager) {
        super(properties, logManager);
        this.contentManager = contentManager;

        this.termCache = CacheBuilder.newBuilder().recordStats().softValues().expireAfterAccess(5, TimeUnit.MINUTES).build();
        CACHE_METRICS_COLLECTOR.addCache("glossary_facade_terms_cache", termCache);
    }

    /**
     * Gets all the glossary terms that are indexed.
     *
     * @param limit      - Maximum amount of terms to retrieve. Used for pagination.
     * @param startIndex - Index from which to start retrieving when results exceed limit.
     *
     * @return Paginated list of glossary terms.
     */
    @GET
    @Path("terms")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get all the glossary terms that are indexed.")
    public final Response getTerms(@Context final Request request, @QueryParam("start_index") final String startIndex,
                                   @QueryParam("limit") final String limit) {

        // Create cache key for use both in browser and server caching:
        String currentContentSHA = this.contentManager.getCurrentContentSHA();
        String cacheKey = String.format("terms@%s-%s+%s", currentContentSHA, startIndex, limit);

        // Calculate the ETag for result browser caching:
        EntityTag etag = new EntityTag(cacheKey.hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag, NUMBER_SECONDS_IN_ONE_HOUR);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        // Check validity of limit and offset:
        int resultsLimit;
        int startIndexOfResults;

        if (null != limit) {
            resultsLimit = Integer.parseInt(limit);
            if (resultsLimit > SEARCH_MAX_WINDOW_SIZE || resultsLimit < 1) {
                return SegueErrorResponse.getBadRequestResponse("Glossary term search limit invalid!");
            }
        } else {
            resultsLimit = DEFAULT_RESULTS_LIMIT;
        }

        if (null != startIndex) {
            startIndexOfResults = Integer.parseInt(startIndex);
            if (startIndexOfResults < 0) {
                return SegueErrorResponse.getBadRequestResponse("Glossary term search start index invalid!");
            }
        } else {
            startIndexOfResults = 0;
        }

        // Get from server cache, else load and cache:
        try {
            ResultsWrapper<ContentDTO> c = termCache.get(cacheKey, () -> {
                List<GitContentManager.BooleanSearchClause> fieldsToMatch = Collections.singletonList(
                        new GitContentManager.BooleanSearchClause(
                                TYPE_FIELDNAME, BooleanOperator.AND, Collections.singletonList("glossaryTerm"))
                );

                return this.contentManager.findByFieldNames(fieldsToMatch, startIndexOfResults, resultsLimit);
            });

            return Response.ok(c).tag(etag).cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true)).build();
        } catch (ExecutionException e) {
            log.warn("Error loading glossary terms!", e);  // Sadly need full stack trace here, since errors are nested!
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error loading glossary terms!").toResponse();
        }
    }

    /**
     * Gets the current version of the segue application.
     *
     * @param term_id - The ID of the term to retrieve.
     *
     * @return segue version as a string wrapped in a response.
     */
    @GET
    @Path("terms/{term_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the term with the given id.")
    public final Response getTermById(@PathParam("term_id") final String term_id) {

        if (null == term_id) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Please specify a term_id.").toResponse();
        }

        ResultsWrapper<ContentDTO> c;
        try {
            c = this.contentManager.getUnsafeCachedDTOsByIdPrefix(term_id, 0, 10000);
            if (null == c) {
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "No glossary term found with id: " + term_id);
                log.debug(error.getErrorMessage());
                return error.toResponse();
            }
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Content acquisition error.", e).toResponse();
        }
        // Calculate the ETag on last modified date of tags list
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + "");
        return Response.ok(c).tag(etag).build();
    }
}