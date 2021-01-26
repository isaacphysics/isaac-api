/*
 * Copyright 2021 Raspberry Pi Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.services.ContentSummarizerService;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;

import javax.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.QUIZ_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_RESULTS_LIMIT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ID_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.VISIBLE_TO_STUDENTS_FIELDNAME;

/**
 * This class will be responsible for managing quizzes.
 *
 */
public class QuizManager {
    private static final Logger log = LoggerFactory.getLogger(QuizManager.class);

    private final IContentManager contentManager;
    private final ContentSummarizerService contentSummarizerService;
    private final String contentIndex;
    private final ContentService contentService;

    /**
     * Creates a quiz manager.
     *  @param contentManager
     *            - so we can augment quiz objects with actual detailed content
     *  @param contentSummarizerService
     *            - so we can summarize content with links
     *  @param contentService
     *            - so we can look up content
     * @param contentIndex
     *            - Index for the content to serve
     */
    @Inject
    public QuizManager(final IContentManager contentManager,
                       final ContentSummarizerService contentSummarizerService,
                       final ContentService contentService,
                       @Named(CONTENT_INDEX) final String contentIndex) {
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.contentSummarizerService = contentSummarizerService;
        this.contentService = contentService;
    }

    public ResultsWrapper<ContentSummaryDTO> getAvailableQuizzes(boolean onlyVisibleToStudents, Integer limit, Integer startIndex) throws ContentManagerException {

        Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch = Maps.newHashMap();
        fieldsToMatch.put(immutableEntry(Constants.BooleanOperator.AND, TYPE_FIELDNAME), Collections.singletonList(QUIZ_TYPE));

        if (onlyVisibleToStudents) {
            fieldsToMatch.put(immutableEntry(Constants.BooleanOperator.AND, VISIBLE_TO_STUDENTS_FIELDNAME), Collections.singletonList(Boolean.toString(true)));
        }

        Integer resultsLimit;
        Integer startIndexOfResults;

        if (null != limit) {
            resultsLimit = limit;
        } else {
            resultsLimit = DEFAULT_RESULTS_LIMIT;
        }

        if (null != startIndex) {
            startIndexOfResults = startIndex;
        } else {
            startIndexOfResults = 0;
        }

        return this.contentSummarizerService.extractContentSummaryFromResultsWrapper(this.contentManager.findByFieldNames(this.contentIndex, fieldsToMatch, startIndexOfResults, resultsLimit));
    }

    /**
     * For use when we expect to only find a single result.
     *
     * @param quizId the id of the quiz.
     *
     * @return The quiz.
     */
    public IsaacQuizDTO findQuiz(final String quizId) throws ContentManagerException {
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(QUIZ_TYPE));

        // options
        fieldsToMatch.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, Collections.singletonList(quizId));

        ResultsWrapper<ContentDTO> resultList = this.contentService.findMatchingContent(this.contentIndex,
            ContentService.generateDefaultFieldToMatch(fieldsToMatch), null, null);
        ContentDTO c;
        if (resultList.getResults().size() > 1) {
            throw new ContentManagerException("Multiple results ("
                + resultList.getResults().size() + ") returned error. For search query: " + fieldsToMatch.values());
        } else if (resultList.getResults().isEmpty()) {
            throw new NotFoundException("No content found that matches the query with parameters: "
                + fieldsToMatch.values());
        } else {
            c = resultList.getResults().get(0);
        }

        if (c instanceof IsaacQuizDTO) {
            return (IsaacQuizDTO) c;
        }

        throw new ContentManagerException("Expected an IsaacQuizDTO, got a " + c.getType());
    }
}
