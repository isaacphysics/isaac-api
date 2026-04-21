package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;

public class BookmarksFacadeIT extends IsaacIntegrationTest {

    private BookmarksFacade bookmarksFacade;

    @BeforeEach
    public void setUp() {
        this.bookmarksFacade = new BookmarksFacade(userAccountManager, contentManager, bookmarksManager, bookmarksDbManager);
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Clean up bookmark added during tests
        PreparedStatement deleteBookmarks = postgresSqlDb.getDatabaseConnection().prepareStatement("DELETE FROM user_bookmarks WHERE user_id = ? AND content_id = ?");
        deleteBookmarks.setLong(1, ITConstants.ALICE_STUDENT_ID);
        deleteBookmarks.setString(2, ITConstants.FUZZY_MATCH_TEST_PAGE_ID);
        deleteBookmarks.executeUpdate();
    }

    @Test
    public void getCurrentUserBookmarks_searchWithoutContentType_returnsAllBookmarks() throws Exception {
        // Arrange: log in, create request
        LoginResult login = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest bookmarksRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(bookmarksRequest);

        // Act: make request
        Response bookmarksResponse = bookmarksFacade.getCurrentUserBookmarks(bookmarksRequest, null);

        // Assert: check response is OK and contains expected bookmarks
        assertEquals(Response.Status.OK.getStatusCode(), bookmarksResponse.getStatus());

        ArrayList<ContentSummaryDTO> responseBody = (ArrayList<ContentSummaryDTO>) bookmarksResponse.getEntity();
        List<String> actualBookmarkIds = responseBody.stream().map(ContentSummaryDTO::getId).toList();

        List<String> expectedBookmarkIds = new ArrayList<>();
        expectedBookmarkIds.add(ITConstants.REGRESSION_TEST_PAGE_ID);
        expectedBookmarkIds.add(ITConstants.ASSIGNMENT_TEST_PAGE_ID);
        expectedBookmarkIds.add(ITConstants.SEARCH_TEST_CONCEPT_ID);

        assertEquals(expectedBookmarkIds, actualBookmarkIds);
    }

    @Test
    public void getCurrentUserBookmarks_searchWithContentType_returnsFilteredBookmarks() throws Exception {
        // Arrange: log in, create request
        LoginResult login = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest bookmarksRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(bookmarksRequest);

        // Act: make request
        Response bookmarksResponse = bookmarksFacade.getCurrentUserBookmarks(bookmarksRequest, QUESTION_TYPE);

        // Assert: check status code is OK and contains expected bookmarks
        assertEquals(Response.Status.OK.getStatusCode(), bookmarksResponse.getStatus());

        ArrayList<ContentSummaryDTO> responseBody = (ArrayList<ContentSummaryDTO>) bookmarksResponse.getEntity();
        List<String> actualBookmarkIds = responseBody.stream().map(ContentSummaryDTO::getId).toList();

        List<String> expectedBookmarkIds = new ArrayList<>();
        expectedBookmarkIds.add(ITConstants.REGRESSION_TEST_PAGE_ID);
        expectedBookmarkIds.add(ITConstants.ASSIGNMENT_TEST_PAGE_ID);

        assertEquals(expectedBookmarkIds, actualBookmarkIds);
    }

