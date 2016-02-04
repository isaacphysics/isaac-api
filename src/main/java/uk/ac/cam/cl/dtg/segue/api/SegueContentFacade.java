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

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ID_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_DAY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_HOUR;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_TEN_MINUTES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_RESULTS_LIMIT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TAGS_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import io.swagger.annotations.Api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;
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

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.configuration.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.api.client.util.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;

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

    private final ContentVersionController contentVersionController;
    private final UserAccountManager userManager;

    /**
     * @param properties
     *            - the fully configured properties loader for the api.
     * @param mapper
     *            - The Content mapper object used for polymorphic mapping of content objects.
     * @param segueConfigurationModule
     *            - The Guice DI configuration module.
     * @param contentVersionController
     *            - The content version controller used by the api.
     * @param userManager
     *            - The manager object responsible for users.
     * @param logManager
     *            - An instance of the log manager used for recording usage of the CMS.

     */
    @Inject
    public SegueContentFacade(final PropertiesLoader properties, final ContentMapper mapper,
            @Nullable final ISegueDTOConfigurationModule segueConfigurationModule,
            final ContentVersionController contentVersionController, final UserAccountManager userManager,
            final ILogManager logManager) {
        super(properties, logManager);

        this.contentVersionController = contentVersionController;
        this.userManager = userManager;

        if (Boolean.parseBoolean(properties.getProperty(Constants.FOLLOW_GIT_VERSION))) {
            try {
                // We need to do this to make sure we have an up to date content repo.
                log.info("Segue just initialized - Sending content index request "
                        + "so that we can service some content requests.");

                this.contentVersionController.triggerSyncJob().get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("Initial segue initialisation failure.");
            }
        }

    }
    

    /**
     * Get Content List By version from the database.
     * 
     * @param version
     *            - version of the content to use.
     * @param tags
     *            - Optional parameter for tags to search for.
     * @param type
     *            - Optional type parameter.
     * @param startIndex
     *            - Start index for results set.
     * @param limit
     *            - integer representing the maximum number of results to return.
     * @return Response object containing a ResultsWrapper
     */
    @GET
    @Path("{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getContentListByVersion(@PathParam("version") final String version,
            @QueryParam("tags") final String tags, @QueryParam("type") final String type,
            @QueryParam("start_index") final String startIndex, @QueryParam("limit") final String limit) {
        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatch = Maps.newHashMap();

        if (null != type) {
            fieldsToMatch.put(immutableEntry(BooleanOperator.AND, TYPE_FIELDNAME), Arrays.asList(type.split(",")));
        }
        if (null != tags) {
            fieldsToMatch.put(immutableEntry(BooleanOperator.AND, TAGS_FIELDNAME), Arrays.asList(tags.split(",")));
        }

        ResultsWrapper<ContentDTO> c;

        try {
            Integer resultsLimit = null;
            Integer startIndexOfResults = null;

            if (null != limit) {
                resultsLimit = Integer.parseInt(limit);
            }

            if (null != startIndex) {
                startIndexOfResults = Integer.parseInt(startIndex);
            }

            c = this.findMatchingContent(version, fieldsToMatch, startIndexOfResults, resultsLimit);

            return Response.ok(c).build();
        } catch (NumberFormatException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "Unable to convert one of the integer parameters provided into numbers. "
                            + "Params provided were: limit" + limit + " and startIndex " + startIndex, e).toResponse();
        }
    }

    /**
     * This method will return a ResultsWrapper<ContentDTO> based on the parameters supplied.
     * 
     * @param version
     *            - the version of the content to search. If null it will default to the current live version.
     * @param fieldsToMatch
     *            - Map representing fieldName -> field value mappings to search for. Note: tags is a special field name
     *            and the list will be split by commas.
     * @param startIndex
     *            - the start index for the search results.
     * @param limit
     *            - the max number of results to return.
     * @return Response containing a ResultsWrapper<ContentDTO> or a Response containing null if none found.
     */
    public final ResultsWrapper<ContentDTO> findMatchingContent(final String version,
            final Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatch,
            @Nullable final Integer startIndex, @Nullable final Integer limit) {
        IContentManager contentPersistenceManager = contentVersionController.getContentManager();

        String newVersion = this.contentVersionController.getLiveVersion();
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
            c = contentPersistenceManager.findByFieldNames(newVersion, fieldsToMatch, newStartIndex, newLimit);
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
     * This method will return a ResultsWrapper<ContentDTO> based on the parameters supplied. Providing the results in a
     * randomised order.
     * 
     * This method is the same as {@link #findMatchingContentRandomOrder(String, Map, Integer, Integer, Long)} but uses
     * a default random seed.
     * 
     * @param version
     *            - the version of the content to search. If null it will default to the current live version.
     * @param fieldsToMatch
     *            - Map representing fieldName -> field value mappings to search for. Note: tags is a special field name
     *            and the list will be split by commas.
     * @param startIndex
     *            - the start index for the search results.
     * @param limit
     *            - the max number of results to return.
     * @return Response containing a ResultsWrapper<ContentDTO> or a Response containing null if none found.
     */
    public final ResultsWrapper<ContentDTO> findMatchingContentRandomOrder(@Nullable final String version,
            final Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatch, final Integer startIndex,
            final Integer limit) {
        return this.findMatchingContentRandomOrder(version, fieldsToMatch, startIndex, limit, null);
    }

    /**
     * This method will return a ResultsWrapper<ContentDTO> based on the parameters supplied. Providing the results in a
     * randomised order.
     * 
     * @param version
     *            - the version of the content to search. If null it will default to the current live version.
     * @param fieldsToMatch
     *            - Map representing fieldName -> field value mappings to search for. Note: tags is a special field name
     *            and the list will be split by commas.
     * @param startIndex
     *            - the start index for the search results.
     * @param limit
     *            - the max number of results to return.
     * @param randomSeed
     *            - to allow some control over the random order of the results.
     * @return Response containing a ResultsWrapper<ContentDTO> or a Response containing null if none found.
     */
    public final ResultsWrapper<ContentDTO> findMatchingContentRandomOrder(@Nullable final String version,
            final Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatch, final Integer startIndex,
            final Integer limit, final Long randomSeed) {
        IContentManager contentPersistenceManager = contentVersionController.getContentManager();

        String newVersion = this.contentVersionController.getLiveVersion();
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
            c = contentPersistenceManager.findByFieldNamesRandomOrder(newVersion, fieldsToMatch, newStartIndex,
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
     * GetContentById from the database.
     * 
     * Currently this method will return a single Json Object containing all of the fields available to the object
     * retrieved from the database.
     * 
     * @param request
     *            - so that we can allow only staff users to use this generic endpoint.
     * @param version
     *            - the version of the datastore to query
     * @param id
     *            - our id not the dbid
     * @return Response object containing the serialized content object. (with no levels of recursion into the content)
     */
    @GET
    @Path("{version}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getContentById(@Context final HttpServletRequest request,
            @PathParam("version") final String version, @PathParam("id") final String id) {
        IContentManager contentPersistenceManager = contentVersionController.getContentManager();

        String newVersion = contentVersionController.getLiveVersion();

        if (version != null) {
            newVersion = version;
        }

        Content c = null;

        // Deserialize object into POJO of specified type, providing one exists.
        try {
            
            if (!super.isUserStaff(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "Only staff users can use this endpoint.")
                        .toResponse();
            }
            
            c = contentPersistenceManager.getContentDOById(newVersion, id);

            if (null == c) {
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "No content found with id: " + id);
                log.debug(error.getErrorMessage());
                return error.toResponse();
            }

        } catch (IllegalArgumentException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error while trying to map to a content object.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        return Response.ok(c).build();
    }

    /**
     * Rest end point that searches the content manager for some search string.
     * 
     * @param searchString
     *            - to pass to the search engine.
     * @param version
     *            - of the content to search.
     * @param types
     *            - a comma separated list of types to include in the search.
     * @param startIndex
     *            - the start index for the search results.
     * @param limit
     *            - the max number of results to return.
     * @return a response containing the search results (results wrapper) or an empty list.
     */
    @GET
    @Path("search/{version}/{searchString}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response search(@PathParam("searchString") final String searchString,
            @PathParam("version") final String version, @QueryParam("types") final String types,
            @QueryParam("start_index") final Integer startIndex, @QueryParam("limit") final Integer limit) {
        Map<String, List<String>> typesThatMustMatch = null;

        if (null != types) {
            typesThatMustMatch = Maps.newHashMap();
            typesThatMustMatch.put(TYPE_FIELDNAME, Arrays.asList(types.split(",")));
        }

        ResultsWrapper<ContentDTO> searchResults;
        try {
            searchResults = this.segueSearch(searchString, version, typesThatMustMatch, startIndex, limit);
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }

        return Response.ok(searchResults).build();
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
        int newLimit = DEFAULT_RESULTS_LIMIT;
        int newStartIndex = 0;
        String newVersion = contentVersionController.getLiveVersion();

        if (version != null) {
            newVersion = version;
        }

        if (limit != null) {
            newLimit = limit;
        }

        if (startIndex != null) {
            newStartIndex = startIndex;
        }

        IContentManager contentPersistenceManager = contentVersionController.getContentManager();

        ResultsWrapper<ContentDTO> searchResults = contentPersistenceManager.searchForContent(newVersion, searchString,
                fieldsThatMustMatch, newStartIndex, newLimit);

        return searchResults;
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
    public final Response getTagListByLiveVersion(@Context final Request request) {
        try {
            return this.getTagListByVersion(contentVersionController.getLiveVersion(), request);
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }
    }

    /**
     * This method provides a set of all tags for a given version of the content.
     * 
     * @param version
     *            of the site to provide the tag list from.
     * @param request
     *            so that we can determine whether we can make use of caching via etags.
     * @return a set of tags used in the specified version
     * @throws ContentManagerException
     *             - an exception when the content is not found
     */
    @GET
    @Path("tags/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getTagListByVersion(@PathParam("version") final String version,
            @Context final Request request) throws ContentManagerException {
        // Calculate the ETag on last modified date of tags list
        EntityTag etag = new EntityTag(this.contentVersionController.getLiveVersion().hashCode()
                + "tagList".hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        IContentManager contentPersistenceManager = contentVersionController.getContentManager();

        Set<String> tags = contentPersistenceManager.getTagsList(version);

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
    public final Response getAllUnitsByLiveVersion(@Context final Request request) {
        return this.getAllUnitsByVersion(request, contentVersionController.getLiveVersion());
    }

    /**
     * This method provides a set of all units for a given version.
     * 
     * @param request
     *            - so that we can set cache headers.
     * @param version
     *            of the site to provide the unit list from.
     * @return a set of units used in the specified version of the site
     */
    @GET
    @Path("units/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getAllUnitsByVersion(@Context final Request request,
            @PathParam("version") final String version) {
        // Calculate the ETag on last modified date of tags list
        EntityTag etag = new EntityTag(this.contentVersionController.getLiveVersion().hashCode()
                + "unitsList".hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        IContentManager contentPersistenceManager = contentVersionController.getContentManager();

        Collection<String> units;
        try {
            units = contentPersistenceManager.getAllUnits(version);
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }

        return Response.ok(units).tag(etag).cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_DAY, true)).build();
    }

    /**
     * getFileContent from the file store.
     * 
     * This method will return a byte array of the contents of a single file for the given path.
     * 
     * This is a temporary method for serving image files directly from git with a view that we can have a CDN cache
     * these for us.
     * 
     * This method will use etags to try and reduce load on the system and utilise browser caches.
     * 
     * @param request
     *            - so that we can do some caching.
     * @param version
     *            number - e.g. a sha
     * @param path
     *            - path of the image file
     * @return Response object containing the serialized content object. (with no levels of recursion into the content)
     *         or containing a SegueErrorResponse
     */
    @GET
    @Path("file_content/{version}/{path:.*}")
    @Produces("*/*")
    @Cache
    @GZIP
    public final Response getImageFileContent(@Context final Request request,
            @PathParam("version") final String version, @PathParam("path") final String path) {

        if (null == version || null == path || Files.getFileExtension(path).isEmpty()) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                    "Bad input to api call. Required parameter not provided.");
            log.debug(error.getErrorMessage());
            return error.toResponse();
        }

        // determine if we can use the cache if so return cached response.
        EntityTag etag = new EntityTag(version.hashCode() + path.hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag, NUMBER_SECONDS_IN_ONE_DAY);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        IContentManager gcm = contentVersionController.getContentManager();

        ByteArrayOutputStream fileContent = null;
        String mimeType = MediaType.WILDCARD;

        switch (Files.getFileExtension(path).toLowerCase()) {
            case "svg":
                mimeType = "image/svg+xml";
                break;

            case "jpg":
                mimeType = "image/jpeg";
                break;

            case "png":
                mimeType = "image/png";
                break;

            default:
                // if it is an unknown type return an error as they shouldn't be
                // using this endpoint.
                SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                        "Invalid file extension requested");
                log.debug(error.getErrorMessage());
                return error.toResponse(getCacheControl(NUMBER_SECONDS_IN_ONE_DAY, false), etag);
        }

        try {
            fileContent = gcm.getFileBytes(version, path);
        } catch (IOException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error reading from file repository", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (UnsupportedOperationException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Multiple files match the search path provided.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }

        if (null == fileContent) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Unable to locate the file: " + path);
            log.warn(error.getErrorMessage());
            return error.toResponse(getCacheControl(NUMBER_SECONDS_IN_TEN_MINUTES, false), etag);
        }

        return Response.ok(fileContent.toByteArray()).type(mimeType)
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_DAY, true))
                .tag(etag).build();
    }
    

    /**
     * Helper method to generate field to match requirements for search queries.
     * 
     * Assumes that everything is AND queries
     * 
     * @param fieldsToMatch
     *            - expects a map of the form fieldname -> list of queries to match
     * @return A map ready to be passed to a content provider
     */
    public static Map<Map.Entry<BooleanOperator, String>, List<String>> generateDefaultFieldToMatch(
            final Map<String, List<String>> fieldsToMatch) {
        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatchOutput = Maps.newHashMap();

        for (Map.Entry<String, List<String>> pair : fieldsToMatch.entrySet()) {
            Map.Entry<BooleanOperator, String> newEntry = null;
            if (pair.getKey().equals(ID_FIELDNAME)) {
                newEntry = immutableEntry(BooleanOperator.OR, pair.getKey());
            } else if (pair.getKey().equals(TYPE_FIELDNAME) && pair.getValue().size() > 1) {
                // special case of when you want to allow more than one
                newEntry = immutableEntry(BooleanOperator.OR, pair.getKey());
            } else {
                newEntry = immutableEntry(BooleanOperator.AND, pair.getKey());
            }

            fieldsToMatchOutput.put(newEntry, pair.getValue());
        }

        return fieldsToMatchOutput;
    }
}
