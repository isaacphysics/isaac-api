package uk.ac.cam.cl.dtg.segue.dos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.eclipse.jgit.revwalk.FooterKey;
import org.reflections.Reflections;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.userBadges.IUserBadge;
import uk.ac.cam.cl.dtg.segue.dos.userBadges.MechanicsBadge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by du220 on 13/04/2018.
 */
public class PgUserBadgeManager {

    private PostgresSqlDb postgresSqlDb;
    private ObjectMapper mapper = new ObjectMapper();


    public PgUserBadgeManager(PostgresSqlDb postgresSqlDb) {
        this.postgresSqlDb = postgresSqlDb;
    }

    public UserBadgeFields getOrCreateBadge(Connection conn, UserBadgeFields.Badge badgeName) throws SQLException {

        if (null == conn) {
            conn = postgresSqlDb.getDatabaseConnection();
        }

        Long userId = 1L;

        PreparedStatement pst;
        pst = conn.prepareStatement("INSERT INTO user_badges (user_id, badge)" +
                " VALUES (?, ?) ON CONFLICT (user_id, badge) DO UPDATE SET user_id = excluded.user_id WHERE user_badges.user_id = ?" +
                " AND user_badges.badge = ? RETURNING *");
        pst.setLong(1, userId);
        pst.setString(2, badgeName.name());
        pst.setLong(3, userId);
        pst.setString(4, badgeName.name());

        ResultSet results = pst.executeQuery();
        results.next();
        UserBadgeFields badge = null;
        try {
            Set<Class<?>> badgeDefinitions = new Reflections("uk.ac.cam.cl.dtg").getTypesAnnotatedWith(UserBadgeDefinition.class);
            for (Class<?> def : badgeDefinitions) {

                if (def.getAnnotation(UserBadgeDefinition.class).value().equals(badgeName)) {
                    badge = (UserBadgeFields) def.newInstance();
                    break;
                }
            }
            // if badge still null then something wrong

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        badge.userId = userId;
        badge.badgeName = badgeName;
        badge.state = results.getObject("state");

        if (null == badge.state) {
            // do history trawl

            initialiseBadgeState(conn, badge);
        }

        return badge;
    }

    public UserBadgeFields updateBadge(Connection conn, UserBadgeFields.Badge badgeName, Object event) throws SQLException {

        if (null == conn) {
            conn = postgresSqlDb.getDatabaseConnection();
        }

        UserBadgeFields badge = getOrCreateBadge(conn, badgeName);

        int oldLevel = badge.getLevel();

        badge.updateState(event);

        int newLevel = badge.getLevel();
        if (newLevel != oldLevel) {
            // do notification
        }

        PreparedStatement pst;
        pst = conn.prepareStatement("UPDATE user_badges SET state = ?::jsonb WHERE user_id = ? and badge = ?");
        try {
            pst.setString(1, mapper.writeValueAsString(badge.state));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        pst.setLong(2, badge.userId);
        pst.setString(3, badge.badgeName.name());

        pst.executeUpdate();

        return badge;
    }

    private void initialiseBadgeState(Connection conn, UserBadgeFields badge) throws SQLException {

        // search history to work out if we have this badge, PG, elasticsearch, etc
        badge.initialiseState();

        // write new state to DB and return it
        PreparedStatement pst;
        pst = conn.prepareStatement("UPDATE user_badges SET state = ?::jsonb where user_id = ? AND badge = ?");
        try {
            pst.setString(1, mapper.writeValueAsString(badge.state));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        pst.setLong(2, badge.userId);
        pst.setString(3, badge.badgeName.name());

        pst.execute();
    }

/*
    private Object initialiseMechanicsBadgeState() {

        return ImmutableMap.of("questions", Lists.newArrayList());
    }

    private Object initialiseDynamicsBadgeState() {

        return new Object();
    }

    private int getMechanicsLevel(Object state) {

        return 1;
    }

    private int getDynamicsLevel(Object state) {

        return 2;
    }

    private Object updateMechanicsBadgeState(Object state, Object event) throws SQLException {

        // return new state based on current event
        ((Map<String, List<Object>>)state).get("questions").add(event);


        return state;
    }

    private Object updateDynamicsBadgeState(Object state, Object event) throws SQLException {

        // return new state based on current event
        return state;
    }*/
}
