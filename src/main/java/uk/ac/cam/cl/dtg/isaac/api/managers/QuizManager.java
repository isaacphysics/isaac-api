/*
 * Copyright 2021 Raspberry Pi Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.services.ContentSummarizerService;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.IHasQuizSummary;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuizSummaryDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * This class will be responsible for managing quizzes.
 *
 */
public class QuizManager {
    private static final Logger log = LoggerFactory.getLogger(QuizManager.class);

    private final AbstractConfigLoader properties;
    private final ContentService contentService;
    private final GitContentManager contentManager;
    private final ContentSummarizerService contentSummarizerService;
    private final ContentMapper mapper;

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
     * @param mapper
     *            - so we can convert cached content DOs to DTOs.
     */
    @Inject
    public QuizManager(final AbstractConfigLoader properties, final ContentService contentService,
                       final GitContentManager contentManager,
                       final ContentSummarizerService contentSummarizerService,
                       final ContentMapper mapper) {
        this.properties = properties;
        this.contentService = contentService;
        this.contentManager = contentManager;
        this.contentSummarizerService = contentSummarizerService;
        this.mapper = mapper;
    }

    public ResultsWrapper<ContentSummaryDTO> getAvailableQuizzes(String visibleToRole, @Nullable Integer startIndex, @Nullable Integer limit) throws ContentManagerException {

        List<GitContentManager.BooleanSearchClause> fieldsToMatch = Lists.newArrayList();
        fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
                TYPE_FIELDNAME, Constants.BooleanOperator.AND, Collections.singletonList(QUIZ_TYPE)));

        if (null != visibleToRole) {
            fieldsToMatch.add(new GitContentManager.BooleanSearchClause(HIDDEN_FROM_ROLES_FIELDNAME,
                    BooleanOperator.NOT, Collections.singletonList(visibleToRole)));
        }

        ResultsWrapper<ContentDTO> content = this.contentService.findMatchingContent(fieldsToMatch, startIndex, limit);

        return this.contentSummarizerService.extractContentSummaryFromResultsWrapper(content, QuizSummaryDTO.class);
    }

    /**
     * For use when we expect to only find a single result. Returns a fresh DTO (suitable for mutation) every time.
     *
     * @param quizId the id of the quiz.
     *
     * @return The quiz.
     */
    public IsaacQuizDTO findQuiz(final String quizId) throws ContentManagerException {
        Content cachedContent = contentManager.getContentDOById(quizId);

        if (cachedContent == null) {
            throw new ContentManagerException("Couldn't find test with id " + quizId);
        }

        if (cachedContent instanceof IsaacQuiz) {
            ContentDTO contentDTO = this.mapper.getDTOByDO(cachedContent);

            if (contentDTO instanceof IsaacQuizDTO) {
                return (IsaacQuizDTO) contentDTO;
            } else {
                throw new ContentManagerException("Expected an IsaacQuizDTO, got a " + contentDTO.getType());
            }
        } else {
            throw new ContentManagerException("Expected an IsaacQuiz (id=" + quizId + "), got a " + cachedContent.getType());
        }
    }

    /**
     * Fetch the quiz for each item and set the quizSummary field.
     *
     * @param items The items to augment.
     */
    public <T extends IHasQuizSummary> void augmentWithQuizSummary(List<T> items) {
        Map<String, ContentSummaryDTO> quizCache = new HashMap<>();
        for (IHasQuizSummary item: items) {
            String quizId = item.getQuizId();
            ContentSummaryDTO quiz = quizCache.get(quizId);
            if (quiz == null) {
                try {
                    quiz = this.contentSummarizerService.extractContentSummary(this.findQuiz(quizId), QuizSummaryDTO.class);
                } catch (ContentManagerException e) {
                    if (item instanceof QuizAttemptDTO) {
                        log.warn("Attempt (" + ((QuizAttemptDTO) item).getId() +  ") exists with test ID ("
                            + item.getQuizId() + ") that does not exist!");
                    } else if (item instanceof QuizAssignmentDTO) {
                        log.warn("Assignment (" + ((QuizAssignmentDTO) item).getId() +  ") exists with test ID ("
                            + item.getQuizId() + ") that does not exist!");
                    }
                }
                quizCache.put(quizId, quiz);
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
                    throw new ContentManagerException("Test id " + quiz.getId() + " contains top-level non-section: " + content);
                }
            }
            return quiz.getChildren().stream().map(c -> ((IsaacQuizSectionDTO) c)).collect(Collectors.toList());
        } else {
            return quiz.getChildren().stream().flatMap(c -> {
                if (c instanceof IsaacQuizSectionDTO) {
                    return Stream.of((IsaacQuizSectionDTO) c);
                } else {
                    log.warn("Test id " + quiz.getId() + " contains top-level non-section with id " + c.getId());
                    return Stream.empty();
                }
            }).collect(Collectors.toList());
        }
    }
}
