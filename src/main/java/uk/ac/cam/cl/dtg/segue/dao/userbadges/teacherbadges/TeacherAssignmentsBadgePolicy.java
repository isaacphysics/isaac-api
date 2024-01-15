package uk.ac.cam.cl.dtg.segue.dao.userbadges.teacherbadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.userbadges.IUserBadgePolicy;

public class TeacherAssignmentsBadgePolicy implements IUserBadgePolicy {
  private static final Logger log = LoggerFactory.getLogger(TeacherAssignmentsBadgePolicy.class);

  private final AssignmentManager assignmentManager;

  public TeacherAssignmentsBadgePolicy(final AssignmentManager assignmentManager) {
    this.assignmentManager = assignmentManager;
  }

  @Override
  public int getLevel(final JsonNode state) {
    return state.get("assignments").size();
  }

  @Override
  public JsonNode initialiseState(final RegisteredUserDTO user) {

    ArrayNode assignments = JsonNodeFactory.instance.arrayNode();

    try {
      for (AssignmentDTO assignment : assignmentManager.getAllAssignmentsSetByUser(user)) {
        assignments = updateAssignments(assignments, assignment.getId().toString());
      }
    } catch (SegueDatabaseException e) {
      log.error("Error initialising state", e);
    }

    return JsonNodeFactory.instance.objectNode().set("assignments", assignments);
  }

  @Override
  public JsonNode updateState(final JsonNode state, final String event) {

    Iterator<JsonNode> iter = state.get("assignments").elements();

    while (iter.hasNext()) {
      if (iter.next().asText().equals(event)) {
        return state;
      }
    }

    ((ArrayNode) state.get("assignments")).add(event);
    return state;
  }

  /**
   * Returns an updated arrayNode object containing a new assignment (if it does not already exist in the array).
   *
   * @param assignments  the current array of assignment IDs
   * @param assignmentId a new assignment ID to add
   * @return the updated arrayNode
   */
  protected ArrayNode updateAssignments(final ArrayNode assignments, final String assignmentId)
      throws SegueDatabaseException {

    if (assignments.has(assignmentId)) {
      return assignments;
    }

    return assignments.add(assignmentId);
  }
}
