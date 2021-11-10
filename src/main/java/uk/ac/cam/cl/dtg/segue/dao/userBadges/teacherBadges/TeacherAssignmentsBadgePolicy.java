package uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dos.ITransaction;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Iterator;

/**
 * Created by du220 on 01/05/2018.
 */
public class TeacherAssignmentsBadgePolicy implements IUserBadgePolicy {

    protected final AssignmentManager assignmentManager;
    protected final GameManager gameManager;

    public TeacherAssignmentsBadgePolicy(AssignmentManager assignmentManager,
                                         GameManager gameManager) {
        this.assignmentManager = assignmentManager;
        this.gameManager = gameManager;
    }

    @Override
    public int getLevel(JsonNode state) {
        return state.get("assignments").size();
    }

    @Override
    public JsonNode initialiseState(RegisteredUserDTO user, ITransaction transaction) {

        ArrayNode assignments = JsonNodeFactory.instance.arrayNode();

        try {
            for (AssignmentDTO assignment : assignmentManager.getAllAssignmentsSetByUser(user)) {
                assignments = updateAssignments(assignments, assignment.getId().toString());
            }
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
        }

        return JsonNodeFactory.instance.objectNode().set("assignments", assignments);
    }

    @Override
    public JsonNode updateState(RegisteredUserDTO user, JsonNode state, String event) {

        Iterator<JsonNode> iter = ((ArrayNode) state.get("assignments")).elements();

        while (iter.hasNext()) {
            if (iter.next().asText().equals(event)) {
                return state;
            }
        }

        ((ArrayNode) state.get("assignments")).add(event);
        return state;
    }

    /**
     * Returns an updated arrayNode object containing a new assignment (if it does not already exist in the array)
     *
     * @param assignments  the current array of assignment IDs
     * @param assignmentId a new assignment ID to add
     * @return the updated arrayNode
     */
    protected ArrayNode updateAssignments(ArrayNode assignments, String assignmentId) throws SegueDatabaseException {

        if (assignments.has(assignmentId)) {
            return assignments;
        }

        return assignments.add(assignmentId);
    }
}