    @Test
    public void getCurrentUserBookmarks_noLoggedInUser_returnsError() {
        // Arrange: create request with no cookie
        HttpServletRequest bookmarksRequest = createRequestWithCookies(new Cookie[]{});
        replay(bookmarksRequest);

        // Act: make request
        Response bookmarksResponse = bookmarksFacade.getCurrentUserBookmarks(bookmarksRequest, QUESTION_TYPE);

        // Assert: check status code is unauthorised
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), bookmarksResponse.getStatus());
    }

    @Test
    public void getCurrentUserBookmarks_invalidContentType_returnsError() throws Exception {
        // Arrange: log in, create request
        LoginResult login = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest bookmarksRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(bookmarksRequest);

        // Act: make request
        Response bookmarksResponse = bookmarksFacade.getCurrentUserBookmarks(bookmarksRequest, EVENT_TYPE);

        // Assert: check status code is bad request
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), bookmarksResponse.getStatus());
    }

    @Test
    public void addCurrentUserBookmark_validContent_addsBookmark() throws Exception {
        // Arrange: log in, create request
        LoginResult login = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest addBookmarkRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(addBookmarkRequest);

        // Act: make request
        Response addBookmarkResponse = bookmarksFacade.addCurrentUserBookmark(addBookmarkRequest, ITConstants.FUZZY_MATCH_TEST_PAGE_ID);

        // Assert: check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), addBookmarkResponse.getStatus());
    }

    @Test
    public void addCurrentUserBookmark_noLoggedInUser_returnsError() {
        // Arrange: create request with no cookie
        HttpServletRequest addBookmarkRequest = createRequestWithCookies(new Cookie[]{});
        replay(addBookmarkRequest);

        // Act: make request
        Response addBookmarkResponse = bookmarksFacade.addCurrentUserBookmark(addBookmarkRequest, ITConstants.FUZZY_MATCH_TEST_PAGE_ID);

        // Assert: check status code is unauthorised
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), addBookmarkResponse.getStatus());
    }

    @Test
    public void addCurrentUserBookmark_notFoundContent_returnsError() throws Exception {
        // Arrange: log in, create request
        LoginResult login = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest addBookmarkRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(addBookmarkRequest);

        // Act: make request
        Response addBookmarkResponse = bookmarksFacade.addCurrentUserBookmark(addBookmarkRequest, "not_a_real_id");

        // Assert: check status code is not found
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), addBookmarkResponse.getStatus());
    }

    @Test
    public void addCurrentUserBookmark_duplicateContent_returnsError() throws Exception {
        // Arrange: log in, create request
        LoginResult login = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest addBookmarkRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(addBookmarkRequest);

        // Act: make request
        Response addBookmarkResponse = bookmarksFacade.addCurrentUserBookmark(addBookmarkRequest, ITConstants.REGRESSION_TEST_PAGE_ID);

        // Assert: check status code is bad request
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), addBookmarkResponse.getStatus());
    }

    @Test
    public void addCurrentUserBookmark_invalidContentType_returnsError() throws Exception {
        // Arrange: log in, create request
        LoginResult login = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest addBookmarkRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(addBookmarkRequest);

        // Act: make request
        Response addBookmarkResponse = bookmarksFacade.addCurrentUserBookmark(addBookmarkRequest, ITConstants.QUIZ_TEST_QUIZ_ID);

        // Assert: check status code is bad request
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), addBookmarkResponse.getStatus());
    }

    @Test
    public void deleteCurrentUserBookmark_validContent_deletesBookmark() throws Exception {
        // Arrange: log in, create request
        LoginResult login = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest deleteBookmarkRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(deleteBookmarkRequest);

        // Act: make request
        Response deleteBookmarkResponse = bookmarksFacade.removeCurrentUserBookmark(deleteBookmarkRequest, ITConstants.SEARCH_TEST_CONCEPT_ID);

        // Assert: check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), deleteBookmarkResponse.getStatus());
    }

    @Test
    public void deleteCurrentUserBookmark_noLoggedInUser_returnsError() throws Exception {
        // Arrange: create request with no cookie
        HttpServletRequest deleteBookmarkRequest = createRequestWithCookies(new Cookie[]{});
        replay(deleteBookmarkRequest);

        // Act: make request
        Response deleteBookmarksResponse = bookmarksFacade.removeCurrentUserBookmark(deleteBookmarkRequest, ITConstants.FUZZY_MATCH_TEST_PAGE_ID);

        // Assert: check status code is unauthorised
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), deleteBookmarksResponse.getStatus());
    }

    @Test
    public void deleteCurrentUserBookmark_notBookmarkedContent_returnsError() throws Exception {
        // Arrange: log in, create request
        LoginResult login = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest deleteBookmarkRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(deleteBookmarkRequest);

        // Act: make request
        Response deleteBookmarkResponse = bookmarksFacade.removeCurrentUserBookmark(deleteBookmarkRequest, ITConstants.SEARCH_TEST_SUPERSEDED_BY_ID);

        // Assert: check status code is bad request
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), deleteBookmarkResponse.getStatus());
    }
}
