package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.dos.IBookmarks;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

import java.util.Date;
import java.util.List;

public class BookmarksManager {

    public IBookmarks bookmarksDbManager;

    @Inject
    public BookmarksManager(final IBookmarks bookmarksDbManager) {
        this.bookmarksDbManager = bookmarksDbManager;
    }

    public List<ContentSummaryDTO> augmentContentSummaryListWithBookmarkInformation(final RegisteredUserDTO user,
                                                                    final List<ContentSummaryDTO> contentSummaries) {

        List<ContentSummaryDTO> bookmarks = this.bookmarksDbManager.getBookmarksForUser(user);

        for (ContentSummaryDTO contentSummary : contentSummaries) {
            for (ContentSummaryDTO bookmark : bookmarks) {
                if (contentSummary.getId().equals(bookmark.getId())) {
                    Date timestamp = bookmark.getBookmarked();
                    contentSummary.setBookmarked(timestamp);
                    break;
                }
            }
        }
        return contentSummaries;
    }
}
