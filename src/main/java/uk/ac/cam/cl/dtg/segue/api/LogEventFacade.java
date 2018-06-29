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

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.LogEventMisuseHandler;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Map;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.ISAAC_LOG_TYPES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_LOG_TYPES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;

/**
 * LogEventFacade. This facade is responsible for allowing the front end to log arbitrary information in the log
 * database.
 */
@Path("/log")
@Api(value = "/log")
public class LogEventFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(LogEventFacade.class);

    private final IMisuseMonitor misuseMonitor;

    private final UserAccountManager userManager;

    /**
     * Injectable constructor.
     * 
     * @param properties
     *            the propertiesLoader.
     * @param logManager
     *            - For logging interesting user events.
     * @param misuseMonitor
     *            - So we can deal with misuse to incoming logged events.
     * @param userManager
     *            - So we can attribute log events to a given user.
     */
    @Inject
    public LogEventFacade(final PropertiesLoader properties, final ILogManager logManager,
            final IMisuseMonitor misuseMonitor, final UserAccountManager userManager) {
        super(properties, logManager);
        this.misuseMonitor = misuseMonitor;
        this.userManager = userManager;
    }

    /**
     * Method to allow clients to log front-end specific behaviour in the database.
     * 
     * @param httpRequest
     *            - to enable retrieval of session information.
     * @param eventJSON
     *            - the event information to record as a json map <String, String>.
     * @return 200 for success or 400 for failure.
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postLog(@Context final HttpServletRequest httpRequest, final Map<String, Object> eventJSON) {
        if (null == eventJSON || eventJSON.get(TYPE_FIELDNAME) == null) {
            log.error("Error during log operation, no event type specified. Event: " + eventJSON);
            return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to record log message as the log has no "
                    + TYPE_FIELDNAME + " property.").toResponse();
        }

        String eventType = (String) eventJSON.get(TYPE_FIELDNAME);

        if (SEGUE_LOG_TYPES.contains(eventType) || ISAAC_LOG_TYPES.contains(eventType)) {
            return new SegueErrorResponse(Status.FORBIDDEN, "Unable to record log message, restricted '"
                    + TYPE_FIELDNAME + "' value.").toResponse();
        }

        // implement arbitrary log size limit.
        AbstractSegueUserDTO currentUser = userManager.getCurrentUser(httpRequest);
        String uid;
        if (currentUser instanceof AnonymousUserDTO) {
            uid = ((AnonymousUserDTO) currentUser).getSessionId();
        } else {
            uid = ((RegisteredUserDTO) currentUser).getId().toString();
        }

        try {
            misuseMonitor.notifyEvent(uid, LogEventMisuseHandler.class.toString(), httpRequest.getContentLength());
        } catch (SegueResourceMisuseException e) {
            log.error(String.format("Logging Event Failed - log event requested (%s bytes) "
                    + "and would exceed daily limit size limit (%s bytes) ", httpRequest.getContentLength(),
                    Constants.MAX_LOG_REQUEST_BODY_SIZE_IN_BYTES));
            return SegueErrorResponse.getRateThrottledResponse(String.format(
                    "Log event request (%s bytes) would exceed limit for this endpoint.",
                    httpRequest.getContentLength()));
        }

        // remove the type information as we don't need it.
        eventJSON.remove(TYPE_FIELDNAME);

        this.getLogManager().logExternalEvent(this.userManager.getCurrentUser(httpRequest), httpRequest, eventType, eventJSON);

        return Response.ok().cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
    }
}
