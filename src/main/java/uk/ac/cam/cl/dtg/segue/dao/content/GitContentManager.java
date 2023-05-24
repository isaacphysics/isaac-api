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

import com.google.api.client.util.Sets;
import com.google.common.base.Functions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.BooleanInstruction;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.IsaacSearchInstructionBuilder;
import uk.ac.cam.cl.dtg.segue.search.IsaacSearchInstructionBuilder.Priority;
import uk.ac.cam.cl.dtg.segue.search.IsaacSearchInstructionBuilder.Strategy;
import uk.ac.cam.cl.dtg.segue.search.SearchInField;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.segue.search.SimpleExclusionInstruction;
import uk.ac.cam.cl.dtg.segue.search.SimpleFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.TermsFilterInstruction;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import jakarta.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics.CACHE_METRICS_COLLECTOR;

/**
 * Implementation that specifically works with Content objects.
 * 
 */
public class GitContentManager {
    private static final Logger log = LoggerFactory.getLogger(GitContentManager.class);

    private static final String CONTENT_TYPE = "content";

    private final GitDb database;
    private final ContentMapper mapper;
    private final ISearchProvider searchProvider;
    private final PropertiesLoader globalProperties;
    private final boolean showOnlyPublishedContent;
    private final boolean hideRegressionTestContent;

    private final Cache<Object, Object> cache;
    private final Cache<String, GetResponse> contentShaCache;

    private final String contentIndex;


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

        this.showOnlyPublishedContent = Boolean.parseBoolean(
                globalProperties.getProperty(Constants.SHOW_ONLY_PUBLISHED_CONTENT));
        if (this.showOnlyPublishedContent) {
            log.info("API Configured to only allow published content to be returned.");
        }

        this.hideRegressionTestContent = Boolean.parseBoolean(
                globalProperties.getProperty(Constants.HIDE_REGRESSION_TEST_CONTENT));
        if (this.hideRegressionTestContent) {
            log.info("API Configured to hide content tagged with 'regression_test'.");
        }

        this.cache = CacheBuilder.newBuilder().recordStats().softValues().expireAfterAccess(1, TimeUnit.DAYS).build();
        CACHE_METRICS_COLLECTOR.addCache("git_content_manager_cache", cache);

        this.contentShaCache = CacheBuilder.newBuilder().softValues().expireAfterWrite(5, TimeUnit.SECONDS).build();

