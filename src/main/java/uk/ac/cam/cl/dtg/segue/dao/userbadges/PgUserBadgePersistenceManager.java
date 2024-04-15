package uk.ac.cam.cl.dtg.segue.dao.userbadges;

import static uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils.getSharedBasicObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import uk.ac.cam.cl.dtg.isaac.dos.ITransaction;
import uk.ac.cam.cl.dtg.isaac.dos.PgTransaction;
import uk.ac.cam.cl.dtg.isaac.dos.UserBadge;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * Created by du220 on 13/04/2018.
 */
public class PgUserBadgePersistenceManager implements IUserBadgePersistenceManager {

  private final ObjectMapper mapper = getSharedBasicObjectMapper();


  /**
   * Postgres specific database management for user badges.
   */
  public PgUserBadgePersistenceManager() {
  }

  @Override
  public UserBadge getBadge(final RegisteredUserDTO user, final UserBadgeManager.Badge badgeName,
                            final ITransaction transaction) throws SegueDatabaseException {

    if (!(transaction instanceof PgTransaction)) {
      throw new SegueDatabaseException("Unable to get badge definition from database.");
    }

    String query = "INSERT INTO user_badges (user_id, badge) VALUES (?, ?)"
        + " ON CONFLICT (user_id, badge) DO UPDATE SET user_id = excluded.user_id"
        + " WHERE user_badges.user_id = ? AND user_badges.badge = ? RETURNING *";

    Connection conn = ((PgTransaction) transaction).getConnection();
    try (PreparedStatement pst = conn.prepareStatement(query)) {
      pst.setLong(FIELD_GET_BADGE_USER_ID, user.getId());
      pst.setString(FIELD_GET_BADGE_BADGE_NAME, badgeName.name());
      pst.setLong(FIELD_GET_BADGE_USER_ID_REPEAT, user.getId());
      pst.setString(FIELD_GET_BADGE_BADGE_NAME_REPEAT, badgeName.name());

      try (ResultSet results = pst.executeQuery()) {
        results.next();
        return new UserBadge(user.getId(), badgeName, (results.getString("state") != null)
            ? mapper.readTree(results.getString("state")) : null);
      }
    } catch (SQLException | IOException e) {
      throw new SegueDatabaseException("Unable to get badge definition from database: " + e);
    }
  }

  @Override
  public void updateBadge(final UserBadge badge, final ITransaction transaction) throws SegueDatabaseException {

    if (!(transaction instanceof PgTransaction)) {
      throw new SegueDatabaseException("Unable to update database badge.");
    }

    String query = "UPDATE user_badges SET state = ?::jsonb WHERE user_id = ? and badge = ?";

    Connection conn = ((PgTransaction) transaction).getConnection();
    try (PreparedStatement pst = conn.prepareStatement(query)) {
      pst.setString(FIELD_UPDATE_BADGE_STATE, mapper.writeValueAsString(badge.getState()));
      pst.setLong(FIELD_UPDATE_BADGE_USER_ID, badge.getUserId());
      pst.setString(FIELD_UPDATE_BADGE_BADGE_NAME, badge.getBadgeName().name());

      pst.executeUpdate();

    } catch (SQLException | JsonProcessingException e) {
      throw new SegueDatabaseException("Unable to update database badge.");
    }
  }

  // Field Constants
  // getBadge
  private static final int FIELD_GET_BADGE_USER_ID = 1;
  private static final int FIELD_GET_BADGE_BADGE_NAME = 2;
  private static final int FIELD_GET_BADGE_USER_ID_REPEAT = 3;
  private static final int FIELD_GET_BADGE_BADGE_NAME_REPEAT = 4;

  // updateBadge
  private static final int FIELD_UPDATE_BADGE_STATE = 1;
  private static final int FIELD_UPDATE_BADGE_USER_ID = 2;
  private static final int FIELD_UPDATE_BADGE_BADGE_NAME = 3;
}
