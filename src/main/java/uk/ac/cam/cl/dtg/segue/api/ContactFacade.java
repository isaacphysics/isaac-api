/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.api;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Contact Facade.
 */
@Path("/contact")
@Tag(name = "/contact")
public class ContactFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(ContactFacade.class);

    private final UserAccountManager userManager;
    private final EmailManager emailManager;

    /**
     * 
     * @param properties
     *            - the fully configured properties loader for the api.
     * @param userManager
     *            - The manager object responsible for users.
     * @param emailManager
     *            - An implementation of ICommunicator for sending communiques
     * @param logManager
     *            - An instance of the log manager used for recording usage of the CMS.
     */
    @Inject
    public ContactFacade(final AbstractConfigLoader properties, final UserAccountManager userManager,
                         final EmailManager emailManager, final ILogManager logManager) {
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
    @Operation(summary = "Submit a contact form request.")
    public Response contactUs(final Map<String, String> form, @Context final HttpServletRequest request) {
        if (StringUtils.isEmpty(form.get("firstName")) || StringUtils.isEmpty(form.get("lastName"))
                || StringUtils.isEmpty(form.get("emailAddress")) || StringUtils.isEmpty(form.get("subject"))
                || StringUtils.isEmpty(form.get("message"))) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Missing form details.");
            return error.toResponse();
        }

        if (!EmailValidator.getInstance().isValid(form.get("emailAddress"))) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid email address.");
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

            String plainTextMessage = form.get("message");
            String htmlMessage = StringEscapeUtils.escapeHtml4(plainTextMessage).replace("\n", "\n<br>");

            emailManager.sendContactUsFormEmail(this.getProperties().getProperty(Constants.MAIL_RECEIVERS),
                    new ImmutableMap.Builder<String, Object>()
                            .put("contactGivenName", form.get("firstName"))
                            .put("contactFamilyName", form.get("lastName"))
                            .put("contactUserId", currentUserId)
                            .put("contactUserRole", currentUserRole)
                            .put("contactEmail", form.get("emailAddress"))
                            .put("contactSubject", form.get("subject"))
                            .put("contactMessage", plainTextMessage)
                            .put("contactMessage_HTML", htmlMessage)
                            .put("replyToName", String.format("%s %s", form.get("firstName"), form.get("lastName")))
                            .build());

            getLogManager().logEvent(
                    userManager.getCurrentUser(request), request, SegueServerLogType.CONTACT_US_FORM_USED,
                    ImmutableMap.of(
                            "message", String.format("%s %s (%s) - %s",
                            form.get("firstName"), form.get("lastName"),
                            form.get("emailAddress"), form.get("message"))
                    )
            );

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
