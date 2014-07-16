package uk.ac.cam.cl.dtg.isaac.api;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.cache.Cache;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Image;
import uk.ac.cam.cl.dtg.segue.dos.users.User;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;

/**
 * Isaac Controller
 * 
 * This class specifically caters for the Rutherford physics server and is
 * expected to provide extended functionality to the Segue api for use only on
 * the Rutherford site.
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
		
		
		// test of user registration - this is just a snippet for future
		// reference as I didn't know where else to put it.
		// User user = api.getCurrentUser(req);
		// // example of requiring user to be logged in.
		// if(null == user)
		// return api.authenticationInitialisation(req, "google");
		// else
		// log.info("User Logged in: " + user.getEmail());
	}
	
	/**
	 * Creates an instance of the isaac controller which provides the REST
	 * endpoints for the isaac api.
	 * 
	 * @param api - Instance of segue Api
	 * @param propertiesLoader - Instance of properties Loader
	 * @param gameManager - Instance of Game Manager
	 */
	public IsaacController(
			final SegueApiFacade api, 
			final PropertiesLoader propertiesLoader, 
			final GameManager gameManager) {
		this.api = api;
		this.propertiesLoader = propertiesLoader;
		this.gameManager = gameManager;
	}

	/**
	 * REST end point to provide a list of concepts.
	 * 
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
	@Produces("application/json")
	public final Response getConceptList(@QueryParam("ids") final String ids,
			@QueryParam("tags") final String tags,
			@QueryParam("start_index") final String startIndex,
			@QueryParam("limit") final String limit) {

		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(CONCEPT_TYPE));
		
		String newLimit = null;
		
		if (limit != null) {
			newLimit = limit;
		}
		
		// options
		if (ids != null) {
			List<String> idsList = Arrays.asList(ids.split(","));
			fieldsToMatch.put(ID_FIELDNAME, idsList);
			newLimit = String.valueOf(idsList.size());
		}
		
		if (tags != null) {
			fieldsToMatch.put(TAGS_FIELDNAME, Arrays.asList(tags.split(",")));
		}

		return listContentObjects(fieldsToMatch, startIndex, newLimit);
	}

	/**
	 * Rest end point that gets a single concept based on a given id.
	 * 
	 * @param conceptId
	 *            as a string
	 * @return A Response object containing a concept object.
	 */
	@GET
	@Path("pages/concepts/{concept_page_id}")
	@Produces("application/json")
	public final Response getConcept(
			@PathParam("concept_page_id") final String conceptId) {
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(CONCEPT_TYPE));

		// options
		if (null != conceptId) {
			fieldsToMatch
					.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX,
							Arrays.asList(conceptId));
		}

		return this.findSingleResult(fieldsToMatch);
	}

	/**
	 * REST end point to provide a list of questions.
	 *
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
	@Produces("application/json")
	public final Response getQuestionList(
			@QueryParam("ids") final String ids, 
			@QueryParam("tags") final String tags,
			@QueryParam("levels") final String level,
			@QueryParam("start_index") final String startIndex,
			@QueryParam("limit") final String limit) {

		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(QUESTION_TYPE));

		String newLimit = null;

		// options	
		if (limit != null) {
			newLimit = limit;
		}
		
		if (ids != null) {
			List<String> idsList = Arrays.asList(ids.split(","));
			fieldsToMatch.put(ID_FIELDNAME, idsList);
			newLimit = String.valueOf(idsList.size());
		}
	
		if (tags != null) {
			fieldsToMatch.put(TAGS_FIELDNAME, Arrays.asList(tags.split(",")));
		}

		if (level != null) {
			fieldsToMatch.put(LEVEL_FIELDNAME, Arrays.asList(level.split(",")));
		}
		
		return listContentObjects(fieldsToMatch, startIndex, newLimit);
	}

	/**
	 * Rest end point that gets a single question based on a given id.
	 * 
	 * @param questionId
	 *            as a string
	 * @return A Response object containing a concept object.
	 */
	@GET
	@Path("pages/questions/{question_page_id}")
	@Produces("application/json")
	public final Response getQuestion(
			@PathParam("question_page_id") final String questionId) {
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		fieldsToMatch.put("type", Arrays.asList(QUESTION_TYPE));

		// options
		if (null != questionId) {
			fieldsToMatch.put(ID_FIELDNAME + "."
					+ UNPROCESSED_SEARCH_FIELD_SUFFIX,
					Arrays.asList(questionId));
		}

		return this.findSingleResult(fieldsToMatch);
	}

	/**
	 * Rest end point that searches the api for some search string.
	 * 
	 * @param searchString
	 *            - to pass to the search engine.
	 * @param types
	 *            - a comma separated list of types to include in the search.
	 * @return a response containing the search results (results wrapper) or an
	 *         empty list.
	 */
	@SuppressWarnings("unchecked")
	@GET
	@Produces("application/json")
	@Path("search/{searchString}")
	public final Response search(
			@PathParam("searchString") final String searchString,
			@QueryParam("types") final String types) {
		ResultsWrapper<Content> searchResults = null;

		Response unknownApiResult = api.search(searchString,
				api.getLiveVersion(), types);
		if (unknownApiResult.getEntity() instanceof ResultsWrapper) {
			searchResults = (ResultsWrapper<Content>) unknownApiResult
					.getEntity();
		} else {
			return unknownApiResult;
		}

		return Response.ok(
				this.extractContentSummaryFromResultsWrapper(searchResults,
						propertiesLoader.getProperty(PROXY_PATH))).build();
	}

	/**
	 * REST end point to provide a gameboard containing a list of questions.
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
	 * @return a Response containing a gameboard object.
	 */
	@GET
	@Path("gameboards")
	@Produces("application/json")
	public final Response generateGameboard(
			@Context final HttpServletRequest request,
			@QueryParam("subjects") final String subjects,
			@QueryParam("fields") final String fields,
			@QueryParam("topics") final String topics,
			@QueryParam("levels") final String levels,
			@QueryParam("concepts") final String concepts) {
		// tags are and relationships except for subject
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

		try {
			GameboardDTO gameboard = gameManager.generateRandomGameboard(
					subjectsList, fieldsList, topicsList, levelsList,
					conceptsList, api.getCurrentUser(request));
			
			if (null == gameboard) {
				return new SegueErrorResponse(Status.NO_CONTENT, 
						"We cannot find any questions based on your filter criteria.").toResponse();
			}
			
			if (gameboard.getOwnerUserId() != null) {
				// go ahead and persist the gameboard
				gameManager.permanentlyStoreGameboard(gameboard);
			}

			return Response.ok(gameboard).build();
		} catch (IllegalArgumentException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"Your gameboard filter request is invalid.").toResponse();
		}
	}

	/**
	 * REST end point to provide a gameboard containing a list of questions.
	 * 
	 * @param gameboardId
	 *            - the unique id of the gameboard to be requested
	 * @return a Response containing a gameboard object.
	 */
	@GET
	@Path("gameboards/{gameboard_id}")
	@Produces("application/json")
	public final Response getGameboard(
			@PathParam("gameboard_id") final String gameboardId) {

		// tags are and relationships except for subject
		try {
			GameboardDTO gameboard = gameManager.getGameboard(gameboardId);

			if (null == gameboard) {
				return new SegueErrorResponse(Status.NOT_FOUND,
						"No Gameboard found for the id specified.")
						.toResponse();
			}

			return Response.ok(gameboard).build();
		} catch (IllegalArgumentException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"Your gameboard filter request is invalid.").toResponse();
		}
	}
	

	/**
	 * REST end point to find all of a user's gameboards.
	 * 
	 * @param request
	 *            - so that we can find out the currently logged in user
	 * @return a Response containing a list of gameboard objects.
	 */
	@GET
	@Path("users/current_user/gameboards")
	@Produces("application/json")
	public final Response getGameboardByUser(
			@Context final HttpServletRequest request) {
		User user = api.getCurrentUser(request);
		
		if (null == user) {
			// user not logged in return not authorized
			return new SegueErrorResponse(Status.UNAUTHORIZED, 
					"User not logged in. Unable to retrieve gameboards.")
				.toResponse();
		}

		List<GameboardDTO> gameboards = gameManager.getUsersGameboards(user.getDbId());
		
		if (null == gameboards) {
			return Response.noContent().build();
		}
		
		return Response.ok(gameboards).build();
	}	

	/**
	 * Rest end point that gets a single page based on a given id.
	 * 
	 * @param pageId
	 *            as a string
	 * @return A Response object containing a page object.
	 */
	@GET
	@Path("pages/{page}")
	@Produces("application/json")
	public final Response getPage(@PathParam("page") final String pageId) {
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(PAGE_TYPE));

		// options
		if (null != pageId) {
			fieldsToMatch
					.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX,
							Arrays.asList(pageId));
		}

		return this.findSingleResult(fieldsToMatch);
	}
	
	/**
	 * Rest end point that gets a single page fragment based on a given id.
	 * 
	 * @param fragmentId
	 *            as a string
	 * @return A Response object containing a page object.
	 */
	@GET
	@Path("pages/fragments/{fragment_id}")
	@Produces("application/json")
	public final Response getPageFragment(@PathParam("fragment_id") final String fragmentId) {
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(PAGE_FRAGMENT_TYPE));

		// options
		if (null != fragmentId) {
			fieldsToMatch
					.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX,
							Arrays.asList(fragmentId));
		}

		return this.findSingleResult(fieldsToMatch);
	}	

	/**
	 * Rest end point to allow images to be requested from the database.
	 * 
	 * @param path
	 *            of image in the database
	 * @return image file contents.
	 */
	@GET
	@Produces("*/*")
	@Path("images/{path:.*}")
	@Cache
	public final Response getImageByPath(@PathParam("path") final String path) {
		return api.getImageFileContent(api.getLiveVersion(), path);
	}

	// @POST
	// @Consumes({"application/x-www-form-urlencoded"})
	// @Path("contact-us/sendContactUsMessage")
	// public ImmutableMap<String, String> postContactUsMessage(
	// @FormParam("full-name") final String fullName,
	// @FormParam("email") final String email,
	// @FormParam("subject") final String subject,
	// @FormParam("message-text") final String messageText,
	// @Context final HttpServletRequest request) {
	//
	// // construct a new instance of the mailer object
	// Mailer contactUsMailer = new Mailer(
	// propertiesLoader.getProperty(MAILER_SMTP_SERVER),
	// propertiesLoader.getProperty(MAIL_FROM_ADDRESS));
	//
	// if (StringUtils.isBlank(fullName) && StringUtils.isBlank(email)
	// && StringUtils.isBlank(subject)
	// && StringUtils.isBlank(messageText)) {
	// log.debug("Contact us required field validation error ");
	// return ImmutableMap
	// .of("result",
	// "message not sent - Missing required field - Validation Error");
	// }
	//
	// // Get IpAddress of client
	// String ipAddress = request.getHeader("X-FORWARDED-FOR");
	//
	// if (ipAddress == null) {
	// ipAddress = request.getRemoteAddr();
	// }
	//
	// // Construct message
	// StringBuilder message = new StringBuilder();
	// message.append("- Sender Details - " + "\n");
	// message.append("From: " + fullName + "\n");
	// message.append("E-mail: " + email + "\n");
	// message.append("IP address: " + ipAddress + "\n");
	// message.append("Message Subject: " + subject + "\n");
	// message.append("- Message - " + "\n");
	// message.append(messageText);
	//
	// try {
	// // attempt to send the message via the smtp server
	// contactUsMailer.sendMail(
	// propertiesLoader.getProperty(MAIL_RECEIVERS).split(","),
	// email, subject, message.toString());
	// log.info("Contact Us - E-mail sent to "
	// + propertiesLoader.getProperty(MAIL_RECEIVERS) + " "
	// + email + " " + subject + " " + message.toString());
	//
	// } catch (AddressException e) {
	// log.warn("E-mail Address validation error " + e.toString());
	// return ImmutableMap.of("result",
	// "message not sent - E-mail address malformed - Validation Error \n "
	// + e.toString());
	//
	// } catch (MessagingException e) {
	// log.error("Messaging error " + e.toString());
	// return ImmutableMap.of(
	// "result",
	// "message not sent - Unknown Messaging error\n "
	// + e.toString());
	// }
	//
	// return ImmutableMap.of("result", "success");
	// }

