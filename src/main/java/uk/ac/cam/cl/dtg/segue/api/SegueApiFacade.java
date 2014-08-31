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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
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

import uk.ac.cam.cl.dtg.segue.api.Constants.EnvironmentType;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.DuplicateAccountException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.FailedToHashPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidTokenException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.CommunicationException;
import uk.ac.cam.cl.dtg.segue.comm.ICommunicator;
import uk.ac.cam.cl.dtg.segue.configuration.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.IAppDataManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.Inject;

/**
 * Segue Api Facade
 * 
 * This class specifically caters for the Rutherford physics server and is
 * expected to provide extended functionality to the Segue api for use only on
 * the Rutherford site.
 * 
 */
@Path("/")
public class SegueApiFacade {
	private static final Logger log = LoggerFactory
			.getLogger(SegueApiFacade.class);

	private static ContentMapper mapper;

	private ContentVersionController contentVersionController;
	private UserManager userManager;
	private QuestionManager questionManager;
	private PropertiesLoader properties;
	private ICommunicator communicator;
	private ILogManager logManager;

	/**
	 * Constructor that allows pre-configuration of the segue api.
	 * 
	 * @param properties
	 *            - the fully configured properties loader for the api.
	 * @param mapper
	 *            - The Content mapper object used for polymorphic mapping of
	 *            content objects.
	 * @param segueConfigurationModule
	 *            - The Guice DI configuration module.
	 * @param contentVersionController
	 *            - The content version controller used by the api.
	 * @param userManager
	 *            - The manager object responsible for users.
	 * @param questionManager
	 *            - A question manager object responsible for managing questions
	 *            and augmenting questions with user information.
	 * @param communicator
	 *            - An implementation of ICommunicator for sending communiques
	 * @param logManager
	 *            - An instance of the log manager used for recording usage of the CMS.          
	 */
	@Inject
	public SegueApiFacade(
			final PropertiesLoader properties,
			final ContentMapper mapper,
			@Nullable final ISegueDTOConfigurationModule segueConfigurationModule,
			final ContentVersionController contentVersionController,
			final UserManager userManager,
			final QuestionManager questionManager,
			final ICommunicator communicator,
			final ILogManager logManager) {

		this.properties = properties;
		this.questionManager = questionManager;
		this.communicator = communicator;

		// We only want to do this if the mapper needs to be changed - I expect
		// the same instance to be injected from Guice each time.
		if (SegueApiFacade.mapper != mapper) {
			SegueApiFacade.mapper = mapper;

			// Add client specific data structures to the set of managed DTOs.
			if (null != segueConfigurationModule) {
				SegueApiFacade.mapper
						.registerJsonTypes(segueConfigurationModule
								.getContentDataTransferObjectMap());
			}
		}

		this.contentVersionController = contentVersionController;
		this.userManager = userManager;

		this.logManager = logManager;
		
		// Check if we want to get the latest from git each time a request is
		// made from segue. - Will add overhead
		if (Boolean.parseBoolean(this.properties
				.getProperty(Constants.FOLLOW_GIT_VERSION))) {
			log.info("Segue just initialized - Sending content index request "
					+ "so that we can service some content requests.");
			this.contentVersionController.triggerSyncJob();
		}
	}

	/**
	 * Method to allow clients to log frontend specific behaviour in the backend.
	 * 
	 * @param httpRequest - to enable retrieval of session information.
	 * @param eventJSON - the event information to record as a json map <String, String>.
	 * @return 200 for success or 400 for failure.
	 */
	@POST
	@Path("log")
	@Consumes("application/json")
	public Response postLog(@Context final HttpServletRequest httpRequest,
			final Map<String, String> eventJSON) {
		
		if (null == eventJSON || eventJSON.get(Constants.TYPE_FIELDNAME) == null) {
			log.error("Error during log operation, no event type specified. Event: " + eventJSON);
			SegueErrorResponse error = new SegueErrorResponse(
					Status.BAD_REQUEST,
					"Unable to record log message as the log has no " + Constants.TYPE_FIELDNAME + " property.");
			return error.toResponse();
		}
		
		String eventType = eventJSON.get(Constants.TYPE_FIELDNAME);
		// remove the type information as we don't need it.
		eventJSON.remove(Constants.TYPE_FIELDNAME);
		
		this.logManager.logEvent(httpRequest, eventType, eventJSON);

		return Response.ok().build();
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
	 *            - integer representing the maximum number of results to
	 *            return.
	 * @return Response object containing a ResultsWrapper
	 */
	@GET
	@Produces("application/json")
	@Path("content/{version}")
	public final Response getContentListByVersion(
			@PathParam("version") final String version,
			@QueryParam("tags") final String tags,
			@QueryParam("type") final String type,
			@QueryParam("start_index") final String startIndex,
			@QueryParam("limit") final String limit) {
		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch = Maps
				.newHashMap();

		if (null != type) {
			fieldsToMatch.put(com.google.common.collect.Maps.immutableEntry(
					Constants.BooleanOperator.AND, Constants.TYPE_FIELDNAME),
					Arrays.asList(type.split(",")));
		}
		if (null != tags) {
			fieldsToMatch.put(com.google.common.collect.Maps.immutableEntry(
					Constants.BooleanOperator.AND, Constants.TAGS_FIELDNAME),
					Arrays.asList(tags.split(",")));
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

			c = this.findMatchingContent(version, fieldsToMatch,
					startIndexOfResults, resultsLimit);

			return Response.ok(c).build();
		} catch (NumberFormatException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"Unable to convert one of the integer parameters provided into numbers. "
							+ "Params provided were: limit" + limit
							+ " and startIndex " + startIndex, e).toResponse();
		}
	}

