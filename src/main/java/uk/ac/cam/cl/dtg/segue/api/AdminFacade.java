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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;

/**
 * @author Stephen Cummins
 *
 */
@Path("/admin")
public class AdminFacade {
	private static final Logger log = LoggerFactory.getLogger(AdminFacade.class);
	
	private final UserManager userManager;
	private final ContentVersionController contentVersionController;

	/**
	 * Create an instance of the administrators facade.
	 * @param userManager
	 *            - The manager object responsible for users.
	 * @param contentVersionController
	 *            - The content version controller used by the api.        
	 */
	@Inject
	public AdminFacade(final UserManager userManager, final ContentVersionController contentVersionController) {
		this.userManager = userManager;
		this.contentVersionController = contentVersionController;
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
	@POST
	@Path("/live_version/{version}")
	@Produces(MediaType.APPLICATION_JSON)
	public final synchronized Response changeLiveVersion(
			@Context final HttpServletRequest request,
			@PathParam("version") final String version) {

		try {
			if (this.userManager.isUserAnAdmin(request)) {
				IContentManager contentPersistenceManager = contentVersionController
						.getContentManager();
				String newVersion;
				if (!contentPersistenceManager.isValidVersion(version)) {
					SegueErrorResponse error = new SegueErrorResponse(
							Status.BAD_REQUEST, "Invalid version selected: " + version);
					log.warn(error.getErrorMessage());
					return error.toResponse();
				}
				
				if (!contentPersistenceManager.getCachedVersionList().contains(version)) {
					newVersion = contentVersionController.triggerSyncJob(version);
				} else {
					newVersion = version;
				}
				
				Collection<String> availableVersions = contentPersistenceManager
						.getCachedVersionList();
				
				if (!availableVersions.contains(version)) {
					SegueErrorResponse error = new SegueErrorResponse(
							Status.BAD_REQUEST, "Invalid version selected: " + version);
					log.warn(error.getErrorMessage());
					return error.toResponse();
				}

				contentVersionController.setLiveVersion(newVersion);
				log.info("Live version of the site changed to: " + newVersion + " by user: "
						+ this.userManager.getCurrentRegisteredUser(request).getEmail());

				return Response.ok().build();
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
	
}
