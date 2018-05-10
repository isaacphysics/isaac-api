package uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.List;

/**
 * Created by du220 on 01/05/2018.
 */
public class TeacherAssignmentsBadgePolicy implements IUserBadgePolicy {

    protected final AssignmentManager assignmentManager;
    protected final GameManager gameManager;
    protected final List<String> bookTags = ImmutableList.of("phys_book_gcse", "physics_skills_14", "chemistry_16");

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
    public JsonNode initialiseState(RegisteredUserDTO user) {

        ArrayNode assignments = JsonNodeFactory.instance.arrayNode();

        try {
            for (AssignmentDTO assignment : assignmentManager.getAllAssignmentsSetByUser(user)) {
                assignments = updateArray(assignments, assignment.getId().toString());
            }
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
        }

        return JsonNodeFactory.instance.objectNode().set("assignments", assignments);
    }

    @Override
    public JsonNode updateState(RegisteredUserDTO user, JsonNode state, String event) throws SegueDatabaseException {

        ((ArrayNode) state.get("assignments")).add(event);
        updateArray(((ArrayNode) state.get("assignments")), event);

        /*if (isBookPage(assignmentManager.getAssignmentById(Long.parseLong(event)))) {
            userBadgeManager.updateBadge(null, user, UserBadgeManager.Badge.TEACHER_BOOK_PAGES_SET, event);
        }*/

        return state;
    }

    /**
     *
     * @param assignments
     * @param assignmentId
     * @return
     */
    protected ArrayNode updateArray(ArrayNode assignments, String assignmentId) throws SegueDatabaseException {

        if (assignments.has(assignmentId)) {
            return assignments;
        }

        return assignments.add(assignmentId);
    }

    /**
     *
     * @param assignment
     * @return
     */
    protected Boolean isBookPage(AssignmentDTO assignment) throws SegueDatabaseException {

        for (GameboardItem item : gameManager.getGameboard(assignment.getGameboardId()).getQuestions()) {

            if (null != item.getTags()) {
                for (String tag : item.getTags()) {

                    if (bookTags.contains(tag)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
