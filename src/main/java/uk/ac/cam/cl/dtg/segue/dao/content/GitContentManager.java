/**
 * Copyright 2014 Stephen Cummins
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Sets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.elasticsearch.common.collect.ImmutableSet;
import org.elasticsearch.common.collect.ImmutableSet.Builder;
import org.elasticsearch.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacEventPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicChemistryQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dos.content.*;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchOperationException;

import javax.annotation.Nullable;
import javax.ws.rs.NotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.collect.Maps.immutableEntry;

/**
 * Implementation that specifically works with Content objects.
 * 
 */
public class GitContentManager implements IContentManager {
    private static final Logger log = LoggerFactory.getLogger(GitContentManager.class);

    private static final String CONTENT_TYPE = "content";
    private static final int MAX_NUMERIC_QUESTION_UNIT_COUNT = 6;

    private final Map<String, Map<Content, List<String>>> indexProblemCache;
    private final Map<String, Set<String>> tagsList;
    private final Map<String, Map<String, String>> allUnits;

    private final GitDb database;
    private final ContentMapper mapper;
    private final ISearchProvider searchProvider;

    private boolean indexOnlyPublishedParentContent = false;

    private Cache<Object, Object> cache;


    /**
     * Constructor for instantiating a new Git Content Manager Object.
     * 
     * @param database
     *            - that the content Manager manages.
     * @param searchProvider
     *            - search provider that the content manager manages and controls.
     * @param contentMapper
     *            - The utility class for mapping content objects.
     */
    @Inject
    public GitContentManager(final GitDb database, final ISearchProvider searchProvider,
            final ContentMapper contentMapper) {
        this.database = database;
        this.mapper = contentMapper;
        this.searchProvider = searchProvider;
       
        this.indexProblemCache = new ConcurrentHashMap<>();
        this.tagsList = new ConcurrentHashMap<>();
        this.allUnits = new ConcurrentHashMap<>();

        this.cache = CacheBuilder.newBuilder().softValues().build();

        searchProvider.registerRawStringFields(Lists.newArrayList(Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME,
                Constants.TYPE_FIELDNAME));
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
     * @param indexProblemCache
     *            - A manually constructed indexProblemCache for testing purposes
     */
    public GitContentManager(final GitDb database, final ISearchProvider searchProvider,
            final ContentMapper contentMapper, final Map<String, Map<Content, List<String>>> indexProblemCache) {
        this.database = database;
        this.mapper = contentMapper;
        this.searchProvider = searchProvider;
        
        this.indexProblemCache = indexProblemCache;
        this.tagsList = new ConcurrentHashMap<>();
        this.allUnits = new ConcurrentHashMap<>();

        searchProvider.registerRawStringFields(Lists.newArrayList(Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME));
    }

    @Override
    public final <T extends Content> String save(final T objectToSave) {
        throw new UnsupportedOperationException(
                "This method is not implemented yet - Git is a readonly data store at the moment.");
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
        if (null == id) {
            return null;
        }

        String k = "getContentDOById~" + version + "~" + id;
        if (!cache.asMap().containsKey(k)) {
            this.ensureCache(version);

            List<Content> searchResults = mapper.mapFromStringListToContentList(this.searchProvider.termSearch(version,
                    CONTENT_TYPE, Collections.singletonList(id),
                    Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, 0, 1).getResults());

            if (null == searchResults || searchResults.isEmpty()) {
                log.error("Failed to locate the content (" + id + ") in the cache for version " + version);
                return null;
            }

            cache.put(k, searchResults.get(0));
        }

        return (Content) cache.getIfPresent(k);

    }

    @SuppressWarnings("unchecked")
    @Override
    public ResultsWrapper<ContentDTO> getByIdPrefix(final String version, final String idPrefix, final int startIndex,
            final int limit) throws ContentManagerException {

        String k = "getByIdPrefix~" + version + "~" + idPrefix + "~" + startIndex + "~" + limit;
        if (!cache.asMap().containsKey(k)) {
            this.ensureCache(version);

            ResultsWrapper<String> searchHits = this.searchProvider.findByPrefix(version, CONTENT_TYPE,
                    Constants.ID_FIELDNAME + "."
                            + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, idPrefix, startIndex, limit);

            List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());

            cache.put(k, new ResultsWrapper<>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults()));
        }

