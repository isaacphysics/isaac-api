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
package uk.ac.cam.cl.dtg.isaac.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import ma.glasnost.orika.MapperFacade;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardListDTO;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.URIManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static com.google.common.collect.Maps.*;

/**
 * Isaac Controller
 * 
 * This class specifically caters for the Rutherford physics server and is
 * expected to provide extended functionality to the Segue api for use only on
 * the Isaac site.
 * 
 */
@Path("/")
public class IsaacController {
	private static final Logger log = LoggerFactory
			.getLogger(IsaacController.class);

	private SegueApiFacade api;
	private PropertiesLoader propertiesLoader;
	private GameManager gameManager;

	/**
	 * Creates an instance of the isaac controller which provides the REST
	 * endpoints for the isaac api.
	 * 
	 */
	public IsaacController() {
		// Get an singleton instances of dependencies
		// without using the rest endpoints.
		Injector injector = Guice.createInjector(
				new IsaacGuiceConfigurationModule(),
				new SegueGuiceConfigurationModule());
		api = injector.getInstance(SegueApiFacade.class);
		propertiesLoader = injector.getInstance(PropertiesLoader.class);
		gameManager = injector.getInstance(GameManager.class);
	}

	/**
	 * Creates an instance of the isaac controller which provides the REST
	 * endpoints for the isaac api.
	 * 
	 * @param api
	 *            - Instance of segue Api
	 * @param propertiesLoader
	 *            - Instance of properties Loader
	 * @param gameManager
	 *            - Instance of Game Manager
	 */
	public IsaacController(final SegueApiFacade api,
			final PropertiesLoader propertiesLoader,
			final GameManager gameManager) {
		this.api = api;
		this.propertiesLoader = propertiesLoader;
		this.gameManager = gameManager;
	}

	/**
	 * REST end point to provide a list of concepts.
	 * 
	 * Uses ETag caching to attempt to reduce load on the server.
	 *
	 * @param request
	 *            - used to determine if we can return a cache response. 
	 * @param ids
	 *            - the ids of the concepts to request.
	 * @param tags
	 *            - a comma separated list of strings
	 * @param startIndex
	 *            - a string value to be converted into an integer which
	 *            represents the start index of the results
	 * @param limit
	 *            - a string value to be converted into an integer that
	 *            represents the number of results to return.
	 * @return A response object which contains a list of concepts or an empty
	 *         list.
	 */
	@GET
	@Path("pages/concepts")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getConceptList(
			@Context final Request request,
			@QueryParam("ids") final String ids,
			@QueryParam("tags") final String tags,
			@QueryParam("start_index") final String startIndex,
			@QueryParam("limit") final String limit) {
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(CONCEPT_TYPE));

		StringBuilder etagCodeBuilder = new StringBuilder();
		
		String newLimit = null;
		
		if (limit != null) {
			newLimit = limit;
			etagCodeBuilder.append(limit);
		}

		// options
		if (ids != null) {
			List<String> idsList = Arrays.asList(ids.split(","));
			fieldsToMatch.put(ID_FIELDNAME, idsList);
			newLimit = String.valueOf(idsList.size());
			etagCodeBuilder.append(ids);
		}

		if (tags != null) {
			fieldsToMatch.put(TAGS_FIELDNAME, Arrays.asList(tags.split(",")));
			etagCodeBuilder.append(tags);
		}
		
		// Calculate the ETag on last modified date of tags list
		// NOTE: Assumes that the latest version of the content is being used.
		EntityTag etag = new EntityTag(this.api.getLiveVersion().hashCode()
				+ etagCodeBuilder.toString().hashCode() + "");
		
		Response cachedResponse = api.generateCachedResponse(request, etag);
		
		if (cachedResponse != null) {
			return cachedResponse;
		}

