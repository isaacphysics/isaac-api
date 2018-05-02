package uk.ac.cam.cl.dtg.segue.dao.userBadges.questionBadges;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.questionBadges.AbstractQuestionsBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by du220 on 27/04/2018.
 */
public class TotalQuestionsAnsweredPolicy extends AbstractQuestionsBadgePolicy {

    private final Map<String, UserBadgeManager.Badge> tagBadges =
            ImmutableMap.of("waves", UserBadgeManager.Badge.QUESTIONS_ANSWERED_WAVES);

    private final Map<Integer, UserBadgeManager.Badge> levelBadges =
            ImmutableMap.of(1, UserBadgeManager.Badge.QUESTIONS_ANSWERED_LEVEL1);

    private final QuestionManager questionManager;
    private final GameManager gameManager;
    private final UserBadgeManager userBadgeManager;
    private final IContentManager contentManager;
    private final String contentIndex;

    /**
     *
     * @param questionManager
     * @param gameManager
     * @param contentManager
     * @param contentIndex
     * @param userBadgeManager
     */
    @Inject
    public TotalQuestionsAnsweredPolicy(QuestionManager questionManager, GameManager gameManager,
                               IContentManager contentManager, String contentIndex,
                               UserBadgeManager userBadgeManager) {
        this.questionManager = questionManager;
        this.gameManager = gameManager;
        this.userBadgeManager = userBadgeManager;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
    }

    @Override
    public Object initialiseState(RegisteredUserDTO user) {

        ObjectNode incompleteAttempts = JsonNodeFactory.instance.objectNode();
        ArrayNode completeAttempts = JsonNodeFactory.instance.arrayNode();

        try {

            Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsByUser =
                    questionManager.getQuestionAttemptsByUser(user);

            for (Map.Entry<String, Map<String, List<QuestionValidationResponse>>> question :
                    questionAttemptsByUser.entrySet()) {

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

        } catch (SegueDatabaseException e) {
            e.printStackTrace();
        } catch (ContentManagerException e) {
            e.printStackTrace();
        }

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.set("completeAttempts", completeAttempts);
        result.set("incompleteAttempts", incompleteAttempts);

        return result;
    }

    @Override
    public Object updateState(RegisteredUserDTO user, Object state, Object event) {

        String questionPageId = ((String) event).split("\\|")[0];
        String questionPartId = ((String) event).split("\\|")[1];
        JsonNode completeAttempts = ((JsonNode) state).get("completeAttempts");

        // is question already completed?
        Iterator<JsonNode> iter = completeAttempts.elements();
        while (iter.hasNext()){
            JsonNode node = iter.next();

            if (node.equals((String) event)) {
                return state;
            }
        }


        JsonNode incompleteAttempts = ((JsonNode) state).get("incompleteAttempts");
        if (!incompleteAttempts.has(questionPageId)) {
            ((ObjectNode) incompleteAttempts).set(questionPageId,
                    JsonNodeFactory.instance.arrayNode());
        }

        // if inner array contains question part id, don't go further
        Iterator<JsonNode> qParts = ((ArrayNode) incompleteAttempts.get(questionPageId)).elements();
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


    /*private void updateDependentBadges(String questionPageId, RegisteredUserDTO user) {

        try {
            ContentDTO questionDetails = this.getQuestionDetails(questionPageId);
            System.out.println(questionDetails);

            // update tag badges
            for (String tag : questionDetails.getTags()) {
                if (tagBadges.containsKey(tag)) {
                    try {
                        this.userBadgeManager.updateBadge(null, user, tagBadges.get(tag), questionPageId);
                    } catch (SQLException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // update level badges
            if (levelBadges.containsKey(questionDetails.getLevel())) {
                try {
                    this.userBadgeManager.updateBadge(null, user, levelBadges.get(questionDetails.getLevel()), questionPageId);
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (ContentManagerException e) {
            e.printStackTrace();
        }
    }*/


    /**
     *
     * @param questionPageId
     * @return
     * @throws ContentManagerException
     */
    private ContentDTO getQuestionDetails(String questionPageId) throws ContentManagerException {
        return this.contentManager.getContentById(this.contentIndex, questionPageId);
    }
}

