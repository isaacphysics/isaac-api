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
package uk.ac.cam.cl.dtg.segue.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;

/**
 * Abstract interface describing behaviour of search providers.
 * 
 * 
 */
public interface ISearchProvider {

    /**
     * Indexes an object with the search provider.
     * 
     * @param index
     *            - the name of the index
     * @param indexType
     *            - the name of the type of document being indexed
     * @param content
     *            - The json string representation of the entire document to be indexed.
     * @throws SegueSearchOperationException
     *             - if there is an error whilst trying to index the document.
     */
    //void indexObject(final String index, final String indexType, final String content)
    //        throws SegueSearchOperationException;

    /**
     * Indexes an object with the search provider.
     * 
     * @param index
     *            - the name of the index
     * @param indexType
     *            - the name of the type of document being indexed
     * @param content
     *            - The json string representation of the entire document to be indexed.
     * @param uniqueId
     *            - A unique id for the document being indexed if available.
     * @throws SegueSearchOperationException
     *             - if there is an error whilst trying to index the document.
     */
    //void indexObject(final String index, final String indexType, final String content, final String uniqueId)
    //        throws SegueSearchOperationException;

    /**
     * Bulk Index operations are useful for indexing large numbers of documents with one request to the search provider.
     * 
     * @param index
     *            - the name of the index
     * @param indexType
     *            - the name of the type of document being indexed
     * @param dataToIndex
     *            - A list of Entries <Key(id), ContentToIndex>
     * @throws SegueSearchOperationException
     *             - if there is an error whilst trying to index the document.
     */
    //void bulkIndex(final String index, final String indexType, final List<Map.Entry<String, String>> dataToIndex)
     //       throws SegueSearchOperationException;

    /**
     * Verifies the existence of a given index.
     * 
     * @param index
     *            to verify
     * @return true if the index exists false if not.
     */
    boolean hasIndex(final String index);
    
    /**
     * @return the list of all indices.
     */
    Collection<String> getAllIndices();
    
    /**
     * Paginated Match search for one field.
     * 
     * @param index
     *            - ElasticSearch index
     * @param indexType
     *            - Index type
     * @param fieldsToMatch
     *            - the field name to use - and the field name search term
     * @param startIndex
     *            - e.g. 0 for the first set of results
     * @param limit
     *            - e.g. 10 for 10 results per page
     * @param sortInstructions
     *            - the map of how to sort each field of interest.
     * @return Results
     */
    ResultsWrapper<String> matchSearch(final String index, final String indexType,
            final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch, final int startIndex,
            final int limit, final Map<String, Constants.SortOrder> sortInstructions);

    /**
     * Paginated Match search for one field.
     * 
     * @param index
     *            - ElasticSearch index
     * @param indexType
     *            - Index type
     * @param fieldsToMatch
     *            - the field name to use - and the field name search term
     * @param startIndex
     *            - e.g. 0 for the first set of results
     * @param limit
     *            - e.g. 10 for 10 results per page
     * @param sortInstructions
     *            - the map of how to sort each field of interest.
     * @param filterInstructions
     *            - the map of how to sort each field of interest.
     * @return Results
     */
    ResultsWrapper<String> matchSearch(final String index, final String indexType,
            final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch, final int startIndex,
            final int limit, final Map<String, Constants.SortOrder> sortInstructions,
            @Nullable final Map<String, AbstractFilterInstruction> filterInstructions);

    /**
     * Executes a multi match search on an array of fields and will consider the fieldsThatMustMatchMap.
     * 
     * This method will only return exact prefix matches for any of the fields requested.
     * 
     * @param index
     *            - the name of the index
     * @param indexType
     *            - the name of the type of document being searched for
     * @param searchString
     *            - the string to use for matching
     * @param startIndex
     *            - e.g. 0 for the first set of results
     * @param limit
     *            - the maximum number of results to return -1 will attempt to return all results.
     * @param fieldsThatMustMatch
     *            - Map of Must match field -> value
     * @param fields
     *            - array (var args) of fields to search using the searchString
     * @return results
     */
    ResultsWrapper<String> basicFieldSearch(final String index, final String indexType, final String searchString,
            final Integer startIndex, final Integer limit,
            @Nullable final Map<String, List<String>> fieldsThatMustMatch, final String... fields);

