package uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * Created by du220 on 01/05/2018.
 */
public class TeacherAssignmentsBadgePolicy implements IUserBadgePolicy {

    private AssignmentManager assignmentManager;

    public TeacherAssignmentsBadgePolicy(AssignmentManager assignmentManager) {
        this.assignmentManager = assignmentManager;
    }

    @Override
    public int getLevel(Object state) {
        return ((JsonNode) state).get("assignments").size();
    }

    @Override
    public Object initialiseState(RegisteredUserDTO user) {

        ArrayNode assignments = JsonNodeFactory.instance.arrayNode();

        try {
            for (AssignmentDTO assignment : assignmentManager.getAllAssignmentsSetByUser(user)) {
                assignments.add(assignment.getId());
            }
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
        }

        return JsonNodeFactory.instance.objectNode().set("assignments", assignments);
    }

    @Override
    public Object updateState(RegisteredUserDTO user, Object state, Object event) {

        ((ArrayNode) ((JsonNode) state).get("assignments")).add((String) event);
        return state;
    }
}
