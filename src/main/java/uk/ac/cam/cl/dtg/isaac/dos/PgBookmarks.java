package uk.ac.cam.cl.dtg.isaac.dos;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Postgres implementation for managing and persisting bookmarks data.
 */
public class PgBookmarks implements IBookmarks {
    private static final Logger log = LoggerFactory.getLogger(PgBookmarks.class);

    private final PostgresSqlDb database;

    /**
     * PgBookmarks.
     *
     * @param database client for postgres.
     */
    @Inject
    public PgBookmarks(final PostgresSqlDb database) {
        this.database = database;
    }

    @Override
    public List<BookmarkDO> getBookmarksForUser(final Long userId) {
        return this.getBookmarksForUser(userId, null);
    }

    @Override
    public List<BookmarkDO> getBookmarksForUser(final Long userId, final String contentType) {

        String query = "SELECT content_id, created FROM user_bookmarks WHERE user_id = ?";

        boolean filterByContentType = false;
        if (null != contentType && !contentType.isEmpty()) {
            if (contentType.equals("isaacQuestionPage") || contentType.equals("isaacConceptPage")) {
                query += " AND content_type = ?";
                filterByContentType = true;
            } else {
                // Should have already been caught at facade level
                log.warn("Invalid content type provided for bookmarks query: {}", contentType);
                throw new IllegalArgumentException("Invalid content type for bookmarks query: " + contentType);
            }
        }

        List<BookmarkDO> bookmarks = new ArrayList<>();

        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);

            if (filterByContentType) {
                pst.setString(2, contentType);
            }

            ResultSet results = pst.executeQuery();
            while (results.next()) {
                String contentId = results.getString("content_id");
                Timestamp created = results.getTimestamp("created");
                bookmarks.add(new BookmarkDO(contentId, created));
            }
        } catch (final SQLException e) {
            e.printStackTrace();
        }
        return bookmarks;
    }

    @Override
    public void addBookmarkForUser(final Long userId, final String contentId, final String contentType) {
        Timestamp created = new Timestamp(System.currentTimeMillis());

        String query = "INSERT INTO user_bookmarks (user_id, content_id, content_type, created) VALUES (?, ?, ?, ?)";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);
            pst.setString(2, contentId);
            pst.setString(3, contentType);
            pst.setTimestamp(4, created);
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save bookmark.");
            }

        } catch (final SQLException | SegueDatabaseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeBookmarkForUser(final Long userId, final String contentId) {
        String query = "DELETE FROM user_bookmarks WHERE user_id = ? AND content_id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);
            pst.setString(2, contentId);
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to remove bookmark.");
            }
        } catch (final SQLException | SegueDatabaseException e) {
            e.printStackTrace();
        }
    }
}
