package uk.ac.cam.cl.dtg.segue.dao.userbadges.teacherbadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.userbadges.IUserBadgePolicy;

public class TeacherGroupsBadgePolicy implements IUserBadgePolicy {
  private static final Logger log = LoggerFactory.getLogger(TeacherGroupsBadgePolicy.class);

  private final GroupManager groupManager;

  public TeacherGroupsBadgePolicy(final GroupManager groupManager) {
    this.groupManager = groupManager;
  }

  @Override
  public int getLevel(final JsonNode state) {
    return state.get("groups").size();
  }

  @Override
  public JsonNode initialiseState(final RegisteredUserDTO user) {

    ArrayNode groups = JsonNodeFactory.instance.arrayNode();

    try {
      for (UserGroupDTO group : groupManager.getGroupsByOwner(user)) {
        groups.add(group.getId().toString());
      }
    } catch (SegueDatabaseException e) {
      log.error("Error initialising state", e);
    }

    return JsonNodeFactory.instance.objectNode().set("groups", groups);
  }

  @Override
  public JsonNode updateState(final JsonNode state, final String event) {

    Iterator<JsonNode> iter = state.get("groups").elements();

    while (iter.hasNext()) {
      if (iter.next().asText().equals(event)) {
        return state;
      }
    }

    ((ArrayNode) state.get("groups")).add(event);

    return state;
  }
}
