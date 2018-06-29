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

import static uk.ac.cam.cl.dtg.segue.api.Constants.LOCAL_AUTH_EMAIL_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOCAL_AUTH_PASSWORD_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.REDIRECT_URL;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SegueLogType;
import io.swagger.annotations.Api;

import java.io.IOException;
import java.net.URI;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AccountAlreadyLinkedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationCodeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CrossSiteRequestForgeryException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.DuplicateAccountException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
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
@Api(value = "/auth")
public class AuthenticationFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationFacade.class);

    private final UserAccountManager userManager;

    private final IMisuseMonitor misuseMonitor;

    /**
     * Create an instance of the authentication Facade.
     * 
     * @param properties
     *            - properties loader for the application
     * @param userManager
     *            - user manager for the application
     * @param logManager
     *            - so we can log interesting events.
     * @param misuseMonitor
     *            - so that we can prevent overuse of protected resources.
     */
    @Inject
    public AuthenticationFacade(final PropertiesLoader properties, final UserAccountManager userManager,
            final ILogManager logManager, final IMisuseMonitor misuseMonitor) {
        super(properties, logManager);
        this.userManager = userManager;
        this.misuseMonitor = misuseMonitor;
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
        
        if (userManager.isRegisteredUserLoggedIn(request)) {
            // if they are already logged in then we do not want to proceed with
            // this authentication flow. We can just return an error response
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "The user is already logged in. You cannot authenticate again.").toResponse();            
        }
        
        try {
            Map<String, URI> redirectResponse = new ImmutableMap.Builder<String, URI>()
                    .put(REDIRECT_URL, userManager.authenticate(request, signinProvider)).build();
            
            return Response.ok(redirectResponse).build();
        }  catch (IOException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "IOException when trying to redirect to OAuth provider", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (AuthenticationProviderMappingException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                    "Error mapping to a known authenticator. The provider: " + signinProvider + " is unknown");
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
        
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
        
        try {
            Map<String, URI> redirectResponse = new ImmutableMap.Builder<String, URI>()
                    .put(REDIRECT_URL, this.userManager.initiateLinkAccountToUserFlow(request, authProviderAsString))
                    .build();
            
            return Response.ok(redirectResponse).build();   
        } catch (IOException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "IOException when trying to redirect to OAuth provider", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (AuthenticationProviderMappingException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                    "Error mapping to a known authenticator. The provider: " + authProviderAsString + " is unknown");
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
        
    }

    /**
     * End point that allows the user to remove a third party auth provider.
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

        try {
            return Response.ok(userManager.authenticateCallback(request, response, signinProvider)).build();
        } catch (IOException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Exception while trying to authenticate a user" + " - during callback step.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (NoUserException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.UNAUTHORIZED,
                    "Unable to locate user information.");
            log.error("No userID exception received. Unable to locate user.", e);
            return error.toResponse();
        } catch (AuthenticationCodeException | CrossSiteRequestForgeryException | AuthenticatorSecurityException
                | CodeExchangeException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.UNAUTHORIZED, e.getMessage());
            log.info("Error detected during authentication: " + e.getClass().toString(), e);
            return error.toResponse();
        } catch (DuplicateAccountException e) {
            log.debug("Duplicate user already exists in the database.", e);
            return new SegueErrorResponse(Status.FORBIDDEN, e.getMessage()).toResponse();
        } catch (AccountAlreadyLinkedException e) {
            log.error("Internal Database error during authentication", e);
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "The account you are trying to link is already attached to a user of this system.").toResponse();
        } catch (SegueDatabaseException e) {
            log.error("Internal Database error during authentication", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Internal database error during authentication.").toResponse();
        } catch (AuthenticationProviderMappingException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to map to a known authenticator. The provider: "
                    + signinProvider + " is unknown").toResponse();
        }

    }

    /**
     * This is the initial step of the authentication process for users who have a local account.
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

        
        // in this case we expect a username and password to have been
        // sent in the json response.
        if (null == credentials || credentials.get(LOCAL_AUTH_EMAIL_FIELDNAME) == null
                || credentials.get(LOCAL_AUTH_PASSWORD_FIELDNAME) == null) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
                    "You must specify credentials email and password to use this authentication provider.");
            return error.toResponse();
        }
        
        String email = credentials.get(LOCAL_AUTH_EMAIL_FIELDNAME);
        String password = credentials.get(LOCAL_AUTH_PASSWORD_FIELDNAME);
        
        final String rateThrottleMessage = "There have been too many attempts to login to this account. "
                + "Please try again after 10 minutes.";

        // Stop users logging in who have already locked their account.
        if (misuseMonitor.hasMisused(credentials.get(LOCAL_AUTH_EMAIL_FIELDNAME).toLowerCase(),
                SegueLoginMisuseHandler.class.toString())) {
            log.error("Segue Login Blocked for (" + credentials.get(LOCAL_AUTH_EMAIL_FIELDNAME)
                    + "). Rate limited - too many logins.");
            return SegueErrorResponse.getRateThrottledResponse(rateThrottleMessage);
        }

        // ok we need to hand over to user manager
        try {
            return Response
                    .ok(userManager.authenticateWithCredentials(request, response, signinProvider, email, password))
                    .build();
        } catch (AuthenticationProviderMappingException e) {
            String errorMsg = "Unable to locate the provider specified";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.BAD_REQUEST, errorMsg).toResponse();
        } catch (IncorrectCredentialsProvidedException | NoUserException | NoCredentialsAvailableException e) {
            try {
                misuseMonitor.notifyEvent(credentials.get(LOCAL_AUTH_EMAIL_FIELDNAME).toLowerCase(),
                        SegueLoginMisuseHandler.class.toString());

                log.info("Incorrect credentials received for (" + credentials.get(LOCAL_AUTH_EMAIL_FIELDNAME)
                        + "). Error reason: " + e.getMessage());
                return new SegueErrorResponse(Status.UNAUTHORIZED, "Incorrect credentials provided.").toResponse();
            } catch (SegueResourceMisuseException e1) {
                log.error("Segue Login Blocked for (" + credentials.get(LOCAL_AUTH_EMAIL_FIELDNAME)
                        + "). Rate limited - too many logins.");
                return SegueErrorResponse.getRateThrottledResponse(rateThrottleMessage);
            }
        } catch (SegueDatabaseException e) {
            String errorMsg = "Internal Database error has occurred during authentication.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        }
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

        this.getLogManager().logEvent(this.userManager.getCurrentUser(request), request, SegueLogType.LOG_OUT,
                Maps.newHashMap());

        userManager.logUserOut(request, response);

        return Response.ok().build();
    }
}