		return listContentObjects(fieldsToMatch, startIndex, newLimit).tag(etag)
				.cacheControl(api.getCacheControl()).build();
	}

	/**
	 * Rest end point that gets a single concept based on a given id.
	 * @param request - so we can deal with caching and ETags.
	 * @param servletRequest - so we can extract user information for logging.
	 * @param conceptId
	 *            as a string
	 * @return A Response object containing a concept object.
	 */
	@GET
	@Path("pages/concepts/{concept_page_id}")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getConcept(
			@Context final Request request,
			@Context final HttpServletRequest servletRequest,
			@PathParam("concept_page_id") final String conceptId) {
		if (null == conceptId || conceptId.isEmpty()) {
			return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid concept id.")
					.toResponse();
		}
		
		// Calculate the ETag on current live version of the content
		// NOTE: Assumes that the latest version of the content is being used.
		EntityTag etag = new EntityTag(this.api.getLiveVersion().hashCode()
				+ "byId".hashCode() + conceptId.hashCode() + "");
		Response cachedResponse = api.generateCachedResponse(request, etag);
		if (cachedResponse != null) {
			return cachedResponse;
		}
		
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(CONCEPT_TYPE));

		// options
		if (null != conceptId) {
			fieldsToMatch
					.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX,
							Arrays.asList(conceptId));
		}
		
		Response result = this.findSingleResult(fieldsToMatch);

		if (result.getEntity() instanceof SeguePageDTO) {
			ImmutableMap<String, String> logEntry = new ImmutableMap.Builder<String, String>()
					.put(CONCEPT_ID_LOG_FIELDNAME, conceptId)
					.put(CONTENT_VERSION, api.getLiveVersion()).build();
					
			// the request log
			this.api.getLogManager().logEvent(servletRequest, Constants.VIEW_CONCEPT, logEntry);
		}
		
		Response cachableResult = Response.status(result.getStatus()).entity(result.getEntity())
				.cacheControl(api.getCacheControl()).tag(etag).build();
		
		return cachableResult;
	}

	/**
	 * REST end point to provide a list of questions.
	 * 
	 * @param request
	 *            - used to determine if we can return a cache response. 
	 * @param ids
	 *            - the ids of the concepts to request.
	 * @param tags
	 *            - a comma separated list of strings
	 * @param level
	 *            - a string value to be converted into an integer which
	 *            represents the levels that must match the questions returned.
	 * @param startIndex
	 *            - a string value to be converted into an integer which
	 *            represents the start index of the results
	 * @param limit
	 *            - a string value to be converted into an integer that
	 *            represents the number of results to return.
	 * @return A response object which contains a list of questions or an empty
	 *         list.
	 */
	@GET
	@Path("pages/questions")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getQuestionList(
			@Context final Request request,
			@QueryParam("ids") final String ids,
			@QueryParam("tags") final String tags,
			@QueryParam("levels") final String level,
			@QueryParam("start_index") final String startIndex,
			@QueryParam("limit") final String limit) {
		StringBuilder etagCodeBuilder = new StringBuilder();
		
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(QUESTION_TYPE));
		etagCodeBuilder.append(QUESTION_TYPE);
		
		String newLimit = null;

		// options
		if (limit != null) {
			newLimit = limit;
		}

		if (ids != null) {
			List<String> idsList = Arrays.asList(ids.split(","));
			fieldsToMatch.put(ID_FIELDNAME, idsList);
			newLimit = String.valueOf(idsList.size());
			etagCodeBuilder.append(ids);
		}

		if (tags != null) {
			fieldsToMatch.put(TAGS_FIELDNAME, Arrays.asList(tags.split(",")));
			etagCodeBuilder.append(tags);
		}

		if (level != null) {
			fieldsToMatch.put(LEVEL_FIELDNAME, Arrays.asList(level.split(",")));
			etagCodeBuilder.append(level);
		}

		// Calculate the ETag on last modified date of tags list
		// NOTE: Assumes that the latest version of the content is being used.
		EntityTag etag = new EntityTag(this.api.getLiveVersion().hashCode()
				+ etagCodeBuilder.toString().hashCode() + "");
		
		Response cachedResponse = api.generateCachedResponse(request, etag);
		
		if (cachedResponse != null) {
			return cachedResponse;
		}
		
		return listContentObjects(fieldsToMatch, startIndex, newLimit).tag(etag)
				.cacheControl(api.getCacheControl()).build();
	}

	/**
	 * Rest end point that gets a single question page based on a given id.
	 * 
	 * @param questionId
	 *            to find as a string
	 * @param request
	 *            - so that we can do etag and cache resolution.
	 * @param httpServletRequest
	 *            - so that we can try and determine if the user is logged in.
	 *            This will allow us to augment the question objects with any
	 *            recorded state.
	 * @return A Response object containing a question page object or a
	 *         SegueErrorResponse.
	 */
	@GET
	@Path("pages/questions/{question_page_id}")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getQuestion(
			@Context final Request request,
			@Context final HttpServletRequest httpServletRequest,
			@PathParam("question_page_id") final String questionId) {
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		fieldsToMatch.put("type", Arrays.asList(QUESTION_TYPE));

		// options
		if (null != questionId) {
			fieldsToMatch
					.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, Arrays.asList(questionId));
		}
		AbstractSegueUserDTO user = api.getCurrentUserIdentifier(httpServletRequest);
		Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts;
		
		try {
			userQuestionAttempts = api.getQuestionAttemptsBySession(user);
		} catch (SegueDatabaseException e) {
			log.error("SegueDatabaseException whilst trying to retrieve user question data", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error whilst trying to access the user question information in the database.", e).toResponse();
		}
	
		// Calculate the ETag 
		EntityTag etag = new EntityTag(questionId.hashCode() + userQuestionAttempts.toString().hashCode() + "");
		
		Response cachedResponse = api.generateCachedResponse(request, etag, NEVER_CACHE_WITHOUT_ETAG_CHECK);
		if (cachedResponse != null) {
			return cachedResponse;
		}
		
		Response response = this.findSingleResult(fieldsToMatch);

		if (response.getEntity() instanceof SeguePageDTO) {
			SeguePageDTO content = (SeguePageDTO) response.getEntity();

			Map<String, String> logEntry = ImmutableMap.of(QUESTION_ID_LOG_FIELDNAME, content.getId(), "contentVersion",
					api.getLiveVersion());
			
			String userId;
			if (user instanceof AnonymousUserDTO) {
				userId = ((AnonymousUserDTO) user).getSessionId();
			} else {
				userId = ((RegisteredUserDTO) user).getDbId();
			}
			
			content = api.getQuestionManager().augmentQuestionObjects(content, userId,
						userQuestionAttempts);

			// the request log
			this.api.getLogManager().logEvent(httpServletRequest, Constants.VIEW_QUESTION, logEntry);

			// return augmented content.
			return Response.ok(content).cacheControl(api.getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK))
					.tag(etag).build();
			
		} else {
			// this is not a segue page so something probably went wrong.
			log.info("This is not a segue question page so just returning it as is.");
			return response;			
		}
	}

	/**
	 * Rest end point that searches the api for some search string.
	 * 
	 * @param request - so that we can handle caching of search responses.
	 * @param httpServletRequest - so we can extract user information for logging.
	 * @param searchString
	 *            - to pass to the search engine.
	 * @param types
	 *            - a comma separated list of types to include in the search.
	 * @return a response containing the search results (results wrapper) or an
	 *         empty list.
	 */
	@SuppressWarnings("unchecked")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("search/{searchString}")
	@GZIP
	public final Response search(
			@Context final Request request,
			@Context final HttpServletRequest httpServletRequest,
			@PathParam("searchString") final String searchString,
			@QueryParam("types") final String types) {
		
		// Calculate the ETag on current live version of the content
		// NOTE: Assumes that the latest version of the content is being used.
		EntityTag etag = new EntityTag(this.api.getLiveVersion().hashCode()
				+ searchString.hashCode() + types.hashCode() + "");
		
		Response cachedResponse = api.generateCachedResponse(request, etag);
		if (cachedResponse != null) {
			return cachedResponse;
		}
		
		ResultsWrapper<ContentDTO> searchResults = null;

		Response unknownApiResult = api.search(searchString,
				api.getLiveVersion(), types);
		if (unknownApiResult.getEntity() instanceof ResultsWrapper) {
			searchResults = (ResultsWrapper<ContentDTO>) unknownApiResult
					.getEntity();
		} else {
			return unknownApiResult;
		}
		
		ImmutableMap<String, String> logMap = new ImmutableMap.Builder<String, String>()
				.put(TYPE_FIELDNAME, types).put("searchString", searchString)
				.put(CONTENT_VERSION, api.getLiveVersion()).build();
		
		this.api.getLogManager().logEvent(httpServletRequest, GLOBAL_SITE_SEARCH, logMap);
		
		return Response
				.ok(this.extractContentSummaryFromResultsWrapper(searchResults,
						propertiesLoader.getProperty(PROXY_PATH))).tag(etag)
				.cacheControl(api.getCacheControl()).build();
	}
	

	/**
	 * REST end point to provide a Temporary Gameboard stored in volatile storage.
	 * 
	 * @param request
	 *            - this allows us to check to see if a user is currently
	 *            loggedin.
	 * @param subjects
	 *            - a comma separated list of subjects
	 * @param fields
	 *            - a comma separated list of fields
	 * @param topics
	 *            - a comma separated list of topics
	 * @param levels
	 *            - a comma separated list of levels
	 * @param concepts
	 *            - a comma separated list of conceptIds
	 * @return a Response containing a gameboard object or containing
	 *         a SegueErrorResponse.
	 */
	@GET
	@Path("gameboards")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response generateTemporaryGameboard(
			@Context final HttpServletRequest request,
			@QueryParam("subjects") final String subjects,
			@QueryParam("fields") final String fields,
			@QueryParam("topics") final String topics,
			@QueryParam("levels") final String levels,
			@QueryParam("concepts") final String concepts) {
		List<String> subjectsList = null;
		List<String> fieldsList = null;
		List<String> topicsList = null;
		List<Integer> levelsList = null;
		List<String> conceptsList = null;

		if (null != subjects && !subjects.isEmpty()) {
			subjectsList = Arrays.asList(subjects.split(","));
		}

		if (null != fields && !fields.isEmpty()) {
			fieldsList = Arrays.asList(fields.split(","));
		}

		if (null != topics && !topics.isEmpty()) {
			topicsList = Arrays.asList(topics.split(","));
		}

		if (null != levels && !levels.isEmpty()) {
			String[] levelsAsString = levels.split(",");

			levelsList = Lists.newArrayList();
			for (int i = 0; i < levelsAsString.length; i++) {
				try {
					levelsList.add(Integer.parseInt(levelsAsString[i]));
				} catch (NumberFormatException e) {
					return new SegueErrorResponse(Status.BAD_REQUEST,
							"Levels must be numbers if specified.", e)
							.toResponse();
				}
			}
		}

		if (null != concepts && !concepts.isEmpty()) {
			conceptsList = Arrays.asList(concepts.split(","));
		}
		
		AbstractSegueUserDTO boardOwner = this.api.getCurrentUserIdentifier(request);
		
		try {
			GameboardDTO gameboard;

			gameboard = gameManager.generateRandomGameboard(subjectsList, fieldsList, topicsList, levelsList,
					conceptsList, boardOwner);
			
			if (null == gameboard) {
				return new SegueErrorResponse(Status.NO_CONTENT,
						"We cannot find any questions based on your filter criteria.")
						.toResponse();
			}
			
			return Response.ok(gameboard).build();
		} catch (IllegalArgumentException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"Your gameboard filter request is invalid.").toResponse();
		} catch (NoWildcardException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Unable to load the wildcard.").toResponse();
		} catch (SegueDatabaseException e) {
			log.error("SegueDatabaseException whilst generating a gameboard", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error whilst trying to access the gameboard in the database.", e).toResponse();
		}
	}
	
	/**
	 * REST end point to retrieve a specific gameboard by Id.
	 * 
	 * @param request
	 *            - so that we can deal with caching and etags.
	 * @param httpServletRequest
	 *            - so that we can extract the users session information if
	 *            available.
	 * @param gameboardId
	 *            - the unique id of the gameboard to be requested
	 * @return a Response containing a gameboard object or containing
	 *         a SegueErrorResponse.
	 */
	@GET
	@Path("gameboards/{gameboard_id}")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getGameboard(
			@Context final Request request,
			@Context final HttpServletRequest httpServletRequest,
			@PathParam("gameboard_id") final String gameboardId) {
		try {
			GameboardDTO gameboard;
			
			AbstractSegueUserDTO randomUser = this.api.getCurrentUserIdentifier(httpServletRequest);
			Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts =
					api.getQuestionAttemptsBySession(randomUser);
		
			// Calculate the ETag 
			EntityTag etag = new EntityTag(gameboardId.hashCode() + userQuestionAttempts.toString().hashCode() + "");
			
			Response cachedResponse = api.generateCachedResponse(request, etag, NEVER_CACHE_WITHOUT_ETAG_CHECK);
			if (cachedResponse != null) {
				return cachedResponse;
			}
			
			// attempt to augment the gameboard with user information.
			gameboard = gameManager.getGameboard(gameboardId, randomUser, userQuestionAttempts);

			if (null == gameboard) {
				return new SegueErrorResponse(Status.NOT_FOUND, "No Gameboard found for the id specified.")
						.toResponse();
			}
			
			// We decided not to log this on the backend as the front end uses this lots.
			return Response.ok(gameboard).cacheControl(api.getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK))
					.tag(etag).build();
		} catch (IllegalArgumentException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST, "Your gameboard filter request is invalid.")
					.toResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error whilst trying to access the gameboard in the database.", e).toResponse();
		}
	}

	/**
	 * REST end point to find all of a user's gameboards. The My Boards endpoint.
	 * 
	 * @param request
	 *            - so that we can find out the currently logged in user
	 * @param startIndex
	 *            - the first board index to return.
	 * @param limit
	 *            - the number of gameboards to return. 
	 * @param sortInstructions
	 *            - the criteria to use for sorting. Default is reverse chronological by created date.
	 * @param showCriteria
	 *            - e.g. completed,incompleted
	 * @return a Response containing a list of gameboard objects or a noContent Response.
	 */
	@GET
	@Path("users/current_user/gameboards")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getGameboardsByCurrentUser(
			@Context final HttpServletRequest request,
			@QueryParam("start_index") final String startIndex,
			@QueryParam("limit") final String limit,
			@QueryParam("sort") final String sortInstructions,
			@QueryParam("show_only") final String showCriteria) {
		RegisteredUserDTO currentUser;
		
		try {
			currentUser = api.getCurrentUser(request);
		} catch (NoUserLoggedInException e1) {
			return new SegueErrorResponse(Status.UNAUTHORIZED,
					"Unable to retrieve the current user's gameboards as no user is currently logged in.")
					.toResponse();		
		}
		
		Integer gameboardLimit = Constants.DEFAULT_GAMEBOARDS_RESULTS_LIMIT;
		if (limit != null) {
			try {
				gameboardLimit = Integer.parseInt(limit);	
			} catch (NumberFormatException e) {
				return new SegueErrorResponse(Status.BAD_REQUEST,
						"The number you entered as the results limit is not valid.").toResponse();
			}
		}
		
		Integer startIndexAsInteger = 0;
		if (startIndex != null) {
			try {
				startIndexAsInteger = Integer.parseInt(startIndex);	
			} catch (NumberFormatException e) {
				return new SegueErrorResponse(Status.BAD_REQUEST,
						"The number you entered as the start_index is not valid.").toResponse();
			}
		}
		
		GameboardState gameboardShowCriteria = null;
		if (showCriteria != null) {
			if (showCriteria.toLowerCase().equals("completed")) {
				gameboardShowCriteria = GameboardState.COMPLETED;
			} else if (showCriteria.toLowerCase().equals("in_progress")) {
				gameboardShowCriteria = GameboardState.IN_PROGRESS;
			} else if (showCriteria.toLowerCase().equals("not_attempted")) {
				gameboardShowCriteria = GameboardState.NOT_ATTEMPTED;
			} else {
				return new SegueErrorResponse(Status.BAD_REQUEST,
						"Unable to interpret showOnly criteria specified " + showCriteria).toResponse();				
			}
		}
		
		List<Map.Entry<String, SortOrder>> parsedSortInstructions = null;
		// sort instructions
		if (sortInstructions != null && !sortInstructions.isEmpty()) {
			parsedSortInstructions = Lists.newArrayList();
			for (String instruction : Arrays.asList(sortInstructions.toLowerCase().split(","))) {
				SortOrder s = SortOrder.ASC;
				if (instruction.startsWith("-")) {
					s = SortOrder.DESC;
					instruction = instruction.substring(1, instruction.length());
				}
				
				if (instruction.equals("created")) {
					parsedSortInstructions.add(immutableEntry(CREATED_DATE_FIELDNAME, s));
				} else if (instruction.equals("visited")) {
					parsedSortInstructions.add(immutableEntry(VISITED_DATE_FIELDNAME, s));
				} else {
					return new SegueErrorResponse(Status.BAD_REQUEST,
							"Sorry we do not recognise the sort instruction " + instruction).toResponse();
				}
			}
		}

		GameboardListDTO gameboards;
		try {
			gameboards = gameManager.getUsersGameboards(currentUser, startIndexAsInteger,
					gameboardLimit, gameboardShowCriteria, parsedSortInstructions);
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error whilst trying to access the gameboard in the database.", e).toResponse();
		}

		if (null == gameboards) {
			return Response.noContent().build();
		}
		
		this.api.getLogManager().logEvent(request, VIEW_MY_BOARDS_PAGE,
				ImmutableMap.builder().put("totalBoards", gameboards.getTotalResults())
						.put("notStartedTotal", gameboards.getTotalNotStarted())
						.put("completedTotal", gameboards.getTotalCompleted())
						.put("inProgressTotal", gameboards.getTotalInProgress())
						.build());
		
		return Response.ok(gameboards).build();
	}

	/**
	 * Rest Endpoint that allows a user to remove a gameboard from their my boards page.
	 * 
	 * This does not delete the gameboard from the system just removes it.
	 * 
	 * @param request - So that we can find the user information. 
	 * @param gameboardId - 
	 * @return noContent response if successful a SegueErrorResponse if not.
	 */
	@DELETE
	@Path("users/current_user/gameboards/{gameboard_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response unlinkUserFromGameboard(@Context final HttpServletRequest request,
			@PathParam("gameboard_id") final String gameboardId) {

		try {
			RegisteredUserDTO user = api.getCurrentUser(request);
			
			Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts =
					api.getQuestionAttemptsBySession(user);
			
			GameboardDTO gameboardDTO = this.gameManager.getGameboard(gameboardId, user, userQuestionAttempts);
			
			if (null == gameboardDTO) {
				return new SegueErrorResponse(Status.NOT_FOUND,
						"Unable to locate the gameboard specified.")
						.toResponse();
			}
			
			this.gameManager.unlinkUserToGameboard(gameboardDTO, user);
			this.api.getLogManager().logEvent(user, request, DELETE_BOARD_FROM_PROFILE, gameboardDTO.getId());

		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error whilst trying to delete a gameboard.", e)
					.toResponse();
		} catch (NoUserLoggedInException e) {
			return new SegueErrorResponse(Status.UNAUTHORIZED,
					"User not logged in. Unable to retrieve delete gameboards.")
					.toResponse();
		}
		
		return Response.noContent().build();
	}
	
	
	/**
	 * REST end point to allow gameboards to be persisted into permanent storage 
	 * and for the title to be updated by users.
	 * 
	 * Currently we only support updating the title and saving the gameboard that exists in 
	 * temporary storage into permanent storage. No other fields can be updated at the moment.
	 * 
	 * TODO: This will need to change if we want to change more than the board title.
	 * 
	 * @param request
	 *            - so that we can find out the currently logged in user
	 * @param gameboardId
	 *            - So that we can look up an existing gameboard to modify.
	 * @param newGameboardObject
	 *            - as a GameboardDTO this should contain all of the updates.
	 * @return a Response containing a list of gameboard objects or containing
	 *         a SegueErrorResponse.
	 */
	@POST
	@Path("gameboards/{id}/")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public final Response updateGameboard(
			@Context final HttpServletRequest request,
			@PathParam("id") final String gameboardId,
			final GameboardDTO newGameboardObject) {
		
		RegisteredUserDTO user;
		try {
			user = api.getCurrentUser(request);
		} catch (NoUserLoggedInException e1) {
			return new SegueErrorResponse(Status.UNAUTHORIZED,
					"User not logged in. Unable to modify gameboards.")
					.toResponse();
		}
		
		if (null == newGameboardObject || null == gameboardId || newGameboardObject.getId() == null) {
			// Gameboard object must be there and have an id.
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"You must provide a gameboard object with updates and the "
					+ "id of the gameboard object in both the object and the endpoint")
					.toResponse();			
		}

		// The id in the path param should match the id of the gameboard object you send me.
		if (!newGameboardObject.getId().equals(gameboardId)) {
			// user not logged in return not authorized
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"The gameboard ID sent in the request body does not match the end point you used.")
					.toResponse();			
		}

		// find what the existing gameboard looks like.
		GameboardDTO existingGameboard;
		try {
			existingGameboard = gameManager.getGameboard(gameboardId);
			
			if (null == existingGameboard) {
				return new SegueErrorResponse(Status.NOT_FOUND,
						"No gameboard found with the id: " + gameboardId)
						.toResponse();
			}
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error whilst trying to access the gameboard in the database.", e).toResponse();
		}
		
		try {
			// go ahead and persist the gameboard (if it is only temporary) / link it to the users my boards account
			gameManager.linkUserToGameboard(existingGameboard, user);
			this.api.getLogManager().logEvent(user, request, ADD_BOARD_TO_PROFILE, existingGameboard.getId());
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error while attempting to save the gameboard.")
					.toResponse();
		}

		// Now determine if the user is trying to change the title and if they have permission.
		if (!(existingGameboard.getTitle() == null ? newGameboardObject.getTitle() == null : existingGameboard
				.getTitle().equals(newGameboardObject.getTitle()))) {

			// do they have permission?
			if (!existingGameboard.getOwnerUserId().equals(user.getDbId())) {
				// user not logged in return not authorized
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You are not allowed to change another user's gameboard.")
						.toResponse();
			}

			// ok so now we can change the title
			GameboardDTO updatedGameboard;
			try {
				updatedGameboard = gameManager.updateGameboardTitle(newGameboardObject);
			} catch (SegueDatabaseException e) {
				return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
						"Error whilst trying to update the gameboard.")
						.toResponse();
			}
			return Response.ok(updatedGameboard).build();
		}
		
		return Response.ok(existingGameboard).build();
	}
	
	
	/**
	 * Rest end point that gets a single page based on a given id.
	 * 
	 * @param request - so we can deal with caching.
	 * @param httpServletRequest - so that we can extract user information.
	 * @param pageId
	 *            as a string
	 * @return A Response object containing a page object or containing a SegueErrorResponse.
	 */
	@GET
	@Path("pages/{page}")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getPage(
			@Context final Request request,
			@Context final HttpServletRequest httpServletRequest,
			@PathParam("page") final String pageId) {
		// Calculate the ETag on current live version of the content
		// NOTE: Assumes that the latest version of the content is being used.
		EntityTag etag = new EntityTag(this.api.getLiveVersion().hashCode()
				+ pageId.hashCode() + "");
		
		Response cachedResponse = api.generateCachedResponse(request, etag);
		if (cachedResponse != null) {
			return cachedResponse;
		}
		
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(PAGE_TYPE));

		// options
		if (null != pageId) {
			fieldsToMatch.put(ID_FIELDNAME + "."
					+ UNPROCESSED_SEARCH_FIELD_SUFFIX, Arrays.asList(pageId));
		}
		
		Response result = this.findSingleResult(fieldsToMatch);
		
		if (result.getEntity() instanceof SeguePageDTO) {
			ImmutableMap<String, String> logEntry = new ImmutableMap.Builder<String, String>()
					.put(PAGE_ID_LOG_FIELDNAME, pageId)
					.put(CONTENT_VERSION, api.getLiveVersion()).build();
					
			// the request log
			this.api.getLogManager().logEvent(httpServletRequest, Constants.VIEW_PAGE, logEntry);			
		}
		
		Response cachableResult = Response.status(result.getStatus()).entity(result.getEntity())
				.cacheControl(api.getCacheControl()).tag(etag).build();
		return cachableResult;
	}

	
	/**
	 * Rest end point that gets a single page fragment based on a given id.
	 * @param request - so that we can deal with caching.
	 * @param fragmentId
	 *            as a string
	 * @return A Response object containing a page fragment object or containing
	 *         a SegueErrorResponse.
	 */
	@GET
	@Path("pages/fragments/{fragment_id}")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getPageFragment(
			@Context final Request request,
			@PathParam("fragment_id") final String fragmentId) {
		
		// Calculate the ETag on current live version of the content
		// NOTE: Assumes that the latest version of the content is being used.
		EntityTag etag = new EntityTag(this.api.getLiveVersion().hashCode()
				+ fragmentId.hashCode() + "");
		Response cachedResponse = api.generateCachedResponse(request, etag);
		if (cachedResponse != null) {
			return cachedResponse;
		}
		
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(PAGE_FRAGMENT_TYPE));

		// options
		if (null != fragmentId) {
			fieldsToMatch.put(ID_FIELDNAME + "."
					+ UNPROCESSED_SEARCH_FIELD_SUFFIX,
					Arrays.asList(fragmentId));
		}
		Response result = this.findSingleResult(fieldsToMatch);
		
		Response cachableResult = Response.status(result.getStatus()).entity(result.getEntity())
				.cacheControl(api.getCacheControl()).tag(etag).build();
		
		return cachableResult;
	}


	/**
	 * Rest end point to allow images to be requested from the database.
	 * 
	 * @param request
	 *            - used for intelligent cache responses.
	 * @param path
	 *            of image in the database
	 * @return a Response containing the image file contents or containing a
	 *         SegueErrorResponse.
	 */
	@GET
	@Produces("*/*")
	@Path("images/{path:.*}")
	@Cache
	@GZIP
	public final Response getImageByPath(@Context final Request request, @PathParam("path") final String path) {
		// entity tags etc are already added by segue
		return api.getImageFileContent(request, api.getLiveVersion(), path);
	}
	
	/**
	 * This method will extract basic information from a content object so the
	 * lighter ContentInfo object can be sent to the client instead.
	 * 
	 * @param content
	 *            - the content object to summarise
	 * @param proxyPath
	 *            - the path prefix used for augmentation of urls
	 * @return ContentSummaryDTO.
	 */
	private ContentSummaryDTO extractContentSummary(final ContentDTO content,
			final String proxyPath) {
		if (null == content) {
			return null;
		}

		// try auto-mapping
		Injector injector = Guice.createInjector(
				new IsaacGuiceConfigurationModule(),
				new SegueGuiceConfigurationModule());
		MapperFacade mapper = injector.getInstance(MapperFacade.class);

		ContentSummaryDTO contentInfo = mapper.map(content,
				ContentSummaryDTO.class);
		contentInfo.setUrl(URIManager.generateApiUrl(content));

		return contentInfo;
	}

	/**
	 * Utility method to convert a list of content objects into a list of
	 * ContentSummaryDTO Objects.
	 * 
	 * @param contentList
	 *            - the list of content to summarise.
	 * @param proxyPath
	 *            - the path used for augmentation of urls.
	 * @return list of shorter ContentSummaryDTO objects.
	 */
	private List<ContentSummaryDTO> extractContentSummaryFromList(
			final List<ContentDTO> contentList, final String proxyPath) {
		if (null == contentList) {
			return null;
		}

		List<ContentSummaryDTO> listOfContentInfo = new ArrayList<ContentSummaryDTO>();

		for (ContentDTO content : contentList) {
			ContentSummaryDTO contentInfo = extractContentSummary(content,
					proxyPath);
			if (null != contentInfo) {
				listOfContentInfo.add(contentInfo);
			}
		}
		return listOfContentInfo;
	}


	/**
	 * Utility method to convert a ResultsWrapper of content objects into one
	 * with ContentSummaryDTO objects.
	 * 
	 * @param contentList
	 *            - the list of content to summarise.
	 * @param proxyPath
	 *            - the path used for augmentation of urls.
	 * @return list of shorter ContentSummaryDTO objects.
	 */
	private ResultsWrapper<ContentSummaryDTO> extractContentSummaryFromResultsWrapper(
			final ResultsWrapper<ContentDTO> contentList, final String proxyPath) {
		if (null == contentList) {
			return null;
		}

		ResultsWrapper<ContentSummaryDTO> contentSummaryResults 
			= new ResultsWrapper<ContentSummaryDTO>(
				new ArrayList<ContentSummaryDTO>(),
				contentList.getTotalResults());

		for (ContentDTO content : contentList.getResults()) {
			ContentSummaryDTO contentInfo = extractContentSummary(content,
					proxyPath);
			if (null != contentInfo) {
				contentSummaryResults.getResults().add(contentInfo);
			}
		}
		return contentSummaryResults;
	}

	
	/**
	 * For use when we expect to only find a single result.
	 * 
	 * By default related content ContentSummary objects will be fully
	 * augmented.
	 * 
	 * @param fieldsToMatch
	 *            - expects a map of the form fieldname -> list of queries to
	 *            match
	 * @return A Response containing a single conceptPage or containing
	 *         a SegueErrorResponse.
	 */
	private Response findSingleResult(
			final Map<String, List<String>> fieldsToMatch) {
		ResultsWrapper<ContentDTO> conceptList = api.findMatchingContent(
				api.getLiveVersion(),
				SegueApiFacade.generateDefaultFieldToMatch(fieldsToMatch),
				null, null); // includes type checking.
		ContentDTO c = null;
		if (conceptList.getResults().size() > 1) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Multiple results (" + conceptList.getResults().size()
							+ ") returned error. For search query: "
							+ fieldsToMatch.values()).toResponse();
		} else if (conceptList.getResults().isEmpty()) {
			return new SegueErrorResponse(Status.NOT_FOUND,
					"No content found that matches the query with parameters: "
							+ fieldsToMatch.values()).toResponse();
		} else {
			c = conceptList.getResults().get(0);
		}

		// String proxyPath = propertiesLoader.getProperty(PROXY_PATH);
		// ContentPage cp = new ContentPage(c.getId(), c,
		// this.buildMetaContentmap(proxyPath, c));

		return Response.ok(api.augmentContentWithRelatedContent(api.getLiveVersion(), c)).build();
	}
	

	/**
	 * Helper method to query segue for a list of content objects.
	 * 
	 * This method will only use the latest version of the content.
	 * 
	 * @param fieldsToMatch
	 *            - expects a map of the form fieldname -> list of queries to
	 *            match
	 * @param startIndex
	 *            - the initial index for the first result.
	 * @param limit
	 *            - the maximums number of results to return
	 * @return Response builder containing a list of content summary objects or containing
	 *         a SegueErrorResponse
	 */
	private Response.ResponseBuilder listContentObjects(
			final Map<String, List<String>> fieldsToMatch,
			final String startIndex, final String limit) {
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

			c = api.findMatchingContent(api.getLiveVersion(),
					SegueApiFacade.generateDefaultFieldToMatch(fieldsToMatch),
					startIndexOfResults, resultsLimit);

		} catch (NumberFormatException e) {
			return new SegueErrorResponse(
					Status.BAD_REQUEST,
					"Unable to convert one of the integer parameters provided "
							+ "into numbers (null is ok). Params provided were: limit "
							+ limit + " and startIndex " + startIndex, e).toResponseBuilder();
		}

		ResultsWrapper<ContentSummaryDTO> summarizedContent = new ResultsWrapper<ContentSummaryDTO>(
				this.extractContentSummaryFromList(c.getResults(),
						propertiesLoader.getProperty(PROXY_PATH)),
				c.getTotalResults());

		return Response.ok(summarizedContent);
	}
}
