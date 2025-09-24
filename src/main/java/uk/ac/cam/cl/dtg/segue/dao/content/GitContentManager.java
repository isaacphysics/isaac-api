/*
 * Copyright 2014 Stephen Cummins and Ian Davies
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
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SidebarDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.IsaacSearchInstructionBuilder;
import uk.ac.cam.cl.dtg.segue.search.IsaacSearchInstructionBuilder.Priority;
import uk.ac.cam.cl.dtg.segue.search.IsaacSearchInstructionBuilder.Strategy;
import uk.ac.cam.cl.dtg.segue.search.SearchInField;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.segue.search.SimpleExclusionInstruction;
import uk.ac.cam.cl.dtg.segue.search.SimpleFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.TermsFilterInstruction;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;
import uk.ac.cam.cl.dtg.util.mappers.ContentMapperMS;
import uk.ac.cam.cl.dtg.util.mappers.MainMapper;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics.CACHE_METRICS_COLLECTOR;

/**
 * Implementation that specifically works with Content objects.
 * 
 */
public class GitContentManager {
    private static final Logger log = LoggerFactory.getLogger(GitContentManager.class);

    private static final String CONTENT_TYPE = "content";

    private final GitDb database;
    private final ContentMapperMS mapper;
    private final ContentMapper mapperUtils;
    private final ISearchProvider searchProvider;
    private final AbstractConfigLoader globalProperties;
    private final boolean showOnlyPublishedContent;
    private final boolean hideRegressionTestContent;

    private final Cache<String, ResultsWrapper<Content>> contentDOcache;
    private final Cache<String, ResultsWrapper<ContentDTO>> contentDTOcache;
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
    public GitContentManager(final GitDb database, final ISearchProvider searchProvider, final ContentMapperMS mapper,
                             final ContentMapper mapperUtils, final AbstractConfigLoader globalProperties) {
        this.database = database;
        this.mapper = mapper;
        this.mapperUtils = mapperUtils;
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

        this.contentDOcache = CacheBuilder.newBuilder().recordStats().softValues().expireAfterAccess(1, TimeUnit.DAYS).build();
        this.contentDTOcache = CacheBuilder.newBuilder().recordStats().softValues().expireAfterAccess(1, TimeUnit.DAYS).build();
        CACHE_METRICS_COLLECTOR.addCache("git_content_manager_do_cache", contentDOcache);
        CACHE_METRICS_COLLECTOR.addCache("git_content_manager_dto_cache", contentDTOcache);

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
                             final ContentMapperMS contentMapper, ContentMapper mapperUtils) {
        this.database = database;
        this.mapper = contentMapper;
        this.searchProvider = searchProvider;
        this.mapperUtils = mapperUtils;
        this.globalProperties = null;
        this.showOnlyPublishedContent = false;
        this.hideRegressionTestContent = false;
        this.contentDOcache = CacheBuilder.newBuilder().recordStats().softValues().expireAfterAccess(1, TimeUnit.DAYS).build();
        this.contentDTOcache = CacheBuilder.newBuilder().recordStats().softValues().expireAfterAccess(1, TimeUnit.DAYS).build();
        this.contentShaCache = CacheBuilder.newBuilder().softValues().expireAfterWrite(1, TimeUnit.MINUTES).build();
        this.contentIndex = null;
    }

    /**
     *  Get a DTO object by its ID or return null.
     *
     *  The object will be retrieved in DO form, and mapped to a DTO.
     *  The DO version will be cached to avoid re-querying the data store, but the DTO
     *  will not be cached to avoid mutations poisoning the cache.
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
     *  The object will be retrieved in DO form, and mapped to a DTO.
     *  The DO version will be cached to avoid re-querying the data store, but the DTO
     *  will not be cached to avoid mutations poisoning the cache.
     *
     * @param id the content object ID.
     * @param failQuietly whether to log a warning if the content cannot be found.
     * @return the content DTO object.
     * @throws ContentManagerException on failure to return the object or null.
     */
    public final ContentDTO getContentById(final String id, final boolean failQuietly) throws ContentManagerException {
        return this.mapperUtils.getDTOByDO(this.getContentDOById(id, failQuietly));
    }

