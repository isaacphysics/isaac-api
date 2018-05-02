package uk.ac.cam.cl.dtg.segue.dao.userBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * Created by du220 on 30/04/2018.
 */
public class SpecificQuestionsAnsweredPolicy extends AbstractQuestionsBadgePolicy {

    private final UserBadgeManager userBadgeManager;

    public SpecificQuestionsAnsweredPolicy(UserBadgeManager userBadgeManager) {
        this.userBadgeManager = userBadgeManager;
    }

    @Override
    public Object initialiseState(RegisteredUserDTO user) {

        try {

            // all badges pertaining to questions answered can be derived from total questions answered
            Object totalQuestionState = userBadgeManager.getOrCreateBadge(null, user,
                    UserBadgeManager.Badge.QUESTIONS_ANSWERED_TOTAL).getState();

            for (JsonNode questionNode : ((JsonNode) totalQuestionState).get("questionAttempts")) {
                if (questionNode.asBoolean()) {
                    //System.out.println(questionNode.);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Object updateState(RegisteredUserDTO user, Object state, Object event) {

        ArrayNode questionPageIds = (ArrayNode) ((ObjectNode) state).get("questionAttempts");

        Iterator<JsonNode> iter = questionPageIds.elements();

        while (iter.hasNext()) {
            JsonNode node = iter.next();

            if (node.equals((String) event)) {
                return state;
            }
        }

        ((ArrayNode) ((ObjectNode) state).get("questionAttempts")).add((String) event);

        return state;
    }
}
