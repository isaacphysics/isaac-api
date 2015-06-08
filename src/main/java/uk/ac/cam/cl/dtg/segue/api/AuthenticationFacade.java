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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.api.client.util.Maps;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * AuthenticationFacade.
 * 
 * @author Stephen Cummins
 */
@Path("/auth")
public class AuthenticationFacade extends AbstractSegueFacade {

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
    public AuthenticationFacade(final PropertiesLoader properties, final UserManager userManager,
            final ILogManager logManager) {
        super(properties, logManager);
        this.userManager = userManager;
    }

    /**
     * This is the initial step of the authentication process.
     * 
     * @param request
     *            - the http request of the user wishing to authenticate
     * @param signinProvider
     *            - string representing the supported auth provider so that we know who to redirect the user to.
     * @return Redirect response to the auth providers site.
     */
    @GET
    @Path("/{provider}/authenticate")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response authenticate(@Context final HttpServletRequest request,
            @PathParam("provider") final String signinProvider) {

        return userManager.authenticate(request, signinProvider);
    }

    /**
     * Link existing user to provider.
     * 
     * @param request
     *            - the http request of the user wishing to authenticate
     * @param authProviderAsString
     *            - string representing the supported auth provider so that we know who to redirect the user to.
     * 
     * @return a redirect to where the client asked to be redirected to.
     */
    @GET
    @Path("/{provider}/link")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response linkExistingUserToProvider(@Context final HttpServletRequest request,
            @PathParam("provider") final String authProviderAsString) {
        if (!this.userManager.isRegisteredUserLoggedIn(request)) {
            return SegueErrorResponse.getNotLoggedInResponse();
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
    @Path("/{provider}/link")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response unlinkUserFromProvider(@Context final HttpServletRequest request,
            @PathParam("provider") final String authProviderAsString) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);
            this.userManager.unlinkUserFromProvider(user, authProviderAsString);
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable to remove account due to a problem with the database.", e).toResponse();
        } catch (MissingRequiredFieldException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "Unable to remove account as this will mean that the user cannot login again in the future.", e)
                    .toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AuthenticationProviderMappingException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to map to a known authenticator. The provider: "
                    + authProviderAsString + " is unknown").toResponse();
        }

        return Response.status(Status.NO_CONTENT).build();
    }

    /**
     * This is the callback url that auth providers should use to send us information about users.
     * 
     * @param request
     *            - http request from user
     * @param response
     *            to tell the browser to store the session in our own segue cookie if successful.
     * @param signinProvider
     *            - requested signing provider string
     * @return Redirect response to send the user to the home page.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{provider}/callback")
    public final Response authenticationCallback(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, @PathParam("provider") final String signinProvider) {
        return userManager.authenticateCallback(request, response, signinProvider);
    }

    /**
     * This is the initial step of the authentication process.
     * 
     * @param request
     *            - the http request of the user wishing to authenticate
     * @param response
     *            to tell the browser to store the session in our own segue cookie if successful.
     * @param signinProvider
     *            - string representing the supported auth provider so that we know who to redirect the user to.
     * @param credentials
     *            - optional field for local authentication only. Credentials should be specified within a user object.
     *            e.g. email and password.
     * @return The users DTO or a SegueErrorResponse
     */
    @POST
    @Path("/{provider}/authenticate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response authenticateWithCredentials(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response, @PathParam("provider") final String signinProvider,
            final Map<String, String> credentials) {
        // ok we need to hand over to user manager
        return userManager.authenticateWithCredentials(request, response, signinProvider, credentials);
    }

    /**
     * End point that allows the user to logout - i.e. destroy our cookie.
     * 
     * @param request
     *            so that we can destroy the associated session
     * @param response
     *            to tell the browser to delete the session for segue.
     * @return successful response to indicate any cookies were destroyed.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    @Path("/logout")
    public final Response userLogout(@Context final HttpServletRequest request,
            @Context final HttpServletResponse response) {

        this.getLogManager().logEvent(this.userManager.getCurrentUser(request), request, Constants.LOG_OUT,
                Maps.newHashMap());

        userManager.logUserOut(request, response);

        return Response.ok().build();
    }
}