    /**
     * Get a DTO object from a DO object.
     *
     * This method merely wraps {@link ContentMapper#getDTOByDO(Content)}, and will trust the content of the DO.
     * Only use for DO objects obtained from {@link #getContentDOById(String)} when the DTO is also required,
     * to avoid the potential cache-miss and ElasticSearch round-trip of {@link #getContentById(String)}.
     *
     * @param content - the DO object to convert.
     * @return the DTO form of the object.
     */
    public final ContentDTO getContentDTOByDO(final Content content) {
        return this.mapperUtils.getDTOByDO(content);
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
        if (null == id || id.isEmpty()) {
            return null;
        }

        String k = "getContentDOById~" + getCurrentContentSHA() + "~" + id;

        try {
            ResultsWrapper<Content> result = contentDOcache.get(k, () -> {

                ResultsWrapper<String> rawResults = searchProvider.termSearch(
                        contentIndex,
                        CONTENT_TYPE, id,
                        Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, 0, 1,
                        getBaseFilters());
                List<Content> searchResults = mapperUtils.mapFromStringListToContentList(rawResults.getResults());

                return new ResultsWrapper<>(searchResults, rawResults.getTotalResults());
            });

            if (null == result.getResults() || result.getResults().isEmpty()) {
                if (!failQuietly) {
                    log.error("Failed to locate content with ID '{}' in the cache for content SHA ({})", id, getCurrentContentSHA());
                }
                return null;
            }

            return result.getResults().get(0);

        } catch (final ExecutionException e) {
            throw new ContentManagerException(e.getCause().getMessage());
        }
    }

