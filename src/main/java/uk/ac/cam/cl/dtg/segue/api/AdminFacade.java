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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.Constants.EnvironmentType;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.api.managers.StatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Admin facade for segue.
 * 
 * @author Stephen Cummins
 * 
 */
@Path("/admin")
public class AdminFacade extends AbstractSegueFacade {
	private static final Logger log = LoggerFactory.getLogger(AdminFacade.class);

	private final UserManager userManager;
	private final ContentVersionController contentVersionController;

	private StatisticsManager statsManager;

	/**
	 * Create an instance of the administrators facade.
	 * 
	 * @param properties
	 *            - the fully configured properties loader for the api.
	 * @param userManager
	 *            - The manager object responsible for users.
	 * @param contentVersionController
	 *            - The content version controller used by the api.
	 * @param logManager
	 *            - So we can log events of interest.
	 * @param statsManager
	 *            - So we can report high level stats.
	 */
	@Inject
	public AdminFacade(final PropertiesLoader properties, final UserManager userManager,
			final ContentVersionController contentVersionController, final ILogManager logManager,
			final StatisticsManager statsManager) {
		super(properties, logManager);
		this.userManager = userManager;
		this.contentVersionController = contentVersionController;
		this.statsManager = statsManager;
	}

	/**
	 * Statistics endpoint.
	 * @param request - to determine access.
	 * @return stats
	 */
	@GET
	@Path("/stats/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getStatistics(@Context final HttpServletRequest request) {
		try {
			if (!isUserStaff(request)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an admin to access this endpoint.").toResponse();
			}
			
			return Response.ok(statsManager.outputGeneralStatistics()).build();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		}
	}
	
