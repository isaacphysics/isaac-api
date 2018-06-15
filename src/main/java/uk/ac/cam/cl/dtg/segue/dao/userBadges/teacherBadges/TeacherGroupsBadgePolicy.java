package uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dos.ITransaction;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Iterator;

/**
 * Created by du220 on 01/05/2018.
 */
public class TeacherGroupsBadgePolicy implements IUserBadgePolicy {

    private GroupManager groupManager;

    public TeacherGroupsBadgePolicy(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public int getLevel(JsonNode state) {
        return state.get("groups").size();
    }

    @Override
    public JsonNode initialiseState(RegisteredUserDTO user, ITransaction transaction) {

        ArrayNode groups = JsonNodeFactory.instance.arrayNode();

        try {
            for (UserGroupDTO group : groupManager.getGroupsByOwner(user)) {
                groups.add(group.getId().toString());
            }
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
        }

        return JsonNodeFactory.instance.objectNode().set("groups", groups);
    }

    @Override
    public JsonNode updateState(RegisteredUserDTO user, JsonNode state, String event) {

        Iterator<JsonNode> iter = ((ArrayNode) state.get("groups")).elements();

        while (iter.hasNext()) {
            if (iter.next().asText().equals(event)) {
                return state;
            }
        }

        ((ArrayNode) state.get("groups")).add(event);

        return state;
    }
}
