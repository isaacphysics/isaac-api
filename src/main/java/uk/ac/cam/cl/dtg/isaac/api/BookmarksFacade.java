package uk.ac.cam.cl.dtg.isaac.api;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.ac.cam.cl.dtg.isaac.dos.IBookmarks;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Bookmarks Facade
 *
 * Responsible for handling requests related to user bookmarks.
 */
@Path("/")
@Tag(name = "BookmarksFacade", description = "/bookmarks")
public class BookmarksFacade {
    private final UserAccountManager userManager;
    private final IBookmarks bookmarksDbManager;

    @Inject
    public BookmarksFacade(final UserAccountManager userManager, final IBookmarks bookmarksDbManager) {
        this.userManager = userManager;
        this.bookmarksDbManager = bookmarksDbManager;
    }

    /**
     * Gets a list of content bookmarked by the current user.
     *
     * @param request
     *            - so we can find the current user.
     * @param contentType
     *           - optional query parameter to filter bookmarks by content type. Valid values are "isaacQuestionPage"
     *              and "isaacConceptPage". If null, all bookmarks will be returned.
     * @return the list of content that the user has bookmarked.
     */
    @GET
    @Path("bookmarks")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get bookmarks for the current user.")
    public final Response getCurrentBookmarks(@Context final HttpServletRequest request,
                                                  @QueryParam("content_type") final String contentType) {
        RegisteredUserDTO user;
        try {
            user = userManager.getCurrentRegisteredUser(request);
        } catch (final NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
        return Response.ok(bookmarksDbManager.getBookmarksForUser(user, contentType)).build();
    }

    /**
     * Adds a bookmark for the current user.
     *
     * @param request
     *            - so we can find the current user.
     * @param contentId
     *           - the id of the content to bookmark.
     */
    @POST
    @Path("bookmarks")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add a bookmark for the current user.")
    public final Response addCurrentUserBookmark(@Context final HttpServletRequest request,
                                                 @QueryParam("content_id") final String contentId) {
        RegisteredUserDTO user;
        try {
            user = userManager.getCurrentRegisteredUser(request);
        } catch (final NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
        if (bookmarksDbManager.getBookmarksForUser(user).size() >= 100) {
            return new SegueErrorResponse(Response.Status.BAD_REQUEST, "You cannot have more than 100 bookmarks.")
                    .toResponse();
        } else {
            bookmarksDbManager.addBookmarkForUser(user, contentId);
        }
        return Response.noContent().build();
    }

    /**
     * Removes a bookmark for the current user.
     *
     * @param request
     *            - so we can find the current user.
     * @param contentId
     *           - the id of the content to be removed from the user's bookmarks.
     */
    @DELETE
    @Path("bookmarks")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Remove a bookmark for the current user.")
    public final Response removeCurrentUserBookmark(@Context final HttpServletRequest request,
                                                    @QueryParam("content_id") final String contentId) {
        RegisteredUserDTO user;
        try {
            user = userManager.getCurrentRegisteredUser(request);
        } catch (final NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
        bookmarksDbManager.removeBookmarkForUser(user, contentId);
        return Response.noContent().build();
    }
}
