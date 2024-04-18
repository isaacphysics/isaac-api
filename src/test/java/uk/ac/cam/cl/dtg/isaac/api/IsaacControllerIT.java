/*
 * Copyright 2022 Matthew Trew
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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.StatisticsManager;

import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_TYPE_FILTER;

public class IsaacControllerIT extends IsaacIntegrationTest {

    private IsaacController isaacControllerFacade;

    @BeforeEach
    public void setUp() {
        this.isaacControllerFacade = new IsaacController(properties, logManager, createNiceMock(StatisticsManager.class),
                userAccountManager, contentManager, userAssociationManager,
                createNiceMock(IUserStreaksManager.class), contentSummarizerService);
    }

    /**
     * This test searches for a term that appears in only one piece of content, a concept page.
     * The query is not filtered by type.
     *
     * We expect to see only the concept page containing the term in the results.
     */
    @Test
    public void searchEndpoint_searchAllTypesSpecificTermAsStudent_returnsOnlyConceptWithMatchingValue() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL,
                ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest searchRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(searchRequest);

        // Act
        // make request
        Response searchResponse = isaacControllerFacade.search(createNiceMock(Request.class), searchRequest,
                "crepuscular", DEFAULT_TYPE_FILTER, 0, -1);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        // check the search returned the expected content summary
        @SuppressWarnings("unchecked") ResultsWrapper<ContentSummaryDTO> responseBody =
                (ResultsWrapper<ContentSummaryDTO>) searchResponse.getEntity();

        Set<String> questionIDs = responseBody.getResults().stream().map(ContentSummaryDTO::getId).collect(Collectors.toSet());
        assertEquals(Set.of(ITConstants.SEARCH_TEST_CONCEPT_ID), questionIDs);
    }

    /**
     * This test searches for a term that appears in only one piece of content, a topic summary.
     * The query is not filtered by type.
     *
     * We expect to see only the topic summary containing the term in the results.
     */
    @Test
    public void searchEndpoint_searchAllTypesSpecificTermAsStudent_returnsOnlyTopicSummaryWithMatchingValue() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL,
                ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest searchRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(searchRequest);

        // Act
        // make request
        Response searchResponse = isaacControllerFacade.search(createNiceMock(Request.class), searchRequest,
                "nethermost", DEFAULT_TYPE_FILTER, 0, -1);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        // check the search returned the expected content summary
        @SuppressWarnings("unchecked") ResultsWrapper<ContentSummaryDTO> responseBody =
                (ResultsWrapper<ContentSummaryDTO>) searchResponse.getEntity();

        Set<String> questionIDs = responseBody.getResults().stream().map(ContentSummaryDTO::getId).collect(Collectors.toSet());
        assertEquals(Set.of(ITConstants.SEARCH_TEST_TOPIC_SUMMARY_ID), questionIDs);
    }

    /**
     * This test searches for a term that appears across multiple pieces of content, each of a different type.
     * The query is filtered by type to return only events.
     *
     * We expect to see only the event containing the term in the results.
     */
    @Test
    public void searchEndpoint_searchEventsOnlyGenericTermAsStudent_returnsOnlyEventWithMatchingValue() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL,
                ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest searchRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(searchRequest);

        // Act
        // make request
        Response searchResponse = isaacControllerFacade.search(createNiceMock(Request.class), searchRequest,
                "habergeon", EVENT_TYPE, 0, -1);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        // check the search returned the expected content summary
        @SuppressWarnings("unchecked") ResultsWrapper<ContentSummaryDTO> responseBody =
                (ResultsWrapper<ContentSummaryDTO>) searchResponse.getEntity();

        Set<String> questionIDs = responseBody.getResults().stream().map(ContentSummaryDTO::getId).collect(Collectors.toSet());
        assertEquals(Set.of(ITConstants.SEARCH_TEST_EVENT_ID), questionIDs);
    }
}
