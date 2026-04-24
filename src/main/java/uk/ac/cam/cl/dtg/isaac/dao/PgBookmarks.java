package uk.ac.cam.cl.dtg.isaac.dao;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.BookmarkDO;
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
            query += " AND content_type = ?";
            filterByContentType = true;
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
                bookmarks.add(new BookmarkDO(userId, contentId, contentType, created));
            }
        } catch (final SQLException e) {
            log.error("Database error saving bookmark!", e);
        }
        return bookmarks;
    }

    @Override
    public void addBookmarkForUser(final BookmarkDO bookmark) {

        String query = "INSERT INTO user_bookmarks (user_id, content_id, content_type, created) VALUES (?, ?, ?, ?)";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, bookmark.userId());
            pst.setString(2, bookmark.contentId());
            pst.setString(3, bookmark.contentType());
            pst.setTimestamp(4, (Timestamp) bookmark.created());
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save bookmark.");
            }
        } catch (final SQLException | SegueDatabaseException e) {
            log.error("Database error saving bookmark!", e);
        }
    }

    @Override
    public void removeBookmarkForUser(final BookmarkDO bookmark) {
        String query = "DELETE FROM user_bookmarks WHERE user_id = ? AND content_id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, bookmark.userId());
            pst.setString(2, bookmark.contentId());
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to remove bookmark.");
            }
        } catch (final SQLException | SegueDatabaseException e) {
            log.error("Database error saving bookmark!", e);
        }
    }
}
