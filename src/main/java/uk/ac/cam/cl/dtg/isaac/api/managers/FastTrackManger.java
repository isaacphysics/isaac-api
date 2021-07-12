package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.FASTTRACK_GAMEBOARD_WHITELIST;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.FASTTRACK_LEVEL;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.FAST_TRACK_QUESTION_TYPE;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.QUESTION_TYPE;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.SEARCH_MAX_WINDOW_SIZE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_RESULTS_LIMIT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ID_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TAGS_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TITLE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX;

public class FastTrackManger {
    private static final Logger log = LoggerFactory.getLogger(FastTrackManger.class);

    private final String contentIndex;
    private final IContentManager contentManager;
    private final GameManager gameboardManager;
    private final Set<String> fastTrackGamebaordIds;

    /**
     * Creates a game manager that operates using the provided api.
     *
     * @param contentManager
     *            - so we can augment game objects with actual detailed content
     * @param gameboardManager
     *            - a gamebaord manager that deals with storing and retrieving gameboards.
     * @param contentIndex
     *            - the current content index of interest.
     */
    @Inject
    public FastTrackManger(final PropertiesLoader properties, final IContentManager contentManager, final GameManager gameboardManager,
                           @Named(CONTENT_INDEX) final String contentIndex) {

        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.gameboardManager = gameboardManager;
        String commaSeparatedIds = properties.getProperty(FASTTRACK_GAMEBOARD_WHITELIST);
        this.fastTrackGamebaordIds = new HashSet<>(Arrays.asList(commaSeparatedIds.split(",")));
    }

    /**
     * Cheacks if a gameboard ID is a valid fasttrack gameboard ID
     * @param gameboardId to check.
     * @return whether or not it is a valid fasttrack gameboard.
     */
    public final boolean isValidFasTrackGameboardId(final String gameboardId) {
        return fastTrackGamebaordIds.contains(gameboardId);
    }

    /**
     * Returns the concept name for a given question ID
     * @param questionId the question ID for which you want to find its fasttrack concept.
     * @return the concept string.
     * @throws ContentManagerException if content cant be found which matches the question ID.
     */
    public final String getConceptFromQuestionId(final String questionId) throws ContentManagerException {
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(FAST_TRACK_QUESTION_TYPE));
        fieldsToMatch.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, Arrays.asList(questionId));
        ResultsWrapper<ContentDTO> resultsList = contentManager.findByFieldNames(contentIndex,
                ContentService.generateDefaultFieldToMatch(fieldsToMatch), 0, DEFAULT_RESULTS_LIMIT);

        String upperConceptTitle = "";
        if (resultsList.getTotalResults() == 1) {
            upperConceptTitle = resultsList.getResults().get(0).getTitle();
        }
        return upperConceptTitle;
    }

    /**
     * Retrieve fasttrack concept progress
     *
     * @param gameboardId to look up.
     * @param conceptTitle concept title.
     * @param userQuestionAttempts - the map of user's question attempts.
     * @return list of gameboard items.
     * @throws ContentManagerException if there is a problem retrieving the content.
     */
    public final List<GameboardItem> getConceptProgress(
            final String gameboardId, final List<FASTTRACK_LEVEL> levelFilters, final String conceptTitle,
            final Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts
    ) throws ContentManagerException {
        List<ContentDTO> fastTrackAssociatedQuestions =
                this.getFastTrackConceptQuestions(gameboardId, levelFilters, conceptTitle);
        return gameboardManager.getGameboardItemProgress(fastTrackAssociatedQuestions, userQuestionAttempts);
    }

    /**
     * Queries the search provider for questions tagged with this board name and concept title.
     * The result is returned sorted.
     *
     * @param boardTag the tag which marks question's association with a certain board - the board's ID.
     * @param conceptTitle the title of the concept which is being searched for.
     * @return ordered list of concept questions associated with the board.
     * @throws ContentManagerException if there is a problem with the content manager (i.e. Elasticsearch)
     */
    private List<ContentDTO> getFastTrackConceptQuestions(
            final String boardTag, final List<FASTTRACK_LEVEL> levelFilters, final String conceptTitle
    ) throws ContentManagerException {
        List<String> stringLevelFilters = levelFilters.stream().map(FASTTRACK_LEVEL::name).collect(Collectors.toList());

        List<IContentManager.BooleanSearchClause> fieldsToMap = Lists.newArrayList();
        fieldsToMap.add(new IContentManager.BooleanSearchClause(
                TYPE_FIELDNAME, Constants.BooleanOperator.OR, Arrays.asList(QUESTION_TYPE, FAST_TRACK_QUESTION_TYPE)));
        fieldsToMap.add(new IContentManager.BooleanSearchClause(
                TITLE_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, Constants.BooleanOperator.AND, Collections.singletonList(conceptTitle)));
        fieldsToMap.add(new IContentManager.BooleanSearchClause(
                TAGS_FIELDNAME, Constants.BooleanOperator.AND, Collections.singletonList(boardTag)));
        fieldsToMap.add(new IContentManager.BooleanSearchClause(
                TAGS_FIELDNAME, Constants.BooleanOperator.OR, stringLevelFilters));

        Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
        sortInstructions.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, Constants.SortOrder.ASC);

        return this.contentManager.findByFieldNames(
                this.contentIndex, fieldsToMap, 0, SEARCH_MAX_WINDOW_SIZE, sortInstructions).getResults();
    }
}
