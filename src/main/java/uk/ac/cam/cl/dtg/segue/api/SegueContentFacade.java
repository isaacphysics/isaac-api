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
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Segue Content Facade
 * 
 * This class specifically caters for the Rutherford physics server and is expected to provide extended functionality to
 * the Segue api for use only on the Rutherford site.
 * 
 */
@Path("/content")
@Api(value = "/content")
public class SegueContentFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(SegueContentFacade.class);

    private final IContentManager contentManager;
    private final String contentIndex;
    private final ContentService contentService;

    /**
     * @param properties
     *            - the fully configured properties loader for the api.
     * @param contentManager
     *            - The content version controller used by the api.
     * @param logManager
     *            - An instance of the log manager used for recording usage of the CMS.

     */
    @Inject
    public SegueContentFacade(final PropertiesLoader properties, final IContentManager contentManager,
                              @Named(CONTENT_INDEX) final String contentIndex,
                              final ILogManager logManager, final ContentService contentService) {
        super(properties, logManager);

        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.contentService = contentService;
    }

    /**
     * This method will return a ResultsWrapper<ContentDTO> based on the parameters supplied.
     * 
     * @param version
     *            - the version of the content to search. If null it will default to the current live version.
     * @param fieldsToMatch
     *            - List of Boolean search clauses that must be true for the returned content.
     * @param startIndex
     *            - the start index for the search results.
     * @param limit
     *            - the max number of results to return.
     * @return Response containing a ResultsWrapper<ContentDTO> or a Response containing null if none found.
     */
    public final ResultsWrapper<ContentDTO> findMatchingContent(final String version,
            final List<IContentManager.BooleanSearchClause> fieldsToMatch,
            @Nullable final Integer startIndex, @Nullable final Integer limit) throws ContentManagerException {

        return contentService.findMatchingContent(version, fieldsToMatch, startIndex, limit);
    }

    /**
     * This method will return a ResultsWrapper<ContentDTO> based on the parameters supplied. Providing the results in a
     * randomised order.
     * 
     * This method is the same as {@link #findMatchingContentRandomOrder(String, List, Integer, Integer, Long)} but uses
     * a default random seed.
     * 
     * @param version
     *            - the version of the content to search. If null it will default to the current live version.
     * @param fieldsToMatch
     *            - List of Boolean search clauses that must be true for the returned content.
     * @param startIndex
     *            - the start index for the search results.
     * @param limit
     *            - the max number of results to return.
     * @return Response containing a ResultsWrapper<ContentDTO> or a Response containing null if none found.
     */
    public final ResultsWrapper<ContentDTO> findMatchingContentRandomOrder(
            @Nullable final String version, final List<IContentManager.BooleanSearchClause> fieldsToMatch,
            final Integer startIndex, final Integer limit) {
        return this.findMatchingContentRandomOrder(version, fieldsToMatch, startIndex, limit, null);
    }

    /**
     * This method will return a ResultsWrapper<ContentDTO> based on the parameters supplied. Providing the results in a
     * randomised order.
     * 
     * @param version
     *            - the version of the content to search. If null it will default to the current live version.
     * @param fieldsToMatch
     *            - List of Boolean search clauses that must be true for the returned content.
     * @param startIndex
     *            - the start index for the search results.
     * @param limit
     *            - the max number of results to return.
     * @param randomSeed
     *            - to allow some control over the random order of the results.
     * @return Response containing a ResultsWrapper<ContentDTO> or a Response containing null if none found.
     */
    public final ResultsWrapper<ContentDTO> findMatchingContentRandomOrder(
            @Nullable final String version, final List<IContentManager.BooleanSearchClause> fieldsToMatch,
            final Integer startIndex, final Integer limit, final Long randomSeed) {

        String newVersion = this.contentIndex;
        Integer newLimit = DEFAULT_RESULTS_LIMIT;
        Integer newStartIndex = 0;
        if (version != null) {
            newVersion = version;
        }
        if (limit != null) {
            newLimit = limit;
        }
        if (startIndex != null) {
            newStartIndex = startIndex;
        }

        ResultsWrapper<ContentDTO> c = null;

        // Deserialize object into POJO of specified type, providing one exists.
        try {
            c = this.contentManager.findByFieldNamesRandomOrder(newVersion, fieldsToMatch, newStartIndex,
                    newLimit, randomSeed);
        } catch (IllegalArgumentException e) {
            log.error("Unable to map content object.", e);
            throw e;
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
        }

        return c;
    }

    /**
     * Library method that searches the content manager for some search string and provides map of fields that must
     * match.
     * 
     * @param searchString
     *            - to pass to the search engine.
     * @param version
     *            - of the content to search.
     * @param fieldsThatMustMatch
     *            - a map of fieldName to list of possible matches.
     * @param startIndex
     *            - the start index for the search results.
     * @param limit
     *            - the max number of results to return.
     * @return a response containing the search results (results wrapper) or an empty list.
     * @throws ContentManagerException
     *             - an exception when the content is not found
     */
    public final ResultsWrapper<ContentDTO> segueSearch(final String searchString, @Nullable final String version,
            @Nullable final Map<String, List<String>> fieldsThatMustMatch, @Nullable final Integer startIndex,
            @Nullable final Integer limit) throws ContentManagerException {

        return contentService.segueSearch(searchString, version, fieldsThatMustMatch, startIndex, limit);
    }

    /**
     * This method provides a set of all tags for the live version of the content.
     * 
     * @param request
     *            so that we can determine whether we can make use of caching via etags.
     * @return a set of all tags used in the live version
     */
    @GET
    @Path("tags")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "List all tags currently in use.")
    public final Response getTagListByLiveVersion(@Context final Request request) {
        // Calculate the ETag on last modified date of tags list
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode()
                + "tagList".hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        Set<String> tags = this.contentManager.getTagsList(this.contentIndex);

        return Response.ok(tags).cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true)).tag(etag).build();
    }

    /**
     * This method provides a set of all units for the live version of the content.
     * 
     * @param request
     *            - so that we can set cache headers.
     * @return a set of all units used in the live version
     */
    @GET
    @Path("units")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "List all units currently in use by numeric questions.")
    public final Response getAllUnitsByLiveVersion(@Context final Request request) {
        // Calculate the ETag on last modified date of tags list
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode()
                + "unitsList".hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        Collection<String> units;
        units = this.contentManager.getAllUnits(this.contentIndex);

        return Response.ok(units).tag(etag).cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_DAY, true)).build();
    }
}
