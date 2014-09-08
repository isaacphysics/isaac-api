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
	 *            - The json string representation of the entire document to be
	 *            indexed.
	 * @return True if successful false if there is an error
	 */
	boolean indexObject(final String index, final String indexType,
			final String content);

	/**
	 * Indexes an object with the search provider.
	 * 
	 * @param index
	 *            - the name of the index
	 * @param indexType
	 *            - the name of the type of document being indexed
	 * @param content
	 *            - The json string representation of the entire document to be
	 *            indexed.
	 * @param uniqueId
	 *            - A unique id for the document being indexed if available.
	 * @return True if successful false if there is an error
	 */
	boolean indexObject(final String index, final String indexType,
			final String content, final String uniqueId);

	/**
	 * Verifies the existence of a given index.
	 * 
	 * @param index
	 *            to verify
	 * @return true if the index exists false if not.
	 */
	boolean hasIndex(final String index);

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
	ResultsWrapper<String> paginatedMatchSearch(
			final String index,
			final String indexType,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final int startIndex, final int limit,
			final Map<String, Constants.SortOrder> sortInstructions);

	/**
	 * Executes a fuzzy search on an array of fields.
	 * 
	 * @param index
	 *            - the name of the index
	 * @param indexType
	 *            - the name of the type of document being searched for
	 * @param searchString
	 *            - the string to use for fuzzy matching
	 * @param fieldsThatMustMatch
	 *            - Map of Must match field -> value
	 * @param fields
	 *            - array (var args) of fields to match against
	 * @return results
	 */
	ResultsWrapper<String> fuzzySearch(final String index,
			final String indexType, final String searchString,
			final Map<String, List<String>> fieldsThatMustMatch,
			final String... fields);

	/**
	 * Executes a terms search using an array of terms on a single field.
	 * 
	 * Useful for tag searches - Currently setting is that results will only be
	 * returned if they match all search search terms.
	 * 
	 * @param index
	 *            - the name of the index
	 * @param indexType
	 *            - the name of the type of document being searched for
	 * @param searchTerms
	 *            - e.g. tags
	 * @param field
	 *            - to match against
	 * @return results
	 */
	ResultsWrapper<String> termSearch(final String index,
			final String indexType, final Collection<String> searchTerms,
			final String field);

	/**
	 * RandomisedPaginatedMatchSearch The same as paginatedMatchSearch but the
	 * results are returned in a random order.
	 * 
	 * @see paginatedMatchSearch
	 * @param index
	 *            index that the content is stored in
	 * @param indexType
	 *            - type of index as registered with search provider.
	 * @param fieldsToMatch
	 *            - Map of Map<Map.Entry<Constants.BooleanOperator, String>,
	 *            List<String>>
	 * @param startIndex
	 *            - start index for results
	 * @param limit
	 *            - the maximum number of results to return.
	 * @return results in a random order for a given match search.
	 */
	ResultsWrapper<String> randomisedPaginatedMatchSearch(
			final String index,
			final String indexType,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			int startIndex, int limit);

	/**
	 * RandomisedPaginatedMatchSearch The same as paginatedMatchSearch but the
	 * results are returned in a random order.
	 * 
	 * @see paginatedMatchSearch
	 * @param index
	 *            index that the content is stored in
	 * @param indexType
	 *            - type of index as registered with search provider.
	 * @param fieldsToMatch
	 *            - Map of Map<Map.Entry<Constants.BooleanOperator, String>,
	 *            List<String>>
	 * @param startIndex
	 *            - start index for results
	 * @param limit
	 *            - the maximum number of results to return.
	 * @param randomSeed
	 *            - random seed.
	 * @return results in a random order for a given match search.
	 */
	ResultsWrapper<String> randomisedPaginatedMatchSearch(String index, String indexType,
			Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatch, int startIndex, int limit,
			Long randomSeed);
	
	/**
	 * Query for a list of Results that match a given id prefix.
	 * 
	 * This is useful if you use unanalysed fields for ids and use the dot
	 * separator as a way of nesting fields.
	 * 
	 * @param index
	 *            index that the content is stored in
	 * @param indexType
	 *            - type of index as registered with search provider.
	 * @param fieldname
	 *            - fieldName to search within.
	 * @param prefix
	 *            - idPrefix to search for.
	 * @return A list of results that match the id prefix.
	 */
	ResultsWrapper<String> findByPrefix(String index, String indexType,
			String fieldname, String prefix);

	/**
	 * Clear a specific index from the search providers cache.
	 * 
	 * @param index
	 *            the index to delete from the search providers cache.
	 * @return true if successful false if not.
	 */
	boolean expungeIndexFromSearchCache(final String index);

	/**
	 * Instruct the search provider to delete all data from all indices.
	 * 
	 * This is a Nuclear option and will affect all indices in the cluster.
	 * 
	 * @return true if successful false if not.
	 */
	boolean expungeEntireSearchCache();

	/**
	 * Clear a specific index type from the search providers cache.
	 * 
	 * @param index
	 *            the index containing the type you wish to delete.
	 * @param indexType
	 *            the index type to delete from the search providers cache.
	 * @return true if successful false if not.
	 */
	boolean expungeIndexTypeFromSearchCache(String index, String indexType);
	
	/**
	 * Register the names of fields that should have clones created (which are
	 * not affected by the search processor e.g. stemming and ignoring
	 * punctuation)
	 * 
	 * @param fieldNames
	 *            to create raw fields of
	 */
	void registerRawStringFields(List<String> fieldNames);
}
