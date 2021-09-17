/**
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
package uk.ac.cam.cl.dtg.isaac.api.services;

import com.google.inject.Inject;
import ma.glasnost.orika.MapperFacade;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuizSummaryDTO;

import java.util.ArrayList;

public class ContentSummarizerService {
    protected final MapperFacade mapper;
    private final URIManager uriManager;

    @Inject
    public ContentSummarizerService(final MapperFacade mapper, final URIManager uriManager) {
        this.mapper = mapper;
        this.uriManager = uriManager;
    }

    /**
     * This method will extract basic information from a content object so the lighter ContentInfo object can be sent to
     * the client instead.
     *
     * @param content
     *            - the content object to summarise
     * @return ContentSummaryDTO.
     */
    private ContentSummaryDTO extractContentSummary(final ContentDTO content) {
        if (null == content) {
            return null;
        }

        // try auto-mapping
        ContentSummaryDTO contentInfo = mapper.map(content, ContentSummaryDTO.class);
        contentInfo.setUrl(uriManager.generateApiUrl(content));

        return contentInfo;
    }

    /**
     * This method will extract basic information from a content object so the lighter ContentInfo object can be sent to
     * the client instead.
     *
     * @param content
     *            - the content object to summarise
     * @return ContentSummaryDTO.
     */
    private QuizSummaryDTO extractQuizSummary(final ContentDTO content) {
        if (null == content) {
            return null;
        }

        // try auto-mapping
        QuizSummaryDTO contentInfo = mapper.map(content, QuizSummaryDTO.class);
        contentInfo.setUrl(uriManager.generateApiUrl(content));

        return contentInfo;
    }

    /**
     * Utility method to convert a ResultsWrapper of content objects into one with ContentSummaryDTO objects.
     *
     * @param contentList
     *            - the list of content to summarise.
     * @return list of shorter ContentSummaryDTO objects.
     */
    public ResultsWrapper<ContentSummaryDTO> extractContentSummaryFromResultsWrapper(
        final ResultsWrapper<ContentDTO> contentList) {
        if (null == contentList) {
            return null;
        }

        ResultsWrapper<ContentSummaryDTO> contentSummaryResults = new ResultsWrapper<ContentSummaryDTO>(
            new ArrayList<ContentSummaryDTO>(), contentList.getTotalResults());

        for (ContentDTO content : contentList.getResults()) {
            ContentSummaryDTO contentInfo = extractContentSummary(content);
            if (null != contentInfo) {
                contentSummaryResults.getResults().add(contentInfo);
            }
        }
        return contentSummaryResults;
    }

    /**
     * Utility method to convert a ResultsWrapper of content objects into one with QuizSummaryDTO objects.
     *
     * @param contentList
     *            - the list of content to summarise.
     * @return list of shorter QuizSummaryDTO objects.
     */
    public ResultsWrapper<QuizSummaryDTO> extractQuizSummaryFromResultsWrapper(
            final ResultsWrapper<ContentDTO> contentList) {
        if (null == contentList) {
            return null;
        }

        ResultsWrapper<QuizSummaryDTO> contentSummaryResults = new ResultsWrapper<QuizSummaryDTO>(
                new ArrayList<QuizSummaryDTO>(), contentList.getTotalResults());

        for (ContentDTO content : contentList.getResults()) {
            QuizSummaryDTO contentInfo = extractQuizSummary(content);
            if (null != contentInfo) {
                contentSummaryResults.getResults().add(contentInfo);
            }
        }
        return contentSummaryResults;
    }
}
