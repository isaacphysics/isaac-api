package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

import java.util.List;

/**
 * IBookmarks.
 *
 * Interface for managing user bookmarks.
 */
public interface IBookmarks {
    List<ContentSummaryDTO> getBookmarksForUser(RegisteredUserDTO user);

    List<ContentSummaryDTO> getBookmarksForUser(RegisteredUserDTO user, String contentType);

    void addBookmarkForUser(RegisteredUserDTO user, String contentId);

    void removeBookmarkForUser(RegisteredUserDTO user, String contentId);
}
