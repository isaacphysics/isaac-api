package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.BookmarksManager;
import uk.ac.cam.cl.dtg.isaac.dos.BookmarkDO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Date;
import java.util.List;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;

/**
 * Bookmarks Facade
 *
 * Responsible for handling requests related to user bookmarks.
 */
@Path("/bookmarks")
@Tag(name = "BookmarksFacade", description = "/bookmarks")
public class BookmarksFacade extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(BookmarksFacade.class);

    private final UserAccountManager userManager;
    private final GitContentManager contentManager;
    private final BookmarksManager bookmarksManager;

    @Inject
    public BookmarksFacade(final AbstractConfigLoader propertiesLoader, final ILogManager logManager,
                           final UserAccountManager userManager, final GitContentManager contentManager,
                           final BookmarksManager bookmarksManager) {
        super(propertiesLoader, logManager);

        this.userManager = userManager;
        this.contentManager = contentManager;
        this.bookmarksManager = bookmarksManager;
    }

    /**
     * Gets a list of content bookmarked by the current user.
     *
     * @param request
     *            - so we can find the current user.
     * @param contentType
     *           - the type of content to filter bookmarks by, or null to return all bookmarks.
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

        List<ContentSummaryDTO> bookmarks = bookmarksManager.getAugmentedBookmarksForUser(user.getId(), contentType);
        return Response.ok(bookmarks).build();
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
    @Path("/{content_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add a bookmark for the current user.")
    public final Response addCurrentUserBookmark(@Context final HttpServletRequest request,
                                                 @PathParam("content_id") final String contentId) {
        RegisteredUserDTO user;
        try {
            user = userManager.getCurrentRegisteredUser(request);
        } catch (final NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        if (null == contentId || contentId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Cannot create bookmark without content ID.").toResponse();
        }

        List<BookmarkDO> currentBookmarks = bookmarksManager.getBookmarksForUser(user.getId(), null);

        if (currentBookmarks.size() >= MAXIMUM_BOOKMARKS) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You already have the maximum number of bookmarks!.").toResponse();
        }

        if (currentBookmarks.stream().anyMatch(b -> b.contentId().equals(contentId))) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You have already bookmarked this content.").toResponse();
        }

        String contentType;
        try {
            ContentDTO content = this.contentManager.getContentById(contentId);
            contentType = content.getType();
            if (null == contentType || !(contentType.equals("isaacQuestionPage") || contentType.equals("isaacConceptPage"))) {
                log.warn("Invalid content type provided for bookmarks query: {}", contentType);
                return new SegueErrorResponse(Status.BAD_REQUEST, "Only question and concept pages can be bookmarked!").toResponse();
            }

            BookmarkDO bookmarkToAdd = new BookmarkDO(user.getId(), contentId, contentType, new Date());
            bookmarksManager.addBookmarkForUser(bookmarkToAdd);

        } catch (final ContentManagerException | NullPointerException e) {
            log.warn("Failed to create bookmark, could not find content with ID: {}", contentId);
            return new SegueErrorResponse(Status.NOT_FOUND, "Unable to find content to bookmark.").toResponse();
        }

        this.getLogManager().logEvent(user, request, IsaacServerLogType.ADD_BOOKMARK,
                ImmutableMap.of(BOOKMARK_USER_ID_LOG_FIELDNAME, user.getId(),
                        BOOKMARK_CONTENT_ID_LOG_FIELDNAME, contentId,
                        BOOKMARK_CONTENT_TYPE_LOG_FIELDNAME, contentType));

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
    @Path("/{content_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Remove a bookmark for the current user.")
    public final Response removeCurrentUserBookmark(@Context final HttpServletRequest request,
                                                    @PathParam("content_id") final String contentId) {
        RegisteredUserDTO user;
        try {
            user = userManager.getCurrentRegisteredUser(request);
        } catch (final NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        if (null == contentId || contentId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Cannot delete bookmark without content ID.").toResponse();
        }

        List<BookmarkDO> currentBookmarks = bookmarksManager.getBookmarksForUser(user.getId(), null);
        if (currentBookmarks.stream().noneMatch(b -> b.contentId().equals(contentId))) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You have not bookmarked this content.").toResponse();
        }

        BookmarkDO bookmarkToRemove = new BookmarkDO(user.getId(), contentId, null, null);
        bookmarksManager.removeBookmarkForUser(bookmarkToRemove);

        this.getLogManager().logEvent(user, request, IsaacServerLogType.DELETE_BOOKMARK,
                ImmutableMap.of(BOOKMARK_USER_ID_LOG_FIELDNAME, user.getId(),
                        BOOKMARK_CONTENT_ID_LOG_FIELDNAME, contentId));

        return Response.noContent().build();
    }
}
