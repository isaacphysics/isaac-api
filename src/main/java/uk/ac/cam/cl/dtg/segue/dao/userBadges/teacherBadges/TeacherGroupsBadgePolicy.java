package uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Iterator;
import uk.ac.cam.cl.dtg.isaac.dos.ITransaction;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;

/**
 * Created by du220 on 01/05/2018.
 */
public class TeacherGroupsBadgePolicy implements IUserBadgePolicy {

  private final GroupManager groupManager;

  public TeacherGroupsBadgePolicy(final GroupManager groupManager) {
    this.groupManager = groupManager;
  }

  @Override
  public int getLevel(final JsonNode state) {
    return state.get("groups").size();
  }

  @Override
  public JsonNode initialiseState(final RegisteredUserDTO user, final ITransaction transaction) {

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
  public JsonNode updateState(final RegisteredUserDTO user, final JsonNode state, final String event) {

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