	/**
	 * This method will return a ResultsWrapper<ContentDTO> based on the parameters supplied.
	 * 
	 * @param version
	 *            - the version of the content to search. If null it will
	 *            default to the current live version.
	 * @param fieldsToMatch
	 *            - Map representing fieldName -> field value mappings to search
	 *            for. Note: tags is a special field name and the list will be
	 *            split by commas.
	 * @param startIndex
	 *            - the start index for the search results.
	 * @param limit
	 *            - the max number of results to return.
	 * @return Response containing a ResultsWrapper<ContentDTO> or a Response containing
	 *         null if none found.
	 */
	public final ResultsWrapper<ContentDTO> findMatchingContent(
			String version,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			@Nullable Integer startIndex, @Nullable Integer limit) {
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		if (null == version) {
			version = contentVersionController.getLiveVersion();
		}

		if (null == limit) {
			limit = Constants.DEFAULT_RESULTS_LIMIT;
		}

		if (null == startIndex) {
			startIndex = 0;
		}

		ResultsWrapper<ContentDTO> c = null;

		// Deserialize object into POJO of specified type, providing one exists.
		try {
			c = contentPersistenceManager.findByFieldNames(version,
					fieldsToMatch, startIndex, limit);
		} catch (IllegalArgumentException e) {
			log.error("Unable to map content object.", e);
			throw e;
		}

		return c;
	}

	/**
	 * This method will return a ResultsWrapper<ContentDTO> based on the parameters supplied.
	 * Providing the results in a randomised order.
	 * 
	 * @param version
	 *            - the version of the content to search. If null it will
	 *            default to the current live version.
	 * @param fieldsToMatch
	 *            - Map representing fieldName -> field value mappings to search
	 *            for. Note: tags is a special field name and the list will be
	 *            split by commas.
	 * @param startIndex
	 *            - the start index for the search results.
	 * @param limit
	 *            - the max number of results to return.
	 * @return Response containing a ResultsWrapper<ContentDTO> or a Response containing
	 *         null if none found.
	 */
	public final ResultsWrapper<ContentDTO> findMatchingContentRandomOrder(
			@Nullable String version,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			Integer startIndex, Integer limit) {
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		if (null == version) {
			version = contentVersionController.getLiveVersion();
		}

		if (null == limit) {
			limit = Constants.DEFAULT_RESULTS_LIMIT;
		}

		if (null == startIndex) {
			startIndex = 0;
		}

		ResultsWrapper<ContentDTO> c = null;

		// Deserialize object into POJO of specified type, providing one exists.
		try {
			c = contentPersistenceManager.findByFieldNamesRandomOrder(version,
					fieldsToMatch, startIndex, limit);
		} catch (IllegalArgumentException e) {
			log.error("Unable to map content object.", e);
			throw e;
		}

		return c;
	}

	/**
	 * GetContentById from the database.
	 * 
	 * Currently this method will return a single Json Object containing all of
	 * the fields available to the object retrieved from the database.
	 * 
	 * @param version
	 *            - the version of the datastore to query
	 * @param id
	 *            - our id not the dbid
	 * @return Response object containing the serialized content object. (with
	 *         no levels of recursion into the content)
	 */
	@GET
	@Produces("application/json")
	@Path("content/{version}/{id}")
	public final Response getContentById(
			@PathParam("version") String version,
			@PathParam("id") final String id) {
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		if (null == version) {
			version = contentVersionController.getLiveVersion();
		}

		Content c = null;

		// Deserialize object into POJO of specified type, providing one exists.
		try {
			c = contentPersistenceManager.getById(id,
					contentVersionController.getLiveVersion());

			if (null == c) {
				SegueErrorResponse error = new SegueErrorResponse(
						Status.NOT_FOUND, "No content found with id: " + id);
				log.debug(error.getErrorMessage());
				return error.toResponse();
			}

		} catch (IllegalArgumentException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.INTERNAL_SERVER_ERROR,
					"Error while trying to map to a content object.", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		}

		return Response.ok().entity(c).build();
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
	 * @return a response containing the search results (results wrapper) or an
	 *         empty list.
	 */
	@GET
	@Produces("application/json")
	@Path("content/search/{version}/{searchString}")
	public final Response search(
			@PathParam("searchString") final String searchString,
			@PathParam("version") final String version,
			@QueryParam("types") final String types) {

		Map<String, List<String>> typesThatMustMatch = null;

		if (null != types) {
			typesThatMustMatch = Maps.newHashMap();
			typesThatMustMatch.put(Constants.TYPE_FIELDNAME,
					Arrays.asList(types.split(",")));
		}

		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		ResultsWrapper<ContentDTO> searchResults = contentPersistenceManager
				.searchForContent(contentVersionController.getLiveVersion(),
						searchString, typesThatMustMatch);

		return Response.ok(searchResults).build();
	}

	/**
	 * Rest end point that searches the content manager for some search string.
	 * Using the live version of the content as the default.
	 * 
	 * @param searchString
	 *            - to pass to the search engine.
	 * @param types
	 *            - a comma separated list of types to include in the search.
	 * @return a response containing the search results (results wrapper) or an
	 *         empty list.
	 */
	@GET
	@Produces("application/json")
	@Path("content/search/{searchString}")
	public final Response search(
			@PathParam("searchString") final String searchString,
			@QueryParam("types") final String types) {

		return this.search(searchString, this.getLiveVersion(), types);
	}

	/**
	 * This method provides a set of all tags for the live version of the
	 * content.
	 * 
	 * @param request
	 *            so that we can determine whether we can make use of caching
	 *            via etags.
	 * @return a set of all tags used in the live version
	 */
	@GET
	@Produces("application/json")
	@Path("content/tags")
	public final Response getTagListByLiveVersion(@Context final Request request) {
		return this.getTagListByVersion(contentVersionController
				.getLiveVersion(), request);
	}

	/**
	 * This method provides a set of all tags for a given version of the
	 * content.
	 * 
	 * @param version
	 *            of the site to provide the tag list from.
	 * @param request
	 *            so that we can determine whether we can make use of caching
	 *            via etags.
	 * @return a set of tags used in the specified version
	 */
	@GET
	@Produces("application/json")
	@Path("content/tags/{version}")
	public final Response getTagListByVersion(
			@PathParam("version") final String version,
			@Context final Request request) {
		// Calculate the ETag on last modified date of tags list
		EntityTag etag = new EntityTag(this.contentVersionController.getLiveVersion().hashCode()
				+ "tagList".hashCode() + "");
		
		Response cachedResponse = generateCachedResponse(request, etag);
		
		if (cachedResponse != null) {
			return cachedResponse;
		}
		
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		Set<String> tags = contentPersistenceManager.getTagsList(version);

		return Response.ok().entity(tags).cacheControl(getCacheControl()).tag(etag).build();
	}

	/**
	 * This method provides a set of all units for the 
	 * live version of the content
	 * 
	 * TODO: This is isaac-specific, so should not be in segue.
	 * 
	 * @param request - so that we can set cache headers.
	 * @return a set of all units used in the live version
	 */
	@GET
	@Produces("application/json")
	@Path("content/units")
	public final Response getAllUnitsByLiveVersion(@Context final Request request) {
		return this.getAllUnitsByVersion(request, contentVersionController
				.getLiveVersion());
	}	
	
	/**
	 * This method provides a set of all units for a given version.
	 * 
	 * TODO: This is isaac-specific, so should not be in segue.
	 * 
	 * @param request - so that we can set cache headers.
	 * @param version of the site to provide the unit list from.
	 * @return a set of units used in the specified version of the site
	 */
	@GET
	@Produces("application/json")
	@Path("content/units/{version}")
	public final Response getAllUnitsByVersion(
			@Context final Request request,
			@PathParam("version") final String version) {
		// Calculate the ETag on last modified date of tags list
		EntityTag etag = new EntityTag(this.contentVersionController.getLiveVersion().hashCode()
				+ "unitsList".hashCode() + "");
		
		Response cachedResponse = generateCachedResponse(request, etag);
		
		if (cachedResponse != null) {
			return cachedResponse;
		}
		
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		Collection<String> units = contentPersistenceManager.getAllUnits(version);

		return Response.ok().entity(units).tag(etag).cacheControl(getCacheControl()).build();
	}

	/**
	 * getFileContent from the file store.
	 * 
	 * This method will return a byte array of the contents of a single file for
	 * the given path.
	 * 
	 * This is a temporary method for serving image files directly from git with
	 * a view that we can have a CDN cache these for us.
	 * 
	 * This method will use etags to try and reduce load on the system and
	 * utilise browser caches.
	 * 
	 * @param request
	 *            - so that we can do some caching.
	 * @param version
	 *            number - e.g. a sha
	 * @param path
	 *            - path of the image file
	 * @return Response object containing the serialized content object. (with
	 *         no levels of recursion into the content) or containing a
	 *         SegueErrorResponse
	 */
	@GET
	@Produces("*/*")
	@Path("content/file_content/{version}/{path:.*}")
	@Cache
	public final Response getImageFileContent(
			@Context final Request request,
			@PathParam("version") final String version,
			@PathParam("path") final String path) {
		
		if (null == version || null == path
				|| Files.getFileExtension(path).isEmpty()) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.BAD_REQUEST,
					"Bad input to api call. Required parameter not provided.");
			log.debug(error.getErrorMessage());
			return error.toResponse();
		}
		
