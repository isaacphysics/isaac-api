package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.IBookmarks;
import uk.ac.cam.cl.dtg.isaac.dos.BookmarkDO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.mappers.MainMapper;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class to augment content with bookmark information, and bookmarks with content information.
 */
public class BookmarksManager {
    private static final Logger log = LoggerFactory.getLogger(BookmarksManager.class);

    private final IBookmarks bookmarksDbManager;
    private final GitContentManager contentManager;
    private final MainMapper mapper;

    /**
     * Fully injected constructor.
     *
     * @param bookmarksDbManager the bookmarks database manager for retrieving bookmark information.
     * @param contentManager the content manager for retrieving content information.
     * @param mapper the mapper for mapping content to content summaries.
     */
    @Inject
    public BookmarksManager(final IBookmarks bookmarksDbManager, final GitContentManager contentManager, final MainMapper mapper) {
        this.bookmarksDbManager = bookmarksDbManager;
        this.contentManager = contentManager;
        this.mapper = mapper;
    }

    /**
     * Augment a list of content summaries with bookmark information for a given user.
     *
     * @param userId the id of the user to augment the content summary list for.
     * @param contentSummaries the content summary list to augment.
     * @return the augmented content summary list.
     */
    public List<ContentSummaryDTO> augmentContentSummaryListWithBookmarkInformation(final Long userId,
                                                                    final List<ContentSummaryDTO> contentSummaries) {
        List<BookmarkDO> bookmarks = this.bookmarksDbManager.getBookmarksForUser(userId);
        return augmentContentSummaryListWithBookmarkInformation(bookmarks, contentSummaries);
    }

    /**
     * Augment a list of content summaries with bookmark information.
     *
     * @param bookmarks the bookmarks to augment the content summaries with.
     * @param contentSummaries the content summary list to augment.
     * @return the augmented content summary list.
     */
    public List<ContentSummaryDTO> augmentContentSummaryListWithBookmarkInformation(final List<BookmarkDO> bookmarks,
                                                                                    final List<ContentSummaryDTO> contentSummaries) {
        Map<String, Date> bookmarkMap = new HashMap<>();
        for (BookmarkDO bookmark : bookmarks) {
            bookmarkMap.put(bookmark.contentId(), bookmark.created());
        }

        for (ContentSummaryDTO contentSummary : contentSummaries) {
            Date created = bookmarkMap.get(contentSummary.getId());
            if (created != null) {
                contentSummary.setBookmarked(created);
            }
        }
        return contentSummaries;
    }

    /**
     * Map a list of bookmarks to a list of content summaries.
     *
     * @param bookmarks the list of bookmarks to map.
     * @return the list of content summaries corresponding to the bookmarks.
     */
    public List<ContentSummaryDTO> mapBookmarkListToContentSummaryList(final List<BookmarkDO> bookmarks) {
        List<String> bookmarkIDs = bookmarks.stream().map(BookmarkDO::contentId).toList();
        ResultsWrapper<ContentDTO> content;

        try {
            content = this.contentManager.getUnsafeCachedContentDTOsMatchingIds(bookmarkIDs, 0, bookmarkIDs.size());
        } catch (final ContentManagerException e) {
            content = new ResultsWrapper<>();
            log.error("Unable to locate content for bookmark", e);
        }

        List<ContentSummaryDTO> contentSummaries = content.getResults().stream().map(this.mapper::mapContentDTOtoContentSummaryDTO).toList();
        contentSummaries = augmentContentSummaryListWithBookmarkInformation(bookmarks, contentSummaries);

        return contentSummaries;
    }
}