//	/**
//	 * This method will look at a content objects related content list and
//	 * return a list of contentInfo objects which can be used for creating links
//	 * etc.
//	 * 
//	 * This method returns null if the content object provided has no related
//	 * Content
//	 * 
//	 * @param proxyPath
//	 *            - the string prefix for the server being used
//	 * @param content
//	 *            - the content object which contains related content
//	 * @return a list of content summary objects.
//	 */
//	private List<ContentSummary> buildMetaContentmap(final String proxyPath,
//			final Content content) {
//		if (null == content) {
//			return null;
//		} else if (content.getRelatedContent() == null
//				|| content.getRelatedContent().isEmpty()) {
//			return null;
//		}
//
//		List<ContentSummary> contentInfoList = new ArrayList<ContentSummary>();
//
//		for (String id : content.getRelatedContent()) {
//			try {
//				Content relatedContent = (Content) api.getContentById(
//						api.getLiveVersion(), id).getEntity();
//
//				if (relatedContent == null) {
//					log.warn("Related content (" + id
//							+ ") does not exist in the data store.");
//				} else {
//					ContentSummary contentInfo = extractContentSummary(
//							relatedContent, proxyPath);
//					contentInfoList.add(contentInfo);
//				}
//			} catch (ClassCastException exception) {
//				log.error("Error whilst trying to cast one object to another.",
//						exception);
//				// TODO: fix how SegueErrorResponse exception objects are
//				// handled - they clearly cannot be cast as content objects
//				// here.
//			}
//		}
//
//		return contentInfoList;
//	}

	/**
	 * Generate a URI that will enable us to find an object again.
	 * 
	 * @param content
	 *            the content object of interest
	 * @return null if we are unable to generate the URL or a string that
	 *         represents the url combined with any proxypath information
	 *         required.
	 */
	public static String generateApiUrl(final Content content) {
		Injector injector = Guice.createInjector(
				new IsaacGuiceConfigurationModule(),
				new SegueGuiceConfigurationModule());
		String proxyPath = injector.getInstance(PropertiesLoader.class)
				.getProperty(PROXY_PATH);

		String resourceUrl = null;
		try {
			// TODO fix this stuff to be less horrid
			if (content instanceof Image) {
				resourceUrl = proxyPath + "/api/images/"
						+ URLEncoder.encode(content.getId(), "UTF-8");
			} else if (content.getType().toLowerCase().contains("question")) {
				resourceUrl = proxyPath + "/api/pages/questions/"
						+ URLEncoder.encode(content.getId(), "UTF-8");
			} else if (content.getType().toLowerCase().contains("concept")) {
				resourceUrl = proxyPath + "/api/pages/concepts/"
						+ URLEncoder.encode(content.getId(), "UTF-8");
			} else {
				resourceUrl = proxyPath + "/api/pages/"
						+ URLEncoder.encode(content.getId(), "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			log.error("Url generation for resource id " + content.getId()
					+ " failed. ", e);
		}

		return resourceUrl;
	}

	/**
	 * This method will extract basic information from a content object so the
	 * lighter ContentInfo object can be sent to the client instead.
	 * 
	 * @param content
	 *            - the content object to summarise
	 * @param proxyPath
	 *            - the path prefix used for augmentation of urls
	 * @return Content summary object.
	 */
	private ContentSummaryDTO extractContentSummary(final Content content,
			final String proxyPath) {
		if (null == content) {
			return null;
		}

		// try auto-mapping with dozer
		Injector injector = Guice.createInjector(
				new IsaacGuiceConfigurationModule(),
				new SegueGuiceConfigurationModule());
		ModelMapper mapper = injector.getInstance(ModelMapper.class);

		ContentSummaryDTO contentInfo = mapper.map(content, ContentSummaryDTO.class);
		contentInfo.setUrl(generateApiUrl(content));

		return contentInfo;
	}

	/**
	 * Utility method to convert a list of content objects into a list of
	 * ContentInfo Objects.
	 * 
	 * @param contentList
	 *            - the list of content to summarise.
	 * @param proxyPath
	 *            - the path used for augmentation of urls.
	 * @return list of shorter contentInfo objects.
	 */
	private List<ContentSummaryDTO> extractContentSummaryFromList(
			final List<Content> contentList, final String proxyPath) {
		if (null == contentList) {
			return null;
		}

		List<ContentSummaryDTO> listOfContentInfo = new ArrayList<ContentSummaryDTO>();

		for (Content content : contentList) {
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
	 * with content Summary objects.
	 * 
	 * @param contentList
	 *            - the list of content to summarise.
	 * @param proxyPath
	 *            - the path used for augmentation of urls.
	 * @return list of shorter contentInfo objects.
	 */
	private ResultsWrapper<ContentSummaryDTO> extractContentSummaryFromResultsWrapper(
			final ResultsWrapper<Content> contentList, final String proxyPath) {
		if (null == contentList) {
			return null;
		}

		ResultsWrapper<ContentSummaryDTO> contentSummaryResults = new ResultsWrapper<ContentSummaryDTO>();

		for (Content content : contentList.getResults()) {
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
	 * @param fieldsToMatch
	 *            - expects a map of the form fieldname -> list of queries to
	 *            match
	 * @return A Response containing a single conceptPage or an error.
	 */
	private Response findSingleResult(
			final Map<String, List<String>> fieldsToMatch) {
		ResultsWrapper<Content> conceptList = api.findMatchingContent(
				api.getLiveVersion(),
				SegueApiFacade.generateDefaultFieldToMatch(fieldsToMatch),
				null, null); // includes type checking.
		Content c = null;
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

//		String proxyPath = propertiesLoader.getProperty(PROXY_PATH);
//		ContentPage cp = new ContentPage(c.getId(), c,
//				this.buildMetaContentmap(proxyPath, c));

		return Response.ok(c).build();
	}

	/**
	 * Helper method to query segue for a list of content objects.
	 * 
	 * @param fieldsToMatch
	 *            - expects a map of the form fieldname -> list of queries to
	 *            match
	 * @param startIndex
	 *            - the initial index for the first result.
	 * @param limit
	 *            - the maximums number of results to return
	 * @return Response containing a list of content summary objects
	 */
	private Response listContentObjects(
			final Map<String, List<String>> fieldsToMatch,
			final String startIndex, final String limit) {
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

			c = api.findMatchingContent(api.getLiveVersion(),
					SegueApiFacade.generateDefaultFieldToMatch(fieldsToMatch),
					startIndexOfResults, resultsLimit);

		} catch (NumberFormatException e) {
			return new SegueErrorResponse(
					Status.BAD_REQUEST,
					"Unable to convert one of the integer parameters provided "
							+ "into numbers (null is ok). Params provided were: limit "
							+ limit + " and startIndex " + startIndex, e)
					.toResponse();
		}

		ResultsWrapper<ContentSummaryDTO> summarizedContent = new ResultsWrapper<ContentSummaryDTO>(
				this.extractContentSummaryFromList(c.getResults(),
						propertiesLoader.getProperty(PROXY_PATH)),
				c.getTotalResults());

		return Response.ok(summarizedContent).build();
	}

}
