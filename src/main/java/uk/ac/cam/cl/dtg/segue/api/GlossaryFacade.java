/*
 * Copyright 2019 Andrea Franceschini
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

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Glossary Facade
 *
 * This facade is intended to provide access to glossary terms.
 *
 */
@Path("/glossary")
@Api(value = "/glossary")
public class GlossaryFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(GlossaryFacade.class);

    private final IContentManager contentManager;
    private final String contentIndex;

    /**
     * @param properties     - to allow access to system properties.
     * @param contentManager - So that metadata about content can be accessed.
     * @param logManager     - for logging events using the logging api.
     */
    @Inject
    public GlossaryFacade(final PropertiesLoader properties, final IContentManager contentManager,
                          @Named(CONTENT_INDEX) final String contentIndex,
                          final ILogManager logManager) {
        super(properties, logManager);
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
    }

    /**
     * Gets all the glossary terms that are indexed.
     */
    @GET
    @Path("terms")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all the glossary terms that are indexed.")
    public final Response getTerms(@QueryParam("start_index") final String startIndex, @QueryParam("limit") final String limit) {
        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatch = Maps.newHashMap();
        fieldsToMatch.put(immutableEntry(BooleanOperator.AND, TYPE_FIELDNAME), Collections.singletonList("glossaryTerm"));

        ResultsWrapper<ContentDTO> c;
        try {
            Integer resultsLimit = null;
            Integer startIndexOfResults = null;

            if (null != limit) {
                resultsLimit = Integer.parseInt(limit);
            } else {
                resultsLimit = DEFAULT_RESULTS_LIMIT;
            }

            if (null != startIndex) {
                startIndexOfResults = Integer.parseInt(startIndex);
            } else {
                startIndexOfResults = 0;
            }

            c = this.contentManager.findByFieldNames(this.contentIndex, fieldsToMatch, startIndexOfResults, resultsLimit);
            return Response.ok(c).build();
        } catch (NumberFormatException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "Unable to convert one of the integer parameters provided into numbers. "
                            + "Params provided were: limit" + DEFAULT_RESULTS_LIMIT + " and startIndex " + 0, e).toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Content acquisition error.", e).toResponse();
        }
    }

    /**
     * Gets the current version of the segue application.
     *
     * @return segue version as a string wrapped in a response.
     */
    @GET
    @Path("terms/{term_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get the term with the given id.")
    public final Response getTermById(@PathParam("term_id") final String term_id) {
        if (null == term_id) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Please specify a term_id.").toResponse();
        }

        Content c = null;
        try {
            c = this.contentManager.getContentDOById(this.contentIndex, term_id);
            if (null == c) {
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "No glossary term found with id: " + term_id);
                log.debug(error.getErrorMessage());
                return error.toResponse();
            }
        } catch (NumberFormatException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "Unable to convert one of the integer parameters provided into numbers. "
                            + "Params provided were: limit" + DEFAULT_RESULTS_LIMIT + " and startIndex 0", e).toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Content acquisition error.", e).toResponse();
        }
        return Response.ok(c).build();
    }
}