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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

/**
 * Shared interface for content managers. This is to allow them to be backed by
 * different databases.
 * 
 * @author Stephen Cummins
 */
public interface IContentManager {
	
	/**
	 * Sets Index Restrictions.
	 * 
	 * @param loadOnlyPublishedContent
	 *            - True if you only wish to index content where the top level
	 *            content object is marked as published. False if all valid
	 *            content should be indexed.
	 */
	void setIndexRestriction(boolean loadOnlyPublishedContent);
	
	/**
	 * Save an object to the content manager.
	 * 
	 * @param <T>
	 *            - the type of the object being saved.
	 * @param objectToSave
	 *            - the object to save to the content Manager.
	 * @return the objects id.
	 */
	<T extends Content> String save(T objectToSave);

	/**
	 * Goes to the configured Database and attempts to find a content item with
	 * the specified ID. This returns the object in the raw DO form.
	 * 
	 * @param id
	 *            id to search for in preconfigured data source.
	 * @param version
	 *            - the version to attempt to retrieve.
	 * 
	 * @return Will return a Content object (or subclass of Content) or Null if
	 *         no content object is found.
	 * @throws ContentManagerException
	 *             - if there is an error retrieving the content requested.
	 */
	Content getContentDOById(String version, String id) throws ContentManagerException;

	/**
	 * Goes to the configured Database and attempts to find a content item with
	 * the specified ID. This returns the object in the DTO form.
	 * 
	 * @param id
	 *            id to search for in preconfigured data source.
	 * @param version
	 *            - the version to attempt to retrieve.
	 * 
	 * @return Will return a Content object (or subclass of Content) or Null if
	 *         no content object is found.
	 * @throws ContentManagerException
	 *             - if there is an error retrieving the content requested.
	 */
	ContentDTO getContentById(String version, String id) throws ContentManagerException;
	
	/**
	 * GetByIdPrefix Returns results that match a given id prefix for a
	 * specified version number.
	 * 
	 * @param version
	 *            - version of the content to search against.
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
	ResultsWrapper<ContentDTO> getByIdPrefix(String version, String idPrefix, int startIndex, int limit)
		throws ContentManagerException;

	/**
	 * Method to allow bulk search of content based on the type field.
	 * 
	 * @param version
	 *            - version of the content to search.
	 * @param fieldsToMatch
	 *            - Map which is used for field matching.
	 * @param startIndex
	 *            - the index of the first item to return.
	 * @param limit
	 *            - the maximum number of results to return.
	 * @return Results Wrapper containing results of the search.
	 * @throws ContentManagerException
	 *             - if there is an error retrieving the content requested.
	 */
	ResultsWrapper<ContentDTO> findByFieldNames(
			String version,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			Integer startIndex, Integer limit) throws ContentManagerException;

	/**
	 * The same as findByFieldNames but the results list is returned in a
	 * randomised order.
	 * 
	 * @param version
	 *            - version of the content to search.
	 * @param fieldsToMatch
	 *            - Map which is used for field matching.
	 * @param startIndex
	 *            - the index of the first item to return.
	 * @param limit
	 *            - the maximum number of results to return.
	 * @return Results Wrapper containing results of the search.
	 * @throws ContentManagerException
	 *             - if there is an error retrieving the content requested.
	 */
	ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(
			String version,
			Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			Integer startIndex, Integer limit) throws ContentManagerException;
	
	/**
	 * The same as findByFieldNames but the results list is returned in a
	 * randomised order.
	 * 
	 * @param version
	 *            - version of the content to search.
	 * @param fieldsToMatch
	 *            - Map which is used for field matching.
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
	ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(String version,
			Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatch, Integer startIndex,
			Integer limit, @Nullable Long randomSeed) throws ContentManagerException;
	
	/**
	 * Allows fullText search using the internal search provider.
	 * 
	 * The fields included in the search is determined by the content manager.
	 * 
	 * @param version
	 *            - version of the content to search.
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
	ResultsWrapper<ContentDTO> searchForContent(String version,
			String searchString, @Nullable Map<String, List<String>> fieldsThatMustMatch, Integer startIndex,
			Integer limit) throws ContentManagerException;

	/**
	 * Search for content by providing a set of tags.
	 * 
	 * @param version
	 *            - version of the content to search.
	 * @param tags
	 *            - set of tags that must match search results.
	 * @return Content objects that are associated with any of the tags
	 *         specified.
	 * @throws ContentManagerException
	 *             - if there is an error retrieving the content requested.
	 * @deprecated I don't think this is used any more.
	 */
	@Deprecated
	ResultsWrapper<ContentDTO> getContentByTags(String version, Set<String> tags) throws ContentManagerException;

