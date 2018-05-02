package uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardCreationMethod;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * Created by du220 on 01/05/2018.
 */
public class TeacherGameboardsBadgePolicy implements IUserBadgePolicy {

    private GameManager gameManager;

    public TeacherGameboardsBadgePolicy(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public int getLevel(Object state) {
        return ((JsonNode) state).get("gameboards").size();
    }

    @Override
    public Object initialiseState(RegisteredUserDTO user) {

        ArrayNode gameboards = JsonNodeFactory.instance.arrayNode();

        try {
            for (GameboardDTO gameboard : gameManager.getUsersGameboards(user, 0,
                    null, null, null).getResults()) {

                if (gameboard.getCreationMethod().equals(GameboardCreationMethod.BUILDER)) {
                    gameboards.add(gameboard.getId());
                }
            }
        } catch (ContentManagerException | SegueDatabaseException e) {
            e.printStackTrace();
        }

        return JsonNodeFactory.instance.objectNode().set("gameboards", gameboards);

    }

    @Override
    public Object updateState(RegisteredUserDTO user, Object state, Object event) {

        ((ArrayNode) ((JsonNode) state).get("gameboards")).add((String) event);
        return state;
    }
}
