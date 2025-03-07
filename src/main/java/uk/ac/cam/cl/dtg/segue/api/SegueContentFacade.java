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

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.GZIP;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import java.util.Set;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Segue Content Facade
 * 
 * This facade lists metadata about content on the platform.
 * 
 */
@Path("/content")
@Tag(name = "SegueContentFacade", description = "/content")
public class SegueContentFacade extends AbstractSegueFacade {

    private final GitContentManager contentManager;

    /**
     * @param properties
     *            - the fully configured properties loader for the api.
     * @param contentManager
     *            - The content version controller used by the api.
     * @param logManager
     *            - An instance of the log manager used for recording usage of the CMS.

     */
    @Inject
    public SegueContentFacade(final AbstractConfigLoader properties, final GitContentManager contentManager,
                              final ILogManager logManager) {
        super(properties, logManager);

        this.contentManager = contentManager;
    }

    /**
     * This method provides a set of all tags for the live version of the content.
     * 
     * @param request
     *            so that we can determine whether we can make use of caching via etags.
     * @return a set of all tags used in the live version
     */
    @GET
    @Path("tags")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "List all tags currently in use.")
    public final Response getTagListByLiveVersion(@Context final Request request) {
        // Calculate the ETag on last modified date of tags list
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode()
                + "tagList".hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        Set<String> tags = this.contentManager.getTagsList();

        return Response.ok(tags).cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true)).tag(etag).build();
    }

    /**
     * This method provides a set of all units for the live version of the content.
     * 
     * @param request
     *            - so that we can set cache headers.
     * @return a set of all units used in the live version
     */
    @GET
    @Path("units")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "List all units currently in use by numeric questions.")
    public final Response getAllUnitsByLiveVersion(@Context final Request request) {
        // Calculate the ETag on last modified date of tags list
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode()
                + "unitsList".hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        Collection<String> units;
        units = this.contentManager.getAllUnits();

        return Response.ok(units).tag(etag).cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_DAY, true)).build();
    }
}
