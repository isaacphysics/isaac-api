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
import uk.ac.cam.cl.dtg.isaac.dto.IHasQuizSummary;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.QUIZ_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.VISIBLE_TO_STUDENTS_FIELDNAME;

/**
 * This class will be responsible for managing quizzes.
 *
 */
public class QuizManager {
    private static final Logger log = LoggerFactory.getLogger(QuizManager.class);

    private final ContentService contentService;
    private final IContentManager contentManager;
    private final ContentSummarizerService contentSummarizerService;
    private final String contentIndex;
    private final PropertiesLoader properties;

    /**
     * Creates a quiz manager.
     *
     * @param properties
     *            - global properties map
     * @param contentService
     *            - so we can look up content
     * @param contentManager
     *            - so we can fetch specific content.
     * @param contentSummarizerService
     *            - so we can summarize content with links
     * @param contentIndex
     *            - the current version of content to use.
     */
    @Inject
    public QuizManager(final PropertiesLoader properties, final ContentService contentService,
                       final IContentManager contentManager,
                       final ContentSummarizerService contentSummarizerService,
                       @Named(CONTENT_INDEX) final String contentIndex) {
        this.properties = properties;
        this.contentService = contentService;
        this.contentManager = contentManager;
        this.contentSummarizerService = contentSummarizerService;
        this.contentIndex = contentIndex;
    }

    public ResultsWrapper<ContentSummaryDTO> getAvailableQuizzes(boolean onlyVisibleToStudents, @Nullable Integer startIndex, @Nullable Integer limit) throws ContentManagerException {

        Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch = Maps.newHashMap();
        fieldsToMatch.put(immutableEntry(Constants.BooleanOperator.AND, TYPE_FIELDNAME), Collections.singletonList(QUIZ_TYPE));

        if (onlyVisibleToStudents) {
            fieldsToMatch.put(immutableEntry(Constants.BooleanOperator.AND, VISIBLE_TO_STUDENTS_FIELDNAME), Collections.singletonList(Boolean.toString(true)));
        }

        ResultsWrapper<ContentDTO> content = this.contentService.findMatchingContent(null, fieldsToMatch, startIndex, limit);

        return this.contentSummarizerService.extractContentSummaryFromResultsWrapper(content);
    }

    /**
     * For use when we expect to only find a single result.
     *
     * @param quizId the id of the quiz.
     *
     * @return The quiz.
     */
    public IsaacQuizDTO findQuiz(final String quizId) throws ContentManagerException {
        ContentDTO contentDTO = contentManager.getContentById(contentIndex, quizId);

        if (contentDTO instanceof IsaacQuizDTO) {
            return (IsaacQuizDTO) contentDTO;
        }

        throw new ContentManagerException("Expected an IsaacQuizDTO, got a " + contentDTO.getType());
    }

    /**
     * Fetch the quiz for each item and set the quizSummary field.
     *
     * @param items The items to augment.
     * @throws ContentManagerException If a quiz is missing.
     */
    public <T extends IHasQuizSummary> void augmentWithQuizSummary(List<T> items) throws ContentManagerException {
        Map<String, ContentSummaryDTO> quizCache = new HashMap<>();
        for (IHasQuizSummary item: items) {
            String quizId = item.getQuizId();
            ContentSummaryDTO quiz = quizCache.get(quizId);
            if (quiz == null) {
                quizCache.put(quizId, quiz = this.contentManager.extractContentSummary(this.findQuiz(quizId)));
            }
            item.setQuizSummary(quiz);
        }
    }

    /**
     * Get all of the sections in a quiz.
     *
     * In DEV mode, this throws exceptions on top-level non-sections. In PROD, it throws them away with a warning.
     *
     * @param quiz The quiz to extract sections from.
     * @return A list of sections.
     * @throws ContentManagerException when a non-section is found at the top-level, but only in DEV.
     */
    public List<IsaacQuizSectionDTO> extractSectionObjects(IsaacQuizDTO quiz) throws ContentManagerException {
        if (properties.getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(Constants.EnvironmentType.DEV.name())) {
            for (ContentBaseDTO content : quiz.getChildren()) {
                if (!(content instanceof IsaacQuizSectionDTO)) {
                    throw new ContentManagerException("Quiz id " + quiz.getId() + " contains top-level non-section: " + content);
                }
            }
            return quiz.getChildren().stream().map(c -> ((IsaacQuizSectionDTO) c)).collect(Collectors.toList());
        } else {
            return quiz.getChildren().stream().flatMap(c -> {
                if (c instanceof IsaacQuizSectionDTO) {
                    return Stream.of((IsaacQuizSectionDTO) c);
                } else {
                    log.warn("Quiz id " + quiz.getId() + " contains top-level non-section with id " + c.getId());
                    return Stream.empty();
                }
            }).collect(Collectors.toList());
        }
    }
}
