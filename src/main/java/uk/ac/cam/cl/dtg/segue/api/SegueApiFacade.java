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
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.elasticsearch.common.collect.Lists;
import org.jboss.resteasy.annotations.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.Choice;
import uk.ac.cam.cl.dtg.segue.dto.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.users.User;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

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

	private static ContentVersionController contentVersionController;

	private QuestionManager questionManager;

	private PropertiesLoader properties;

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
	 */
	@Inject
	public SegueApiFacade(
			final PropertiesLoader properties,
			final ContentMapper mapper,
			@Nullable final ISegueDTOConfigurationModule segueConfigurationModule,
			final ContentVersionController contentVersionController) {

		this.properties = properties;
		this.questionManager = new QuestionManager();

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

		if (null == SegueApiFacade.contentVersionController) {
			SegueApiFacade.contentVersionController = contentVersionController;
		}

		// Check if we want to get the latest from git each time a request is
		// made from segue. - Will add overhead
		if (Boolean.parseBoolean(this.properties
				.getProperty(Constants.FOLLOW_GIT_VERSION))) {
			log.info("Segue just initialized - Sending content index request "
					+ "so that we can service some content requests.");
			this.synchroniseDataStores();
		}
	}

	// @POST
	// @Path("log")
	// @Produces("application/json")
	// public ImmutableMap<String, Boolean> postLog(
	// @Context final HttpServletRequest req,
	// @FormParam("sessionId") final String sessionId,
	// @FormParam("cookieId") final String cookieId,
	// @FormParam("event") final String eventJSON) {
	//
	// Injector injector = Guice
	// .createInjector(new SegueGuiceConfigurationModule());
	// ILogManager logPersistenceManager = injector
	// .getInstance(ILogManager.class);
	//
	// boolean success = logPersistenceManager.log(sessionId, cookieId,
	// eventJSON);
	//
	// return ImmutableMap.of("success", success);
	// }

	/**
	 * GetContentById from the database.
	 * 
	 * Routing endpoint: this method will either return results from one of the
	 * following: getContentByTags getContentByType
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
	public final Response getContentList(
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

		ResultsWrapper<Content> c;

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

			return Response.ok().entity(c).build();
		} catch (NumberFormatException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"Unable to convert one of the integer parameters provided into numbers. "
							+ "Params provided were: limit" + limit
							+ " and startIndex " + startIndex, e).toResponse();
		}
	}

	/**
	 * This method will return a List<Content> based on the parameters supplied.
	 * 
	 * @param version
	 * @param fieldsToMatch
	 *            - Map representing fieldName -> field value mappings to search
	 *            for. Note: tags is a special field name and the list will be
	 *            split by commas.
	 * @param startIndex
	 * @param limit
	 * @return Response containing a list of content or a Response containing
	 *         null if none found.
	 */
	public final ResultsWrapper<Content> findMatchingContent(
			String version,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			@Nullable Integer startIndex, @Nullable Integer limit) {
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		if (null == version) {
			version = contentVersionController.getLiveVersion();
		}

		if (null == limit) {
			limit = Constants.DEFAULT_SEARCH_LIMIT;
		}

		if (null == startIndex) {
			startIndex = 0;
		}

		ResultsWrapper<Content> c = null;

		// Deserialize object into POJO of specified type, providing one exists.
		try {
			log.info("Finding all content from the api with fields: "
					+ fieldsToMatch);

			c = contentPersistenceManager.findByFieldNames(version,
					fieldsToMatch, startIndex, limit);
		} catch (IllegalArgumentException e) {
			log.error("Unable to map content object.", e);
			throw e;
		}

		return c;
	}

	/**
	 * This method will return a List<Content> based on the parameters supplied.
	 * Providing the results in a randomised order.
	 * 
	 * @param version
	 * @param fieldsToMatch
	 *            - Map representing fieldName -> field value mappings to search
	 *            for. Note: tags is a special field name and the list will be
	 *            split by commas.
	 * @param startIndex
	 * @param limit
	 * @return Response containing a list of content or a Response containing
	 *         null if none found.
	 */
	public final ResultsWrapper<Content> findMatchingContentRandomOrder(
			String version,
			Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			Integer startIndex, Integer limit) {
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		if (null == version)
			version = contentVersionController.getLiveVersion();

		if (null == limit) {
			limit = Constants.DEFAULT_SEARCH_LIMIT;
		}

		if (null == startIndex) {
			startIndex = 0;
		}

		ResultsWrapper<Content> c = null;

		// Deserialize object into POJO of specified type, providing one exists.
		try {
			log.info("Finding all content from the api with fields: "
					+ fieldsToMatch);

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
	public final Response getContentById(@PathParam("version") String version,
			@PathParam("id") String id) {
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		if (null == version)
			version = contentVersionController.getLiveVersion();

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

		ResultsWrapper<Content> searchResults = contentPersistenceManager
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
	 * This method provides a set of all tags for a given version of the
	 * content.
	 * 
	 * @return a version info as json response
	 */
	@GET
	@Produces("application/json")
	@Path("content/tags")
	public final Response getTagListByLiveVersion() {
		return this.getTagListByVersion(contentVersionController
				.getLiveVersion());
	}

	/**
	 * This method provides a set of all tags for a given version of the
	 * content.
	 * 
	 * @param version
	 *            of the site to provide the tag list from.
	 * @return a version info as json response
	 */
	@GET
	@Produces("application/json")
	@Path("content/tags/{version}")
	public final Response getTagListByVersion(@PathParam("version") final String version) {
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		Set<String> tags = contentPersistenceManager.getTagsList(version);

		return Response.ok().entity(tags).build();
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
	 * @param version
	 *            number - e.g. a sha
	 * @param path
	 *            - path of the image file
	 * @return Response object containing the serialized content object. (with
	 *         no levels of recursion into the content)
	 */
	@GET
	@Produces("*/*")
	@Path("content/file_content/{version}/{path:.*}")
	@Cache
	public final Response getImageFileContent(
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

		IContentManager gcm = contentVersionController.getContentManager();

		ByteArrayOutputStream fileContent = null;
		String mimeType = MediaType.WILDCARD;

		switch (Files.getFileExtension(path).toLowerCase()) {
			case "svg": {
				mimeType = "image/svg+xml";
				break;
			}
			case "jpg": {
				mimeType = "image/jpeg";
				break;
			}
			default: {
				// if it is an unknown type return an error as they shouldn't be
				// using this endpoint.
				SegueErrorResponse error = new SegueErrorResponse(
						Status.BAD_REQUEST, "Invalid file extension requested");
				log.debug(error.getErrorMessage());
				return error.toResponse();
			}
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

		return Response.ok().type(mimeType).entity(fileContent.toByteArray())
				.build();
	}

	/**
	 * This method will allow the live version served by the site to be changed
	 * TODO: Maybe some security???!
	 * 
	 * @param version - version to use as updated version of content store.
	 * @return Success shown by returning the new liveSHA or failed message
	 *         "Invalid version selected".
	 */
	@PUT
	@Produces("application/json")
	@Path("admin/live_version/{version}")
	public final synchronized Response changeLiveVersion(
			@PathParam("version") final String version) {
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
	public final Response getVersionsList(@QueryParam("limit") final String limit) {
		// try to parse the integer
		Integer limitAsInt = null;

		try {
			if (null == limit) {
				limitAsInt = 10;
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

		Injector injector = Guice
				.createInjector(new SegueGuiceConfigurationModule());
		IContentManager contentPersistenceManager = injector
				.getInstance(IContentManager.class);

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
				.put("segueVersion", this.properties.getProperty(Constants.SEGUE_APP_VERSION))
				.build();
		
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
	@Path("info/content_versions/live_version")
	public final Response getLiveVersionInfo() {
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		ImmutableMap<String, String> result = new ImmutableMap.Builder<String, String>()
				.put("liveVersion", contentVersionController.getLiveVersion())
				.put("latestKnownVersion",
						contentPersistenceManager.getLatestVersionId()).build();

		return Response.ok().entity(result).build();
	}

	/**
	 * This method return a json response containing version related information.
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

		return Response.ok().entity(result).build();
	}

	/**
	 * get the live version of the content hosted by the api.
	 * 
	 * @return a string representing the live version.
	 */
	public final String getLiveVersion() {
		return contentVersionController.getLiveVersion();
	}

	/**
	 * Get the details of the currently logged in user.
	 * 
	 * @param request
	 *            - the request which may contain session information.
	 * @return Returns the current user DTO if we can get it or null if we can't
	 */
	@GET
	@Produces("application/json")
	@Path("users/current_user")
	public final User getCurrentUser(@Context final HttpServletRequest request) {
		Injector injector = Guice
				.createInjector(new SegueGuiceConfigurationModule());
		UserManager userManager = injector.getInstance(UserManager.class);

		return userManager.getCurrentUser(request);
	}

	/**
	 * This is the initial step of the authentication process.
	 * 
	 * @param request
	 *            - the http request of the user wishing to authenticate
	 * @param signinProvider
	 *            - string representing the supported auth provider so that we
	 *            know who to redirect the user to.
	 * @param redirectUrl - optional redirect url after authentication has completed.            
	 * @return Redirect response to the auth providers site.
	 */
	@GET
	@Produces("application/json")
	@Path("auth/{provider}/authenticate")
	public final Response authenticationInitialisation(
			@Context final HttpServletRequest request,
			@PathParam("provider") final String signinProvider,
			@QueryParam("redirect") final String redirectUrl) {
		
		Injector injector = Guice
				.createInjector(new SegueGuiceConfigurationModule());
		UserManager userManager = injector.getInstance(UserManager.class);

		User currentUser = getCurrentUser(request);

		if (null != currentUser) {
			return Response.ok().entity(currentUser).build();
		}
		
		String newRedirectUrl = null;
		if (!redirectUrl.contains("http://")) {
			// TODO: Make this redirection stuff less horrid.
			newRedirectUrl = "http://" + this.properties.getProperty(Constants.HOST_NAME) 
					+ redirectUrl;
		} else {
			newRedirectUrl = redirectUrl;
		}

		// ok we need to hand over to user manager
		return userManager.authenticate(request, signinProvider, newRedirectUrl);
	}

	/**
	 * This is the callback url that auth providers should use to send us
	 * information about users.
	 * 
	 * @param request
	 *            - http request from user
	 * @param response
	 *            - http response from server
	 * @param signinProvider
	 *            - requested signing provider string
	 * @return Redirect response to send the user to the home page.
	 */
	@GET
	@Produces("application/json")
	@Path("auth/{provider}/callback")
	public final Response authenticationCallback(
			@Context final HttpServletRequest request,
			@Context final HttpServletResponse response,
			@PathParam("provider") final String signinProvider) {
		Injector injector = Guice
				.createInjector(new SegueGuiceConfigurationModule());
		UserManager userManager = injector.getInstance(UserManager.class);

		return userManager.authenticateCallback(request, response, signinProvider);
	}

	/**
	 * End point that allows the user to logout - i.e. destroy our cookie.
	 * 
	 * @param request
	 *            so that we can destroy the associated session
	 * @return successful response.
	 */
	@GET
	@Produces("application/json")
	@Path("auth/logout")
	public final Response userLogout(@Context final HttpServletRequest request) {
		Injector injector = Guice
				.createInjector(new SegueGuiceConfigurationModule());
		UserManager userManager = injector.getInstance(UserManager.class);

		userManager.logUserOut(request);

		return Response.ok("success").build();
	}

	/**
	 * This method will try to bring the live version that Segue is using to
	 * host content up-to-date with the latest in the database.
	 * 
	 * @return a response to indicate the synchronise job has triggered.
	 */
	@POST
	@Produces("application/json")
	@Path("admin/synchronise_datastores")
	public final synchronized Response synchroniseDataStores() {
		log.info("Informed of content change; "
				+ "so triggering new synchronisation job.");
		contentVersionController.triggerSyncJob();
		return Response.ok("success - job started").build();
	}

	/**
	 * This method will delete all cached data from the CMS and any search
	 * indices.
	 * 
	 * @return the latest version id that will be cached if content is
	 *         requested.
	 */
	@POST
	@Produces("application/json")
	@Path("admin/clear_caches")
	public final synchronized Response clearCaches() {
		IContentManager contentPersistenceManager = contentVersionController
				.getContentManager();

		log.info("Clearing all caches...");
		contentPersistenceManager.clearCache();

		ImmutableMap<String, String> response = new ImmutableMap.Builder<String, String>()
				.put("result", "success").build();

		return Response.ok(response).build();
	}

	/**
	 * Answer a question.
	 * @param request - the servlet request so we can find out if it is a known user. 
	 * @param questionId that you are attempting
	 * @param jsonAnswer - answer body.
	 * @return Response containing a QuestionValidationResponse object.
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces("application/json")
	@Path("questions/{question_id}/answer")
	public final Response answerQuestion(
			@Context final HttpServletRequest request,
			@PathParam("question_id") final String questionId,
			final String jsonAnswer) {
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
		List<Choice> answersFromClient = Lists.newArrayList();

		try {
			// convert single object into a list.
			Choice answerFromClient = mapper.getContentObjectMapper()
					.readValue(jsonAnswer, Choice.class);
			answersFromClient.add(answerFromClient);
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

		Response response = this.questionManager.validateAnswer(question,
				Lists.newArrayList(answersFromClient));
		
		User user = this.getCurrentUser(request);
		if (user != null && response.getEntity() instanceof QuestionValidationResponse) {
			Injector injector = Guice
					.createInjector(new SegueGuiceConfigurationModule());
			UserManager userManager = injector.getInstance(UserManager.class);
			userManager.recordUserQuestionInformation(user, 
					(QuestionValidationResponse) response.getEntity());
		}

		return response;
	}

	/**
	 * Rest end point to allow content editors to see the content which failed
	 * to import into segue.
	 * 
	 * @return a content object, such that the content object has children. The
	 *         children represent each source file in error and the grand
	 *         children represent each error.
	 */
	@GET
	@Produces("application/json")
	@Path("admin/content_problems")
	public final Response getContentProblems() {

		Map<Content, List<String>> problemMap = contentVersionController
				.getContentManager().getProblemMap(
						contentVersionController.getLiveVersion());

		if (null == problemMap) {
			return Response.ok(new Content("No problems found.")).build();
		}

		// build up a content object to return.
		int brokenFiles = 0;
		int errors = 0;

		Content c = new Content();
		c.setId("dyanmic_problem_report");
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
			Map.Entry<Constants.BooleanOperator, String> newEntry = com.google.common.collect.Maps
					.immutableEntry(Constants.BooleanOperator.AND,
							pair.getKey());

			fieldsToMatchOutput.put(newEntry, pair.getValue());
		}

		return fieldsToMatchOutput;
	}
}
