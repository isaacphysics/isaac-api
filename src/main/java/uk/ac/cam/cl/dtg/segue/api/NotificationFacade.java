/*
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
package uk.ac.cam.cl.dtg.segue.api;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.NotificationPicker;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.isaac.dos.IUserNotification.NotificationStatus;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;

/**
 * This facade is to support the sending of notifications to lots of users. 
 * 
 * Currently we support sending something like a user study participation request to all users.
 */
@Path("/notifications")
@Tag(name = "/notifications")
public class NotificationFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(NotificationFacade.class);

    private NotificationPicker notificationPicker;
    private UserAccountManager userManager;

    /**
     * NotificationFacade.
     * 
     * @param properties
     *            - loader
     * @param logManager
     *            - log management
     * @param userManager
     *            - so we can look up user information.
     * @param notificationPicker
     *            - so we can identify which notifications are relevant.
     */
    @Inject
    public NotificationFacade(final PropertiesLoader properties, final ILogManager logManager,
            final UserAccountManager userManager, final NotificationPicker notificationPicker) {
        super(properties, logManager);
        this.userManager = userManager;
        this.notificationPicker = notificationPicker;
    }

    /**
     * @param request
     *            - for user lookup.
     * @return gets the list of all outstanding notifications.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "List any notifications for the current user.",
                  description = "This is the old notification delivery method. Newer notifications may be WebSocket based.")
    public Response getMyNotifications(@Context final HttpServletRequest request) {
        try {
            List<ContentDTO> listOfNotifications;
            try {
                listOfNotifications = notificationPicker.getAvailableNotificationsForUser(this.userManager
                        .getCurrentRegisteredUser(request));

            } catch (ContentManagerException e) {
                return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Problem locating the content", e)
                        .toResponse();
            } catch (SegueDatabaseException e) {
                log.error("Database Error", e);
                return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Internal Database Error", e).toResponse();
            }
            return Response.ok(listOfNotifications)
                    .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * updateNotificationStatus.
     * 
     * @param request
     *            - for user lookup.
     * @param notificationId
     *            - the id of interest.
     * @param responseFromUser
     *            - the response from the user.
     * @return success or error.
     */
    @POST
    @Path("/{notification_id}/{response_from_user}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Record user response to a notification.",
                  description = "The response should be one of: ACKNOWLEDGED, POSTPONED, DISABLED.")
    public Response updateNotificationStatus(@Context final HttpServletRequest request,
            @PathParam("notification_id") final String notificationId,
            @PathParam("response_from_user") final String responseFromUser) {
        RegisteredUserDTO user;
        
        try {
            NotificationStatus responseToSave = NotificationStatus.valueOf(responseFromUser.toUpperCase());
            user = this.userManager.getCurrentRegisteredUser(request);
            this.notificationPicker.recordNotificationAction(user, notificationId, responseToSave);
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (ResourceNotFoundException e) {
            return SegueErrorResponse.getResourceNotFoundResponse("The requested notification doesn't exist.");
        } catch (SegueDatabaseException e) {
            log.error("Database Error", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Internal Database Error", e).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content Error", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Content Manager Error", e).toResponse();
        } catch (IllegalArgumentException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "Illegal response: Must be either: DISMISSED, POSTPONED or DISABLED").toResponse();
        }

        return Response.noContent().build();
    }
}
