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

package uk.ac.cam.cl.dtg.segue.search;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;

/**
 * Interface describing behaviour of search providers.
 */
public interface ISearchProvider {

  String getNestedFieldConnector();

  /**
   * Verifies the existence of a given index.
   *
   * @param indexBase to verify
   * @param indexType to verify
   * @return true if the index exists false if not.
   */
  boolean hasIndex(String indexBase, String indexType);

  /**
   * @return the list of all indices.
   */
  Collection<String> getAllIndices();

  /**
   * Paginated Match search for one field.
   *
   * @param basicSearchParameters - a Data Object containing the following common search parameters:
   *                              <p>indexBase - ElasticSearch index base string
   *                              <p>indexType - Index type
   *                              <p>startIndex - e.g. 0 for the first set of results
   *                              <p>limit - e.g. 10 for 10 results per page
   * @param fieldsToMatch         - the field name to use - and the field name search term
   * @param sortInstructions      - the map of how to sort each field of interest.
   * @param filterInstructions    - the map of how to sort each field of interest.
   * @return Results
   */
  ResultsWrapper<String> matchSearch(
      BasicSearchParameters basicSearchParameters, List<GitContentManager.BooleanSearchClause> fieldsToMatch,
      Map<String, Constants.SortOrder> sortInstructions,
      @Nullable Map<String, AbstractFilterInstruction> filterInstructions
  ) throws SegueSearchException;

  /**
   * Executes a fuzzy search on an array of fields and will consider the fieldsThatMustMatchMap.
   * <p>
   * This method should prioritise exact prefix matches and then fill it with fuzzy ones.
   *
   * @param basicSearchParameters - a Data Object containing the following common search parameters:
   *                              <p>indexBase - the base string for the name of the index
   *                              <p>indexType - the name of the type of document being searched for
   *                              <p>startIndex - e.g. 0 for the first set of results
   *                              <p>limit - the maximum number of results to return -1 will attempt to return all results.
   * @param searchString          - the string to use for fuzzy matching
   * @param fieldsThatMustMatch   - Map of Must match field -> value
   * @param filterInstructions    - post search filter instructions e.g. remove content of a certain type.
   * @param fields                - array (var args) of fields to search using the searchString
   * @return results
   */
  ResultsWrapper<String> fuzzySearch(
      BasicSearchParameters basicSearchParameters, String searchString, Map<String, List<String>> fieldsThatMustMatch,
      @Nullable Map<String, AbstractFilterInstruction> filterInstructions, String... fields
  ) throws SegueSearchException;

  ResultsWrapper<String> nestedMatchSearch(
      BasicSearchParameters basicSearchParameters, String searchString,
      @NotNull BooleanMatchInstruction matchInstruction,
      @Nullable Map<String, AbstractFilterInstruction> filterInstructions
  ) throws SegueSearchException;

  /**
   * Executes a terms search using an array of terms on a single field.
   * <p>
   * Useful for tag searches - Current setting is that results will only be returned if they match all search terms.
   * <p>
   * note: null searches are allowed providing a filter is specified.
   *
   * @param basicSearchParameters - a Data Object containing the following common search parameters:
   *                              <p>indexBase - the base string for the name of the index
   *                              <p>indexType - the name of the type of document being searched for
   *                              <p>startIndex - start index for results
   *                              <p>limit - the maximum number of results to return -1 will attempt to return all results.
   * @param searchTerms           - e.g. tags can be null
   * @param field                 - to match against - cannot be null if searchterm is not null.
   * @param filterInstructions    - instructions for filtering the results
   * @return results
   */
  ResultsWrapper<String> termSearch(
      BasicSearchParameters basicSearchParameters, String searchTerms, String field,
      Map<String, AbstractFilterInstruction> filterInstructions
  ) throws SegueSearchException;

  /**
   * RandomisedPaginatedMatchSearch The same as paginatedMatchSearch but the results are returned in a random order.
   *
   * @param basicSearchParameters - a Data Object containing the following common search parameters:
   *                              <p>indexBase - base string for the index that the content is stored in
   *                              <p>indexType - type of index as registered with search provider
   *                              <p>startIndex - start index for results
   *                              <p>limit - the maximum number of results to return
   * @param fieldsToMatch         - List of boolean clauses used for field matching.
   * @param randomSeed            - random seed.
   * @param filterInstructions    - post search filter instructions e.g. remove content of a certain type.
   * @return results in a random order for a given match search.
   */
  ResultsWrapper<String> randomisedMatchSearch(
      BasicSearchParameters basicSearchParameters, List<GitContentManager.BooleanSearchClause> fieldsToMatch,
      Long randomSeed, Map<String, AbstractFilterInstruction> filterInstructions
  ) throws SegueSearchException;

  /**
   * Query for a list of Results that exactly match a given id.
   *
   * @param basicSearchParameters - a Data Object containing the following common search parameters:
   *                              <p>indexBase - base string for the index that the content is stored in
   *                              <p>indexType - type of index as registered with search provider
   *                              <p>startIndex - start index for results
   *                              <p>limit - the maximum number of results to return -1 will attempt to return all results
   * @param fieldName             - fieldName to search within.
   * @param needle                - needle to search for.
   * @param filterInstructions    - post search filter instructions e.g. remove content of a certain type.
   * @return A list of results that match the id prefix.
   */
  ResultsWrapper<String> findByExactMatch(
      BasicSearchParameters basicSearchParameters, String fieldName, String needle,
      @Nullable Map<String, AbstractFilterInstruction> filterInstructions
  ) throws SegueSearchException;

  /**
   * Query for a list of Results that match a given id prefix.
   * <p>
   * This is useful if you use un-analysed fields for ids and use the dot separator as a way of nesting fields.
   *
   * @param basicSearchParameters - a Data Object containing the following common search parameters:
   *                              <p>indexBase - base string for the index that the content is stored in
   *                              <p>indexType - type of index as registered with search provider
   *                              <p>startIndex - start index for results
   *                              <p>limit - the maximum number of results to return -1 will attempt to return all results
   * @param fieldname             - fieldName to search within.
   * @param prefix                - idPrefix to search for.
   * @param filterInstructions    - post search filter instructions e.g. remove content of a certain type.
   * @return A list of results that match the id prefix.
   */
  ResultsWrapper<String> findByPrefix(
      BasicSearchParameters basicSearchParameters, String fieldname, String prefix,
      @Nullable Map<String, AbstractFilterInstruction> filterInstructions
  ) throws SegueSearchException;

  /**
   * Find content by a regex.
   *
   * @param basicSearchParameters - a Data Object containing the following common search parameters:
   *                              <p>indexBase - base string for the index that the content is stored in
   *                              <p>indexType - type of index as registered with search provider
   *                              <p>startIndex - start index for results
   *                              <p>limit - the maximum number of results to return -1 will attempt to return all results
   * @param fieldname             - fieldName to search within.
   * @param regex                 - regex to search for.
   * @param filterInstructions    - post search filter instructions e.g. remove content of a certain type.
   * @return A list of results that match the id prefix.
   */
  ResultsWrapper<String> findByRegEx(
      BasicSearchParameters basicSearchParameters, String fieldname, String regex,
      @Nullable Map<String, AbstractFilterInstruction> filterInstructions
  ) throws SegueSearchException;

  /*
   * TODO: We need to change the return type of these two methods to avoid having ES specific things
   */
  GetResponse getById(String indexBase, String indexType, String id) throws SegueSearchException;

  SearchResponse getAllFromIndex(String indexBase, String indexType) throws SegueSearchException;
}
