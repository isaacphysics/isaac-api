package uk.ac.cam.cl.dtg.segue.dao.userBadges.questionBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Abstract question answer badge policy
 *
 * Created by du220 on 14/05/2018.
 */
public abstract class AbstractQuestionsAnsweredBadgePolicy implements IUserBadgePolicy {

    private final QuestionManager questionManager;
    private final GameManager gameManager;
    protected final List<Integer> thresholds = Arrays.asList(1, 5, 10, 15, 25, 50, 75, 100, 150, 200);


    /**
     * Constructor
     *
     * @param questionManager for retrieving question attempt data
     * @param gameManager for retrieving question page data
     */
    public AbstractQuestionsAnsweredBadgePolicy(QuestionManager questionManager, GameManager gameManager) {
        this.questionManager = questionManager;
        this.gameManager = gameManager;
    }

    @Override
    public int getLevel(JsonNode state) {

        Integer count = state.get("completeAttempts").size();

        if (count != 0) {
            for (Integer i = 0; i < thresholds.size(); i++) {
                if (count < thresholds.get(i)) {
                    return thresholds.get(i - 1);
                }
            }

            // if not returned from the loop, return the highest threshold
            return thresholds.get(thresholds.size() - 1);
        } else {
            return 0;
        }
    }

    @Override
    public JsonNode initialiseState(RegisteredUserDTO user) {

        ObjectNode incompleteAttempts = JsonNodeFactory.instance.objectNode();
        ArrayNode completeAttempts = JsonNodeFactory.instance.arrayNode();

        try {
            Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsByUser =
                    questionManager.getQuestionAttemptsByUser(user);

            for (Map.Entry<String, Map<String, List<QuestionValidationResponse>>> question :
                    questionAttemptsByUser.entrySet()) {

                // filter by type of question we want to track
                if (!isRelevantQuestion(question.getKey())) {
                    continue;
                }

                ArrayNode questionPartAttempts = JsonNodeFactory.instance.arrayNode();

                Collection<QuestionDTO> availableQuestionParts =
                        gameManager.getAllMarkableQuestionPartsDFSOrder(question.getKey());

                for (QuestionDTO questionPart : availableQuestionParts) {

                    if (question.getValue().containsKey(questionPart.getId())) {
                        for (QuestionValidationResponse validationResponse : question.getValue().get(questionPart.getId())) {

                            if (validationResponse.isCorrect() != null && validationResponse.isCorrect() &&
                                    !questionPartAttempts.has(validationResponse.getQuestionId().split("\\|")[1])) {

                                questionPartAttempts.add(validationResponse.getQuestionId().split("\\|")[1]);
                                break;
                            }
                        }
                    }
                }

                if (questionPartAttempts.size() == availableQuestionParts.size()) {
                    // whole question correct
                    incompleteAttempts.remove(question.getKey().split("\\|")[0]);
                    completeAttempts.add(question.getKey().split("\\|")[0]);

                } else {
                    // make note of parts already correct
                    incompleteAttempts.set(question.getKey().split("\\|")[0], questionPartAttempts);
                }
            }

        } catch (SegueDatabaseException | ContentManagerException e) {
            e.printStackTrace();
        }

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.set("completeAttempts", completeAttempts);
        result.set("incompleteAttempts", incompleteAttempts);

        return result;
    }

    @Override
    public JsonNode updateState(RegisteredUserDTO user, JsonNode state, String questionId)
            throws SegueDatabaseException {

        String questionPageId = questionId.split("\\|")[0];

        // filter by type of question we want to track
        /*if (!isRelevantQuestion(questionPageId)) {
            return state;
        }*/

        String questionPartId = questionId.split("\\|")[1];
        JsonNode completeAttempts = state.get("completeAttempts");

        // is question already completed?
        Iterator<JsonNode> iter = completeAttempts.elements();
        while (iter.hasNext()) {
            JsonNode node = iter.next();

            if (node.asText().equals(questionId)) {
                return state;
            }
        }

        JsonNode incompleteAttempts = state.get("incompleteAttempts");
        if (!incompleteAttempts.has(questionPageId)) {
            ((ObjectNode) incompleteAttempts).set(questionPageId,
                    JsonNodeFactory.instance.arrayNode());
        }

        // if inner array contains question part id, don't go further
        Iterator<JsonNode> qParts = incompleteAttempts.get(questionPageId).elements();
        while (qParts.hasNext()) {

            JsonNode qPart = qParts.next();
            if (qPart.asText().equals(questionPartId)) {
                return state;
            }
        }

        // updating for new part attempts
        try {
            Collection<QuestionDTO> availableQuestionParts =
                    gameManager.getAllMarkableQuestionPartsDFSOrder(questionPageId);

            if (!incompleteAttempts.get(questionPageId).has(questionPartId) &&
                    availableQuestionParts.size() == incompleteAttempts.get(questionPageId).size() + 1) {

                ((ObjectNode) incompleteAttempts).remove(questionPageId);
                ((ArrayNode) completeAttempts).add(questionPageId);

                return state;
            }

        } catch (ContentManagerException e) {
            e.printStackTrace();
        }

        ((ArrayNode) incompleteAttempts.get(questionPageId)).add(questionPartId);

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.set("completeAttempts", completeAttempts);
        result.set("incompleteAttempts", incompleteAttempts);

        return result;
    }

    /**
     * Abstract helper method that returns true if the given question is of a type we are interested in counting
     * Filter to be defined by inheriting classes
     *
     * @param questionPageId the id of the question page
     * @return truth of whether question is of interest
     */
    abstract Boolean isRelevantQuestion(String questionPageId);
}
