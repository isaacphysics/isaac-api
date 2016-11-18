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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.ws.rs.NotFoundException;

import com.google.common.base.Functions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import uk.ac.cam.cl.dtg.segue.dos.content.*;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Implementation that specifically works with Content objects.
 * 
 */
public class GitContentManager implements IContentManager {
    private static final Logger log = LoggerFactory.getLogger(GitContentManager.class);

    private static final String CONTENT_TYPE = "content";

    private final Map<String, Map<Content, List<String>>> indexProblemCache;
    private final Map<String, Set<String>> tagsList;
    private final Map<String, Map<String, String>> allUnits;

    private final GitDb database;
    private final ContentMapper mapper;
    private final ISearchProvider searchProvider;
    private final PropertiesLoader globalProperties;

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
                             final ContentMapper contentMapper, final PropertiesLoader globalProperties) {
        this.database = database;
        this.mapper = contentMapper;
        this.searchProvider = searchProvider;
        this.globalProperties = globalProperties;
       
        this.indexProblemCache = new ConcurrentHashMap<String, Map<Content, List<String>>>();
        this.tagsList = new ConcurrentHashMap<String, Set<String>>();
        this.allUnits = new ConcurrentHashMap<String, Map<String, String>>();

        this.cache = CacheBuilder.newBuilder().softValues().build();

        //searchProvider.registerRawStringFields(Lists.newArrayList(Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME,
        //        Constants.TYPE_FIELDNAME));
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
        this.globalProperties = null;
        
        this.indexProblemCache = indexProblemCache;
        this.tagsList = new ConcurrentHashMap<String, Set<String>>();
        this.allUnits = new ConcurrentHashMap<String, Map<String, String>>();

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
            //this.ensureCache(version);

            List<Content> searchResults = mapper.mapFromStringListToContentList(this.searchProvider.termSearch(version,
                    CONTENT_TYPE, id,
                    Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, 0, 1).getResults());

            if (null == searchResults || searchResults.isEmpty()) {
                log.error("Failed to locate the content (" + id + ") in the cache for version " + version);
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
            //this.ensureCache(version);

            ResultsWrapper<String> searchHits = this.searchProvider.findByPrefix(version, CONTENT_TYPE,
                    Constants.ID_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, idPrefix, startIndex, limit);

            List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());

            cache.put(k, new ResultsWrapper<ContentDTO>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults()));
        }

        return (ResultsWrapper<ContentDTO>) cache.getIfPresent(k);
    }
    
    @Override
    public ResultsWrapper<ContentDTO> getAllByTypeRegEx(final String version, final String regex, final int startIndex,
            final int limit) throws ContentManagerException {

        String k = "getAllByTypeRegEx~" + version + "~" + regex + "~" + startIndex + "~" + limit;

        if (!cache.asMap().containsKey(k)) {

            //this.ensureCache(version);

            ResultsWrapper<String> searchHits = this.searchProvider.findByRegEx(version, CONTENT_TYPE,
                    Constants.TYPE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, regex, startIndex, limit);

            List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());

            cache.put(k, new ResultsWrapper<ContentDTO>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults()));
        }

        return (ResultsWrapper<ContentDTO>) cache.getIfPresent(k);
    }

    @Override
    public final ResultsWrapper<ContentDTO> searchForContent(final String version, final String searchString,
            @Nullable final Map<String, List<String>> fieldsThatMustMatch, 
            final Integer startIndex, final Integer limit) throws ContentManagerException {

        //this.ensureCache(version);

        ResultsWrapper<String> searchHits = searchProvider.fuzzySearch(version, CONTENT_TYPE, searchString, startIndex,
                limit, fieldsThatMustMatch, Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME,
                Constants.TAGS_FIELDNAME, Constants.VALUE_FIELDNAME, Constants.CHILDREN_FIELDNAME);

        List<Content> searchResults = mapper.mapFromStringListToContentList(searchHits.getResults());

        return new ResultsWrapper<ContentDTO>(mapper.getDTOByDOList(searchResults), searchHits.getTotalResults());
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
        ResultsWrapper<ContentDTO> finalResults = new ResultsWrapper<ContentDTO>();

        //this.ensureCache(version);

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

        finalResults = new ResultsWrapper<ContentDTO>(contentDTOResults, searchHits.getTotalResults());

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
        ResultsWrapper<ContentDTO> finalResults = new ResultsWrapper<ContentDTO>();

        //this.ensureCache(version);

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

        finalResults = new ResultsWrapper<ContentDTO>(contentDTOResults, searchHits.getTotalResults());

        return finalResults;
    }

    @Override
    public final ByteArrayOutputStream getFileBytes(final String version, final String filename) throws IOException {
        return database.getFileByCommitSHA(version, filename);
    }

    @Override
    public final List<String> listAvailableVersions() {

        List<String> result = new ArrayList<String>();
        for (RevCommit rc : database.listCommits()) {
            result.add(rc.getName());
        }

        return result;
    }
    
    @Override
    public final boolean isValidVersion(final String version) {
        if (null == version || version.isEmpty()) {
            return false;
        }

        return this.database.verifyCommitExists(version);
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

        List<Object> tagObjects = (List<Object>)searchProvider.getById(version, "metadata", "tags").getSource().get("tags");

        return new HashSet<>(Lists.transform(tagObjects, Functions.toStringFunction()));
    }

    @Override
    public final Collection<String> getAllUnits(final String version) throws ContentManagerException {
        Validate.notBlank(version);

        SearchResponse r =  searchProvider.getAllByType(globalProperties.getProperty(Constants.CONTENT_VERSION), "unit");
        SearchHits hits = r.getHits();
        ArrayList<String> units = new ArrayList<String>((int)hits.getTotalHits());
        for(SearchHit hit : hits) {
            units.add((String)hit.getSource().get("unit"));
        }

        return units;
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
            throw new ContentVersionUnavailableException(String.format("Version %s does not exist in the searchIndex.", version));
        }
    }

    @Override
    public final Map<Content, List<String>> getProblemMap(final String version) {

        SearchResponse r = searchProvider.getAllByType(globalProperties.getProperty(Constants.CONTENT_VERSION), "contentError");

        SearchHits hits = r.getHits();
        Map<Content, List<String>> map = new HashMap<>();
        for(SearchHit hit : hits) {

            Content partialContentWithErrors = new Content();
            Map src = hit.getSource();
            partialContentWithErrors.setId((String)src.get("id"));
            partialContentWithErrors.setTitle((String)src.get("title"));
            //partialContentWithErrors.setTags(pair.getKey().getTags()); // TODO: Support tags
            partialContentWithErrors.setPublished((Boolean)src.get("published"));
            partialContentWithErrors.setCanonicalSourceFile((String)src.get("canonicalSourceFile"));

            ArrayList<String> errors = new ArrayList<>();
            for (Object v : (List)hit.getSource().get("errors") ) {
                errors.add((String)v);
            }

            map.put(partialContentWithErrors, errors);
        }


        //Object o = searchProvider.getById(version, "metadata", "content_errors", Collections.singletonList("content_errors"))
        //        .getField("content_errors").getValue();

        return map;
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
        Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMap
            = new HashMap<Map.Entry<Constants.BooleanOperator, String>, List<String>>();

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
        ContentSummaryDTO contentInfo = mapper.getAutoMapper().map(content, ContentSummaryDTO.class);

        return contentInfo;
    }

    @Override
    public String getCurrentContentSHA() {
        GetResponse r = searchProvider.getById(globalProperties.getProperty(Constants.CONTENT_VERSION), "metadata", "version");
        return (String)r.getSource().get("version");
    }


}
