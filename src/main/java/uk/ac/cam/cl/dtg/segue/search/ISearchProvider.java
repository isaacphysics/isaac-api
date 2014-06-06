package uk.ac.cam.cl.dtg.segue.search;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;

public interface ISearchProvider {
	
	/**
	 * Indexes an object with the search provider
	 *  
	 * @param index - the name of the index
	 * @param indexType - the name of the type of document being indexed
	 * @param content - The json string representation of the entire document to be indexed.
	 * @return True if successful false if there is an error
	 */
	public boolean indexObject(final String index, final String indexType, final String content);
	
	/**
	 * Indexes an object with the search provider
	 *  
	 * @param index - the name of the index
	 * @param indexType - the name of the type of document being indexed
	 * @param content - The json string representation of the entire document to be indexed.
	 * @param uniqueId - A unique id for the document being indexed if available.
	 * @return True if successful false if there is an error
	 */
	public boolean indexObject(final String index, final String indexType, final String content, final String uniqueId);

	/**
	 * Verifies the existence of a given index.
	 * 
	 * @param index to verify
	 * @return true if the index exists false if not.
	 */
	public boolean hasIndex(final String index);
	
	/**
	 * Paginated Match search for one field
	 * 
	 * @param index - ElasticSearch index
	 * @param indexType - Index type
	 * @param fieldName - the field name to use
	 * @param fieldValue - the field name search term
	 * @param startIndex - e.g. 0 for the first set of results
	 * @param limit - e.g. 10 for 10 results per page
	 * @param sortInstructions - the map of how to sort each field of interest.
	 * @return Results
	 */
	public ResultsWrapper<String> paginatedMatchSearch(final String index, final String indexType, final Map<String,List<String>> fieldsToMatch, final int startIndex, final int limit, final Map<String, Constants.SortOrder> sortInstructions);
	
	/**
	 * Executes a fuzzy search on an array of fields.
	 * 
	 * @param index - the name of the index
	 * @param indexType - the name of the type of document being searched for
	 * @param searchString - the string to use for fuzzy matching
	 * @param fields - array (var args) of fields to match against 
	 * @return results
	 */
	public ResultsWrapper<String> fuzzySearch(final String index, final String indexType, final String searchString, final String... fields);

	/**
	 * Executes a terms search using an array of terms on a single field.
	 * 
	 * Useful for tag searches - Currently setting is that results will only be returned if they match all search search terms.
	 * 
	 * @param index - the name of the index
	 * @param indexType - the name of the type of document being searched for
	 * @param searchTerms - e.g. tags
	 * @param field - to match against 
	 * @return results
	 */	
	public ResultsWrapper<String> termSearch(final String index, final String indexType, final Collection<String> searchTerms, final String field);
	
	/**
	 * Clear a specific index from the search providers cache.
	 * 
	 * @param index
	 * @return true if successful false if not.
	 */
	public boolean expungeIndexFromSearchCache(final String index);

	/**
	 * Instruct the search provider to delete all data from all indices.
	 * 
	 * @param index
	 * @return true if successful false if not.
	 */
	public boolean expungeEntireSearchCache();

	/**
	 * Register the names of fields that should have clones created (which are not affected by the search processor e.g. stemming and ignoring punctuation)
	 * 
	 * @param fieldNames to create raw fields of 
	 */
	public void registerRawStringFields(List<String> fieldNames);	
}
