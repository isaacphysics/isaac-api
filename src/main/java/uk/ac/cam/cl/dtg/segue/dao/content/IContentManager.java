/*
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

import uk.ac.cam.cl.dtg.segue.api.Constants.*;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared interface for content managers. This is to allow them to be backed by different databases.
 * 
 * @author Stephen Cummins
 */
public interface IContentManager {

    class BooleanSearchClause {
        private String field;
        private BooleanOperator operator;
        private List<String> values;
        public BooleanSearchClause(final String field, final BooleanOperator operator, final List<String> values) {
            this.field = field;
            this.operator = operator;
            this.values = values;
        }
        public String getField() {
            return this.field;
        }
        public BooleanOperator getOperator() {
            return this.operator;
        }
        public List<String> getValues() {
            return this.values;
        }
    }

    /**
     * Goes to the configured Database and attempts to find a content item with the specified ID. This returns the
     * object in the raw DO form.
     * 
     * @param id
     *            id to search for in preconfigured data source.
     * @return Will return a Content object (or subclass of Content) or Null if no content object is found.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    Content getContentDOById(String id) throws ContentManagerException;
    Content getContentDOById(String id, boolean failQuietly) throws ContentManagerException;

    /**
     * Goes to the configured Database and attempts to find a content item with the specified ID. This returns the
     * object in the DTO form.
     * 
     * @param id
     *            id to search for in preconfigured data source.
     * @return Will return a Content object (or subclass of Content) or Null if no content object is found.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    ContentDTO getContentById(String id) throws ContentManagerException;
    ContentDTO getContentById(String id, boolean failQuietly) throws ContentManagerException;

    /**
     * GetByIdPrefix Returns results that match a given id prefix for a specified version number.
     * 
     * @param idPrefix
     *            - id prefix to search for.
     * @param startIndex
     *            - start index for results
     * @param limit
     *            - the maximum number of results to return -1 will attempt to return all results.
     * @return ResultsWrapper of objects that match the id prefix.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    ResultsWrapper<ContentDTO> getByIdPrefix(String idPrefix, int startIndex, int limit)
            throws ContentManagerException;

    /**
     * getContentMatchingIds Returns results that match any of the listed ids for a specified version number.
     *
     * This method will retrieve all content that matches at least one of the given list of IDs.
     *
     * @param ids
     *            - ids to match.
     * @param startIndex
     *            - start index for results
     * @param limit
     *            - the maximum number of results to return -1 will attempt to return all results.
     * @return ResultsWrapper of objects that match the id prefix.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    ResultsWrapper<ContentDTO> getContentMatchingIds(Collection<String> ids, int startIndex, int limit)
            throws ContentManagerException;

    /**
     * Method to allow bulk search of content based on the type field.
     * 
     * @param fieldsToMatch
     *            - List of boolean clauses used for field matching.
     * @param startIndex
     *            - the index of the first item to return.
     * @param limit
     *            - the maximum number of results to return.
     * @return Results Wrapper containing results of the search.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    ResultsWrapper<ContentDTO> findByFieldNames(
            final List<BooleanSearchClause> fieldsToMatch, Integer startIndex, Integer limit
    ) throws ContentManagerException;

    /**
     * Method to allow bulk search of content based on the type field.
     * 
     * @param fieldsToMatch
     *            - List of boolean clauses used for field matching.
     * @param startIndex
     *            - the index of the first item to return.
     * @param limit
     *            - the maximum number of results to return.
     * @param sortInstructions
     *            - The sort instructions for results returned by this method.
     * @return Results Wrapper containing results of the search.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    ResultsWrapper<ContentDTO> findByFieldNames(
            List<BooleanSearchClause> fieldsToMatch, Integer startIndex, Integer limit,
            Map<String, SortOrder> sortInstructions
    ) throws ContentManagerException;

    /**
     * Method to allow bulk search of content based on the type field.
     * 
     * @param fieldsToMatch
     *            - List of boolean clauses used for field matching.
     * @param startIndex
     *            - the index of the first item to return.
     * @param limit
     *            - the maximum number of results to return.
     * @param sortInstructions
     *            - The sort instructions for results returned by this method.
     * @param filterInstructions
     *            - Any filter instructions.
     * @return Results Wrapper containing results of the search.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    ResultsWrapper<ContentDTO> findByFieldNames(
            List<BooleanSearchClause> fieldsToMatch, Integer startIndex, Integer limit,
            Map<String, SortOrder> sortInstructions, @Nullable final Map<String, AbstractFilterInstruction> filterInstructions
    ) throws ContentManagerException;

    /**
     * The same as findByFieldNames but the results list is returned in a randomised order.
     * 
     * @param fieldsToMatch
     *            - List of boolean clauses used for field matching.
     * @param startIndex
     *            - the index of the first item to return.
     * @param limit
     *            - the maximum number of results to return.
     * @return Results Wrapper containing results of the search.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(
            List<BooleanSearchClause> fieldsToMatch, Integer startIndex, Integer limit
    ) throws ContentManagerException;

    /**
     * The same as findByFieldNames but the results list is returned in a randomised order.
     * 
     * @param fieldsToMatch
     *            - List of boolean clauses used for field matching.
     * @param startIndex
     *            - the index of the first item to return.
     * @param limit
     *            - the maximum number of results to return.
     * @param randomSeed
     *            - random seed.
     * @return Results Wrapper containing results of the search.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(
            List<BooleanSearchClause> fieldsToMatch, Integer startIndex, Integer limit, @Nullable Long randomSeed
    ) throws ContentManagerException;

    /**
     * Allows fullText search using the internal search provider.
     * 
     * The fields included in the search is determined by the content manager.
     * 
     * @param searchString
     *            - string to use as search term.
     * @param fieldsThatMustMatch
     *            - map of fields to values which must match. - this can be null and will be ignored
     * @param startIndex
     *            - the index of the first item to return.
     * @param limit
     *            - the maximum number of results to return.
     * @return list of results ordered by relevance.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    ResultsWrapper<ContentDTO> searchForContent(String searchString,
                                                @Nullable Map<String, List<String>> fieldsThatMustMatch, Integer startIndex, Integer limit)
            throws ContentManagerException;

    ResultsWrapper<ContentDTO> siteWideSearch(
            final String searchString, final List<String> documentTypes,
            final boolean includeHiddenContent, final Integer startIndex, final Integer limit
    ) throws  ContentManagerException;


    /**
     * Method allows raw output to be retrieved for given files in the git repository. This is mainly so we can retrieve
     * image files.
     *
     * @param filename
     *            - The full path of the file you wish to retrieve.
     * @return The output stream of the file contents
     * @throws IOException
     *             if failed IO occurs.
     */
    ByteArrayOutputStream getFileBytes(String filename) throws IOException;
    /**
     * Get the latest version number from the database.
     * 
     * This is a low level operation that will return the latest version of the content that is known to the underlying
     * database.
     * 
     * @return the latest version id
     */
    String getLatestContentSHA();


    /**
     * A method that will return an unordered set of tags registered for a particular version of the content.
     * 
     * @return A set of tags that have been already used in a particular version of the content
     */
    Set<String> getTagsList();

    /**
     * A method that will return an unordered set of all units registered for a particular version of the content.
     * 
     * @return A set of units that have been already used in a particular version of the content
     */
    Collection<String> getAllUnits();

    /**
     * Provides a Set of currently indexed and cached versions.
     * 
     * @return A set of all of the version id's which are currently available without reindexing.
     */
    Set<String> getCachedContentSHAList();

    /**
     * Get the problem map for a particular version.
     * 
     * @return the map containing the content objects with problems and associated list of problem messages. Or null if
     *         there is no problem index.
     */
    Map<Content, List<String>> getProblemMap();

    /**
     * Augment content DTO with related content.
     * 
     * @param contentDTO
     *            - the destination contentDTO which should have content summaries created.
     * @return fully populated contentDTO.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    ContentDTO populateRelatedContent(ContentDTO contentDTO) throws ContentManagerException;

    /**
     * Get the SHA for the current content being presented.
     * This will look up the underlying SHA that is returned for the alias used.
     *
     * @return a git content SHA.
     */
    String getCurrentContentSHA();
}