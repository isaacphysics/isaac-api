package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

import java.util.List;

/**
 * Interface for managing and persisting user bookmarks.
 */
public interface IBookmarks {
    List<BookmarkDO> getBookmarksForUser(RegisteredUserDTO user);

    List<BookmarkDO> getBookmarksForUser(RegisteredUserDTO user, String contentType);

    void addBookmarkForUser(RegisteredUserDTO user, String contentId, String contentType);

    void removeBookmarkForUser(RegisteredUserDTO user, String contentId);
}
