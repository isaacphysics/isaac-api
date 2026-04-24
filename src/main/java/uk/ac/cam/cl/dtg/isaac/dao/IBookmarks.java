package uk.ac.cam.cl.dtg.isaac.dao;

import uk.ac.cam.cl.dtg.isaac.dos.BookmarkDO;

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
