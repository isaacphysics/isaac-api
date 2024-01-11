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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;


/**
 * TutorFacade. This class is responsible for handling requests to the /tutor endpoint.
 */
@Path("/tutor")
@Tag(name = "/tutor")
public class TutorFacade extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(QuizFacade.class);

    /**
     * TutorFacade.
     *
     * @param properties
     *            - the properties loader.
     * @param logManager
     *            - the log manager.
     */
    @Inject
    public TutorFacade(final AbstractConfigLoader properties, final ILogManager logManager) {
        super(properties, logManager);
    }

    @POST
    @Path("/threads")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new tutor thread")
    public final Response createATutorThread(@Context final Request request, @Context final HttpServletRequest httpServletRequest) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

}
