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
import static uk.ac.cam.cl.dtg.isaac.api.Constants.PROXY_PATH;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANSWER_QUESTION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_DAY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_HOUR;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_TEN_MINUTES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_THIRTY_DAYS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTACT_US_FORM_USED;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_RESULTS_LIMIT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ID_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_APP_ENVIRONMENT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TAGS_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import io.swagger.annotations.Api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

import org.elasticsearch.common.collect.Lists;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.configuration.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.Inject;

/**
 * Segue Api Facade
 * 
 * This class specifically caters for the Rutherford physics server and is expected to provide extended functionality to
 * the Segue api for use only on the Rutherford site.
 * 
 */
@Path("/")
@Api(value = "/")
public class SegueApiFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(SegueApiFacade.class);

    private static ContentMapper mapper;

    private ContentVersionController contentVersionController;
    private UserManager userManager;
    private QuestionManager questionManager;

    private EmailManager emailManager;


    /**
     * Constructor that allows pre-configuration of the segue api.
     * 
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
     * @param questionManager
     *            - A question manager object responsible for managing questions and augmenting questions with user
     *            information.
     * @param emailManager
     *            - An implementation of ICommunicator for sending communiques
     * @param logManager
     *            - An instance of the log manager used for recording usage of the CMS.
     */
    @Inject
    public SegueApiFacade(final PropertiesLoader properties, final ContentMapper mapper,
            @Nullable final ISegueDTOConfigurationModule segueConfigurationModule,
            final ContentVersionController contentVersionController, final UserManager userManager,
            final QuestionManager questionManager, final EmailManager emailManager,
            final ILogManager logManager) {
        super(properties, logManager);

        this.questionManager = questionManager;
        this.emailManager = emailManager;

        // We only want to do this if the mapper needs to be changed - I expect
        // the same instance to be injected from Guice each time.
        if (SegueApiFacade.mapper != mapper) {
            SegueApiFacade.mapper = mapper;

            // Add client specific data structures to the set of managed DTOs.
            if (null != segueConfigurationModule) {
                SegueApiFacade.mapper.registerJsonTypes(segueConfigurationModule.getContentDataTransferObjectMap());
            }
        }

        this.contentVersionController = contentVersionController;
        this.userManager = userManager;

        try {
            // We need to do this to make sure we have an up to date content repo.
            log.info("Segue just initialized - Sending content index request "
                    + "so that we can service some content requests.");

            this.contentVersionController.triggerSyncJob().get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Initial segue initialisation failure.");
        }
    }

    /**
     * Method to allow clients to log front-end specific behaviour in the database.
     * 
     * @param httpRequest
     *            - to enable retrieval of session information.
     * @param eventJSON
     *            - the event information to record as a json map <String, String>.
     * @return 200 for success or 400 for failure.
     */
    @POST
    @Path("log")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postLog(@Context final HttpServletRequest httpRequest, final Map<String, Object> eventJSON) {

        if (null == eventJSON || eventJSON.get(TYPE_FIELDNAME) == null) {
            log.error("Error during log operation, no event type specified. Event: " + eventJSON);
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                    "Unable to record log message as the log has no " + TYPE_FIELDNAME + " property.");
            return error.toResponse();
        }

        String eventType = (String) eventJSON.get(TYPE_FIELDNAME);
        // remove the type information as we don't need it.
        eventJSON.remove(TYPE_FIELDNAME);

        this.getLogManager().logEvent(this.userManager.getCurrentUser(httpRequest), httpRequest, eventType, eventJSON);

        return Response.ok().cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
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

        return Response.temporaryRedirect(new URI(uri.toString())).build();
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
    @Path("content/{version}")
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

        String newVersion = this.getLiveVersion();
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

        String newVersion = this.getLiveVersion();
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
    @Path("content/{version}/{id}")
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
    @Path("content/search/{version}/{searchString}")
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
    @Path("content/tags")
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
    @Path("content/tags/{version}")
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
    @Path("content/units")
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
    @Path("content/units/{version}")
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
    @Path("content/file_content/{version}/{path:.*}")
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
     * This method returns all versions as an immutable map version_list.
     * 
     * @param limit
     *            parameter if not null will set the limit of the number entries to return the default is the latest 10
     *            (indices starting at 0).
     * 
     * @return a Response containing an immutable map version_list: [x..y..]
     */
    @GET
    @Path("info/content_versions")
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

        IContentManager contentPersistenceManager = contentVersionController.getContentManager();

        List<String> allVersions = contentPersistenceManager.listAvailableVersions();
        List<String> limitedVersions = null;
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
    @Path("info/segue_version")
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
    @Path("info/log_event_types")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getLogEventTypes(@Context final Request request) {
        ImmutableMap<String, Collection<String>> result;
        try {
            result = new ImmutableMap.Builder<String, Collection<String>>().put(
                    "results", getLogManager().getAllEventTypes()).build();
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
    @Path("info/segue_environment")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getSegueEnvironment(@Context final Request request) {
        EntityTag etag = new EntityTag(this.contentVersionController.getLiveVersion().hashCode() + "");
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
    @Path("info/content_versions/live_version")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getLiveVersionInfo() {
        ImmutableMap<String, String> result = new ImmutableMap.Builder<String, String>().put("liveVersion",
                contentVersionController.getLiveVersion()).build();

        return Response.ok(result).build();
    }

    /**
     * This method return a json response containing version related information.
     * 
     * @return a version info as json response
     */
    @GET
    @Path("info/content_versions/cached")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getCachedVersions() {
        IContentManager contentPersistenceManager = contentVersionController.getContentManager();

        ImmutableMap<String, Collection<String>> result = new ImmutableMap.Builder<String, Collection<String>>().put(
                "cachedVersions", contentPersistenceManager.getCachedVersionList()).build();

        return Response.ok(result).build();
    }

    /**
     * get the live version of the content hosted by the api.
     * 
     * @return a string representing the live version.
     */
    public String getLiveVersion() {
        return contentVersionController.getLiveVersion();
    }

    /**
     * This is a library method that provides access to a users question attempts.
     * 
     * @param user
     *            - Anonymous user or registered user.
     * @return map of question attempts (QuestionPageId -> QuestionID -> [QuestionValidationResponse]
     * @throws SegueDatabaseException
     *             - If there is an error in the database call.
     */
    public final Map<String, Map<String, List<QuestionValidationResponse>>> getQuestionAttemptsBySession(
            final AbstractSegueUserDTO user) throws SegueDatabaseException {

        return this.questionManager.getQuestionAttemptsByUser(user);
    }

    /**
     * Library method to retrieve the current logged in user DTO.
     * 
     * NOTE: This should never be exposed as an endpoint.
     * 
     * @param request
     *            which may contain session information.
     * @return User DTO.
     * @throws NoUserLoggedInException
     *             - User is not logged in.
     */
    public RegisteredUserDTO getCurrentUser(final HttpServletRequest request) throws NoUserLoggedInException {
        return userManager.getCurrentRegisteredUser(request);
    }

    /**
     * Library method to retrieve the current logged in AbstractSegueUser DTO.
     * 
     * NOTE: This should never be exposed as an endpoint.
     * 
     * @param request
     *            which may contain session information.
     * @return User DTO.
     */
    public AbstractSegueUserDTO getCurrentUserIdentifier(final HttpServletRequest request) {
        return userManager.getCurrentUser(request);
    }

    /**
     * Library method to determine if a current user is currently logged in .
     * 
     * NOTE: This should never be exposed as an endpoint.
     * 
     * @param request
     *            which may contain session information.
     * @return True if a user is logged in, false if not.
     */
    public boolean hasCurrentUser(final HttpServletRequest request) {
        return userManager.isRegisteredUserLoggedIn(request);
    }

    /**
     * Record that a user has answered a question.
     * 
     * @param request
     *            - the servlet request so we can find out if it is a known user.
     * @param questionId
     *            that you are attempting to answer.
     * @param jsonAnswer
     *            - answer body which will be parsed as a Choice and then converted to a ChoiceDTO.
     * @return Response containing a QuestionValidationResponse object or containing a SegueErrorResponse .
     */
    @POST
    @Path("questions/{question_id}/answer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response answerQuestion(@Context final HttpServletRequest request,
            @PathParam("question_id") final String questionId, final String jsonAnswer) {
        if (null == jsonAnswer || jsonAnswer.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "No answer received.").toResponse();
        }

        Content contentBasedOnId;
        try {
            contentBasedOnId = contentVersionController.getContentManager().getContentDOById(
                    contentVersionController.getLiveVersion(), questionId);
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }

        Question question = null;
        if (contentBasedOnId instanceof Question) {
            question = (Question) contentBasedOnId;
        } else {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND,
                    "No question object found for given id: " + questionId);
            log.warn(error.getErrorMessage());
            return error.toResponse();
        }

        // decide if we have been given a list or an object and put it in a list
        // either way
        List<ChoiceDTO> answersFromClient = Lists.newArrayList();
        try {
            // convert single object into a list.
            Choice answerFromClient = mapper.getSharedContentObjectMapper().readValue(jsonAnswer, Choice.class);
            // convert to a DTO so that it strips out any untrusted data.
            ChoiceDTO answerFromClientDTO = mapper.getAutoMapper().map(answerFromClient, ChoiceDTO.class);

            answersFromClient.add(answerFromClientDTO);
        } catch (JsonMappingException | JsonParseException e) {
            log.info("Failed to map to any expected input...", e);
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Unable to map response to a "
                    + "Choice object so failing with an error", e);
            return error.toResponse();
        } catch (IOException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Unable to map response to a "
                    + "Choice object so failing with an error", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }

        // validate the answer.
        Response response;
        try {
            response = this.questionManager.validateAnswer(question, Lists.newArrayList(answersFromClient));

            AbstractSegueUserDTO currentUser = this.userManager.getCurrentUser(request);

            if (response.getEntity() instanceof QuestionValidationResponseDTO) {
                questionManager.recordQuestionAttempt(currentUser,
                        (QuestionValidationResponseDTO) response.getEntity());
            }

            this.getLogManager().logEvent(currentUser, request, ANSWER_QUESTION, response.getEntity());

            return response;

        } catch (IllegalArgumentException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Bad request - " + e.getMessage(), e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
    }

    /**
     * Endpoint that handles contact us form submissions.
     * 
     * @param form
     *            - Map containing the message details
     * @param request
     *            - for logging purposes.
     * @return - Successful response if no error occurs, otherwise error response
     */
    @POST
    @Path("contact/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response contactUs(final Map<String, String> form, @Context final HttpServletRequest request) {
        if (form.get("firstName") == null || form.get("lastName") == null || form.get("emailAddress") == null
                || form.get("subject") == null || form.get("message") == null) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Missing form details.");
            return error.toResponse();
        }
        
        // Build email
        StringBuilder builder = new StringBuilder();
        builder.append("The contact form has been submitted, please see the details below.\n\n");

        builder.append("First Name: ");
        builder.append(form.get("firstName"));
        builder.append("\n");

        builder.append("Last Name: ");
        builder.append(form.get("lastName"));
        builder.append("\n");

        builder.append("Email Address: ");
        builder.append(form.get("emailAddress"));
        builder.append("\n");

        builder.append("Subject: ");
        builder.append(form.get("subject"));
        builder.append("\n\n");

        builder.append("Message:\n");
        builder.append(form.get("message"));
        
        //TODO create contact form email template, and use that instead of string builder
        try {
			emailManager.sendContactUsFormEmail("Contact Isaac: " + form.get("subject"), builder.toString(), 
							this.getProperties().getProperty("MAIL_RECEIVERS"), form.get("emailAddress"));
			getLogManager().logEvent(userManager.getCurrentUser(request), request, 
							CONTACT_US_FORM_USED, builder.toString());
			return Response.ok().build();
		} catch (ContentManagerException e) {
            log.error("Content error has occurred", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "A content error has occurred.").toResponse();
		} catch (SegueDatabaseException e1) {
            log.error("Database error has occurred", e1);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "A database error has occurred.").toResponse();
		}

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

    /**
     * Library method for finding content by id prefix.
     * 
     * @deprecated this can be accessed using dependency injectable ContentVersionController or IContentManager
     * @param version
     *            - of the content to search for.
     * @param idPrefix
     *            - prefix / id to match against.
     * @return a results wrapper containing any matching content.
     * @throws ContentManagerException
     *             - an exception when the content is not found
     */
    @Deprecated
    public final ResultsWrapper<ContentDTO> searchByIdPrefix(final String version, final String idPrefix)
            throws ContentManagerException {
        return this.contentVersionController.getContentManager().getByIdPrefix(idPrefix, version, 0, -1);
    }

    /**
     * Library method to allow the question manager to be accessed.
     * 
     * @return question manager.
     */
    public QuestionManager getQuestionManager() {
        return this.questionManager;
    }

    /**
     * Utility method to allow related content to be populated as summary objects.
     * 
     * By default content summary objects may just have ids.
     * 
     * @param version
     *            - version of the content to use for augmentation.
     * @param contentToAugment
     *            - the content to augment.
     * @return content which has been augmented
     * @throws ContentManagerException
     *             - an exception when the content is not found
     */
    public ContentDTO augmentContentWithRelatedContent(final String version, final ContentDTO contentToAugment)
            throws ContentManagerException {
        return this.contentVersionController.getContentManager().populateRelatedContent(version, contentToAugment);
    }
}