        return (ResultsWrapper<ContentDTO>) cache.getIfPresent(k);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResultsWrapper<ContentDTO> getAllByTypeRegEx(final String version, final String regex, final int startIndex,
            final int limit) throws ContentManagerException {

        String k = "getAllByTypeRegEx~" + version + "~" + regex + "~" + startIndex + "~" + limit;

        if (!cache.asMap().containsKey(k)) {

            this.ensureCache(version);

            ResultsWrapper<String> searchHits = this.searchProvider.findByRegEx(version, CONTENT_TYPE,
                    Constants.TYPE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                    regex, startIndex, limit);

            List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());

            cache.put(k, new ResultsWrapper<>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults()));
        }

        return (ResultsWrapper<ContentDTO>) cache.getIfPresent(k);
    }

    @Override
    public final ResultsWrapper<ContentDTO> searchForContent(final String version, final String searchString,
            @Nullable final Map<String, List<String>> fieldsThatMustMatch, 
            final Integer startIndex, final Integer limit) throws ContentManagerException {

        this.ensureCache(version);

        ResultsWrapper<String> searchHits = searchProvider.fuzzySearch(version, CONTENT_TYPE, searchString, startIndex,
                limit, fieldsThatMustMatch, Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME,
                Constants.TAGS_FIELDNAME, Constants.VALUE_FIELDNAME, Constants.CHILDREN_FIELDNAME);

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

        this.ensureCache(version);

        final Map<String, Constants.SortOrder> newSortInstructions;
        if (null == sortInstructions || sortInstructions.isEmpty()) {
            newSortInstructions = Maps.newHashMap();
            newSortInstructions.put(Constants.TITLE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                    Constants.SortOrder.ASC);
        } else {
            newSortInstructions = sortInstructions;
        }

        ResultsWrapper<String> searchHits = searchProvider.matchSearch(version, CONTENT_TYPE, fieldsToMatch,
                startIndex, limit, newSortInstructions, filterInstructions);

        // setup object mapper to use preconfigured deserializer module.
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

        this.ensureCache(version);

        ResultsWrapper<String> searchHits;
        if (null == randomSeed) {
            searchHits = searchProvider.randomisedMatchSearch(version, CONTENT_TYPE, fieldsToMatch, startIndex, limit);
        } else {
            searchHits = searchProvider.randomisedMatchSearch(version, CONTENT_TYPE, fieldsToMatch, startIndex, limit,
                    randomSeed);
        }

        // setup object mapper to use preconfigured deserializer module.
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
        return database.pullLatestFromRemote();
    }

    @Override
    public final Set<String> getCachedVersionList() {
        Builder<String> builder = ImmutableSet.builder();
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
    public final void clearCache() {
        log.info("Clearing all content caches.");
        searchProvider.expungeEntireSearchCache();
        indexProblemCache.clear();
        tagsList.clear();
        allUnits.clear();
        cache.invalidateAll();
    }

    @Override
    public final void clearCache(final String version) {
        Validate.notBlank(version);

        if (this.searchProvider.hasIndex(version)) {
            indexProblemCache.remove(version);
            searchProvider.expungeIndexFromSearchCache(version);
            tagsList.remove(version);
            allUnits.remove(version);
            cache.invalidateAll();
        }
    }

    @Override
    public final Set<String> getTagsList(final String version) throws ContentManagerException {
        Validate.notBlank(version);

        if (!tagsList.containsKey(version)) {
            log.warn("The version requested does not exist in the tag list. Reindexing.");
            buildGitContentIndex(version);
        }

        if (!tagsList.containsKey(version)) {
            log.warn("The version requested does not exist in the tag list, even after reindexing.");
            return null;
        }

        return tagsList.get(version);
    }

    @Override
    public final Collection<String> getAllUnits(final String version) throws ContentManagerException {
        Validate.notBlank(version);

        if (!allUnits.containsKey(version)) {
            log.warn("The version requested does not exist in the set of all units. Reindexing.");
            buildGitContentIndex(version);
        }

        if (!allUnits.containsKey(version)) {
            log.warn("The version requested does not exist in the set of all units, even after reindexing.");
            return null;
        }

        return allUnits.get(version).values();
    }

    @Override
    public void ensureCache(final String version) throws ContentManagerException {
        if (null == version) {
            throw new ContentVersionUnavailableException(
                    "You must specify a non-null version to make sure it is cached.");
        }

        // In order to serve all content requests we need to index both the search provider and problem cache.
        boolean searchIndexed;
        if (!searchProvider.hasIndex(version)) {
            synchronized (this) {
                final Map<String, Content> gitCache;

                // now we have acquired the lock check if someone else has indexed this.
                searchIndexed = searchProvider.hasIndex(version);
                if (searchIndexed && this.indexProblemCache.containsKey(version)) {
                    return;
                }

                log.info(String.format(
                        "Rebuilding content index as sha (%s) does not exist in search provider.",
                        version));
                
                // anytime we build the git index we have to empty the problem cache
                this.indexProblemCache.remove(version);
                gitCache = buildGitContentIndex(version);

                // may as well spawn a new thread to do the validation
                // work now.
                Thread validationJob = new Thread() {
                    @Override
                    public void run() {
                        checkForContentErrors(version, gitCache);
                    }
                };

                validationJob.setDaemon(true);
                validationJob.start();
                
                if (!searchIndexed) {
                    buildSearchIndexFromLocalGitIndex(version, gitCache);                   
                } else {
                    log.info(String.format("Search index for %s is already available. Not reindexing...", version));
                }
            }
        }

        // verification step. Make sure that this segue instance is happy it can access the content requested.
        // if not then throw an exception.
        StringBuilder errorMessageStringBuilder = new StringBuilder();
        searchIndexed = searchProvider.hasIndex(version);
        
        if (!searchIndexed) {
            errorMessageStringBuilder.append(String.format("Version %s does not exist in the searchIndex.", version));
        }

        String message = errorMessageStringBuilder.toString();
        if (!message.isEmpty()) {
            throw new ContentVersionUnavailableException(message);
        }
    }

    @Override
    public final Map<Content, List<String>> getProblemMap(final String version) {
        if (indexProblemCache.get(version) == null) {
            // this check is to prevent memory leaks as currently the search provider is the only thing we use to keep
            // track of what is available to serve.
            if (!this.searchProvider.hasIndex(version)) {
                log.error("Cannot request a problem map for a version that is not currently "
                        + "indexed by the search provider.");
                return null;
            }
            
            // on the off chance that we haven't prepared the problem map requested go ahead and build it.
            try {
                checkForContentErrors(version, buildGitContentIndex(version));
            } catch (ContentManagerException e) {
                log.error("Unable to build problem map requested", e);
            }
        }
        return indexProblemCache.get(version);
    }

    /**
     * Populate content summary object.
     * 
     * @param version
     *            - the version of the content to use for the augmentation process.
     * 
     * @param contentDTO
     *            - the destination contentDTO which should have content summaries created.
     * @return fully populated contentDTO.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    @Override
    public ContentDTO populateRelatedContent(final String version, final ContentDTO contentDTO)
            throws ContentManagerException {
        if (contentDTO.getRelatedContent() == null || contentDTO.getRelatedContent().isEmpty()) {
            return contentDTO;
        }

        // build query the db to get full content information
        Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMap = new HashMap<>();

        List<String> relatedContentIds = Lists.newArrayList();
        for (ContentSummaryDTO summary : contentDTO.getRelatedContent()) {
            relatedContentIds.add(summary.getId());
        }

        fieldsToMap.put(
                Maps.immutableEntry(Constants.BooleanOperator.OR, Constants.ID_FIELDNAME + '.'
                        + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX), relatedContentIds);

        ResultsWrapper<ContentDTO> results = this.findByFieldNames(version, fieldsToMap, 0, relatedContentIds.size());

        List<ContentSummaryDTO> relatedContentDTOs = Lists.newArrayList();

        for (ContentDTO relatedContent : results.getResults()) {
            ContentSummaryDTO summary = this.mapper.getAutoMapper().map(relatedContent, ContentSummaryDTO.class);
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
    
    /**
     * This method will send off the information in the git cache to the search provider for indexing.
     * 
     * @param sha
     *            - the version in the git cache to send to the search provider.
     * @param gitCache
     *            a map that represents indexed content for a given sha.
     */
    private synchronized void buildSearchIndexFromLocalGitIndex(final String sha, final Map<String, Content> gitCache) {
        if (this.searchProvider.hasIndex(sha)) {
            log.info("Search index has already been updated by" + " another thread. No need to reindex. Aborting...");
            return;
        }

        log.info("Building search index for: " + sha);

        // setup object mapper to use pre-configured deserializer module.
        // Required to deal with type polymorphism
        List<Map.Entry<String, String>> thingsToIndex = Lists.newArrayList();
        ObjectMapper objectMapper = mapper.generateNewPreconfiguredContentMapper();
        for (Content content : gitCache.values()) {
            try {
                thingsToIndex.add(immutableEntry(content.getId(), objectMapper.writeValueAsString(content)));
            } catch (JsonProcessingException e) {
                log.error("Unable to serialize content object: " + content.getId()
                        + " for indexing with the search provider.", e);
                this.registerContentProblem(sha, content, "Search Index Error: " + content.getId()
                        + content.getCanonicalSourceFile() + " Exception: " + e.toString());
            }
        }

        try {
            this.searchProvider.bulkIndex(sha, CONTENT_TYPE, thingsToIndex);
            log.info("Search index request sent for: " + sha);
        } catch (SegueSearchOperationException e) {
            log.error("Error whilst trying to perform bulk index operation.", e);
        }
    }

    /**
     * This method will populate the internal gitCache based on the content object files found for a given SHA.
     * 
     * Currently it only looks for json files in the repository.
     * 
     * @param sha
     *            - the version to index.
     * @return the map representing all indexed content.
     * @throws ContentManagerException 
     */
    private synchronized Map<String, Content> buildGitContentIndex(final String sha) throws ContentManagerException {
        // This set of code only needs to happen if we have to read from git
        // again.
        if (null == sha) {
            throw new ContentManagerException("SHA: sha is null. Cannot index.");
        }

        if (this.indexProblemCache.containsKey(sha)) {
            throw new ContentManagerException(String.format("SHA: %s has already been indexed. Failing... ", sha)); 
        }
        
        // iterate through them to create content objects
        Repository repository = database.getGitRepository();

        try {
            ObjectId commitId = repository.resolve(sha);

            if (null == commitId) {
                throw new ContentManagerException("Failed to buildGitIndex - Unable to locate resource with SHA: "
                        + sha);
            }

            Map<String, Content> shaCache = new HashMap<>();

            TreeWalk treeWalk = database.getTreeWalk(sha, ".json");
            log.info("Populating git content cache based on sha " + sha + " ...");

            // Traverse the git repository looking for the .json files
            while (treeWalk.next()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
                loader.copyTo(out);

                // setup object mapper to use preconfigured deserializer
                // module. Required to deal with type polymorphism
                ObjectMapper objectMapper = mapper.getSharedContentObjectMapper();

                Content content;
                try {
                    content = (Content) objectMapper.readValue(out.toString(), ContentBase.class);

                    // check if we only want to index published content
                    if (indexOnlyPublishedParentContent && !content.getPublished()) {
                        log.debug("Skipping unpublished content: " + content.getId());
                        continue;
                    }

                    content = this.augmentChildContent(content, treeWalk.getPathString(), null);

                    if (null != content) {
                        // add children (and parent) from flattened Set to
                        // cache if they have ids
                        for (Content flattenedContent : this.flattenContentObjects(content)) {
                            if (flattenedContent.getId() == null) {
                                continue;
                            }

                            if (flattenedContent.getId().contains(".")) {
                                // Otherwise, duplicate IDs with different content,
                                // therefore log an error
                                log.warn("Resource with invalid ID (" + content.getId()
                                        + ") detected in cache. Skipping " + treeWalk.getPathString());

                                this.registerContentProblem(sha, flattenedContent, "Index failure - Invalid ID "
                                        + flattenedContent.getId() + " found in file " + treeWalk.getPathString()
                                        + ". Must not contain restricted characters.");
                                continue;
                            }

                            // check if we have seen this key before if
                            // we have then we don't want to add it
                            // again
                            if (!shaCache.containsKey(flattenedContent.getId())) {
                                // It must be new so we can add it
                                log.debug("Loading into cache: " + flattenedContent.getId() + "("
                                        + flattenedContent.getType() + ")" + " from " + treeWalk.getPathString());
                                shaCache.put(flattenedContent.getId(), flattenedContent);
                                registerTagsWithVersion(sha, flattenedContent.getTags());

                                // If this is a numeric question, extract any
                                // units from its answers.

                                if (flattenedContent instanceof IsaacNumericQuestion) {
                                    registerUnitsWithVersion(sha, (IsaacNumericQuestion) flattenedContent);
                                }

                                continue; // our work here is done
                            }

                            // shaCache contains key already, compare the
                            // content
                            if (shaCache.get(flattenedContent.getId()).equals(flattenedContent)) {
                                // content is the same therefore it is just
                                // reuse of a content object so that is
                                // fine.
                                log.debug("Resource (" + content.getId() + ") already seen in cache. Skipping "
                                        + treeWalk.getPathString());
                                continue;
                            }

                            // Otherwise, duplicate IDs with different content,
                            // therefore log an error
                            log.warn("Resource with duplicate ID (" + content.getId()
                                    + ") detected in cache. Skipping " + treeWalk.getPathString());
                            this.registerContentProblem(sha, flattenedContent,
                                    "Index failure - Duplicate ID found in file " + treeWalk.getPathString() + " and "
                                            + shaCache.get(flattenedContent.getId()).getCanonicalSourceFile()
                                            + " only one will be available");
                        }
                    }
                } catch (JsonMappingException e) {
                    log.warn(String.format("Unable to parse the json file found %s as a content object. "
                            + "Skipping file due to error: \n %s", treeWalk.getPathString(), e.getMessage()));
                    Content dummyContent = new Content();
                    dummyContent.setCanonicalSourceFile(treeWalk.getPathString());
                    this.registerContentProblem(sha, dummyContent, "Index failure - Unable to parse json file found - "
                            + treeWalk.getPathString() + ". The following error occurred: " + e.getMessage());
                } catch (IOException e) {
                    log.error("IOException while trying to parse " + treeWalk.getPathString(), e);
                    Content dummyContent = new Content();
                    dummyContent.setCanonicalSourceFile(treeWalk.getPathString());
                    this.registerContentProblem(sha, dummyContent,
                            "Index failure - Unable to read the json file found - " + treeWalk.getPathString()
                                    + ". The following error occurred: " + e.getMessage());
                }
            }
            
            repository.close();
            log.debug("Tags available " + tagsList);
            log.debug("All units: " + allUnits);
            log.info("Git content cache population for " + sha + " completed!");
            
            return shaCache;
        } catch (IOException e) {
            log.error("IOException while trying to access git repository. ", e);
            throw new ContentManagerException("Unable to index content, due to an IOException.");
        }
    }

    /**
     * Augments all child objects recursively to include additional information.
     * 
     * This should be done before saving to the local gitCache in memory storage.
     * 
     * This method will also attempt to reconstruct object id's of nested content such that they are unique to the page
     * by default.
     * 
     * @param content
     *            - content to augment
     * @param canonicalSourceFile
     *            - source file to add to child content
     * @param parentId
     *            - used to construct nested ids for child elements.
     * @return Content object with new reference
     */
    private Content augmentChildContent(final Content content, final String canonicalSourceFile,
            @Nullable final String parentId) {
        if (null == content) {
            return null;
        }

        // If this object is of type question then we need to give it a random
        // id if it doesn't have one.
        if (content instanceof Question && content.getId() == null) {
            log.warn("Found question without id " + content.getTitle() + " " + canonicalSourceFile);
        }

        // Try to figure out the parent ids.
        String newParentId;
        if (null == parentId && content.getId() != null) {
            newParentId = content.getId();
        } else {
            if (content.getId() != null) {
                newParentId = parentId + Constants.ID_SEPARATOR + content.getId();
            } else {
                newParentId = parentId;
            }
        }

        content.setCanonicalSourceFile(canonicalSourceFile);

        if (!content.getChildren().isEmpty()) {
            for (ContentBase cb : content.getChildren()) {
                if (cb instanceof Content) {
                    Content c = (Content) cb;

                    this.augmentChildContent(c, canonicalSourceFile, newParentId);
                }
            }
        }

        if (content instanceof Choice) {
            Choice choice = (Choice) content;
            this.augmentChildContent((Content) choice.getExplanation(), canonicalSourceFile,
                    newParentId);
        }
        
        // TODO: hack to get hints to apply as children
        if (content instanceof Question) {
            Question question = (Question) content;
            if (question.getHints() != null) {
                for (ContentBase cb : question.getHints()) {
                    Content c = (Content) cb;
                    this.augmentChildContent(c, canonicalSourceFile, newParentId);
                }
            }

            // Augment question answers
            if (question.getAnswer() != null) {
                Content answer = (Content) question.getAnswer();
                if (answer.getChildren() != null) {
                    for (ContentBase cb : answer.getChildren()) {
                        Content c = (Content) cb;
                        this.augmentChildContent(c, canonicalSourceFile, newParentId);
                    }
                }
            }
            
            if (content instanceof ChoiceQuestion) {
                ChoiceQuestion choiceQuestion = (ChoiceQuestion) content;
                if (choiceQuestion.getChoices() != null) {
                    for (ContentBase cb : choiceQuestion.getChoices()) {
                        Content c = (Content) cb;
                        this.augmentChildContent(c, canonicalSourceFile, newParentId);
                    }
                }
            }
        }

        // try to determine if we have media as fields to deal with in this class
        Method[] methods = content.getClass().getDeclaredMethods();
        for (Method method: methods) {
            if (Media.class.isAssignableFrom(method.getReturnType())) {
                try {
                    Media media = (Media) method.invoke(content);
                    if (media != null) {
                        media.setSrc(fixMediaSrc(canonicalSourceFile, media.getSrc()));
                    }
                } catch (SecurityException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    log.error("Unable to access method using reflection: attempting to fix Media Src", e);
                }
            }
        }

        if (content instanceof Media) {
            Media media = (Media) content;
            media.setSrc(fixMediaSrc(canonicalSourceFile, media.getSrc()));

            // for tracking purposes we want to generate an id for all image content objects.
            if (media.getId() == null && media.getSrc() != null) {
                media.setId(parentId + Constants.ID_SEPARATOR
                        + Base64.encodeBase64(media.getSrc().getBytes()).toString());
            }
        }

        // Concatenate the parentId with our id to get a fully qualified
        // identifier.
        if (content.getId() != null && parentId != null) {
            content.setId(parentId + Constants.ID_SEPARATOR + content.getId());
        }

        return content;
    }

    /**
     * @param canonicalSourceFile
     *            - the canonical path to use for concat operations.
     * @param originalSrc
     *            - to modify
     * @return src with relative paths fixed.
     */
    private String fixMediaSrc(final String canonicalSourceFile, final String originalSrc) {
        if (originalSrc != null && !(originalSrc.startsWith("http://") || originalSrc.startsWith("https://"))) {
            return FilenameUtils.normalize(FilenameUtils.getPath(canonicalSourceFile) + originalSrc, true);
        }
        return originalSrc;
    }

    /**
     * This method will attempt to traverse the cache to ensure that all content references are valid.
     * 
     * @param sha
     *            version to validate integrity of.
     * @param gitCache
     *            Data structure containing all content for a given sha.
     * @return True if we are happy with the integrity of the git repository, False if there is something wrong.
     */
    private boolean checkForContentErrors(final String sha, final Map<String, Content> gitCache) {
        log.info(String.format("Starting content Validation (%s).", sha));
        Set<Content> allObjectsSeen = new HashSet<>();
        Set<String> expectedIds = new HashSet<>();
        Set<String> definedIds = new HashSet<>();
        Set<String> missingContent = new HashSet<>();
        Map<String, Content> whoAmI = new HashMap<>();

        // Build up a set of all content (and content fragments for validation)
        for (Content c : gitCache.values()) {
// TODO work out why this was here and why removing it didn't seem to do anything!
//            if (c instanceof IsaacSymbolicQuestion) {
//                // do not validate these questions for now.
//                continue;
//            }
            allObjectsSeen.addAll(this.flattenContentObjects(c));
        }

        // Start looking for issues in the flattened content data
        for (Content c : allObjectsSeen) {
            // add the id to the list of defined ids
            if (c.getId() != null) {
                definedIds.add(c.getId());
            }

            // add the ids to the list of expected ids
            if (c.getRelatedContent() != null) {
                expectedIds.addAll(c.getRelatedContent());
                // record which content object was referencing which ID
                for (String id : c.getRelatedContent()) {
                    whoAmI.put(id, c);
                }
            }

            // ensure content does not have children and a value
            if (c.getValue() != null && !c.getChildren().isEmpty()) {
                String id = c.getId();
                String firstLine = "Content";
                if (id != null) {
                    firstLine += ": " + id;
                }

                this.registerContentProblem(sha, c, firstLine + " in " + c.getCanonicalSourceFile()
                        + " found with both children and a value. "
                        + "Content objects are only allowed to have one or the other.");

                log.error("Invalid content item detected: The object with ID (" + id
                        + ") has both children and a value.");
            }

            // content type specific checks
            if (c instanceof Media) {
                Media f = (Media) c;

                if (f.getSrc() != null 
                        && !f.getSrc().startsWith("http") && !database.verifyGitObject(sha, f.getSrc())) {
                    this.registerContentProblem(sha, c, "Unable to find Image: " + f.getSrc()
                            + " in Git. Could the reference be incorrect? SourceFile is " + c.getCanonicalSourceFile());
                }

                // check that there is some alt text.
                if (f.getAltText() == null || f.getAltText().isEmpty()) {
                    this.registerContentProblem(sha, c, "No altText attribute set for media element: " + f.getSrc()
                            + " in Git source file " + c.getCanonicalSourceFile());
                }
            }
            if (c instanceof Question && c.getId() == null) {
                this.registerContentProblem(sha, c, "Question: " + c.getTitle() + " in " + c.getCanonicalSourceFile()
                        + " found without a unqiue id. " + "This question cannot be logged correctly.");
            }
            // TODO: remove reference to isaac specific types from here.
            if (c instanceof ChoiceQuestion
                    && !(c.getType().equals("isaacQuestion"))) {
                ChoiceQuestion question = (ChoiceQuestion) c;

                if (question.getChoices() == null || question.getChoices().isEmpty()) {
                    this.registerContentProblem(sha, question,
                            "Question: " + question.getId() + " found without any choice metadata. "
                                    + "This question will always be automatically " + "marked as incorrect");
                } else {
                    boolean correctOptionFound = false;
                    for (Choice choice : question.getChoices()) {
                        if (choice.isCorrect()) {
                            correctOptionFound = true;
                        }
                    }

                    if (!correctOptionFound) {
                        this.registerContentProblem(sha, question,
                                "Question: " + question.getId() + " found without a correct answer. "
                                        + "This question will always be automatically marked as incorrect");
                    }
                }
            }

            if (c instanceof EmailTemplate) {
                EmailTemplate e = (EmailTemplate) c;
                if (e.getPlainTextContent() == null) {
                    this.registerContentProblem(sha, c,
                            "Email template should always have plain text content field");
                }

                if (e.getReplyToEmailAddress() != null && null == e.getReplyToName()) {
                    this.registerContentProblem(sha, c,
                            "Email template contains replyToEmailAddress but not replyToName");
                }
            }

            if (c instanceof IsaacEventPage) {
                IsaacEventPage e = (IsaacEventPage) c;
                if (e.getEndDate().before(e.getDate())) {
                    this.registerContentProblem(sha, c, "Event has end date before start date");
                }
            }

            // TODO: the following things are all highly Isaac specific. I guess they should be elsewhere . . .

            if (c instanceof IsaacNumericQuestion) {
                IsaacNumericQuestion q = (IsaacNumericQuestion) c;

                String regExp = "[\\x00-\\x20]*[+-]?(((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)"
                        + "([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)|"
                        + "(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?"
                        + "(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*";

                // Check for at most 6 available symbols
                if (q.getAvailableUnits().size() > MAX_NUMERIC_QUESTION_UNIT_COUNT) {
                    this.registerContentProblem(sha, c, "Numeric Question: " + q.getId() + " has "
                            + q.getAvailableUnits().size() + " available units. Expected at most "
                            + MAX_NUMERIC_QUESTION_UNIT_COUNT + ".");
                }

                // Check if available units are specified when units are required.
                if (q.getAvailableUnits().size() > 0 && !q.getRequireUnits()) {
                    this.registerContentProblem(sha, c, "Numeric Question: " + q.getId()
                            + " has custom units specified but does not require units.");
                }

                for (Choice choice : q.getChoices()) {
                    if (choice instanceof Quantity) {
                        Quantity quantity = (Quantity) choice;

                        // Find quantities with values that cannot be parsed as numbers.
                        if (!quantity.getValue().matches(regExp)) {
                            this.registerContentProblem(sha, c,
                                    "Numeric Question: " + q.getId() + " has Quantity (" + quantity.getValue()
                                            + ")  with value that cannot be interpreted as a number. "
                                            + "Users will never be able to match this answer.");
                        }

                    } else if (q.getRequireUnits()) {
                        this.registerContentProblem(sha, c, "Numeric Question: " + q.getId()
                                + " has non-Quantity Choice (" + choice.getValue()
                                + "). It must be deleted and a new Quantity Choice created.");
                    }
                }
            }

            // Find Symbolic Questions with broken properties. Need to exclude Chemistry questions!
            if (c instanceof IsaacSymbolicQuestion) {
                if (c.getClass().equals(IsaacSymbolicQuestion.class)) {
                    IsaacSymbolicQuestion q = (IsaacSymbolicQuestion) c;
                    for (String sym : q.getAvailableSymbols()) {
                        if (sym.contains("\\")) {
                            this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId()
                                    + " has availableSymbol (" + sym + ") which contains a '\\' character.");
                        }
                    }
                    for (Choice choice : q.getChoices()) {
                        if (choice instanceof Formula) {
                            Formula f = (Formula) choice;
                            if (f.getPythonExpression().contains("\\")) {
                                this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId()
                                        + " has Formula (" + choice.getValue()
                                        + ") with pythonExpression which contains a '\\' character.");
                            } else if (f.getPythonExpression() == null || f.getPythonExpression().isEmpty()) {
                                this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId() + " has Formula ("
                                        + choice.getValue() + ") with empty pythonExpression!");
                            }
                        } else {
                            this.registerContentProblem(sha, c, "Symbolic Question: " + q.getId()
                                    + " has non-Formula Choice (" + choice.getValue()
                                    + "). It must be deleted and a new Formula Choice created.");
                        }
                    }
                } else if (c.getClass().equals(IsaacSymbolicChemistryQuestion.class)) {
                    IsaacSymbolicChemistryQuestion q = (IsaacSymbolicChemistryQuestion) c;
                    for (Choice choice : q.getChoices()) {
                        if (choice instanceof ChemicalFormula) {
                            ChemicalFormula f = (ChemicalFormula) choice;
                            if (f.getMhchemExpression() == null || f.getMhchemExpression().isEmpty()) {
                                this.registerContentProblem(sha, c, "Chemistry Question: " + q.getId()
                                        + " has ChemicalFormula with empty mhchemExpression!");
                            }
                        } else {
                            this.registerContentProblem(sha, c, "Chemistry Question: " + q.getId()
                                    + " has non-ChemicalFormula Choice (" + choice.getValue()
                                    + "). It must be deleted and a new ChemicalFormula Choice created.");
                        }
                    }
                }
            }
        }

        if (expectedIds.equals(definedIds) && missingContent.isEmpty()) {
            return true;
        } else {
            expectedIds.removeAll(definedIds);
            missingContent.addAll(expectedIds);

            for (String id : missingContent) {
                this.registerContentProblem(sha, whoAmI.get(id), "This id (" + id + ") was referenced by "
                        + whoAmI.get(id).getCanonicalSourceFile() + " but the content with that "
                        + "ID cannot be found.");
            }
            if (missingContent.size() > 0) {
                log.warn("Referential integrity broken for (" + missingContent.size() + ") related Content items. "
                        + "The following ids are referenced but do not exist: " + expectedIds.toString());
            }
        }
        log.info(String.format("Validation processing (%s) complete. There are %s files with content problems", sha,
                this.indexProblemCache.get(sha).size()));

        return false;
    }

    /**
     * Unpack the content objects into one big set. Useful for validation but could produce a very large set
     * 
     * @param content
     *            content object to flatten
     * @return Set of content objects comprised of all children and the parent.
     */
    private Set<Content> flattenContentObjects(final Content content) {
        Set<Content> setOfContentObjects = new HashSet<>();
        if (!content.getChildren().isEmpty()) {

            List<ContentBase> children = content.getChildren();

            for (ContentBase child : children) {
                setOfContentObjects.add((Content) child);
                setOfContentObjects.addAll(flattenContentObjects((Content) child));
            }
        }

        setOfContentObjects.add(content);

        return setOfContentObjects;
    }

    /**
     * Helper function to build up a set of used tags for each version.
     * 
     * @param version
     *            - version to register the tag for.
     * @param tags
     *            - set of tags to register.
     */
    private synchronized void registerTagsWithVersion(final String version, final Set<String> tags) {
        Validate.notBlank(version);

        if (null == tags || tags.isEmpty()) {
            // don't do anything.
            return;
        }

        if (!tagsList.containsKey(version)) {
            tagsList.put(version, new HashSet<String>());
        }
        Set<String> newTagSet = Sets.newHashSet();

        // sanity check that tags are trimmed.
        for (String tag : tags) {
            newTagSet.add(tag.trim());
        }

        tagsList.get(version).addAll(newTagSet);
    }

    /**
     * Helper function to accumulate the set of all units used in numeric question answers.
     * 
     * @param version
     *            - version to register the units for.
     * @param q
     *            - numeric question from which to extract units.
     */
    private synchronized void registerUnitsWithVersion(final String version, final IsaacNumericQuestion q) {

        HashMap<String, String> newUnits = Maps.newHashMap();

        for (Choice c : q.getChoices()) {
            if (c instanceof Quantity) {
                Quantity quantity = (Quantity) c;

                if (!quantity.getUnits().isEmpty()) {
                    String units = quantity.getUnits();
                    String cleanKey = units.replace("\t", "").replace("\n", "").replace(" ", "");

                    // May overwrite previous entry, doesn't matter as there is
                    // no mechanism by which to choose a winner
                    newUnits.put(cleanKey, units);
                }
            }
        }

        if (newUnits.isEmpty()) {
            // This question contained no units.
            return;
        }

        if (!allUnits.containsKey(version)) {
            allUnits.put(version, new HashMap<String, String>());
        }

        allUnits.get(version).putAll(newUnits);
    }

    /**
     * Helper method to register problems with content objects.
     * 
     * @param version
     *            - to which the problem relates
     * @param c
     *            - Partial content object to represent the object that has problems.
     * @param message
     *            - Error message to associate with the problem file / content.
     */
    private synchronized void registerContentProblem(final String version, final Content c, final String message) {
        Validate.notNull(c);

        // try and make sure each dummy content object has a title
        if (c.getTitle() == null) {
            c.setTitle(Paths.get(c.getCanonicalSourceFile()).getFileName().toString());
        }

        if (!indexProblemCache.containsKey(version)) {
            indexProblemCache.put(version, new HashMap<Content, List<String>>());
        }

        if (!indexProblemCache.get(version).containsKey(c)) {
            indexProblemCache.get(version).put(c, new ArrayList<String>());
        }

        indexProblemCache.get(version).get(c).add(message); //.replace("_", "\\_"));
    }

    @Override
    public void setIndexRestriction(final boolean loadOnlyPublishedContent) {
        this.indexOnlyPublishedParentContent = loadOnlyPublishedContent;
    }
}
