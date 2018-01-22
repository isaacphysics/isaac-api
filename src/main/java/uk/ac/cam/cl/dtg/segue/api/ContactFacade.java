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

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTACT_US_FORM_USED;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.Inject;

/**
 * Contact Facade.
 */
@Path("/contact")
@Api(value = "/contact")
public class ContactFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(ContactFacade.class);

    private final UserAccountManager userManager;
    private final EmailManager emailManager;

    /**
     * 
     * @param properties
     *            - the fully configured properties loader for the api.
     * @param mapper
     *            - The Content mapper object used for polymorphic mapping of content objects.
     * @param userManager
     *            - The manager object responsible for users.
     * @param emailManager
     *            - An implementation of ICommunicator for sending communiques
     * @param logManager
     *            - An instance of the log manager used for recording usage of the CMS.
     */
    @Inject
    public ContactFacade(final PropertiesLoader properties, final ContentMapper mapper,
            final UserAccountManager userManager, final EmailManager emailManager, final ILogManager logManager) {
        super(properties, logManager);
        this.userManager = userManager;
        this.emailManager = emailManager;
    }

    /**
     * Endpoint that handles contact us form submissions.
     * 
     * @param form
     *            - Map containing the message details
     * @param request
     *            - for logging purposes.
     * @return - Successful response if no error occurs, otherwise error response
     */
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response contactUs(final Map<String, String> form, @Context final HttpServletRequest request) {
        if (form.get("firstName") == null || form.get("lastName") == null || form.get("emailAddress") == null
                || form.get("subject") == null || form.get("message") == null) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Missing form details.");
            return error.toResponse();
        }

        try {
            String currentUserId;
            String currentUserRole;
            if (userManager.isRegisteredUserLoggedIn(request)) {
                RegisteredUserDTO currentUser = this.userManager.getCurrentRegisteredUser(request);
                currentUserId = currentUser.getId().toString();
                currentUserRole = currentUser.getRole().toString();
            } else {
                currentUserId = "N/A";
                currentUserRole = "N/A";
            }

            emailManager.sendContactUsFormEmail(this.getProperties().getProperty(Constants.MAIL_RECEIVERS),
                    new ImmutableMap.Builder<String, Object>()
                            .put("contactGivenName", form.get("firstName") == null ? "" : form.get("firstName"))
                            .put("contactFamilyName", form.get("lastName") == null ? "" : form.get("lastName"))
                            .put("contactUserId", currentUserId)
                            .put("contactUserRole", currentUserRole)
                            .put("contactEmail", form.get("emailAddress") == null ? "" : form.get("emailAddress"))
                            .put("contactSubject", form.get("subject") == null ? "" : form.get("subject"))
                            .put("contactMessage", form.get("message") == null ? "" : form.get("message"))
                            .put("replyToName", String.format("%s %s", form.get("firstName"), form.get("lastName")))
                            .build());

            getLogManager().logEvent(userManager.getCurrentUser(request), request, CONTACT_US_FORM_USED,
                    ImmutableMap.of("message", String.format("%s %s (%s) - %s", form.get("firstName"), form.get("lastName"),
                            form.get("emailAddress"), form.get("message"))));

            return Response.ok().build();
        } catch (ContentManagerException e) {
            log.error("Content error has occurred", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "A content error has occurred.").toResponse();
        } catch (SegueDatabaseException e1) {
            log.error("Database error has occurred", e1);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "A database error has occurred.").toResponse();
        } catch (NoUserLoggedInException e2) {
            log.error("Unexpected NoUserLoggedInException!", e2);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "An error has occurred.").toResponse();
        }
    }
}