        this.contentIndex = globalProperties.getProperty(Constants.CONTENT_INDEX);
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
        this.showOnlyPublishedContent = false;
        this.hideRegressionTestContent = false;
        this.cache = CacheBuilder.newBuilder().softValues().expireAfterAccess(1, TimeUnit.DAYS).build();
        this.contentShaCache = CacheBuilder.newBuilder().softValues().expireAfterWrite(1, TimeUnit.MINUTES).build();
        this.contentIndex = null;
    }

    /**
     *  Get a DTO object by its ID or return null.
     *
     *  This may return a cached object, and will temporarily cache the object.
     *  Do not modify the returned DTO object.
     *  The object will be retrieved in DO form, and mapped to a DTO. Both versions will be
     *  locally cached to avoid re-querying the data store and the deserialization costs.
     *
     * @param id the content object ID.
     * @return the content DTO object.
     * @throws ContentManagerException on failure to return the object or null.
     */
    public final ContentDTO getContentById(final String id) throws ContentManagerException {
        return getContentById(id, false);
    }

    /**
     *  Get a DTO object by its ID or return null.
     *
     *  This may return a cached object, and will temporarily cache the object.
     *  Do not modify the returned DTO object.
     *  The object will be retrieved in DO form, and mapped to a DTO. Both versions will be
     *  locally cached to avoid re-querying the data store and the deserialization costs.
     *
     * @param id the content object ID.
     * @param failQuietly whether to log a warning if the content cannot be found.
     * @return the content DTO object.
     * @throws ContentManagerException on failure to return the object or null.
     */
    public final ContentDTO getContentById(final String id, final boolean failQuietly) throws ContentManagerException {
        String k = "getContentById~" + getCurrentContentSHA() + "~" + id;
        if (!cache.asMap().containsKey(k)) {
            ContentDTO c = this.mapper.getDTOByDO(this.getContentDOById(id, failQuietly));
            if (c != null) {
                cache.put(k, c);
            }
        }

        return (ContentDTO) cache.getIfPresent(k);
    }

    /**
     *  Get a DO object by its ID or return null.
     *
     *  This may return a cached object, and will temporarily cache the object
     *  to avoid re-querying the data store and the deserialization costs.
     *  Do not modify the returned DO object.
     *
     * @param id the content object ID.
     * @return the content DTO object.
     * @throws ContentManagerException on failure to return the object or null.
     */
    public final Content getContentDOById(final String id) throws ContentManagerException {
        return getContentDOById(id, false);
    }

    /**
     *  Get a DO object by its ID or return null.
     *
     *  This may return a cached object, and will temporarily cache the object
     *  to avoid re-querying the data store and the deserialization costs.
     *  Do not modify the returned DO object.
     *
     * @param id the content object ID.
     * @param failQuietly whether to log a warning if the content cannot be found.
     * @return the content DTO object.
     * @throws ContentManagerException on failure to return the object or null.
     */
    public final Content getContentDOById(final String id, final boolean failQuietly) throws ContentManagerException {
        if (null == id || id.equals("")) {
            return null;
        }

        String k = "getContentDOById~" + getCurrentContentSHA() + "~" + id;
        if (!cache.asMap().containsKey(k)) {

            List<Content> searchResults = mapper.mapFromStringListToContentList(this.searchProvider.termSearch(
                    contentIndex,
                    CONTENT_TYPE, id,
                    Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, 0, 1,
                    this.getBaseFilters()).getResults()
            );

            if (null == searchResults || searchResults.isEmpty()) {
                if (!failQuietly) {
                    log.error(String.format("Failed to locate content with ID '%s' in the cache for content SHA (%s)", id, getCurrentContentSHA()));
                }
                return null;
            }

            cache.put(k, searchResults.get(0));
        }

        return (Content) cache.getIfPresent(k);

    }

    /**
     *  Retrieve all DTO content matching an ID prefix.
     *
     *  This may return cached objects, and will temporarily cache the objects
     *  to avoid re-querying the data store and the deserialization costs.
     *  Do not modify the returned DTO objects.
     *
     * @param idPrefix the content object ID prefix.
     * @param startIndex the integer start index for pagination.
     * @param limit the limit for pagination.
     * @return a ResultsWrapper of the matching content.
     * @throws ContentManagerException on failure to return the objects.
     */
    public ResultsWrapper<ContentDTO> getByIdPrefix(final String idPrefix, final int startIndex,
                                                    final int limit) throws ContentManagerException {

        String k = "getByIdPrefix~" + getCurrentContentSHA() + "~" + idPrefix + "~" + startIndex + "~" + limit;
        if (!cache.asMap().containsKey(k)) {

            ResultsWrapper<String> searchHits = this.searchProvider.findByPrefix(contentIndex, CONTENT_TYPE,
                    Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                    idPrefix, startIndex, limit, this.getBaseFilters());

            List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());

            cache.put(k, new ResultsWrapper<>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults()));
        }

        return (ResultsWrapper<ContentDTO>) cache.getIfPresent(k);
    }

    /**
     *  Get a list of DTO objects by their IDs.
     *
     *  This may return cached objects, and will temporarily cache the objects
     *  to avoid re-querying the data store and the deserialization costs.
     *  Do not modify the returned DTO objects.
     *
     * @param ids the list of content object IDs.
     * @param startIndex the integer start index for pagination.
     * @param limit the limit for pagination.
     * @return a ResultsWrapper of the matching content.
     * @throws ContentManagerException on failure to return the objects.
     */
    public ResultsWrapper<ContentDTO> getContentMatchingIds(final Collection<String> ids,
                                                            final int startIndex, final int limit)
            throws ContentManagerException {

        String k = "getContentMatchingIds~" + getCurrentContentSHA() + "~" + ids.toString() + "~" + startIndex + "~" + limit;
        if (!cache.asMap().containsKey(k)) {

            Map<String, AbstractFilterInstruction> finalFilter = Maps.newHashMap();
            finalFilter.putAll(new ImmutableMap.Builder<String, AbstractFilterInstruction>()
                                .put(Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                                    new TermsFilterInstruction(ids))
                                .build());

            if (getBaseFilters() != null) {
                finalFilter.putAll(getBaseFilters());
            }

            ResultsWrapper<String> searchHits = this.searchProvider.termSearch(
                    contentIndex,
                    CONTENT_TYPE,
                    null,
                    null,
                    startIndex,
                    limit,
                    finalFilter
            );

            List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());
            cache.put(k, new ResultsWrapper<>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults()));
        }

        return (ResultsWrapper<ContentDTO>) cache.getIfPresent(k);
    }

    public final ResultsWrapper<ContentDTO> searchForContent(@Nullable final String searchString, final Set<String> ids,
                                                             final Set<String> tags, final Set<String> levels, final Set<String> stages,
                                                             final Set<String> difficulties, final Set<String> examBoards,
                                                             final Set<String> contentTypes, final Integer startIndex,
                                                             final Integer limit, final boolean showNoFilterContent)
            throws ContentManagerException {

        Set<String> searchTerms = Set.of();
        if (searchString != null && !searchString.isBlank()) {
            searchTerms = Arrays.stream(searchString.split(" ")).collect(Collectors.toSet());
        }

        BooleanInstruction matchInstruction = new IsaacSearchInstructionBuilder(searchProvider,
                this.showOnlyPublishedContent,
                this.hideRegressionTestContent,
                !showNoFilterContent)
                .includeContentTypes(contentTypes)
                .searchFor(new SearchInField(Constants.ID_FIELDNAME, ids).strategy(Strategy.SIMPLE))
                .searchFor(new SearchInField(Constants.TAGS_FIELDNAME, tags).strategy(Strategy.SIMPLE).required(true))
                .searchFor(new SearchInField(Constants.LEVEL_FIELDNAME, levels).strategy(Strategy.SIMPLE).required(true))
                .searchFor(new SearchInField(Constants.STAGE_FIELDNAME, stages).strategy(Strategy.SIMPLE).required(true))
                .searchFor(new SearchInField(Constants.DIFFICULTY_FIELDNAME, difficulties).strategy(Strategy.SIMPLE).required(true))
                .searchFor(new SearchInField(Constants.EXAM_BOARD_FIELDNAME, examBoards).strategy(Strategy.SIMPLE).required(true))
                .searchFor(new SearchInField(Constants.ID_FIELDNAME, searchTerms).priority(Priority.HIGH).strategy(Strategy.FUZZY))
                .searchFor(new SearchInField(Constants.TITLE_FIELDNAME, searchTerms).priority(Priority.HIGH).strategy(Strategy.FUZZY))
                .searchFor(new SearchInField(Constants.TAGS_FIELDNAME, searchTerms).priority(Priority.HIGH).strategy(Strategy.FUZZY))
                .searchFor(new SearchInField(Constants.VALUE_FIELDNAME, searchTerms).strategy(Strategy.FUZZY))
                .searchFor(new SearchInField(Constants.CHILDREN_FIELDNAME, searchTerms).strategy(Strategy.FUZZY))
                .build();

        // If no search terms were provided, sort by ascending alphabetical order of title.
        Map<String, Constants.SortOrder> sortOrder = null;

        if (searchTerms.isEmpty()) {
            sortOrder = new HashMap<>();
            sortOrder.put(Constants.TITLE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, Constants.SortOrder.ASC);
        }

        ResultsWrapper<String> searchHits = searchProvider.nestedMatchSearch(
                contentIndex,
                CONTENT_TYPE,
                startIndex,
                limit,
                matchInstruction,
                sortOrder
        );

        List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());

        return new ResultsWrapper<>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults());
    }

    public final ResultsWrapper<ContentDTO> siteWideSearch(
            final String searchString, final List<String> documentTypes,
            final boolean showNoFilterContent, final Integer startIndex, final Integer limit
    ) throws  ContentManagerException {

        BooleanInstruction matchInstruction = new IsaacSearchInstructionBuilder(searchProvider,
                this.showOnlyPublishedContent,
                this.hideRegressionTestContent,
                !showNoFilterContent)
                .includeContentTypes(Set.copyOf(documentTypes))
                .includeContentTypes(Set.of(TOPIC_SUMMARY_PAGE_TYPE), Priority.HIGH)
                .searchFor(new SearchInField(Constants.SEARCHABLE_CONTENT_FIELDNAME, Set.of(searchString)))
                .searchFor(new SearchInField(Constants.ADDRESS_PSEUDO_FIELDNAME, Set.of(searchString)))
                .searchFor(new SearchInField(Constants.TITLE_FIELDNAME, Set.of(searchString)).priority(Priority.HIGH))
                .searchFor(new SearchInField(Constants.ID_FIELDNAME, Set.of(searchString)).priority(Priority.HIGH))
                .searchFor(new SearchInField(Constants.SUMMARY_FIELDNAME, Set.of(searchString)).priority(Priority.HIGH))
                .searchFor(new SearchInField(Constants.TAGS_FIELDNAME, Set.of(searchString)).priority(Priority.HIGH))
                .includePastEvents(false)
                .build();

        ResultsWrapper<String> searchHits = searchProvider.nestedMatchSearch(
                contentIndex,
                CONTENT_TYPE,
                startIndex,
                limit,
                matchInstruction,
                null
        );

        List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());

        return new ResultsWrapper<>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults());
    }

    public final ResultsWrapper<ContentDTO> findByFieldNames(
            final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex, final Integer limit
    ) throws ContentManagerException {
        return this.findByFieldNames(fieldsToMatch, startIndex, limit, null);
    }

    public final ResultsWrapper<ContentDTO> findByFieldNames(
            final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex,
            final Integer limit, @Nullable final Map<String, Constants.SortOrder> sortInstructions
    ) throws ContentManagerException {
        return this.findByFieldNames(fieldsToMatch, startIndex, limit, sortInstructions, null);
    }

    public final ResultsWrapper<ContentDTO> findByFieldNames(
            final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex, final Integer limit,
            @Nullable final Map<String, Constants.SortOrder> sortInstructions,
            @Nullable final Map<String, AbstractFilterInstruction> filterInstructions
    ) throws ContentManagerException {
        ResultsWrapper<ContentDTO> finalResults;

        final Map<String, Constants.SortOrder> newSortInstructions;
        if (null == sortInstructions || sortInstructions.isEmpty()) {
            newSortInstructions = Maps.newHashMap();
            newSortInstructions.put(Constants.TITLE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                    Constants.SortOrder.ASC);
        } else {
            newSortInstructions = sortInstructions;
        }

        // add base filters to filter instructions
        Map<String, AbstractFilterInstruction> newFilterInstructions = filterInstructions;
        if (this.getBaseFilters() != null) {
            if (null == newFilterInstructions) {
                newFilterInstructions = Maps.newHashMap();
            }
            newFilterInstructions.putAll(this.getBaseFilters());
        }

        ResultsWrapper<String> searchHits = searchProvider.matchSearch(contentIndex, CONTENT_TYPE, fieldsToMatch,
                startIndex, limit, newSortInstructions, newFilterInstructions);

        // setup object mapper to use pre-configured deserializer module.
        // Required to deal with type polymorphism
        List<Content> result = mapper.mapFromStringListToContentList(searchHits.getResults());

        List<ContentDTO> contentDTOResults = mapper.getDTOByDOList(result);

        finalResults = new ResultsWrapper<>(contentDTOResults, searchHits.getTotalResults());

        return finalResults;
    }

    public final ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(
            final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex,
            final Integer limit
    ) throws ContentManagerException {
        return this.findByFieldNamesRandomOrder(fieldsToMatch, startIndex, limit, null);
    }

    public final ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(
            final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex,
            final Integer limit, @Nullable final Long randomSeed
    ) throws ContentManagerException {
        ResultsWrapper<ContentDTO> finalResults;

        ResultsWrapper<String> searchHits;
        searchHits = searchProvider.randomisedMatchSearch(contentIndex, CONTENT_TYPE, fieldsToMatch, startIndex, limit, randomSeed,
                this.getBaseFilters());

        // setup object mapper to use pre-configured deserializer module.
        // Required to deal with type polymorphism
        List<Content> result = mapper.mapFromStringListToContentList(searchHits.getResults());

        List<ContentDTO> contentDTOResults = mapper.getDTOByDOList(result);

        finalResults = new ResultsWrapper<>(contentDTOResults, searchHits.getTotalResults());

        return finalResults;
    }

    public final ByteArrayOutputStream getFileBytes(final String filename) throws IOException {
        return database.getFileByCommitSHA(getCurrentContentSHA(), filename);
    }

    public final String getLatestContentSHA() {
        return database.fetchLatestFromRemote();
    }

    public final Set<String> getCachedContentSHAList() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (String index : this.searchProvider.getAllIndices()) {
            // check to see if index looks like a content sha otherwise we will get loads of other search indexes come
            // back.
            if (index.matches("[a-fA-F0-9]{40}_.*")) {
                // We just want the commit SHA, not the type description after the underscore:
                builder.add(index.replaceAll("_.*$", ""));
            }
        }
        return builder.build();
    }

    public final Set<String> getTagsList() {
        try {
            List<Object> tagObjects = (List<Object>) searchProvider.getById(
                    contentIndex,
                    Constants.CONTENT_INDEX_TYPE.METADATA.toString(),
                    "tags"
            ).getSource().get("tags");
            return new HashSet<>(Lists.transform(tagObjects, Functions.toStringFunction()));
        } catch (SegueSearchException e) {
            log.error("Failed to retrieve tags from search provider", e);
            return Sets.newHashSet();
        }
    }

    public final Collection<String> getAllUnits() {
        String unitType = Constants.CONTENT_INDEX_TYPE.UNIT.toString();
        if (globalProperties.getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(Constants.EnvironmentType.PROD.name())) {
            unitType = Constants.CONTENT_INDEX_TYPE.PUBLISHED_UNIT.toString();
        }
        try {
            SearchResponse r = searchProvider.getAllFromIndex(globalProperties.getProperty(Constants.CONTENT_INDEX), unitType);
            SearchHits hits = r.getHits();
            ArrayList<String> units = new ArrayList<>((int) hits.getTotalHits().value);
            for (SearchHit hit : hits) {
                units.add((String) hit.getSourceAsMap().get("unit"));
            }

            return units;
        } catch (SegueSearchException e) {
            log.error("Failed to retrieve all units from search provider", e);
            return Collections.emptyList();
        }
    }

    public final Map<Content, List<String>> getProblemMap() {
        try {
            SearchResponse r = searchProvider.getAllFromIndex(contentIndex,
                    Constants.CONTENT_INDEX_TYPE.CONTENT_ERROR.toString());
            SearchHits hits = r.getHits();
            Map<Content, List<String>> map = new HashMap<>();

            for (SearchHit hit : hits) {
                Content partialContentWithErrors = new Content();
                Map src = hit.getSourceAsMap();
                partialContentWithErrors.setId((String) src.get("id"));
                partialContentWithErrors.setTitle((String) src.get("title"));
                //partialContentWithErrors.setTags(pair.getKey().getTags()); // TODO: Support tags
                partialContentWithErrors.setPublished((Boolean) src.get("published"));
                partialContentWithErrors.setCanonicalSourceFile((String) src.get("canonicalSourceFile"));

                ArrayList<String> errors = new ArrayList<>();
                for (Object v : (List) hit.getSourceAsMap().get("errors")) {
                    errors.add((String) v);
                }

                map.put(partialContentWithErrors, errors);
            }
            return map;

        } catch (SegueSearchException e) {
            log.error("Failed to retrieve problem map from search provider", e);
            return Maps.newHashMap();
        }
    }

    public ContentDTO populateRelatedContent(final ContentDTO contentDTO)
            throws ContentManagerException {
        if (contentDTO.getChildren() != null) {
            for (ContentBaseDTO childBaseContentDTO : contentDTO.getChildren()) {
                if (childBaseContentDTO instanceof ContentDTO) {
                    this.populateRelatedContent((ContentDTO) childBaseContentDTO);
                }
            }
        }
        if (contentDTO.getRelatedContent() == null || contentDTO.getRelatedContent().isEmpty()) {
            return contentDTO;
        }

        // build query the db to get full content information
        List<BooleanSearchClause> fieldsToMap = Lists.newArrayList();

        List<String> relatedContentIds = Lists.newArrayList();
        for (ContentSummaryDTO summary : contentDTO.getRelatedContent()) {
            relatedContentIds.add(summary.getId());
        }

        fieldsToMap.add(new BooleanSearchClause(
                Constants.ID_FIELDNAME + '.' + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                Constants.BooleanOperator.OR, relatedContentIds));

        ResultsWrapper<ContentDTO> results = this.findByFieldNames(fieldsToMap, 0, relatedContentIds.size());

        List<ContentSummaryDTO> relatedContentDTOs = Lists.newArrayList();

        Map<String, ContentDTO> resultsMappedById = Maps.newHashMap();
        for (ContentDTO relatedContent : results.getResults()) {
            resultsMappedById.put(relatedContent.getId(), relatedContent);
        }
        // Iterate over relatedContentIds so that relatedContentDTOs maintain order defined in content not result order
        for (String contentId : relatedContentIds) {
            ContentDTO relatedContent = resultsMappedById.get(contentId);
            if (relatedContent != null) {
                ContentSummaryDTO summary = this.mapper.getAutoMapper().map(relatedContent, ContentSummaryDTO.class);
                GitContentManager.generateDerivedSummaryValues(relatedContent, summary);
                relatedContentDTOs.add(summary);
            } else {
                log.error("Related content with ID '" + contentId + "' not returned by elasticsearch query");
            }
        }

        contentDTO.setRelatedContent(relatedContentDTOs);

        return contentDTO;
    }

    public String getCurrentContentSHA() {
        GetResponse shaResponse = contentShaCache.getIfPresent(contentIndex);
        try {
            if (null == shaResponse) {
                shaResponse =
                        searchProvider.getById(
                                contentIndex,
                                Constants.CONTENT_INDEX_TYPE.METADATA.toString(),
                                "general"
                        );
                contentShaCache.put(contentIndex, shaResponse);
            }
            return (String) shaResponse.getSource().get("version");
        } catch (SegueSearchException e) {
            log.error("Failed to retrieve current content SHA from search provider", e);
            return "unknown";
        }
    }

    /**
     * Returns the basic filter configuration.
     *
     * @return either null or a map setup with filter/exclusion instructions, based on environment properties.
     */
    private Map<String, AbstractFilterInstruction> getBaseFilters() {
        if (!this.hideRegressionTestContent && !this.showOnlyPublishedContent) {
            return null;
        }

        HashMap<String, AbstractFilterInstruction> filters = new HashMap<>();

        if (this.hideRegressionTestContent) {
            filters.put("tags", new SimpleExclusionInstruction("regression_test"));
        }
        if (this.showOnlyPublishedContent) {
            filters.put("published", new SimpleFilterInstruction("true"));
        }
        return ImmutableMap.copyOf(filters);
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

    /**
     * An abstract representation of a search clause that can be interpreted as desired by different search providers.
     *
     * @deprecated in favour of {@code BooleanMatchInstruction}, as an attempt to unify approaches to searching.
     */
    @Deprecated
    public static class BooleanSearchClause {
        private final String field;
        private final Constants.BooleanOperator operator;
        private final List<String> values;
        public BooleanSearchClause(final String field, final Constants.BooleanOperator operator, final List<String> values) {
            this.field = field;
            this.operator = operator;
            this.values = values;
        }
        public String getField() {
            return this.field;
        }
        public Constants.BooleanOperator getOperator() {
            return this.operator;
        }
        public List<String> getValues() {
            return this.values;
        }
    }
}
