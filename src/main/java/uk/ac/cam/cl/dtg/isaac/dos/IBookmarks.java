package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

import java.util.List;

/**
 * Interface for managing and persisting user bookmarks.
 */
public interface IBookmarks {
    List<BookmarkDO> getBookmarksForUser(Long userId);

    List<BookmarkDO> getBookmarksForUser(Long userId, String contentType);

    void addBookmarkForUser(BookmarkDO bookmark);

    void removeBookmarkForUser(BookmarkDO bookmark);
}