	/**
	 * Method allows raw output to be retrieved for given files in the git
	 * repository. This is mainly so we can retrieve image files.
	 * 
	 * @param version
	 *            - The version of the content to retrieve
	 * @param filename
	 *            - The full path of the file you wish to retrieve.
	 * @return The output stream of the file contents
	 * @throws IOException
	 *             if failed IO occurs.
	 */
	ByteArrayOutputStream getFileBytes(String version, String filename)
		throws IOException;

	/**
	 * Provide a list of all possible versions from the underlying database
	 * 
	 * This method will only work on IContentManagers that store snapshots with
	 * attached version numbers.
	 * 
	 * @return the list of available versions
	 */
	List<String> listAvailableVersions();

	/**
	 * Get the latest version number from the database.
	 * 
	 * This is a low level operation that will return the latest version of the
	 * content that is known to the underlying database.
	 * 
	 * @return the latest version id
	 */
	String getLatestVersionId();

	/**
	 * A utility method to instruct the content manager to evict ALL of its
	 * cached data including data held within its search providers.
	 * 
	 * WARNING: this is a nuclear method. Re-indexing will definitely have to
	 * occur if you do this.
	 * 
	 */
	void clearCache();

	/**
	 * A utility method to instruct a content manager to evict a particular
	 * version of the content from its caches. This includes data held within
	 * its search providers.
	 * 
	 * @param version
	 *            - version to dump the cache of.
	 */
	void clearCache(String version);

	/**
	 * A method that will return an unordered set of tags registered for a
	 * particular version of the content.
	 * 
	 * @param version
	 *            - version to look up tag list for.
	 * @return A set of tags that have been already used in a particular version
	 *         of the content
	 * @throws ContentManagerException
	 *             - if there is an error retrieving the content requested.
	 */
	Set<String> getTagsList(String version) throws ContentManagerException;

	/**
	 * A method that will return an unordered set of all units registered for a
	 * particular version of the content.
	 * 
	 * @param version
	 *            - version to look up unit list for.
	 * @return A set of units that have been already used in a particular version
	 *         of the content
	 * @throws ContentManagerException
	 *             - if there is an error retrieving the content requested.
	 */
	Collection<String> getAllUnits(String version) throws ContentManagerException;

	/**
	 * Provides a Set of currently indexed and cached versions.
	 * 
	 * @return A set of all of the version id's which are currently available
	 *         without reindexing.
	 */
	Set<String> getCachedVersionList();

	/**
	 * Utility method that will check whether a version number supplied
	 * validates.
	 * 
	 * @param version
	 *            - version to validate.
	 * @return true if the version specified is valid and can potentially be
	 *         indexed, false if it cannot.
	 */
	boolean isValidVersion(String version);

	/**
	 * Will build and add the content for the version specified to the cache if it does not
	 * already exist. 
	 *  
	 * This method will throw an exception if there is a problem.
	 * 
	 * Note: it is the responsibility of the caller to manage the cache size.
	 * 
	 * @param version
	 *            - version to make sure exists.
	 * @throws ContentManagerException
	 *             - if there is an error retrieving the content requested.
	 */
	void ensureCache(String version) throws ContentManagerException;

	/**
	 * This method will compare two versions to determine which is the newer.
	 * 
	 * @param version1
	 *            - Version 1 to compare.
	 * @param version2
	 *            - Version 2 to compare.
	 * 
	 * @return a positive number if version1 is newer, zero if they are the
	 *         same, and a negative number if version 2 is newer.
	 */
	int compareTo(String version1, String version2);

	/**
	 * Get the problem map for a particular version.
	 * 
	 * @param version
	 *            - version of the content to get problem map for.
	 * @return the map containing the content objects with problems and
	 *         associated list of problem messages. Or null if there is no
	 *         problem index.
	 */
	Map<Content, List<String>> getProblemMap(String version);

	/**
	 * Augment content DTO with related content.
	 * 
	 * @param version
	 *            - the version of the content to use for the augmentation
	 *            operation.
	 * @param contentDTO
	 *            - the destination contentDTO which should have content
	 *            summaries created.
	 * @return fully populated contentDTO.
	 * @throws ContentManagerException
	 *             - if there is an error retrieving the content requested.
	 */
	ContentDTO populateContentSummaries(String version, ContentDTO contentDTO) throws ContentManagerException;

}