    /**
     *  Get a list of DTO objects by their IDs.
     *
     *  This will always return cached objects, and temporarily caches the objects to avoid re-querying
     *  the data store and the deserialization costs.
     *  Do not modify the returned DTO objects!
     *
     * @param ids the list of content object IDs.
     * @param startIndex the integer start index for pagination.
     * @param limit the limit for pagination.
     * @return a ResultsWrapper of the matching content.
     * @throws ContentManagerException on failure to return the objects.
     */
    public ResultsWrapper<ContentDTO> getUnsafeCachedContentDTOsMatchingIds(final Collection<String> ids,
                                                                            final int startIndex, final int limit)
            throws ContentManagerException {

        String k = "getContentMatchingIds~" + getCurrentContentSHA() + "~" + ids.toString() + "~" + startIndex + "~" + limit;

        try {
            return contentDTOcache.get(k, () -> {

                Map<String, AbstractFilterInstruction> finalFilter = Maps.newHashMap();
                finalFilter.putAll(new ImmutableMap.Builder<String, AbstractFilterInstruction>()
                        .put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX,
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

                List<Content> searchResults = mapperUtils.mapFromStringListToContentList(searchHits.getResults());
                return new ResultsWrapper<>(mapperUtils.getDTOByDOList(searchResults), searchHits.getTotalResults());
            });
        } catch (final ExecutionException e) {
            throw new ContentManagerException(e.getCause().getMessage());
        }
    }

    /** Search the content for specified types that match a given user provided search string from a given index.
     * This effectively search the entire site for content that matches the provided string.
     *
     * @param searchString User provided search string
     * @param contentTypes The types of content to be returned
     * @param startIndex Index to start searching from
     * @param limit The number of questions to match
     * @param showNoFilterContent Whether nofilter content should be displayed
     * @return The search hits
     * @throws ContentManagerException The search may result in a content exception
     */
    public final ResultsWrapper<ContentDTO> siteWideSearch(
            @Nullable final String searchString,
            final Set<String> contentTypes, final Integer startIndex,
            final Integer limit, final boolean showNoFilterContent
    ) throws ContentManagerException {

        // Create a set of search terms from the initial search string by splitting on spaces.
        Set<String> searchTerms = new HashSet<>();
        if (searchString != null && !searchString.isBlank()) {
            // If it is a search phrase, also try to match each word individually
            searchTerms = Arrays.stream(searchString.split(" ")).collect(Collectors.toSet());
            searchTerms.add(searchString);
        }

        IsaacSearchInstructionBuilder searchInstructionBuilder = new IsaacSearchInstructionBuilder(
                searchProvider, this.showOnlyPublishedContent, this.hideRegressionTestContent, !showNoFilterContent)

                // Restrict content types
                .includeContentTypes(contentTypes)

                // Fuzzy search term matches
                .searchFor(new SearchInField(Constants.ID_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.FUZZY))
                .searchFor(new SearchInField(Constants.TITLE_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.FUZZY))
                .searchFor(new SearchInField(Constants.SUBTITLE_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.FUZZY))
                .searchFor(new SearchInField(Constants.SUMMARY_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.FUZZY))
                .searchFor(new SearchInField(Constants.TAGS_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.FUZZY))
                .searchFor(new SearchInField(Constants.PRIORITISED_SEARCHABLE_CONTENT_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.FUZZY))
                .searchFor(new SearchInField(Constants.SEARCHABLE_CONTENT_FIELDNAME, searchTerms)
                        .strategy(Strategy.FUZZY))

                // Event specific queries
                .searchFor(new SearchInField(Constants.ADDRESS_PSEUDO_FIELDNAME, searchTerms))
                .includePastEvents(false);

        // If no search terms were provided, sort by ascending alphabetical order of title.
        Map<String, Constants.SortOrder> sortOrder = null;
        if (searchTerms.isEmpty()) {
            sortOrder = new HashMap<>();
            sortOrder.put(
                    Constants.TITLE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                    Constants.SortOrder.ASC
            );
        }

        ResultsWrapper<String> searchHits = searchProvider.nestedMatchSearch(
                contentIndex,
                CONTENT_TYPE,
                startIndex,
                limit,
                searchInstructionBuilder.build(),
                null,
                sortOrder
        );

        List<Content> searchResults = mapperUtils.mapFromStringListToContentList(searchHits.getResults());

        return new ResultsWrapper<>(mapperUtils.getDTOByDOList(searchResults), searchHits.getTotalResults());
    }

    /** Search the content for questions (and fasttrack questions) that match a given user provided search string and
     * filter values starting from a given index.
     *
     * @param searchString User provided search string
     * @param filterFieldNamesToValues Map of filters to a set of values to match
     * @param fasttrack Whether fasttrack questions should be searched for
     * @param startIndex Index to start searching from
     * @param limit The number of questions to match
     * @param showNoFilterContent Whether nofilter content should be displayed
     * @return The search hits
     * @throws ContentManagerException The search may result in a content exception
     */
    public final ResultsWrapper<ContentDTO> questionSearch(
            @Nullable final String searchString,
            @Nullable final Long randomSeed,
            final Map<String, Set<String>> filterFieldNamesToValues,
            final Integer startIndex, final Integer limit,
            final boolean fasttrack, final boolean showNoFilterContent, final boolean showSupersededContent
    ) throws ContentManagerException {

        // Set question type (content type) based on fasttrack status
        Set<String> contentTypes = new HashSet<>();
        if (fasttrack) {
            contentTypes.add(FAST_TRACK_QUESTION_TYPE);
        } else {
            contentTypes.add(QUESTION_TYPE);
        }

        // Create a set of search terms from the initial search string by splitting on spaces.
        Set<String> searchTerms = new HashSet<>();
        if (searchString != null && !searchString.isBlank()) {
            // If it is a search phrase, also try to match each word individually
            searchTerms = Arrays.stream(searchString.split(" ")).collect(Collectors.toSet());
            searchTerms.add(searchString);
        }

        IsaacSearchInstructionBuilder searchInstructionBuilder = new IsaacSearchInstructionBuilder(
                searchProvider, this.showOnlyPublishedContent, this.hideRegressionTestContent, !showNoFilterContent)

                // Filter superseded questions if necessary:
                .excludeSupersededContent(!showSupersededContent)

                // Restrict content types
                .includeContentTypes(contentTypes)

                // Search term matches
                .searchFor(new SearchInField(Constants.ID_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.SIMPLE))
                .searchFor(new SearchInField(Constants.TITLE_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.SUBSTRING))
                .searchFor(new SearchInField(Constants.SUBTITLE_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.SUBSTRING))
                .searchFor(new SearchInField(Constants.SUMMARY_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.SUBSTRING))
                .searchFor(new SearchInField(Constants.TAGS_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.SUBSTRING))
                .searchFor(new SearchInField(Constants.PRIORITISED_SEARCHABLE_CONTENT_FIELDNAME, searchTerms)
                        .priority(Priority.HIGH).strategy(Strategy.SUBSTRING))
                .searchFor(new SearchInField(Constants.SEARCHABLE_CONTENT_FIELDNAME, searchTerms)
                        .strategy(Strategy.SUBSTRING));

        // FIXME: Make this and PageFacade agnostic
        // It doesn't need to know about books, just have required tags
        // It doesn't need to know about subject, field or topic, just have atLeastOne tags
        // Add a required filtering rule for each field that has a value
        for (Map.Entry<String, Set<String>> entry : filterFieldNamesToValues.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                if (Arrays.asList(SUBJECTS_FIELDNAME, FIELDS_FIELDNAME, TOPICS_FIELDNAME, CATEGORIES_FIELDNAME, TAGS_FIELDNAME, BOOKS_FIELDNAME)
                        .contains(entry.getKey())) {
                    searchInstructionBuilder.searchFor(new SearchInField(TAGS_FIELDNAME, entry.getValue())
                            .strategy(Strategy.SIMPLE)
                            .atLeastOne(true));
                } else {
                    boolean applyOrFilterBetweenValues = ID_FIELDNAME.equals(entry.getKey());
                    searchInstructionBuilder.searchFor(new SearchInField(entry.getKey(), entry.getValue())
                            .strategy(Strategy.SIMPLE)
                            .required(!applyOrFilterBetweenValues));
                }
            }
        }

        // If no search terms or random seed, sort by ascending alphabetical order of title.
        Map<String, Constants.SortOrder> sortOrder = null;
        if (searchTerms.isEmpty() && null == randomSeed) {
            sortOrder = new HashMap<>();
            sortOrder.put(
                    Constants.TITLE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                    Constants.SortOrder.ASC
            );
        }

        ResultsWrapper<String> searchHits = searchProvider.nestedMatchSearch(
                contentIndex,
                CONTENT_TYPE,
                startIndex,
                limit,
                searchInstructionBuilder.build(),
                randomSeed,
                sortOrder
        );

        List<Content> searchResults = mapperUtils.mapFromStringListToContentList(searchHits.getResults());

        return new ResultsWrapper<>(mapperUtils.getDTOByDOList(searchResults), searchHits.getTotalResults());
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
        List<Content> result = mapperUtils.mapFromStringListToContentList(searchHits.getResults());

        List<ContentDTO> contentDTOResults = mapperUtils.getDTOByDOList(result);

        finalResults = new ResultsWrapper<>(contentDTOResults, searchHits.getTotalResults());

        return finalResults;
    }

