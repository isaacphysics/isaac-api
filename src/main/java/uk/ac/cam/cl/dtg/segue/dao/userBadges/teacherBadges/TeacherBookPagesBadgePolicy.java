package uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.util.List;

/**
 * TODO: the inheritance of the teacher assignments badge policy might want to be improved
 *
 * Created by du220 on 14/05/2018.
 */
public class TeacherBookPagesBadgePolicy extends TeacherAssignmentsBadgePolicy {

    private final List<String> bookTags = ImmutableList.of("phys_book_gcse", "physics_skills_14", "chemistry_16");

    public TeacherBookPagesBadgePolicy(AssignmentManager assignmentManager, GameManager gameManager) {
        super(assignmentManager, gameManager);
    }

    @Override
    protected ArrayNode updateAssignments(ArrayNode assignments, String assignmentId) throws SegueDatabaseException {

        if (assignments.has(assignmentId) ||
                !isBookPage(assignmentManager.getAssignmentById(Long.parseLong(assignmentId)))) {
            return assignments;
        }

        return assignments.add(assignmentId);
    }

    /**
     * Determines whether an assignment can be classed as a "book assignment"
     *
     * @param assignment the assignment object
     * @return truth of "book assignment" definition
     */
    private Boolean isBookPage(AssignmentDTO assignment) throws SegueDatabaseException {

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
