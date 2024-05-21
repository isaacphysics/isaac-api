/**
 * Copyright 2019 Andrea Franceschini
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api;

import static uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_MAX_WINDOW_SIZE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_RESULTS_LIMIT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_TEN_MINUTES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Glossary Facade
 * <br>
 * This facade is intended to provide access to glossary terms.
 */
@Path("/glossary")
@Tag(name = "/glossary")
public class GlossaryFacade extends AbstractSegueFacade {
  private static final Logger log = LoggerFactory.getLogger(GlossaryFacade.class);

  private final GitContentManager contentManager;
  private final String contentIndex;

  /**
   * Constructor for the GlossaryFacade.
   *
   * @param properties     to allow access to system properties.
   * @param contentManager so that metadata about content can be accessed.
   * @param contentIndex   to access the right version of the content.
   * @param logManager     for logging events using the logging api.
   */
  @Inject
  public GlossaryFacade(final PropertiesLoader properties, final GitContentManager contentManager,
                        @Named(CONTENT_INDEX) final String contentIndex,
                        final ILogManager logManager) {
    super(properties, logManager);
    this.contentManager = contentManager;
    this.contentIndex = contentIndex;
  }

  /**
   * Gets all the glossary terms that are indexed.
   *
   * @param limit      Maximum amount of terms to retrieve. Used for pagination.
   * @param startIndex Index from which to start retrieving when results exceed limit.
   * @return Paginated list of glossary terms.
   */
  @GET
  @Path("terms")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get all the glossary terms that are indexed.")
  public final Response getTerms(@QueryParam("start_index") final String startIndex,
                                 @QueryParam("limit") final String limit) {

    List<GitContentManager.BooleanSearchClause> fieldsToMatch = Lists.newArrayList();
    fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
        TYPE_FIELDNAME, BooleanOperator.AND, Collections.singletonList("glossaryTerm")));

    ResultsWrapper<ContentDTO> c;
    try {
      int resultsLimit;
      int startIndexOfResults;

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

      c = this.contentManager.findByFieldNames(fieldsToMatch, startIndexOfResults, resultsLimit);
    } catch (ContentManagerException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          "Content acquisition error.", e).toResponse();
    }
    // Calculate the ETag on last modified date of tags list
    // NOTE: Assumes that the latest version of the content is being used.
    EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + "");
    return Response.ok(c).tag(etag).cacheControl(getCacheControl(NUMBER_SECONDS_IN_TEN_MINUTES, true)).build();
  }

  /**
   * Gets the current version of the segue application.
   *
   * @param termId The ID of the term to retrieve.
   * @return segue version as a string wrapped in a response.
   */
  @GET
  @Path("terms/{term_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get the term with the given id.")
  public final Response getTermById(@PathParam("term_id") final String termId) {

    if (null == termId) {
      return new SegueErrorResponse(Status.BAD_REQUEST, "Please specify a term_id.").toResponse();
    }

    ResultsWrapper<ContentDTO> c;
    try {
      c = this.contentManager.getByIdPrefix(termId, 0, DEFAULT_MAX_WINDOW_SIZE);
      if (null == c) {
        SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "No glossary term found with id: "
            + sanitiseExternalLogValue(termId));
        log.debug(error.getErrorMessage());
        return error.toResponse();
      }
    } catch (ContentManagerException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          "Content acquisition error.", e).toResponse();
    }
    // Calculate the ETag on last modified date of tags list
    // NOTE: Assumes that the latest version of the content is being used.
    EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + "");
    return Response.ok(c).tag(etag).build();
  }
}