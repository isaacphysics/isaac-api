package uk.ac.cam.cl.dtg.isaac.api;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.BookmarksManager;
import uk.ac.cam.cl.dtg.isaac.dos.BookmarkDO;
import uk.ac.cam.cl.dtg.isaac.dos.IBookmarks;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;

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
import jakarta.ws.rs.core.Response.Status;
import java.util.List;

/**
 * Bookmarks Facade
 *
 * Responsible for handling requests related to user bookmarks.
 */
@Path("/bookmarks")
@Tag(name = "BookmarksFacade", description = "/bookmarks")
public class BookmarksFacade {
    private static final Logger log = LoggerFactory.getLogger(BookmarksFacade.class);

    private final UserAccountManager userManager;
    private final GitContentManager contentManager;
    private final BookmarksManager bookmarksManager;
    private final IBookmarks bookmarksDbManager;

    @Inject
    public BookmarksFacade(final UserAccountManager userManager, final GitContentManager contentManager,
                           final BookmarksManager bookmarksManager, final IBookmarks bookmarksDbManager) {
        this.userManager = userManager;
        this.contentManager = contentManager;
        this.bookmarksManager = bookmarksManager;
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
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get bookmarks for the current user.")
    public final Response getCurrentUserBookmarks(@Context final HttpServletRequest request,
                                                  @QueryParam("content_type") final String contentType) {
        RegisteredUserDTO user;
        try {
            user = userManager.getCurrentRegisteredUser(request);
        } catch (final NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        if (null != contentType && !contentType.isEmpty()
                && !(contentType.equals("isaacQuestionPage") || contentType.equals("isaacConceptPage"))) {
            log.warn("Invalid content type provided for bookmarks query: {}", contentType);
            return new SegueErrorResponse(Status.BAD_REQUEST, "Only question and concept pages can be bookmarked!").toResponse();
        }

        List<BookmarkDO> bookmarks = bookmarksDbManager.getBookmarksForUser(user.getId(), contentType);
        return Response.ok(bookmarksManager.mapBookmarkListToContentSummaryList(bookmarks)).build();
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
    @Path("/")
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

        if (null == contentId || contentId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Cannot create bookmark without content ID.").toResponse();
        }

        List<BookmarkDO> currentBookmarks = bookmarksDbManager.getBookmarksForUser(user.getId());

        if (currentBookmarks.size() >= 100) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You cannot have more than 100 bookmarks.").toResponse();
        }

        if (currentBookmarks.stream().anyMatch(b -> b.contentId().equals(contentId))) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You have already bookmarked this content.").toResponse();
        }

        try {
            ContentDTO content = this.contentManager.getContentById(contentId);
            String contentType = content.getType();
            if (null == contentType || !(contentType.equals("isaacQuestionPage") || contentType.equals("isaacConceptPage"))) {
                log.warn("Invalid content type provided for bookmarks query: {}", contentType);
                return new SegueErrorResponse(Status.BAD_REQUEST, "Only question and concept pages can be bookmarked!").toResponse();
            }
            bookmarksDbManager.addBookmarkForUser(user.getId(), contentId, contentType);
        } catch (final ContentManagerException | NullPointerException e) {
            log.warn("Failed to create bookmark, could not find content with ID: {}", contentId);
            return new SegueErrorResponse(Status.NOT_FOUND, "Unable to find content to bookmark.").toResponse();
        }

        return Response.ok().build();
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
    @Path("/")
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

        if (null == contentId || contentId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Cannot delete bookmark without content ID.").toResponse();
        }

        List<BookmarkDO> currentBookmarks = bookmarksDbManager.getBookmarksForUser(user.getId());
        if (currentBookmarks.stream().noneMatch(b -> b.contentId().equals(contentId))) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You have not bookmarked this content.").toResponse();
        }

        bookmarksDbManager.removeBookmarkForUser(user.getId(), contentId);
        return Response.ok().build();
    }
}
