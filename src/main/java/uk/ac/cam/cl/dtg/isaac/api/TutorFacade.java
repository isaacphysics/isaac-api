/*
 * Copyright 2024 Meurig Thomas
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
package uk.ac.cam.cl.dtg.isaac.api;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.TutorManager;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.io.IOException;
import java.util.Map;

import static jakarta.ws.rs.core.Response.ok;


/**
 * TutorFacade. This class is responsible for handling requests to the /tutor endpoint.
 */
@Path("/tutor")
@Tag(name = "/tutor")
public class TutorFacade extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(QuizFacade.class);

    private final TutorManager tutorManager;

    /**
     * TutorFacade.
     *
     * @param properties
     *            - the properties loader.
     * @param logManager
     *            - the log manager.
     */
    @Inject
    public TutorFacade(final AbstractConfigLoader properties, final ILogManager logManager,
                       final TutorManager tutorManager) {
        super(properties, logManager);
        this.tutorManager = tutorManager;
    }

    @POST
    @Path("/threads")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Create a new tutor thread")
    public final Response createNewTutorThread() {
        try {
            // TODO check the user is signed in
            return ok(tutorManager.createNewThread()).build();
        } catch (IOException e) {
            log.error("Failed to create new tutor thread", e);
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/threads/{threadId}/messages")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Retrieve thread's messages")
    public final Response retrieveMessages(@PathParam("threadId") final String threadId) {
        try {
            // TODO check the user is signed in and owns the thread
            return ok(tutorManager.getThreadMessages(threadId)).build();
        } catch (IOException e) {
            log.error("Failed to retrieve tutor messages", e);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("/threads/{threadId}/messages")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Add a message to a thread")
    public final Response addMessageToThread(@PathParam("threadId") final String threadId, final String jsonMessage) {
        try {
            if (null == jsonMessage || jsonMessage.isEmpty()) {
                return new SegueErrorResponse(Response.Status.BAD_REQUEST, "No message received.").toResponse();
            }

            // TODO check the user is signed in and owns the thread

            return ok(tutorManager.addMessageToThread(threadId, jsonMessage)).build();
        } catch (IOException e) {
            log.error("Failed to send message to tutor", e);
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/threads/{threadId}/runs/{runId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Retrieve a thread's run status")
    public final Response retrieveRun(
            @PathParam("threadId") final String threadId, @PathParam("runId") final String runId
    ) {
        try {
            // TODO check the user is signed in and owns the thread
            return ok(tutorManager.getRun(threadId, runId)).build();
        } catch (IOException e) {
            log.error("Failed to check assistant status", e);
            return Response.serverError().build();
        }
    }

}
