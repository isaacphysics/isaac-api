package uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * Created by du220 on 01/05/2018.
 */
public class TeacherGroupsBadgePolicy implements IUserBadgePolicy {

    private GroupManager groupManager;

    public TeacherGroupsBadgePolicy(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public int getLevel(Object state) {
        return ((JsonNode) state).get("groups").size();
    }

    @Override
    public Object initialiseState(RegisteredUserDTO user) {

        ArrayNode groups = JsonNodeFactory.instance.arrayNode();

        try {
            for (UserGroupDTO group : groupManager.getGroupsByOwner(user)) {
                groups.add(group.getId());
            }
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
        }

        return JsonNodeFactory.instance.objectNode().set("groups", groups);
    }

    @Override
    public Object updateState(RegisteredUserDTO user, Object state, Object event) {

        ((ArrayNode) ((JsonNode) state).get("groups")).add((String) event);
        return state;
    }
}
