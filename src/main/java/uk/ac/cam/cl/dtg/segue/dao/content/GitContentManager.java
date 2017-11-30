/*
 * Copyright 2014 Stephen Cummins and Ian Davies
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
package uk.ac.cam.cl.dtg.segue.dao.content;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.ws.rs.NotFoundException;

import com.google.common.base.Functions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.Validate;
import org.eclipse.jgit.revwalk.RevCommit;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SimpleFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.TermsFilterInstruction;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Implementation that specifically works with Content objects.
 * 
 */
public class GitContentManager implements IContentManager {
    private static final Logger log = LoggerFactory.getLogger(GitContentManager.class);

    private static final String CONTENT_TYPE = "content";

    private final GitDb database;
    private final ContentMapper mapper;
    private final ISearchProvider searchProvider;
    private final PropertiesLoader globalProperties;
    private final boolean allowOnlyPublishedContent;

    private final Random randomNumberGenerator = new Random();

    private final Cache<Object, Object> cache;


    /**
     * Constructor for instantiating a new Git Content Manager Object.
     * 
     * @param database
     *            - that the content Manager manages.
     * @param searchProvider
     *            - search provider that the content manager manages and controls.
     * @param contentMapper
     *            - The utility class for mapping content objects.
     * @param globalProperties
     *            - global properties.
     */
    @Inject
    public GitContentManager(final GitDb database, final ISearchProvider searchProvider,
                             final ContentMapper contentMapper, final PropertiesLoader globalProperties) {
        this.database = database;
        this.mapper = contentMapper;
        this.searchProvider = searchProvider;
        this.globalProperties = globalProperties;
        this.allowOnlyPublishedContent = Boolean.parseBoolean(
                globalProperties.getProperty(Constants.SHOW_ONLY_PUBLISHED_CONTENT));

        if (this.allowOnlyPublishedContent) {
            log.info("API Configured to only allow published content to be returned.");
        }

        this.cache = CacheBuilder.newBuilder().softValues().expireAfterAccess(1, TimeUnit.DAYS).build();
    }

    /**
     * FOR TESTING PURPOSES ONLY - Constructor for instantiating a new Git Content Manager Object.
     * 
     * @param database
     *            - that the content Manager manages.
     * @param searchProvider
     *            - search provider that the content manager manages and controls.
     * @param contentMapper
     *            - The utility class for mapping content objects.
     */
    public GitContentManager(final GitDb database, final ISearchProvider searchProvider,
            final ContentMapper contentMapper) {
        this.database = database;
        this.mapper = contentMapper;
        this.searchProvider = searchProvider;
        this.globalProperties = null;
        this.allowOnlyPublishedContent = false;
        this.cache = CacheBuilder.newBuilder().softValues().expireAfterAccess(1, TimeUnit.DAYS).build();
    }

    @Override
    public final ContentDTO getContentById(final String version, final String id) throws ContentManagerException {
        String k = "getContentById~" + version + "~" + id;
        if (!cache.asMap().containsKey(k)) {
            ContentDTO c = this.mapper.getDTOByDO(this.getContentDOById(version, id));
            if (c != null) {
                cache.put(k, c);
            }
        }

        return (ContentDTO) cache.getIfPresent(k);
    }

    @Override
    public final Content getContentDOById(final String version, final String id) throws ContentManagerException {
        if (null == id || id.equals("")) {
            return null;
        }

        String k = "getContentDOById~" + version + "~" + id;
        if (!cache.asMap().containsKey(k)) {

            List<Content> searchResults = mapper.mapFromStringListToContentList(this.searchProvider.termSearch(version,
                    CONTENT_TYPE, id,
                    Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, 0, 1,
                    this.getUnpublishedFilter()).getResults());

            if (null == searchResults || searchResults.isEmpty()) {
                log.error("Failed to locate the content (" + id + ") in the cache for version "
                        + getCurrentContentSHA() + " (" + version + ")");
                return null;
            }

            cache.put(k, searchResults.get(0));
        }

        return (Content) cache.getIfPresent(k);

    }