	/**
	 * Statistics endpoint.
	 * @param request - to determine access.
	 * @return stats
	 */
	@GET
	@Path("/stats/schools")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchoolStatistics(@Context final HttpServletRequest request) {
		try {
			if (!isUserAnAdmin(request)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an admin user to access this endpoint.").toResponse();
			}
			
			Map<School, Integer> map = statsManager.getUsersBySchool();
			
			final String school = "school";
			final String connections = "connections";

			List<Map<String, Object>> result = Lists.newArrayList();
			for (Entry<School, Integer> e : map.entrySet()) {
				result.add(ImmutableMap.of(school, e.getKey(), connections, e.getValue()));
			}
		
			Collections.sort(result, new Comparator<Map<String, Object>>() {
				/**
				 * Descending numerical order
				 */
				@Override
				public int compare(final Map<String, Object> o1, final Map<String, Object> o2) {

					if ((Integer) o1.get(connections) < (Integer) o2.get(connections)) {
						return 1;
					}

					if ((Integer) o1.get(connections) > (Integer) o2.get(connections)) {
						return -1;
					}

					return 0;
				}
			});
			
			return Response.ok(result).build();
		} catch (UnableToIndexSchoolsException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Unable To Index Schools Exception in admin facade", e).toResponse();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		}
	}
	
	/**
	 * Get user last seen information map.
	 * @param request - to determine access.
	 * @return stats
	 */
	@GET
	@Path("/stats/users/last_access")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUserLastAccessInformation(@Context final HttpServletRequest request) {
		try {
			if (!isUserAnAdmin(request)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an admin user to access this endpoint.").toResponse();
			}
			
			return Response.ok(statsManager.getLastSeenUserMap()).build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} 
	}		
	
	/**
	 * This method will allow the live version served by the site to be changed.
	 * 
	 * @param request
	 *            - to help determine access rights.
	 * @param version
	 *            - version to use as updated version of content store.
	 * @return Success shown by returning the new liveSHA or failed message
	 *         "Invalid version selected".
	 */
	@POST
	@Path("/live_version/{version}")
	@Produces(MediaType.APPLICATION_JSON)
	public final synchronized Response changeLiveVersion(@Context final HttpServletRequest request,
			@PathParam("version") final String version) {

		try {
			if (isUserAnAdmin(request)) {
				IContentManager contentPersistenceManager = contentVersionController.getContentManager();
				String newVersion;
				if (!contentPersistenceManager.isValidVersion(version)) {
					SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
							"Invalid version selected: " + version);
					log.warn(error.getErrorMessage());
					return error.toResponse();
				}

				if (!contentPersistenceManager.getCachedVersionList().contains(version)) {
					newVersion = contentVersionController.triggerSyncJob(version).get();
				} else {
					newVersion = version;
				}

				Collection<String> availableVersions = contentPersistenceManager.getCachedVersionList();

				if (!availableVersions.contains(version)) {
					SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
							"Invalid version selected: " + version);
					log.warn(error.getErrorMessage());
					return error.toResponse();
				}

				contentVersionController.setLiveVersion(newVersion);
				log.info("Live version of the site changed to: " + newVersion + " by user: "
						+ this.userManager.getCurrentRegisteredUser(request).getEmail());

				return Response.ok().build();
			} else {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be logged in as an admin to access this function.").toResponse();
			}
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (InterruptedException e) {
			log.error("ExecutorException during version change.", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error while trying to terminate a process.", e).toResponse();
		} catch (ExecutionException e) {
			log.error("ExecutorException during version change.", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error during verison change.", e).toResponse();
		}
	}

	/**
	 * This method will try to bring the live version that Segue is using to
	 * host content up-to-date with the latest in the database.
	 * 
	 * @param request
	 *            - to enable security checking.
	 * @return a response to indicate the synchronise job has triggered.
	 */
	@POST
	@Path("/synchronise_datastores")
	public final synchronized Response synchroniseDataStores(@Context final HttpServletRequest request) {
		try {
			// check if we are authorized to do this operation.
			// no authorisation required in DEV mode, but in PROD we need to be
			// an admin.
			if (!this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(
					Constants.EnvironmentType.PROD.name())
					|| isUserAnAdmin(request)) {
				log.info("Informed of content change; " + "so triggering new synchronisation job.");
				contentVersionController.triggerSyncJob().get();
				return Response.ok("success - job started").build();
			} else {
				log.warn("Unable to trigger synch job as not an admin or this server is set to the PROD environment.");
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an administrator to use this function.").toResponse();
			}
		} catch (NoUserLoggedInException e) {
			log.warn("Unable to trigger synch job as not logged in.");
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (InterruptedException e) {
			log.error("ExecutorException during synchronise datastores operation.", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error while trying to terminate a process.", e).toResponse();
		} catch (ExecutionException e) {
			log.error("ExecutorException during synchronise datastores operation.", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error during verison change.", e).toResponse();
		}
	}
	
	/**
	 * This method is only intended to be used on development / staging servers.
	 * 
	 * It will try to bring the live version that Segue is using to
	 * host content up-to-date with the latest in the database.
	 * 
	 * @param request
	 *            - to enable security checking.
	 * @return a response to indicate the synchronise job has triggered.
	 */
	@POST
	@Path("/new_version_alert")
	@Produces(MediaType.APPLICATION_JSON)
	public final synchronized Response versionChangeNotification(@Context final HttpServletRequest request) {
		// check if we are authorized to do this operation.
		// no authorisation required in DEV mode, but in PROD we need to be
		// an admin.
		try {
			if (!this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(
					Constants.EnvironmentType.PROD.name()) || this.isUserAnAdmin(request)) {
				log.info("Informed of content change; so triggering new async synchronisation job.");
				// on this occasion we don't want to wait for a response.
				contentVersionController.triggerSyncJob();
				return Response.ok().build();
			} else {
				log.warn("Unable to trigger synch job as this segue environment is "
						+ "configured in PROD mode unless you are an ADMIN.");
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an administrator to use this function on the PROD environment.").toResponse();
			}
		} catch (NoUserLoggedInException e) {
			return new SegueErrorResponse(Status.UNAUTHORIZED,
					"You must be logged in to use this function on a PROD environment.").toResponse();
		}
	}

	/**
	 * This method will delete all cached data from the CMS and any search
	 * indices.
	 * 
	 * @param request
	 *            - containing user session information.
	 * 
	 * @return the latest version id that will be cached if content is
	 *         requested.
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/clear_caches")
	public final synchronized Response clearCaches(@Context final HttpServletRequest request) {
		try {
			if (isUserAnAdmin(request)) {
				IContentManager contentPersistenceManager = contentVersionController.getContentManager();

				log.info("Clearing all caches...");
				contentPersistenceManager.clearCache();

				ImmutableMap<String, String> response = new ImmutableMap.Builder<String, String>().put(
						"result", "success").build();

				return Response.ok(response).build();
			} else {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an administrator to use this function.").toResponse();
			}

		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		}
	}

	/**
	 * Rest end point to allow content editors to see the content which failed
	 * to import into segue.
	 * 
	 * @param request
	 *            - to identify if the user is authorised.
	 * 
	 * @return a content object, such that the content object has children. The
	 *         children represent each source file in error and the grand
	 *         children represent each error.
	 */
	@GET
	@Path("/content_problems")
	@Produces(MediaType.APPLICATION_JSON)
	public final Response getContentProblems(@Context final HttpServletRequest request) {
		Map<Content, List<String>> problemMap = contentVersionController.getContentManager().getProblemMap(
				contentVersionController.getLiveVersion());

		if (this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(EnvironmentType.PROD.name())) {
			try {
				if (!isUserStaff(request)) {
					return new SegueErrorResponse(Status.FORBIDDEN,
							"You must be an admin to access this endpoint.").toResponse();

				}
			} catch (NoUserLoggedInException e) {
				return SegueErrorResponse.getNotLoggedInResponse();
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

				erroredContentObject.setId(pair.getKey().getId() + "_error_" + errors);

				child.getChildren().add(erroredContentObject);

				errors++;
			}
			c.getChildren().add(child);
			child.setId(pair.getKey().getId() + "_problem_report_" + errors);
		}

		c.setSubtitle("Total Broken files: " + brokenFiles + " Total errors : " + errors);

		return Response.ok(c).build();
	}

	/**
	 * List users by id or email.
	 * 
	 * @param httpServletRequest
	 *            - for checking permissions
	 * @param userId
	 *            - if searching by id
	 * @param email
	 *            - if searching by e-mail
	 * @return a userDTO or a segue error response
	 */
	@GET
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findUsers(@Context final HttpServletRequest httpServletRequest,
			@QueryParam("id") final String userId, @QueryParam("email") final String email) {
		try {
			if (!isUserAnAdmin(httpServletRequest)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be logged in as an admin to access this function.").toResponse();
			}
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		}

		try {
			RegisteredUserDTO userPrototype = new RegisteredUserDTO();
			if (null != userId && !userId.isEmpty()) {
				userPrototype.setDbId(userId);	
			}
			
			if (null != email && !email.isEmpty()) {
				userPrototype.setEmail(email);	
			}

			return Response.ok(this.userManager.findUsers(userPrototype)).build();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Database error while looking up user information.").toResponse();
		}
	}
	
	/**
	 * Get a user by id or email.
	 * 
	 * @param httpServletRequest
	 *            - for checking permissions
	 * @param userId
	 *            - if searching by id
	 * @return a userDTO or a segue error response
	 */
	@GET
	@Path("/users/{user_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findUsers(@Context final HttpServletRequest httpServletRequest,
			@PathParam("user_id") final String userId) {
		try {
			if (!isUserAnAdmin(httpServletRequest)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be logged in as an admin to access this function.").toResponse();
			}
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		}

		try {
			return Response.ok(this.userManager.getUserDTOById(userId)).build();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Database error while looking up user information.").toResponse();
		} catch (NoUserException e) {
			return new SegueErrorResponse(Status.NOT_FOUND,
					"Unable to locate the user with the requested id: " + userId).toResponse();
		}
	}
	
	/**
	 * Delete all user data for a particular user account.
	 * 
	 * @param httpServletRequest
	 *            - for checking permissions
	 * @param userId
	 *            - the id of the user to delete.
	 * @return a userDTO or a segue error response
	 */
	@DELETE
	@Path("/users/{user_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteUserAccount(@Context final HttpServletRequest httpServletRequest,
			@PathParam("user_id") final String userId) {
		try {
			if (!isUserAnAdmin(httpServletRequest)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be logged in as an admin to access this function.").toResponse();
			}
			
			RegisteredUserDTO currentlyLoggedInUser = this.userManager.getCurrentRegisteredUser(httpServletRequest);
			if (currentlyLoggedInUser.getDbId().equals(userId)) {
				return new SegueErrorResponse(Status.BAD_REQUEST,
						"You are not allowed to delete yourself.").toResponse();
			}
			
			this.userManager.deleteUserAccount(userId);
			log.info("Admin User: " + currentlyLoggedInUser.getEmail()
					+ " has just deleted the user account with id: " + userId);
			
			return Response.noContent().build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Database error while looking up user information.").toResponse();
		} catch (NoUserException e) {
			return new SegueErrorResponse(Status.NOT_FOUND,
					"Unable to locate the user with the requested id: " + userId).toResponse();
		}
	}
	
	/**
	 * Get users by school id.
	 * @param request - to determine access.
	 * @param schoolId - of the school of interest.
	 * @return stats
	 */
	@GET
	@Path("/users/schools/{school_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchoolStatistics(@Context final HttpServletRequest request,
			@PathParam("school_id") final String schoolId) {
		try {
			if (!isUserAnAdmin(request)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an admin user to access this endpoint.").toResponse();
			}
			
			return Response.ok(statsManager.getUsersBySchoolId(schoolId)).build();
		} catch (UnableToIndexSchoolsException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Unable To Index Schools Exception in admin facade", e).toResponse();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (ResourceNotFoundException e) {
			return new SegueErrorResponse(Status.NOT_FOUND, "We cannot locate the school requested")
					.toResponse();
		} catch (SegueDatabaseException e) {
			log.error("Error while trying to list users belonging to a school.", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Database error").toResponse();
		}
	}	
	
	/**
	 * Get users by school id.
	 * 
	 * @param request
	 *            - to determine access.
	 * @return ok
	 * @deprecated - one off function for db update.
	 */
	@GET
	@Path("/users/updateLastSeen")
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response updateLastSeen(@Context final HttpServletRequest request) {
		try {
			if (!isUserAnAdmin(request)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an admin user to access this endpoint.").toResponse();
			}
			
			final List<RegisteredUserDTO> allUsers = this.userManager.findUsers(new RegisteredUserDTO());
			final Map<String, Date> lastSeenUserMap = this.statsManager.getLastSeenUserMap();
			// may as well spawn a new thread to do the validation
			// work now.
			Thread bulkUpdateJob = new Thread() {
				@SuppressWarnings("deprecation")
				@Override
				public void run() {					
					log.info("Beginning lastSeen update");
					int updated = 0;
					for (RegisteredUserDTO user : allUsers) {
						Date lastEventDate = lastSeenUserMap.get(user.getDbId());
						if (user.getLastSeen() == null) {
							// use registration date if we need to.
							if (lastEventDate == null) {
								lastEventDate = user.getRegistrationDate();
								log.info("Using registration date for " + user.getDbId());
							}
							
							try {
								userManager.updateLastSeenData(user, lastEventDate);
								updated++;
							} catch (SegueDatabaseException e) {
								log.error("Error while updating last seen data", e);
							}
						}
					}
					
					log.info("LastSeen update complete - " + updated + " users updated");
				}
			};
			bulkUpdateJob.setDaemon(true);
			bulkUpdateJob.start();
			
			return Response.ok("update task started. See server logs for progress.").build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error").toResponse();
		}
	}	
	
	/**
	 * Is the current user an admin.
	 * 
	 * @param request - with session information
	 * @return true if user is logged in as an admin, false otherwise.
	 * @throws NoUserLoggedInException - if we are unable to tell because they are not logged in.
	 */
	private boolean isUserAnAdmin(final HttpServletRequest request) throws NoUserLoggedInException {
		return userManager.checkUserRole(request, Arrays.asList(Role.ADMIN));
	}
	
	/**
	 * Is the current user in a staff role.
	 * 
	 * @param request - with session information
	 * @return true if user is logged in as an admin, false otherwise.
	 * @throws NoUserLoggedInException - if we are unable to tell because they are not logged in.
	 */
	private boolean isUserStaff(final HttpServletRequest request) throws NoUserLoggedInException {
		return userManager.checkUserRole(request, Arrays.asList(Role.ADMIN, Role.STAFF, Role.CONTENT_EDITOR));
	}
}