    @Deprecated
    public final ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(
            final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex,
            final Integer limit
    ) throws ContentManagerException {
        return this.findByFieldNamesRandomOrder(fieldsToMatch, startIndex, limit, null);
    }

    @Deprecated
    public final ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(
            final List<BooleanSearchClause> fieldsToMatch, final Integer startIndex,
            final Integer limit, @Nullable final Long randomSeed
    ) throws ContentManagerException {
        ResultsWrapper<ContentDTO> finalResults;

        ResultsWrapper<String> searchHits;
        searchHits = searchProvider.randomisedMatchSearch(
                contentIndex, CONTENT_TYPE, fieldsToMatch, startIndex, limit, randomSeed, this.getBaseFilters());

        // setup object mapper to use pre-configured deserializer module.
        // Required to deal with type polymorphism
        List<Content> result = mapperUtils.mapFromStringListToContentList(searchHits.getResults());

        List<ContentDTO> contentDTOResults = mapperUtils.getDTOByDOList(result);

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
            return tagObjects.stream().map(Functions.toStringFunction()).collect(Collectors.toSet());
        } catch (SegueSearchException e) {
            log.error("Failed to retrieve tags from search provider", e);
            return Sets.newHashSet();
        }
    }

    public final Collection<String> getAllUnits() {
        String unitType = Constants.CONTENT_INDEX_TYPE.UNIT.toString();
        if (globalProperties.getProperty(Constants.SEGUE_APP_ENVIRONMENT)
                .equals(Constants.EnvironmentType.PROD.name())) {
            unitType = Constants.CONTENT_INDEX_TYPE.PUBLISHED_UNIT.toString();
        }
        try {
            SearchResponse r = searchProvider.getAllFromIndex(
                    globalProperties.getProperty(Constants.CONTENT_INDEX), unitType);
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
                Map<String, Object> src = hit.getSourceAsMap();
                partialContentWithErrors.setId((String) src.get("id"));
                partialContentWithErrors.setTitle((String) src.get("title"));
                //partialContentWithErrors.setTags(pair.getKey().getTags()); // TODO: Support tags
                partialContentWithErrors.setPublished((Boolean) src.get("published"));
                partialContentWithErrors.setCanonicalSourceFile((String) src.get("canonicalSourceFile"));

                ArrayList<String> errors = new ArrayList<>();
                for (Object v : (List<?>) hit.getSourceAsMap().get("errors")) {
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
                ContentSummaryDTO summary = this.mapper.map(relatedContent, ContentSummaryDTO.class);
                GitContentManager.generateDerivedSummaryValues(relatedContent, summary);
                relatedContentDTOs.add(summary);
            } else {
                log.error("Related content with ID '" + contentId + "' not returned by elasticsearch query");
            }
        }

        contentDTO.setRelatedContent(relatedContentDTOs);

        return contentDTO;
    }

    public static ContentSummaryDTO populateContentSummaryValues(ContentDTO content, ContentSummaryDTO summary) {
        generateDerivedSummaryValues(content, summary);
        return summary;
    }

    /**
     * Replace a placeholder sidebar object with an augmented sidebar.
     *
     * Augmentation will not happen if a sidebar with the right ID cannot be found.
     *
     * @param seguePageDTO the page to augment.
     * @throws ContentManagerException if loading the sidebar errors.
     */
    public void populateSidebar(final SeguePageDTO seguePageDTO) throws ContentManagerException {
        if (null != seguePageDTO.getSidebar()) {
            ContentDTO potentialSidebar = getContentById(seguePageDTO.getSidebar().getId(), true);
            if (potentialSidebar instanceof SidebarDTO) {
                seguePageDTO.setSidebar((SidebarDTO) potentialSidebar);
            }
        }
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
            filters.put("tags", new SimpleExclusionInstruction(REGRESSION_TEST_TAG));
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
        List<QuestionDTO> questionParts = GameManager.getAllMarkableQuestionPartsDFSOrder(content);
        List<String> questionPartIds = questionParts.stream().map(QuestionDTO::getId).collect(Collectors.toList());
        summary.setQuestionPartIds(questionPartIds);
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

        public BooleanSearchClause(final String field,
                                   final Constants.BooleanOperator operator,
                                   final List<String> values) {
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