    @Override
    public ResultsWrapper<ContentDTO> getByIdPrefix(final String version, final String idPrefix, final int startIndex,
            final int limit) throws ContentManagerException {

        String k = "getByIdPrefix~" + version + "~" + idPrefix + "~" + startIndex + "~" + limit;
        if (!cache.asMap().containsKey(k)) {

            ResultsWrapper<String> searchHits = this.searchProvider.findByPrefix(version, CONTENT_TYPE,
                    Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                    idPrefix, startIndex, limit, this.getUnpublishedFilter());

            List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());

            cache.put(k, new ResultsWrapper<>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults()));
        }

        return (ResultsWrapper<ContentDTO>) cache.getIfPresent(k);
    }

    @Override
    public ResultsWrapper<ContentDTO> getContentMatchingIds(final String version, final Collection<String> ids,
                                                            final int startIndex, final int limit)
            throws ContentManagerException {

        String k = "getContentMatchingIds~" + version + "~" + ids.toString() + "~" + startIndex + "~" + limit;
        if (!cache.asMap().containsKey(k)) {

            Map<String, AbstractFilterInstruction> finalFilter = Maps.newHashMap();
            finalFilter.putAll(new ImmutableMap.Builder<String, AbstractFilterInstruction>()
                                .put(Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                                    new TermsFilterInstruction(ids))
                                .build());

            if (getUnpublishedFilter() != null) {
                finalFilter.putAll(getUnpublishedFilter());
            }

            ResultsWrapper<String> searchHits = this.searchProvider.termSearch(version, CONTENT_TYPE, null,
                    null,
                    startIndex, limit, finalFilter);

            List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());
            cache.put(k, new ResultsWrapper<>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults()));
        }

        return (ResultsWrapper<ContentDTO>) cache.getIfPresent(k);
    }

    @Override
    public ResultsWrapper<ContentDTO> getAllByTypeRegEx(final String version, final String regex, final int startIndex,
            final int limit) throws ContentManagerException {

        String k = "getAllByTypeRegEx~" + version + "~" + regex + "~" + startIndex + "~" + limit;

        if (!cache.asMap().containsKey(k)) {

            ResultsWrapper<String> searchHits = this.searchProvider.findByRegEx(version, CONTENT_TYPE,
                    Constants.TYPE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                    regex, startIndex, limit, this.getUnpublishedFilter());

            List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());

            cache.put(k, new ResultsWrapper<>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults()));
        }

        return (ResultsWrapper<ContentDTO>) cache.getIfPresent(k);
    }

    @Override
    public final ResultsWrapper<ContentDTO> searchForContent(final String version, final String searchString,
            @Nullable final Map<String, List<String>> fieldsThatMustMatch, 
            final Integer startIndex, final Integer limit) throws ContentManagerException {

        ResultsWrapper<String> searchHits = searchProvider.fuzzySearch(version, CONTENT_TYPE, searchString, startIndex,
                limit, fieldsThatMustMatch, this.getUnpublishedFilter(), Constants.ID_FIELDNAME,
                Constants.TITLE_FIELDNAME, Constants.TAGS_FIELDNAME, Constants.VALUE_FIELDNAME,
                Constants.CHILDREN_FIELDNAME);

        List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());

        return new ResultsWrapper<>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults());
    }

    @Override
    public final ResultsWrapper<ContentDTO> findByFieldNames(final String version,
            final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
            final Integer startIndex, final Integer limit) throws ContentManagerException {

        return this.findByFieldNames(version, fieldsToMatch, startIndex, limit, null);
    }

    @Override
    public final ResultsWrapper<ContentDTO> findByFieldNames(final String version,
            final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
            final Integer startIndex, final Integer limit,
            @Nullable final Map<String, Constants.SortOrder> sortInstructions) throws ContentManagerException {
        return this.findByFieldNames(version, fieldsToMatch, startIndex, limit, sortInstructions, null);
    }

    @Override
    public final ResultsWrapper<ContentDTO> findByFieldNames(final String version,
            final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
            final Integer startIndex, final Integer limit,
            @Nullable final Map<String, Constants.SortOrder> sortInstructions,
            @Nullable final Map<String, AbstractFilterInstruction> filterInstructions) throws ContentManagerException {
        ResultsWrapper<ContentDTO> finalResults;

        final Map<String, Constants.SortOrder> newSortInstructions;
        if (null == sortInstructions || sortInstructions.isEmpty()) {
            newSortInstructions = Maps.newHashMap();
            newSortInstructions.put(Constants.TITLE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                    Constants.SortOrder.ASC);
        } else {
            newSortInstructions = sortInstructions;
        }

        // deal with unpublished filter if necessary
        Map<String, AbstractFilterInstruction> newFilterInstructions = filterInstructions;
        if (this.getUnpublishedFilter() != null) {
            if (null == newFilterInstructions) {
                newFilterInstructions = Maps.newHashMap();
            }
            newFilterInstructions.putAll(this.getUnpublishedFilter());
        }

        ResultsWrapper<String> searchHits = searchProvider.matchSearch(version, CONTENT_TYPE, fieldsToMatch,
                startIndex, limit, newSortInstructions, newFilterInstructions);

        // setup object mapper to use pre-configured deserializer module.
        // Required to deal with type polymorphism
        List<Content> result = mapper.mapFromStringListToContentList(searchHits.getResults());

        List<ContentDTO> contentDTOResults = mapper.getDTOByDOList(result);

        finalResults = new ResultsWrapper<>(contentDTOResults, searchHits.getTotalResults());

        return finalResults;
    }

    @Override
    public final ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(final String version,
            final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
            final Integer startIndex, final Integer limit) throws ContentManagerException {
        return this.findByFieldNamesRandomOrder(version, fieldsToMatch, startIndex, limit, null);
    }

    @Override
    public final ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(final String version,
            final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
            final Integer startIndex, final Integer limit, final Long randomSeed) throws ContentManagerException {
        ResultsWrapper<ContentDTO> finalResults;

        ResultsWrapper<String> searchHits;
        if (null == randomSeed) {
            searchHits = searchProvider.randomisedMatchSearch(version, CONTENT_TYPE, fieldsToMatch, startIndex, limit,
                    randomNumberGenerator.nextLong(), this.getUnpublishedFilter());
        } else {
            searchHits = searchProvider.randomisedMatchSearch(version, CONTENT_TYPE, fieldsToMatch, startIndex, limit,
                    randomSeed, this.getUnpublishedFilter());
        }

        // setup object mapper to use pre-configured deserializer module.
        // Required to deal with type polymorphism
        List<Content> result = mapper.mapFromStringListToContentList(searchHits.getResults());

        List<ContentDTO> contentDTOResults = mapper.getDTOByDOList(result);

        finalResults = new ResultsWrapper<>(contentDTOResults, searchHits.getTotalResults());

        return finalResults;
    }

    @Override
    public final ByteArrayOutputStream getFileBytes(final String version, final String filename) throws IOException {
        return database.getFileByCommitSHA(version, filename);
    }

    @Override
    public final List<String> listAvailableVersions() {

        List<String> result = new ArrayList<>();
        for (RevCommit rc : database.listCommits()) {
            result.add(rc.getName());
        }

        return result;
    }
    
    @Override
    public final boolean isValidVersion(final String version) {
        return !(null == version || version.isEmpty()) && this.database.verifyCommitExists(version);
    }

    @Override
    public final int compareTo(final String version1, final String version2) {
        Validate.notBlank(version1);
        Validate.notBlank(version2);

        int version1Epoch;
        try {
            version1Epoch = this.database.getCommitTime(version1);
        } catch (NotFoundException e) {
            version1Epoch = 0; // We didn't find it in the repo, so this commit is VERY old for all useful purposes.
        }

        int version2Epoch;
        try {
            version2Epoch = this.database.getCommitTime(version2);
        } catch (NotFoundException e) {
            version2Epoch = 0; // We didn't find it in the repo, so this commit is VERY old for all useful purposes.
        }

        return version1Epoch - version2Epoch;
    }

    @Override
    public final String getLatestVersionId() {
        return database.fetchLatestFromRemote();
    }

    @Override
    public final Set<String> getCachedVersionList() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (String index : this.searchProvider.getAllIndices()) {
            // check to see if index looks like a content sha otherwise we will get loads of other search indexes come
            // back.
            if (index.matches("[a-fA-F0-9]{40}")) {
                builder.add(index);
            }
        }
        return builder.build();
    }

    @Override
    public final Set<String> getTagsList(final String version) throws ContentManagerException {
        Validate.notBlank(version);

        List<Object> tagObjects = (List<Object>) searchProvider.getById(version, "metadata", "tags")
                .getSource().get("tags");

        return new HashSet<>(Lists.transform(tagObjects, Functions.toStringFunction()));
    }

    @Override
    public final Collection<String> getAllUnits(final String version) throws ContentManagerException {
        Validate.notBlank(version);

        SearchResponse r =  searchProvider.getAllByType(globalProperties.getProperty(Constants.CONTENT_INDEX), "unit");
        SearchHits hits = r.getHits();
        ArrayList<String> units = new ArrayList<>((int) hits.getTotalHits());
        for (SearchHit hit : hits) {
            units.add((String) hit.getSource().get("unit"));
        }

        return units;
    }

    @Override
    public void ensureCache(final String version) throws ContentManagerException {
        if (null == version) {
            throw new ContentVersionUnavailableException(
                    "You must specify a non-null version to make sure it is cached.");
        }

        if (!searchProvider.hasIndex(version)) {
            throw new ContentVersionUnavailableException(String.format("Version %s does not exist in the searchIndex.",
                    version));
        }
    }

    @Override
    public final Map<Content, List<String>> getProblemMap(final String version) {
        SearchResponse r = searchProvider.getAllByType(globalProperties.getProperty(Constants.CONTENT_INDEX),
                "contentError");

        SearchHits hits = r.getHits();
        Map<Content, List<String>> map = new HashMap<>();
        for (SearchHit hit : hits) {

            Content partialContentWithErrors = new Content();
            Map src = hit.getSource();
            partialContentWithErrors.setId((String) src.get("id"));
            partialContentWithErrors.setTitle((String) src.get("title"));
            //partialContentWithErrors.setTags(pair.getKey().getTags()); // TODO: Support tags
            partialContentWithErrors.setPublished((Boolean) src.get("published"));
            partialContentWithErrors.setCanonicalSourceFile((String) src.get("canonicalSourceFile"));

            ArrayList<String> errors = new ArrayList<>();
            for (Object v : (List) hit.getSource().get("errors")) {
                errors.add((String) v);
            }

            map.put(partialContentWithErrors, errors);
        }

        return map;
    }

    @Override
    public ContentDTO populateRelatedContent(final String version, final ContentDTO contentDTO)
            throws ContentManagerException {
        if (contentDTO.getChildren() != null) {
            for (ContentBaseDTO childBaseContentDTO : contentDTO.getChildren()) {
                if (childBaseContentDTO instanceof ContentDTO) {
                    this.populateRelatedContent(version, (ContentDTO) childBaseContentDTO);
                }
            }
        }
        if (contentDTO.getRelatedContent() == null || contentDTO.getRelatedContent().isEmpty()) {
            return contentDTO;
        }

        // build query the db to get full content information
        Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMap
            = new HashMap<>();

        List<String> relatedContentIds = Lists.newArrayList();
        for (ContentSummaryDTO summary : contentDTO.getRelatedContent()) {
            relatedContentIds.add(summary.getId());
        }

        fieldsToMap.put(
                Maps.immutableEntry(Constants.BooleanOperator.OR, Constants.ID_FIELDNAME + '.'
                        + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX), relatedContentIds);

        ResultsWrapper<ContentDTO> results = this.findByFieldNames(version, fieldsToMap, 0, relatedContentIds.size());

        List<ContentSummaryDTO> relatedContentDTOs = Lists.newArrayList();

        Map<String, ContentDTO> resultsMappedById = Maps.newHashMap();
        for (ContentDTO relatedContent : results.getResults()) {
            resultsMappedById.put(relatedContent.getId(), relatedContent);
        }
        // Iterate over relatedContentIds so that relatedContentDTOs maintain order defined in content not result order
        for (String contentId : relatedContentIds) {
            ContentDTO relatedContent = resultsMappedById.get(contentId);
            ContentSummaryDTO summary = this.mapper.getAutoMapper().map(relatedContent, ContentSummaryDTO.class);
            GitContentManager.generateDerivedSummaryValues(relatedContent, summary);
            relatedContentDTOs.add(summary);
        }

        contentDTO.setRelatedContent(relatedContentDTOs);

        return contentDTO;
    }

    @Override
    public ContentSummaryDTO extractContentSummary(final ContentDTO content) {
        if (null == content) {
            return null;
        }

        // try auto-mapping
        return mapper.getAutoMapper().map(content, ContentSummaryDTO.class);
    }

    @Override
    public String getCurrentContentSHA() {
        GetResponse r = searchProvider.getById(globalProperties.getProperty(Constants.CONTENT_INDEX), "metadata",
                "general");
        return (String) r.getSource().get("version");
    }

    /**
     * Helper to decide whether the published filter should be set.
     *
     * @return either null or a map setup with the published filter config.
     */
    private Map<String, AbstractFilterInstruction> getUnpublishedFilter() {
        if (this.allowOnlyPublishedContent) {
            return ImmutableMap.of("published", new SimpleFilterInstruction("true"));
        }
        return null;
    }

    /**
     * A method which adds information to the contentSummaryDTO, summary, from values evaluated from the content.
     * @param content the original content object which was used to create the summary.
     *                Its instance should not get altered from calling this method.
     * @param summary summary of the content.
     *                The values of this instance could be changed by this method.
     */
    private static void generateDerivedSummaryValues(final ContentDTO content, final ContentSummaryDTO summary) {
        List<String> questionPartIds = Lists.newArrayList();
        GitContentManager.collateQuestionPartIds(content, questionPartIds);
        summary.setQuestionPartIds(questionPartIds);
    }

    /**
     * Recursively walk through the content object and its children to populate the questionPartIds list with the IDs
     * of any content of type QuestionDTO.
     * @param content the content page and, on recursive invocations, its children.
     * @param questionPartIds a list to track the question part IDs in the content and its children.
     */
    private static void collateQuestionPartIds(final ContentDTO content, final List<String> questionPartIds) {
        if (content instanceof QuestionDTO) {
            questionPartIds.add(content.getId());
        }
        List<ContentBaseDTO> children = content.getChildren();
        if (children != null) {
            for (ContentBaseDTO child : children) {
                if (child instanceof ContentDTO) {
                    ContentDTO childContent = (ContentDTO) child;
                    collateQuestionPartIds(childContent, questionPartIds);
                }
            }
        }
    }
}