		// determine if we can use the cache if so return cached response.
		EntityTag etag = new EntityTag(version.hashCode() + path.hashCode() + "");
		Response cachedResponse = generateCachedResponse(request, etag, Constants.CACHE_FOR_ONE_DAY);
		
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
				SegueErrorResponse error = new SegueErrorResponse(
						Status.BAD_REQUEST, "Invalid file extension requested");
				log.debug(error.getErrorMessage());
				return error.toResponse();
		}

		try {
			fileContent = gcm.getFileBytes(version, path);
		} catch (IOException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.INTERNAL_SERVER_ERROR,
					"Error reading from file repository", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		} catch (UnsupportedOperationException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.INTERNAL_SERVER_ERROR,
					"Multiple files match the search path provided.", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		}

		if (null == fileContent) {
			SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND,
					"Unable to locate the file: " + path);
			log.error(error.getErrorMessage());
			return error.toResponse();
		}

		return Response.ok(fileContent.toByteArray()).type(mimeType)
				.cacheControl(getCacheControl(Constants.CACHE_FOR_ONE_DAY)).tag(etag)
				.build();
	}

	/**
	 * This method will allow the live version served by the site to be changed.
	 * 
	 * @param request - to help determine access rights.
	 * @param version
	 *            - version to use as updated version of content store.
	 * @return Success shown by returning the new liveSHA or failed message
	 *         "Invalid version selected".
	 */
	@PUT
	@Produces("application/json")
	@Path("admin/live_version/{version}")
	public final synchronized Response changeLiveVersion(
			@Context final HttpServletRequest request,
			@PathParam("version") final String version) {

		try {
			if (this.userManager.isUserAnAdmin(request)) {
				IContentManager contentPersistenceManager = contentVersionController
						.getContentManager();

				List<String> availableVersions = contentPersistenceManager
						.listAvailableVersions();

				if (!availableVersions.contains(version)) {
					SegueErrorResponse error = new SegueErrorResponse(
							Status.BAD_REQUEST, "Invalid version selected: " + version);
					log.warn(error.getErrorMessage());
					return error.toResponse();
				}

				String newVersion = contentVersionController.triggerSyncJob(version);

				contentVersionController.setLiveVersion(newVersion);
				log.info("Live version of the site changed to: " + newVersion);

				return Response.ok().entity("live Version changed to " + version)
						.build();
			} else {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be logged in as an admin to access this function.")
						.toResponse();
			}
		} catch (NoUserLoggedInException e) {
			return new SegueErrorResponse(Status.UNAUTHORIZED,
					"You must be logged in to access this function.")
					.toResponse();
		}
	}

	/**
	 * This method returns all versions as an immutable map version_list.
	 * 
	 * @param limit
	 *            parameter if not null will set the limit of the number entries
	 *            to return the default is the latest 10 (indices starting at
	 *            0).
	 * 
	 * @return a Response containing an immutable map version_list: [x..y..]
	 */
	@GET
	@Produces("application/json")
	@Path("info/content_versions")
	public final Response getVersionsList(
			@QueryParam("limit") final String limit) {
		// try to parse the integer
		Integer limitAsInt = null;

		try {
			if (null == limit) {
				limitAsInt = Constants.DEFAULT_RESULTS_LIMIT;
			} else {
				limitAsInt = Integer.parseInt(limit);
			}
		} catch (NumberFormatException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.BAD_REQUEST,
					"The limit requested is not a valid number.");
			log.debug(error.getErrorMessage());
			return error.toResponse();
		}

		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		List<String> allVersions = contentPersistenceManager
				.listAvailableVersions();
		List<String> limitedVersions = null;
		try {
			limitedVersions = new ArrayList<String>(allVersions.subList(0,
					limitAsInt));
		} catch (IndexOutOfBoundsException e) {
			// they have requested a stupid limit so just give them what we have
			// got.
			limitedVersions = allVersions;
			log.debug("Bad index requested for version number."
					+ " Using maximum index instead.");
		} catch (IllegalArgumentException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.BAD_REQUEST, "Invalid limit specified: " + limit, e);
			log.debug(error.getErrorMessage(), e);
			return error.toResponse();
		}

		ImmutableMap<String, Collection<String>> result 
			= new ImmutableMap.Builder<String, Collection<String>>()
				.put("version_list", limitedVersions).build();

		return Response.ok().entity(result).build();
	}

	/**
	 * Gets the current version of the segue application.
	 * 
	 * @return segue version as a string wrapped in a response.
	 */
	@GET
	@Produces("application/json")
	@Path("info/segue_version")
	public final Response getSegueAppVersion() {
		ImmutableMap<String, String> result = new ImmutableMap.Builder<String, String>()
				.put("segueVersion",
						this.properties
								.getProperty(Constants.SEGUE_APP_VERSION))
				.build();

		return Response.ok(result).build();
	}
	
	/**
	 * Gets the current mode that the segue application is running in.
	 * 
	 * @param request - for cache control purposes.
	 * @return segue mode as a string wrapped in a response. e.g {segueMode:DEV}
	 */
	@GET
	@Produces("application/json")
	@Path("info/segue_environment")
	public final Response getSegueEnvironment(@Context final Request request) {
		EntityTag etag = new EntityTag(this.contentVersionController.getLiveVersion().hashCode() + "");
		Response cachedResponse = generateCachedResponse(request, etag);
		if (cachedResponse != null) {
			return cachedResponse;
		}
		
		ImmutableMap<String, String> result = new ImmutableMap.Builder<String, String>()
				.put("segueEnvironment",
						this.properties
								.getProperty(Constants.SEGUE_APP_ENVIRONMENT))
				.build();

		return Response.ok(result).cacheControl(this.getCacheControl(Constants.CACHE_FOR_ONE_DAY)).tag(etag)
				.build();
	}

	/**
	 * This method return a json response containing version related
	 * information.
	 * 
	 * @return a version info as json response
	 */
	@GET
	@Produces("application/json")
	@Path("info/content_versions/live_version")
	public final Response getLiveVersionInfo() {
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		ImmutableMap<String, String> result = new ImmutableMap.Builder<String, String>()
				.put("liveVersion", contentVersionController.getLiveVersion())
				.put("latestKnownVersion",
						contentPersistenceManager.getLatestVersionId()).build();

		return Response.ok(result).build();
	}

	/**
	 * This method return a json response containing version related
	 * information.
	 * 
	 * @return a version info as json response
	 */
	@GET
	@Produces("application/json")
	@Path("info/content_versions/cached")
	public final Response getCachedVersions() {
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		ImmutableMap<String, Collection<String>> result 
			= new ImmutableMap.Builder<String, Collection<String>>()
				.put("cachedVersions",
						contentPersistenceManager.getCachedVersionList())
				.build();

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
	 * Get the details of the currently logged in user.
	 * 
	 * @param request - request information used for caching.
	 * @param httpServletRequest
	 *            - the request which may contain session information.
	 * @return Returns the current user DTO if we can get it or null response if
	 *         we can't. It will be a 204 No Content
	 */
	@GET
	@Produces("application/json")
	@Path("users/current_user")
	@GZIP
	public Response getCurrentUserEndpoint(
			@Context final Request request,
			@Context final HttpServletRequest httpServletRequest) {		
		try {
			RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);
			
			// Calculate the ETag based on User we just retrieved from the DB
			EntityTag etag = new EntityTag("currentUser".hashCode() + currentUser.hashCode() + "");
			Response cachedResponse = generateCachedResponse(request, etag, Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK);
			if (cachedResponse != null) {
				return cachedResponse;
			}
			
			return Response.ok(currentUser).tag(etag)
					.cacheControl(this.getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK)).build();
		} catch (NoUserLoggedInException e) {
			return new SegueErrorResponse(Status.UNAUTHORIZED,
					"Unable to retrieve the current user as no user is currently logged in.")
					.toResponse();
		}
	}

	/**
	 * This is a library method that provides access to a users question attempts.
	 * @param user - Anonymous user or registered user.
	 * @return map of question attempts (QuestionPageId -> QuestionID -> [QuestionValidationResponse]
	 * @throws SegueDatabaseException - If there is an error in the database call.
	 */
	public final Map<String, Map<String, List<QuestionValidationResponse>>> getQuestionAttemptsBySession(
			final AbstractSegueUserDTO user) throws SegueDatabaseException {

		return this.userManager.getQuestionAttemptsByUser(user);
	}
	
	/**
	 * This method allows users to create a local account or update their
	 * settings.
	 * 
	 * @param request
	 *            - the http request of the user wishing to authenticate
	 * @param userObjectString
	 *            - object containing all user account information including
	 *            passwords.
	 * @return the updated users object.
	 */
	@POST
	@Produces("application/json")
	@Path("users/")
	@Consumes("application/json")
	public final Response createOrUpdateUserSettings(
			@Context final HttpServletRequest request, final String userObjectString) {

		ObjectMapper tempObjectMapper = new ObjectMapper();
		tempObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		RegisteredUser userObject;
		try {
			userObject = tempObjectMapper.readValue(userObjectString, RegisteredUser.class);
		} catch (IOException e1) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"Unable to parser the user object you provided.")
					.toResponse();
		}
		
		if (null == userObject) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"No user settings provided.").toResponse();
		}


		// determine if this is intended to be an update or create.
		// if it is an update we need to do some security checks.
		if (userObject.getDbId() != null) {
			try {
				RegisteredUserDTO currentUser = this.getCurrentUser(request);
				if (!currentUser.getDbId().equals(userObject.getDbId())) {
					return new SegueErrorResponse(Status.FORBIDDEN,
							"You cannot change someone elses' user settings.")
							.toResponse();
				}
				
			} catch (NoUserLoggedInException e) {
				return new SegueErrorResponse(Status.UNAUTHORIZED,
						"You must be logged in to change your user settings.")
						.toResponse();
			}
		}

		try {
			RegisteredUserDTO savedUser = userManager.createOrUpdateUserObject(userObject);
			// we need to tell segue that the user who we just created is the one that is logged in.
			this.userManager.createSession(request, savedUser.getDbId());
			
			return Response.ok(savedUser).build();
		} catch (InvalidPasswordException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"Invalid password. You cannot have an empty password.")
					.toResponse();
		} catch (FailedToHashPasswordException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Unable to set a password.").toResponse();
		} catch (MissingRequiredFieldException e) {
			log.warn("Missing field during update operation. ", e);
			return new SegueErrorResponse(
					Status.BAD_REQUEST,
					"You are missing a required field. "
					+ "Please make sure you have specified all mandatory fields in your response.")
					.toResponse();
		} catch (DuplicateAccountException e) {
			return new SegueErrorResponse(
					Status.BAD_REQUEST,
					"Duplicate key found. An existing account may "
					+ "already exist with the e-mail address specified.")
					.toResponse();
		} catch (SegueDatabaseException e) {
			String errorMsg = "Unable to set a password, due to an internal database error.";
			log.error(errorMsg, e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					errorMsg).toResponse();
		} catch (AuthenticationProviderMappingException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Unable to map to a known authenticator. The provider: is unknown")
					.toResponse();
		}
	}

	/**
	 * Library method to retrieve the current logged in user DTO.
	 * 
	 * NOTE: This should never be exposed as an endpoint.
	 * 
	 * @param request
	 *            which may contain session information.
	 * @return User DTO.
	 * @throws NoUserLoggedInException - User is not logged in.
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
	 * End point that allows a local user to generate a password reset request.
	 * 
	 * Step 1 of password reset process - send user an e-mail
	 *
	 * @param userObject - A user object containing the email of the user requesting a reset
	 * @return a successful response regardless of whether the email exists
	 *         or an error code if there is a technical fault
	 */
	@POST
	@Path("users/resetpassword")
	@Consumes("application/json")
	public final Response generatePasswordResetToken(final RegisteredUserDTO userObject) {
		if (null == userObject) {
			log.debug("User is null");
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"No user settings provided.").toResponse();
		}

		try {
			userManager.resetPasswordRequest(userObject);

			return Response.ok().build();
		} catch (CommunicationException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.INTERNAL_SERVER_ERROR,
					"Error sending reset message.", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		} catch (Exception e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.INTERNAL_SERVER_ERROR,
					"Error generate password reset token.", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		}
	}

	/**
	 * End point that verifies whether or not a password reset token is valid.
	 * 
	 * Optional Step 2 - validate token is correct
	 * 
	 * @param token - A password reset token
	 * @return Success if the token is valid, otherwise returns not found
	 */
	@GET
	@Produces("application/json")
	@Path("users/resetpassword/{token}")
	public final Response validatePasswordResetRequest(@PathParam("token") final String token) {
		try {
			if (userManager.validatePasswordResetToken(token)) {
				return Response.ok().build();
			}
		} catch (SegueDatabaseException e) {
			log.error("Internal database error, while validating Password Reset Request.", e);
			SegueErrorResponse error = new SegueErrorResponse(
					Status.INTERNAL_SERVER_ERROR,
					"Database error has occurred. Unable to access token list.");
			return error.toResponse();
		}

		SegueErrorResponse error = new SegueErrorResponse(
				Status.NOT_FOUND,
				"Invalid password reset token.");
		log.debug(String.format("Invalid password reset token: %s", token));
		return error.toResponse();
	}

	/**
	 * Final step of password reset process. Change password.
	 * 
	 * @param token - A password reset token
	 * @param userObject - A user object containing password information.
	 * @return successful response.
	 */
	@POST
	@Path("users/resetpassword/{token}")
	@Consumes("application/json")
	public final Response resetPassword(@PathParam("token") final String token, final RegisteredUser userObject) {
		try {
			userManager.resetPassword(token, userObject);
		} catch (InvalidTokenException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.BAD_REQUEST,
					"Invalid password reset token.");
			return error.toResponse();
		} catch (InvalidPasswordException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.BAD_REQUEST,
					"No password supplied.");
			return error.toResponse();
		} catch (SegueDatabaseException e) {
			String errorMsg = "Database error has occurred during reset password process. Please try again later"; 
			log.error(errorMsg, e);
			SegueErrorResponse error = new SegueErrorResponse(
					Status.INTERNAL_SERVER_ERROR,
					errorMsg);
			return error.toResponse();
		}

		return Response.ok().build();
	}

	/**
	 * This is the initial step of the authentication process.
	 * 
	 * @param request
	 *            - the http request of the user wishing to authenticate
	 * @param signinProvider
	 *            - string representing the supported auth provider so that we
	 *            know who to redirect the user to.
	 * @return Redirect response to the auth providers site.
	 */
	@GET
	@Produces("application/json")
	@Path("auth/{provider}/authenticate")
	public final Response authenticate(
			@Context final HttpServletRequest request,
			@PathParam("provider") final String signinProvider) {

		return userManager.authenticate(request, signinProvider);
	}
	
	/**
	 * Link existing user to provider.
	 * 
	 * @param request
	 *            - the http request of the user wishing to authenticate
	 * @param authProviderAsString
	 *            - string representing the supported auth provider so that we
	 *            know who to redirect the user to.
	 *            
	 * @return a redirect to where the client asked to be redirected to.
	 */
	@GET
	@Path("auth/{provider}/link")
	@Produces("application/json")	
	public final Response linkExistingUserToProvider(@Context final HttpServletRequest request,
			@PathParam("provider") final String authProviderAsString) {

		if (!this.hasCurrentUser(request)) {
			return new SegueErrorResponse(Status.UNAUTHORIZED,
					"Unable to retrieve the current user as no user is currently logged in.")
					.toResponse();			
		}
	
		return this.userManager.initiateLinkAccountToUserFlow(request, authProviderAsString);
	}
	
	/**
	 * End point that allows the user to logout - i.e. destroy our cookie.
	 * 
	 * @param request
	 *            - request so we can authenticate the user.
	 * @param authProviderAsString
	 *            - the provider to dis-associate.
	 * @return successful response.
	 */
	@DELETE
	@Path("auth/{provider}/link")
	@Produces("application/json")
	public final Response unlinkUserFromProvider(@Context final HttpServletRequest request,
			@PathParam("provider") final String authProviderAsString) {
		
		try {
			RegisteredUserDTO user = this.getCurrentUser(request);
			this.userManager.unlinkUserFromProvider(user, authProviderAsString);
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Unable to remove account due to a problem with the database.", e)
					.toResponse();	
		} catch (MissingRequiredFieldException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"Unable to remove account as this will mean that the user cannot login again in the future.", e)
					.toResponse();	
		} catch (NoUserLoggedInException e) {
			return new SegueErrorResponse(Status.UNAUTHORIZED,
					"Unable to retrieve the current user as no user is currently logged in.")
					.toResponse();		
		} catch (AuthenticationProviderMappingException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"Unable to map to a known authenticator. The provider: " + authProviderAsString
							+ " is unknown").toResponse();
		}
		
		return Response.status(Status.NO_CONTENT).build();
	}	

	/**
	 * This is the callback url that auth providers should use to send us
	 * information about users.
	 * 
	 * @param request
	 *            - http request from user
	 * @param signinProvider
	 *            - requested signing provider string
	 * @return Redirect response to send the user to the home page.
	 */
	@GET
	@Produces("application/json")
	@Path("auth/{provider}/callback")
	public final Response authenticationCallback(
			@Context final HttpServletRequest request,
			@PathParam("provider") final String signinProvider) {

		return userManager.authenticateCallback(request, signinProvider);
	}

	/**
	 * This is the initial step of the authentication process.
	 * 
	 * @param request
	 *            - the http request of the user wishing to authenticate
	 * @param signinProvider
	 *            - string representing the supported auth provider so that we
	 *            know who to redirect the user to.
	 * @param credentials
	 *            - optional field for local authentication only. Credentials
	 *            should be specified within a user object. e.g. email and
	 *            password.
	 * @return The users DTO or a SegueErrorResponse
	 */
	@POST
	@Produces("application/json")
	@Path("auth/{provider}/authenticate")
	@Consumes("application/json")
	public final Response authenticateWithCredentials(
			@Context final HttpServletRequest request,
			@PathParam("provider") final String signinProvider,
			final Map<String, String> credentials) {

		// ok we need to hand over to user manager
		return userManager.authenticateWithCredentials(request, signinProvider, credentials);
	}
	
	/**
	 * End point that allows the user to logout - i.e. destroy our cookie.
	 * 
	 * @param request
	 *            so that we can destroy the associated session
	 * @return successful response to indicate any cookies were destroyed.
	 */
	@POST
	@Produces("application/json")
	@Path("auth/logout")
	public final Response userLogout(@Context final HttpServletRequest request) {
		userManager.logUserOut(request);

		return Response.ok().build();
	}

	/**
	 * This method will try to bring the live version that Segue is using to
	 * host content up-to-date with the latest in the database.
	 * @param request - to enable security checking.
	 * @return a response to indicate the synchronise job has triggered.
	 */
	@POST
	@Produces("application/json")
	@Path("admin/synchronise_datastores")
	public final synchronized Response synchroniseDataStores(@Context final HttpServletRequest request) {
		try {
			// check if we are authorized to do this operation.
			// no authorisation required in DEV mode, but in PROD we need to be an admin.
			if (!this.properties.getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(
					Constants.EnvironmentType.PROD.name())
					|| this.userManager.isUserAnAdmin(request)) {
				log.info("Informed of content change; "
						+ "so triggering new synchronisation job.");
				contentVersionController.triggerSyncJob();
				return Response.ok("success - job started").build();
			} else {
				log.warn("Unable to trigger synch job as not an admin.");
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an administrator to use this function.")
						.toResponse();
			}
		} catch (NoUserLoggedInException e) {
			log.warn("Unable to trigger synch job as not logged in.");
			return new SegueErrorResponse(Status.UNAUTHORIZED,
					"You must be logged in to access this function.")
					.toResponse();
		}
	}

	/**
	 * This method will delete all cached data from the CMS and any search
	 * indices.
	 * 
	 * @param request - containing user session information.
	 * 
	 * @return the latest version id that will be cached if content is
	 *         requested.
	 */
	@POST
	@Produces("application/json")
	@Path("admin/clear_caches")
	public final synchronized Response clearCaches(@Context final HttpServletRequest request) {
		try {
			if (this.userManager.isUserAnAdmin(request)) {
				IContentManager contentPersistenceManager = contentVersionController
						.getContentManager();

				log.info("Clearing all caches...");
				contentPersistenceManager.clearCache();

				ImmutableMap<String, String> response = new ImmutableMap.Builder<String, String>()
						.put("result", "success").build();
				
				return Response.ok(response).build();				
			} else {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an administrator to use this function.")
						.toResponse();
			}
			
		} catch (NoUserLoggedInException e) {
			return new SegueErrorResponse(Status.UNAUTHORIZED,
					"You must be logged in to access this function.")
					.toResponse();
		}		
	}

	/**
	 * Record that a user has answered a question.
	 * 
	 * @param request
	 *            - the servlet request so we can find out if it is a known
	 *            user.
	 * @param questionId
	 *            that you are attempting to answer.
	 * @param jsonAnswer
	 *            - answer body which will be parsed as a Choice and then
	 *            converted to a ChoiceDTO.
	 * @return Response containing a QuestionValidationResponse object or
	 *         containing a SegueErrorResponse .
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("questions/{question_id}/answer")
	public final Response answerQuestion(
			@Context final HttpServletRequest request,
			@PathParam("question_id") final String questionId,
			final String jsonAnswer) {
		AbstractSegueUserDTO user = this.getCurrentUserIdentifier(request);
		
		Content contentBasedOnId = contentVersionController.getContentManager()
				.getById(questionId, contentVersionController.getLiveVersion());

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
			Choice answerFromClient = mapper.getContentObjectMapper()
					.readValue(jsonAnswer, Choice.class);
			// convert to a DTO so that it strips out any untrusted data.
			ChoiceDTO answerFromClientDTO = mapper.getAutoMapper().map(
					answerFromClient, ChoiceDTO.class);
			
			answersFromClient.add(answerFromClientDTO);
		} catch (JsonMappingException | JsonParseException e) {
			log.info("Failed to map to any expected input...", e);
			SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND,
					"Unable to map response to a "
							+ "Choice object so failing with an error", e);
			return error.toResponse();
		} catch (IOException e) {
			SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND,
					"Unable to map response to a "
							+ "Choice object so failing with an error", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		}

		// validate the answer.
		Response response = this.questionManager.validateAnswer(question,
				Lists.newArrayList(answersFromClient));


		if (response.getEntity() instanceof QuestionValidationResponseDTO) {
			userManager.recordQuestionAttempt(user,
					(QuestionValidationResponseDTO) response.getEntity());
		}
		
		this.logManager.logEvent(request, Constants.ANSWER_QUESTION, response.getEntity());
		
		return response;
	}

	/**
	 * Rest end point to allow content editors to see the content which failed
	 * to import into segue.
	 * 
	 * @param request - to identify if the user is authorised. 
	 * 
	 * @return a content object, such that the content object has children. The
	 *         children represent each source file in error and the grand
	 *         children represent each error.
	 */
	@GET
	@Produces("application/json")
	@Path("admin/content_problems")
	public final Response getContentProblems(@Context final HttpServletRequest request) {
		Map<Content, List<String>> problemMap = contentVersionController
				.getContentManager().getProblemMap(
						contentVersionController.getLiveVersion());

		if (this.properties.getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(EnvironmentType.PROD.name())) {
			try {
				if (!this.userManager.isUserAnAdmin(request)) {
					return Response.status(Status.FORBIDDEN)
							.entity("This page is only available to administrators in PROD mode.").build();

				}
			} catch (NoUserLoggedInException e) {
				return Response.status(Status.UNAUTHORIZED)
						.entity("You must be logged in to view this page in PROD mode.").build();
			}
		}
		
		if (null == problemMap) {
			return Response.ok(new Content("No problems found.")).build();
		}

		// build up a content object to return.
		int brokenFiles = 0;
		int errors = 0;

		Content c = new Content();
		c.setId("dynamic_problem_report");
		for (Map.Entry<Content, List<String>> pair : problemMap.entrySet()) {
			Content child = new Content();
			child.setTitle(pair.getKey().getTitle());
			child.setCanonicalSourceFile(pair.getKey().getCanonicalSourceFile());
			brokenFiles++;

			for (String s : pair.getValue()) {
				Content erroredContentObject = new Content(s);

				erroredContentObject.setId(pair.getKey().getId() + "_error_"
						+ errors);

				child.getChildren().add(erroredContentObject);

				errors++;
			}
			c.getChildren().add(child);
			child.setId(pair.getKey().getId() + "_problem_report_" + errors);
		}

		c.setSubtitle("Total Broken files: " + brokenFiles + " Total errors : "
				+ errors);

		return Response.ok(c).build();
	}

	/**
	 * Endpoint that handles contact us form submissions.
	 *
	 * @param form - Map containing the message details
	 * @return - Successful response if no error occurs, otherwise error response
	 */
	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("contact/")
	public Response contactUs(final Map<String, String> form) {
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

		try {
			communicator.sendMessage(properties.getProperty("MAIL_RECEIVERS"), "Administrator", "Contact Us Form",
				builder.toString());
		} catch (CommunicationException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.INTERNAL_SERVER_ERROR,
					"Error sending message.", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		}

		return Response.ok().build();
	}
	
	/**
	 * Helper method to generate field to match requirements for search queries.
	 * 
	 * Assumes that everything is AND queries
	 * 
	 * @param fieldsToMatch
	 *            - expects a map of the form fieldname -> list of queries to
	 *            match
	 * @return A map ready to be passed to a content provider
	 */
	public static Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> 
	generateDefaultFieldToMatch(
			final Map<String, List<String>> fieldsToMatch) {
		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatchOutput = Maps
				.newHashMap();

		for (Map.Entry<String, List<String>> pair : fieldsToMatch.entrySet()) {
			Map.Entry<Constants.BooleanOperator, String> newEntry = null;
			if (pair.getKey().equals(Constants.ID_FIELDNAME)) {
				newEntry = com.google.common.collect.Maps.immutableEntry(
						Constants.BooleanOperator.OR, pair.getKey());

			} else {
				newEntry = com.google.common.collect.Maps.immutableEntry(
						Constants.BooleanOperator.AND, pair.getKey());
			}

			fieldsToMatchOutput.put(newEntry, pair.getValue());
		}

		return fieldsToMatchOutput;
	}

	/**
	 * Library method to allow applications to access a segue persistence
	 * manager. This allows applications to save data using the segue database.
	 * 
	 * These objects should be used with care as it is possible to create
	 * managers for segue managed objects and get conflicts. e.g. requesting a
	 * manager that manages users could give you an object equivalent to a low
	 * level segue object.
	 * 
	 * @param <T>
	 *            - the type that the app data manager looks after.
	 * @param databaseName
	 *            - the databaseName / collection name / internal reference for
	 *            objects of this type.
	 * @param classType
	 *            - the class of the type <T>.
	 * @return IAppDataManager where <T> is the type the manager is responsible
	 *         for.
	 */
	public final <T> IAppDataManager<T> requestAppDataManager(
			final String databaseName, final Class<T> classType) {
		return SegueGuiceConfigurationModule.getAppDataManager(databaseName,
				classType);
	}

	/**
	 * Library method for finding content by id prefix.
	 * 
	 * @param version
	 *            - of the content to search for.
	 * @param idPrefix
	 *            - prefix / id to match against.
	 * @return a results wrapper containing any matching content.
	 */
	public final ResultsWrapper<ContentDTO> searchByIdPrefix(
			final String version, final String idPrefix) {
		return this.contentVersionController.getContentManager().getByIdPrefix(
				idPrefix, version);
	}

	/**
	 * Library method to allow the question manager to be accessed.
	 * @return question manager.
	 */
	public QuestionManager getQuestionManager() {
		return this.questionManager;
	}
	
	/**
	 * Utility method to allow related content to be populated as summary
	 * objects.
	 * 
	 * By default content summary objects may just have ids.
	 * 
	 * @param version
	 *            - version of the content to use for augmentation.
	 * @param contentToAugment
	 *            - the content to augment.
	 * @return content which has been augmented
	 */
	public ContentDTO augmentContentWithRelatedContent(final String version,
			final ContentDTO contentToAugment) {
		return this.contentVersionController.getContentManager()
				.populateContentSummaries(version, contentToAugment);
	}
	
	/**
	 * Library method to provide access to the Segue Log Manager.
	 * 
	 * @return an instance of the log manager.
	 */
	public ILogManager getLogManager() {
		return this.logManager;
	}
	
	/**
	 * generateCachedResponse This method will accept a request and an entity
	 * tag and determine whether the entity tag is the same.
	 * 
	 * If the entity tag is the same a response will be returned which is ready
	 * to be sent to the client as we do not need to resent anything.
	 * 
	 * @param request
	 *            - clients request
	 * @param etag
	 *            - the entity tag we have computed for the resource being
	 *            requested.
	 * @return if the resource etag provided is the same as the one sent by the
	 *         client then a Response will be returned. This can be sent
	 *         directly to the client. If not (i.e. if the resource has changed
	 *         since the client last requested it) a null value is returned.
	 *         This indicates that we need to send a new version of the
	 *         resource.
	 */
	public Response generateCachedResponse(final Request request, final EntityTag etag) {
		return this.generateCachedResponse(request, etag, null);
	}
	
	/**
	 * generateCachedResponse This method will accept a request and an entity
	 * tag and determine whether the entity tag is the same.
	 * 
	 * If the entity tag is the same a response will be returned which is ready
	 * to be sent to the client as we do not need to resent anything.
	 * 
	 * @param request
	 *            - clients request
	 * @param etag
	 *            - the entity tag we have computed for the resource being
	 *            requested.
	 * @param maxAge - this allows you to set the time at which the cache response will go stale.
	 * @return if the resource etag provided is the same as the one sent by the
	 *         client then a Response will be returned. This can be sent
	 *         directly to the client. If not (i.e. if the resource has changed
	 *         since the client last requested it) a null value is returned.
	 *         This indicates that we need to send a new version of the
	 *         resource.
	 */
	public Response generateCachedResponse(final Request request, final EntityTag etag, final Integer maxAge) {
		Response.ResponseBuilder rb = null;

		// Verify if it matched with etag available in http request
		rb = request.evaluatePreconditions(etag);

		// If ETag matches the rb will be non-null;
		if (rb != null) {
			// Use the rb to return the response without any further processing
			log.debug("This resource is unchanged. Serving empty request with etag.");
			return rb.cacheControl(getCacheControl(maxAge)).tag(etag).build();
		}
		// the resource must have changed as the etags are different.
		return null;
	}
	
	/**
	 * Set the max age to the server default.
	 * @return preconfigured cache control.
	 */
	public CacheControl getCacheControl() {
		return this.getCacheControl(null);
	}
	
	/**
	 * Helper to get cache control information for response objects that can be cached. 
	 * 
	 * @param maxAge in seconds for the returned object to remain fresh.
	 * @return a CacheControl object configured with a MaxAge.
	 */
	public CacheControl getCacheControl(final Integer maxAge) {
		// Create cache control header
		CacheControl cc = new CacheControl();
		
		Integer maxCacheAge;
		if (null == maxAge) {
			// set max age to server default.
			maxCacheAge = Integer.parseInt(this.properties.getProperty(Constants.MAX_CONTENT_CACHE_TIME));	
		} else {
			maxCacheAge = maxAge;
		}
		
		cc.setMaxAge(maxCacheAge);
		
		return cc;
	}
}
