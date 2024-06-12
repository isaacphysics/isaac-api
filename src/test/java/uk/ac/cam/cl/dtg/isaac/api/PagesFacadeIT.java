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
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.*;


public class PagesFacadeIT extends IsaacIntegrationTest{

    private PagesFacade pagesFacade;

    @BeforeEach
    public void setUp() {
        this.pagesFacade = new PagesFacade(new ContentService(contentManager), properties, logManager,
                mapperFacade, contentManager, userAccountManager, new URIManager(properties), questionManager, gameManager, userAttemptManager);
    }

    @Test
    public void getQuestionList_searchSpecificIDAsStudent_returnsOnlyQuestionWithID() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL,
                ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest searchRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(searchRequest);

        // Act
        // make request
        Response searchResponse = pagesFacade.getQuestionList(createNiceMock(Request.class), searchRequest,
                ITConstants.REGRESSION_TEST_PAGE_ID, "", "", "", "", "", "", "", "", "", "", false, false, 0, -1);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        // check the search returned the expected content summary
        @SuppressWarnings("unchecked") ResultsWrapper<ContentSummaryDTO> responseBody =
                (ResultsWrapper<ContentSummaryDTO>) searchResponse.getEntity();

        Set<String> questionIDs = responseBody.getResults().stream().map(ContentSummaryDTO::getId).collect(Collectors.toSet());
        assertEquals(Set.of(ITConstants.REGRESSION_TEST_PAGE_ID), questionIDs);
    }

    @Test
    public void getQuestionList_searchByIDOfNonQuestionPageAsStudent_doesNotReturnPage() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL,
                ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest searchRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(searchRequest);

        // Act
        // make request
        Response searchResponse = pagesFacade.getQuestionList(createNiceMock(Request.class), searchRequest,
                ITConstants.SEARCH_TEST_CONCEPT_ID, "", "", "", "", "", "", "", "", "", "", false, false, 0, -1);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        // check the search returned the expected content summary
        @SuppressWarnings("unchecked") ResultsWrapper<ContentSummaryDTO> responseBody =
                (ResultsWrapper<ContentSummaryDTO>) searchResponse.getEntity();

        Set<String> questionIDs = responseBody.getResults().stream().map(ContentSummaryDTO::getId).collect(Collectors.toSet());
        assertEquals(Set.of(), questionIDs);
    }

    @Test
    public void getQuestionList_searchByIDOfNonFastTrackQuestionPageAsStudentWhenFastTrackOnly_doesNotReturnPage() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL,
                ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest searchRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(searchRequest);

        // Act
        // make request
        Response searchResponse = pagesFacade.getQuestionList(createNiceMock(Request.class), searchRequest,
                ITConstants.REGRESSION_TEST_PAGE_ID, "", "", "", "", "", "", "", "", "", "", true, false, 0, -1);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        // check the search returned the expected content summary
        @SuppressWarnings("unchecked") ResultsWrapper<ContentSummaryDTO> responseBody =
                (ResultsWrapper<ContentSummaryDTO>) searchResponse.getEntity();

        Set<String> questionIDs = responseBody.getResults().stream().map(ContentSummaryDTO::getId).collect(Collectors.toSet());
        assertEquals(Set.of(), questionIDs);
    }

    @Test
    public void getQuestionList_searchSpecificIDsAsStudent_returnsOnlyQuestionsWithIDs() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL,
                ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest searchRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(searchRequest);

        // Act
        // make request
        Response searchResponse = pagesFacade.getQuestionList(createNiceMock(Request.class), searchRequest,
                String.format("%s,%s", ITConstants.REGRESSION_TEST_PAGE_ID, ITConstants.ASSIGNMENT_TEST_PAGE_ID), "",
                "", "", "", "", "", "", "", "", "", false, false, 0, -1);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        // check the search returned the expected content summary
        @SuppressWarnings("unchecked") ResultsWrapper<ContentSummaryDTO> responseBody =
                (ResultsWrapper<ContentSummaryDTO>) searchResponse.getEntity();

        Set<String> questionIDs = responseBody.getResults().stream().map(ContentSummaryDTO::getId).collect(Collectors.toSet());
        assertEquals(Set.of(ITConstants.REGRESSION_TEST_PAGE_ID, ITConstants.ASSIGNMENT_TEST_PAGE_ID), questionIDs);
    }

    @Test
    public void getQuestionList_searchByStringAsStudent_returnsQuestionsWithSimilarTitlesInOrder() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL,
                ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest searchRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(searchRequest);

        // Act
        // make request
        Response searchResponse = pagesFacade.getQuestionList(createNiceMock(Request.class), searchRequest,
                "", "Regression Test Page", "", "", "", "", "", "", "", "", "", false, false, 0, -1);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        // check the search returned the expected content summary
        @SuppressWarnings("unchecked") ResultsWrapper<ContentSummaryDTO> responseBody =
                (ResultsWrapper<ContentSummaryDTO>) searchResponse.getEntity();

        List<String> questionIDs = responseBody.getResults()
                .stream()
                .map(ContentSummaryDTO::getId)
                .collect(Collectors.toList());

        assertEquals(
                List.of(ITConstants.REGRESSION_TEST_PAGE_ID, ITConstants.ASSIGNMENT_TEST_PAGE_ID,
                        // These pages have "page" in their content thus match "Regression Test _Page_"
                        ITConstants.EXACT_MATCH_TEST_PAGE_ID, ITConstants.FUZZY_MATCH_TEST_PAGE_ID),
                questionIDs
        );
    }

    @Test
    public void getQuestionList_limitedSearchByStringAsStudent_returnsLimitedNumberOfQuestions() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL,
                ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest searchRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(searchRequest);

        // Act
        // make request
        Response searchResponse = pagesFacade.getQuestionList(createNiceMock(Request.class), searchRequest,
                "", "Regression Test Page", "", "", "", "", "", "", "", "", "",false, false, 0, 1);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        // check the search returned the expected content summary
        @SuppressWarnings("unchecked") ResultsWrapper<ContentSummaryDTO> responseBody =
                (ResultsWrapper<ContentSummaryDTO>) searchResponse.getEntity();

        List<String> questionIDs = responseBody.getResults().stream().map(ContentSummaryDTO::getId).collect(Collectors.toList());
        assertEquals(List.of(ITConstants.REGRESSION_TEST_PAGE_ID), questionIDs);
    }

    /**
     * This test searches for a page that has an exact and fuzzy match in the title.
     *
     * We expect to only see the exact match page.
     */
    @Test
    public void getQuestionList_searchForPhrase_returnExactlyMatchesPhrase() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL,
                ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest searchRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(searchRequest);

        // Act
        // make request
        Response searchResponse = pagesFacade.getQuestionList(createNiceMock(Request.class), searchRequest,
                "", "Canary", "", "", "", "", "", "", "", "", "", false, false, 0, 1);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), searchResponse.getStatus());

        // check the search returned the expected content summary
        @SuppressWarnings("unchecked") ResultsWrapper<ContentSummaryDTO> responseBody =
                (ResultsWrapper<ContentSummaryDTO>) searchResponse.getEntity();

        List<String> questionIDs = responseBody.getResults()
                .stream()
                .map(ContentSummaryDTO::getId)
                .collect(Collectors.toList());
        // This should not match the Fuzzy match titled "Catenary"
        assertEquals(List.of(ITConstants.EXACT_MATCH_TEST_PAGE_ID), questionIDs);
    }
}