    /**
     * Executes a fuzzy search on an array of fields and will consider the fieldsThatMustMatchMap.
     * 
     * This method should prioritise exact prefix matches and then fill it with fuzzy ones.
     * 
     * @param index
     *            - the name of the index
     * @param indexType
     *            - the name of the type of document being searched for
     * @param searchString
     *            - the string to use for fuzzy matching
     * @param startIndex
     *            - e.g. 0 for the first set of results
     * @param limit
     *            - the maximum number of results to return -1 will attempt to return all results.
     * @param fieldsThatMustMatch
     *            - Map of Must match field -> value
     * @param fields
     *            - array (var args) of fields to search using the searchString
     * @return results
     */
    ResultsWrapper<String> fuzzySearch(final String index, final String indexType, final String searchString,
            final Integer startIndex, final Integer limit, final Map<String, List<String>> fieldsThatMustMatch,
            final String... fields);

    /**
     * Executes a terms search using an array of terms on a single field.
     * 
     * Useful for tag searches - Current setting is that results will only be returned if they match all search terms.
     * 
     * @param index
     *            - the name of the index
     * @param indexType
     *            - the name of the type of document being searched for
     * @param searchTerms
     *            - e.g. tags
     * @param field
     *            - to match against
     * @param startIndex
     *            - start index for results
     * @param limit
     *            - the maximum number of results to return -1 will attempt to return all results.
     * @return results
     */
    ResultsWrapper<String> termSearch(final String index, final String indexType, final Collection<String> searchTerms,
            final String field, final int startIndex, final int limit);

    /**
     * RandomisedPaginatedMatchSearch The same as paginatedMatchSearch but the results are returned in a random order.
     * 
     * @see paginatedMatchSearch
     * @param index
     *            index that the content is stored in
     * @param indexType
     *            - type of index as registered with search provider.
     * @param fieldsToMatch
     *            - Map of Map<Map.Entry<Constants.BooleanOperator, String>, List<String>>
     * @param startIndex
     *            - start index for results
     * @param limit
     *            - the maximum number of results to return.
     * @return results in a random order for a given match search.
     */
    ResultsWrapper<String> randomisedMatchSearch(final String index, final String indexType,
            final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch, int startIndex,
            int limit);

    /**
     * RandomisedPaginatedMatchSearch The same as paginatedMatchSearch but the results are returned in a random order.
     * 
     * @see paginatedMatchSearch
     * @param index
     *            index that the content is stored in
     * @param indexType
     *            - type of index as registered with search provider.
     * @param fieldsToMatch
     *            - Map of Map<Map.Entry<Constants.BooleanOperator, String>, List<String>>
     * @param startIndex
     *            - start index for results
     * @param limit
     *            - the maximum number of results to return.
     * @param randomSeed
     *            - random seed.
     * @return results in a random order for a given match search.
     */
    ResultsWrapper<String> randomisedMatchSearch(String index, String indexType,
            Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatch, 
            int startIndex, int limit, Long randomSeed);

    /**
     * Query for a list of Results that match a given id prefix.
     * 
     * This is useful if you use unanalysed fields for ids and use the dot separator as a way of nesting fields.
     * 
     * @param index
     *            index that the content is stored in
     * @param indexType
     *            - type of index as registered with search provider.
     * @param fieldname
     *            - fieldName to search within.
     * @param prefix
     *            - idPrefix to search for.
     * @param startIndex
     *            - start index for results
     * @param limit
     *            - the maximum number of results to return -1 will attempt to return all results.
     * @return A list of results that match the id prefix.
     */
    ResultsWrapper<String> findByPrefix(String index, String indexType, String fieldname, String prefix,
            int startIndex, int limit);
    
    /**
     * Find content by a regex.
     * 
     * Note: the fieldname specified must be be declared as a raw field (i.e. should be specified as unanalyzed). See
     * {@link #registerRawStringFields(List)}
     * 
     * @param index
     *            index that the content is stored in
     * @param indexType
     *            - type of index as registered with search provider.
     * @param fieldname
     *            - fieldName to search within.
     * @param regex
     *            - regex to search for.
     * @param startIndex
     *            - start index for results
     * @param limit
     *            - the maximum number of results to return -1 will attempt to return all results.
     * @return A list of results that match the id prefix.
     */
    ResultsWrapper<String> findByRegEx(String index, String indexType, String fieldname, String regex, int startIndex,
            int limit);    

    /**
     * Clear a specific index from the search providers cache.
     * 
     * @param index
     *            the index to delete from the search providers cache.
     * @return true if successful false if not.
     */
    //boolean expungeIndexFromSearchCache(final String index);

    /**
     * Instruct the search provider to delete all data from all indices.
     * 
     * This is a Nuclear option and will affect all indices in the cluster.
     * 
     * @return true if successful false if not.
     */
    //boolean expungeEntireSearchCache();
    
    /**
     * Register the names of fields that should have clones created (which are not affected by the search processor e.g.
     * stemming and ignoring punctuation)
     * 
     * @param fieldNames
     *            to create raw fields of
     */
    //void registerRawStringFields(List<String> fieldNames);
}
