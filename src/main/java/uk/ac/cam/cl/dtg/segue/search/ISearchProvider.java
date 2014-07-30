package uk.ac.cam.cl.dtg.segue.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import uk.ac.cam.cl.dtg.segue.api.Constants;
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
	 * @return true if successful false if not.
	 */
	boolean expungeEntireSearchCache();

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
