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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * AuthorizationFacade.
 * 
 * @author Stephen Cummins
 */
@Path("/authorize")
public class AuthorizationFacade extends AbstractSegueFacade {
	private final UserManager userManager;

	/**
	 * Create an instance of the authentication Facade.
	 * 
	 * @param properties
	 *            - properties loader for the application
	 * @param userManager
	 *            - user manager for the application
	 * @param logManager
	 *            - so we can log interesting events.
	 */
	@Inject
	public AuthorizationFacade(final PropertiesLoader properties, final UserManager userManager,
			final ILogManager logManager) {
		super(properties, logManager);
		this.userManager = userManager;
	}

	/**
	 * Function to allow users to create an AssociationToken.
	 * 
	 * This token can be used by another user to grant view permissions to their
	 * user data.
	 * 
	 * @param request
	 *            - so we can find out who the current user is
	 * @param label
	 *            - so we can create a group for associated users to fall into.
	 * @return a Response containing an association token or a
	 *         SegueErrorResponse.
	 */
	@GET
	@Path("/token/{label}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAssociationToken(@Context final HttpServletRequest request,
			@PathParam("label") final String label) {

		return null;
	}
}
