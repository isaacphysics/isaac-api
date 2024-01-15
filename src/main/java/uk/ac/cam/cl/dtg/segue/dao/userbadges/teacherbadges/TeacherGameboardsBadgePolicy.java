package uk.ac.cam.cl.dtg.segue.dao.userbadges.teacherbadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardCreationMethod;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.userbadges.IUserBadgePolicy;

public class TeacherGameboardsBadgePolicy implements IUserBadgePolicy {
  private static final Logger log = LoggerFactory.getLogger(TeacherGameboardsBadgePolicy.class);
  private final GameManager gameManager;

  public TeacherGameboardsBadgePolicy(final GameManager gameManager) {
    this.gameManager = gameManager;
  }

  @Override
  public int getLevel(final JsonNode state) {
    return state.get("gameboards").size();
  }

  @Override
  public JsonNode initialiseState(final RegisteredUserDTO user) {

    ArrayNode gameboards = JsonNodeFactory.instance.arrayNode();

    try {
      for (GameboardDTO gameboard : gameManager.getUsersGameboards(user, 0,
          null, null, null).getResults()) {

        if (user.getId().equals(gameboard.getOwnerUserId())
            && GameboardCreationMethod.BUILDER.equals(gameboard.getCreationMethod())) {
          gameboards.add(gameboard.getId());
        }
      }
    } catch (ContentManagerException | SegueDatabaseException e) {
      log.error("Error initialising state", e);
    }

    return JsonNodeFactory.instance.objectNode().set("gameboards", gameboards);

  }

  @Override
  public JsonNode updateState(final JsonNode state, final String event) {

    Iterator<JsonNode> iter = state.get("gameboards").elements();

    while (iter.hasNext()) {
      if (iter.next().asText().equals(event)) {
        return state;
      }
    }

    ((ArrayNode) state.get("gameboards")).add(event);
    return state;
  }
}
