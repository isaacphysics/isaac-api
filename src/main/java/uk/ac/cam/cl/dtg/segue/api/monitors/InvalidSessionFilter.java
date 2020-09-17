/**
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.api.monitors;

import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Filters out requests which have a segue auth cookie but requesting the user throws a NoUserLoggedInException.
 *
 */
@Provider
public class InvalidSessionFilter implements ContainerRequestFilter {
    private final UserAccountManager userAccountManager;
    private final UserAuthenticationManager userAuthenticationManager;

    @Context
    private HttpServletRequest request;

    /**
     * InvalidSessionFilter.
     */
    @Inject
    public InvalidSessionFilter(final UserAccountManager userAccountManager, final UserAuthenticationManager userAuthenticationManager) {
        this.userAccountManager = userAccountManager;
        this.userAuthenticationManager = userAuthenticationManager;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        boolean hasAuthCookie = userAuthenticationManager.hasAuthCookie(request);
        if (hasAuthCookie) {
            try {
                if (!(requestContext.getUriInfo().getPath()).equals("/auth/logout")) {
                    userAccountManager.getCurrentRegisteredUser(request);
                }
            } catch (NoUserLoggedInException e) {
                // Has an auth cookie but no user is logged in -> cookie is invalid
                requestContext.abortWith(Response
                        .status(Response.Status.PRECONDITION_FAILED)
                        .entity("Auth cookie is invalid")
                        .build());
            }
        }
    }
}
