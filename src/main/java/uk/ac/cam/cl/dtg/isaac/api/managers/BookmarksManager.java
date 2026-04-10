package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.BookmarkDO;
import uk.ac.cam.cl.dtg.isaac.dos.IBookmarks;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.mappers.MainMapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
     * @param user the user to augment the content summary list for.
     * @param contentSummaries the content summary list to augment.
     * @return the augmented content summary list.
     */
    public List<ContentSummaryDTO> augmentContentSummaryListWithBookmarkInformation(final RegisteredUserDTO user,
                                                                    final List<ContentSummaryDTO> contentSummaries) {

        List<BookmarkDO> bookmarks = this.bookmarksDbManager.getBookmarksForUser(user);

        for (ContentSummaryDTO contentSummary : contentSummaries) {
            for (BookmarkDO bookmark : bookmarks) {
                if (contentSummary.getId().equals(bookmark.contentId())) {
                    Date created = bookmark.created();
                    contentSummary.setBookmarked(created);
                    break;
                }
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

        List<ContentSummaryDTO> contentSummaries = new ArrayList<>();

        for (BookmarkDO bookmark : bookmarks) {
            try {
                ContentDTO content = this.contentManager.getContentById(bookmark.contentId());
                ContentSummaryDTO contentSummary = this.mapper.mapContentDTOtoContentSummaryDTO(content);

                Date created = bookmark.created();
                contentSummary.setBookmarked(created);

                contentSummaries.add(contentSummary);
            } catch (final ContentManagerException e) {
                log.warn("Error retrieving content for bookmark with content id {}: {}", bookmark.contentId(), e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return contentSummaries;
    }

    public String getBookmarkContentType(final String contentId) {
        try {
            ContentDTO content = this.contentManager.getContentById(contentId);
            String contentType = content.getType();
            if ((null == contentType) || !(contentType.equals("isaacQuestionPage") || contentType.equals("isaacConceptPage"))) {
                log.warn("Failed to bookmark content with invalid content type: {}", contentType);
                throw new IllegalArgumentException("Invalid content type for bookmark: " + contentType);
            }
            return contentType;
        } catch (final ContentManagerException e) {
            log.warn("Error retrieving content for bookmark with content id {}: {}", contentId, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
