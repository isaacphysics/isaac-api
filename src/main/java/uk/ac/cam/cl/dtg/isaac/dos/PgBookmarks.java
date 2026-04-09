package uk.ac.cam.cl.dtg.isaac.dos;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.util.mappers.MainMapper;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PgBookmarks implements IBookmarks {
    private static final Logger log = LoggerFactory.getLogger(PgBookmarks.class);

    private final PostgresSqlDb database;
    private final GitContentManager contentManager;
    private final MainMapper mapper = MainMapper.INSTANCE;

    /**
     * PgBookmarks.
     *
     * @param database client for postgres.
     */
    @Inject
    public PgBookmarks(final PostgresSqlDb database, final GitContentManager contentManager) {
        this.database = database;
        this.contentManager = contentManager;
    }

    @Override
    public List<ContentSummaryDTO> getBookmarksForUser(final RegisteredUserDTO user) {
        return this.getBookmarksForUser(user, null);
    }

    @Override
    public List<ContentSummaryDTO> getBookmarksForUser(final RegisteredUserDTO user, final String contentType) {

        String query = "SELECT content_id, timestamp FROM user_bookmarks WHERE user_id = ?";

        boolean filterByContentType = false;
        if (null != contentType) {
            if (contentType.equals("isaacQuestionPage") || contentType.equals("isaacConceptPage")) {
                query += " AND content_type = ?";
                filterByContentType = true;
            } else {
                log.warn("Invalid content type provided for bookmarks query: {}", contentType);
                throw new IllegalArgumentException("Invalid content type for bookmarks query: " + contentType);
            }
        }

        List<ContentSummaryDTO> bookmarks = new ArrayList<>();

        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, user.getId());

            if (filterByContentType) {
                pst.setString(2, contentType);
            }

            try (ResultSet results = pst.executeQuery()) {
                while (results.next()) {
                    String contentId = results.getString("content_id");
                    ContentDTO content = this.contentManager.getContentById(contentId);
                    ContentSummaryDTO contentSummary = this.mapper.mapContentDTOtoContentSummaryDTO(content);

                    Date timestamp = results.getDate("timestamp");
                    contentSummary.setBookmarked(timestamp);

                    bookmarks.add(contentSummary);
                }
            } catch (final ContentManagerException e) {
                throw new RuntimeException(e);
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return bookmarks;
    }

    @Override
    public void addBookmarkForUser(final RegisteredUserDTO user, final String contentId) {
        Date timestamp = new Date(System.currentTimeMillis());

        String contentType = "";
        try {
            ContentDTO content = this.contentManager.getContentById(contentId);
            contentType = content.getType();
        } catch (final ContentManagerException e) {
            throw new RuntimeException(e);
        }

        if ((null == contentType) || !(contentType.equals("isaacQuestionPage") || contentType.equals("isaacConceptPage"))) {
            log.warn("Failed to bookmark content with invalid content type: {}", contentType);
            throw new IllegalArgumentException("Invalid content type for bookmark: " + contentType);
        }

        String query = "INSERT INTO user_bookmarks (user_id, content_id, content_type, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, user.getId());
            pst.setString(2, contentId);
            pst.setString(3, contentType);
            pst.setDate(4, timestamp);
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save bookmark.");
            }

        } catch (final SQLException | SegueDatabaseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeBookmarkForUser(final RegisteredUserDTO user, final String contentId) {
        String query = "DELETE FROM user_bookmarks WHERE user_id = ? AND content_id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, user.getId());
            pst.setString(2, contentId);
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to remove bookmark.");
            }
        } catch (final SQLException | SegueDatabaseException e) {
            e.printStackTrace();
        }
    }
}
