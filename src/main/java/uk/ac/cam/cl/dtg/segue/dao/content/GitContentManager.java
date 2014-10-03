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

import static com.google.common.collect.Maps.immutableEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.elasticsearch.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Sets;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacFeaturedProfile;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.Media;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchOperationException;

/**
 * Implementation that specifically works with Content objects.
 * 
 */
public class GitContentManager implements IContentManager {
	private static final Logger log = LoggerFactory
			.getLogger(GitContentManager.class);

	private static final String CONTENT_TYPE = "content";

	private final Map<String, Map<String, Content>> gitCache;
	private final Map<String, Map<Content, List<String>>> indexProblemCache;
	private final Map<String, Set<String>> tagsList;
	private final Map<String, Map<String, String>> allUnits;

	private final GitDb database;
	private final ContentMapper mapper;
	private final ISearchProvider searchProvider;

	private boolean indexOnlyPublishedParentContent = false;
	
	/**
	 * Constructor for instantiating a new Git Content Manager Object.
	 * 
	 * @param database
	 *            - that the content Manager manages.
	 * @param searchProvider
	 *            - search provider that the content manager manages and
	 *            controls.
	 * @param contentMapper
	 *            - The utility class for mapping content objects.
	 */
	@Inject
	public GitContentManager(final GitDb database,
			final ISearchProvider searchProvider,
			final ContentMapper contentMapper) {
		this.database = database;
		this.mapper = contentMapper;
		this.searchProvider = searchProvider;

		this.gitCache = new ConcurrentHashMap<String, Map<String, Content>>();
		this.indexProblemCache = new ConcurrentHashMap<String, Map<Content, List<String>>>();
		this.tagsList = new ConcurrentHashMap<String, Set<String>>();
		this.allUnits = new ConcurrentHashMap<String, Map<String, String>>();

		searchProvider.registerRawStringFields(Lists.newArrayList(
				Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME));
	}

	/**
	 * FOR TESTING PURPOSES ONLY - Constructor for instantiating a new Git
	 * Content Manager Object.
	 * 
	 * @param database
	 *            - that the content Manager manages.
	 * @param searchProvider
	 *            - search provider that the content manager manages and
	 *            controls.
	 * @param contentMapper
	 *            - The utility class for mapping content objects.
	 * @param gitCache
	 *            - A manually constructed gitCache for testing purposes.
	 * @param indexProblemCache
	 *            - A manually constructed indexProblemCache for testing
	 *            purposes
	 */
	public GitContentManager(final GitDb database,
			final ISearchProvider searchProvider,
			final ContentMapper contentMapper,
			final Map<String, Map<String, Content>> gitCache,
			final Map<String, Map<Content, List<String>>> indexProblemCache) {
		this.database = database;
		this.mapper = contentMapper;
		this.searchProvider = searchProvider;

		this.gitCache = gitCache;
		this.indexProblemCache = indexProblemCache;
		this.tagsList = new ConcurrentHashMap<String, Set<String>>();
		this.allUnits = new ConcurrentHashMap<String, Map<String, String>>();
		
		searchProvider.registerRawStringFields(Lists.newArrayList(
				Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME));
	}

	@Override
	public final <T extends Content> String save(final T objectToSave) {
		throw new UnsupportedOperationException(
				"This method is not implemented yet - Git is a readonly data store at the moment.");
	}

	@Override
	public final Content getById(final String id, final String version) {
		if (null == id) {
			return null;
		}

		if (this.ensureCache(version)) {
			Content result = gitCache.get(version).get(id);
			if (null == result) {
				log.error("Failed to locate the content (" + id
						+ ") in the cache for version " + version);
			} else {
				log.debug("Loading content from cache: " + id);
			}
			return result;
		} else {
			return null;
		}
	}

	@Override
	public ResultsWrapper<ContentDTO> getByIdPrefix(final String idPrefix,
			final String version) {
		if (this.ensureCache(version)) {
			ResultsWrapper<String> searchHits = this.searchProvider
					.findByPrefix(version, CONTENT_TYPE, Constants.ID_FIELDNAME
							+ "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
							idPrefix);

			List<Content> searchResults = mapper
					.mapFromStringListToContentList(searchHits.getResults());

			return new ResultsWrapper<ContentDTO>(
					mapper.getDTOByDOList(searchResults),
					searchHits.getTotalResults());
		} else {
			log.error("Unable to ensure cache for requested version" + version);
			return null;
		}
	}

	@Override
	public final ResultsWrapper<ContentDTO> searchForContent(
			final String version, final String searchString,
			@Nullable final Map<String, List<String>> fieldsThatMustMatch) {
		if (this.ensureCache(version)) {
			ResultsWrapper<String> searchHits = searchProvider.fuzzySearch(
					version, CONTENT_TYPE, searchString, fieldsThatMustMatch,
					Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME,
					Constants.TAGS_FIELDNAME, Constants.VALUE_FIELDNAME,
					Constants.CHILDREN_FIELDNAME);

			List<Content> searchResults = mapper
					.mapFromStringListToContentList(searchHits.getResults());

			return new ResultsWrapper<ContentDTO>(
					mapper.getDTOByDOList(searchResults),
					searchHits.getTotalResults());
		} else {
			log.error("Unable to ensure cache for requested version" + version);
			return null;
		}
	}

	@Override
	public final ResultsWrapper<ContentDTO> findByFieldNames(
			final String version,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final Integer startIndex, final Integer limit) {
		ResultsWrapper<ContentDTO> finalResults = new ResultsWrapper<ContentDTO>();

		if (this.ensureCache(version)) {
			// TODO: Fix to allow sort order to be changed, currently it is hard
			// coded to sort ASC by title..
			Map<String, Constants.SortOrder> sortInstructions = Maps
					.newHashMap();

			sortInstructions.put(Constants.TITLE_FIELDNAME + "."
					+ Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
					Constants.SortOrder.ASC);

			ResultsWrapper<String> searchHits = searchProvider
					.paginatedMatchSearch(version, CONTENT_TYPE, fieldsToMatch,
							startIndex, limit, sortInstructions);

			// setup object mapper to use preconfigured deserializer module.
			// Required to deal with type polymorphism
			List<Content> result = mapper
					.mapFromStringListToContentList(searchHits.getResults());
			List<ContentDTO> contentDTOResults = mapper.getDTOByDOList(result);

			finalResults = new ResultsWrapper<ContentDTO>(contentDTOResults,
					searchHits.getTotalResults());
		}

		return finalResults;
	}

	@Override
	public final ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(
			final String version,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final Integer startIndex, final Integer limit) {
		return this.findByFieldNamesRandomOrder(version, fieldsToMatch, startIndex, limit, null);
	}
	
	@Override
	public final ResultsWrapper<ContentDTO> findByFieldNamesRandomOrder(
			final String version,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final Integer startIndex, final Integer limit, final Long randomSeed) {		
		ResultsWrapper<ContentDTO> finalResults = new ResultsWrapper<ContentDTO>();

		if (this.ensureCache(version)) {
			ResultsWrapper<String> searchHits;
			if (null == randomSeed) {
				searchHits = searchProvider.randomisedPaginatedMatchSearch(version, CONTENT_TYPE,
						fieldsToMatch, startIndex, limit);
			} else {
				searchHits = searchProvider.randomisedPaginatedMatchSearch(version, CONTENT_TYPE,
						fieldsToMatch, startIndex, limit, randomSeed);				
			}

			// setup object mapper to use preconfigured deserializer module.
			// Required to deal with type polymorphism
			List<Content> result = mapper
					.mapFromStringListToContentList(searchHits.getResults());

			List<ContentDTO> contentDTOResults = mapper.getDTOByDOList(result);

			finalResults = new ResultsWrapper<ContentDTO>(contentDTOResults,
					searchHits.getTotalResults());
		}

		return finalResults;
	}

	@Override
	public final ByteArrayOutputStream getFileBytes(final String version,
			final String filename) throws IOException {
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

		int version1Epoch = this.database.getCommitTime(version1);
		int version2Epoch = this.database.getCommitTime(version2);

		return version1Epoch - version2Epoch;
	}

	@Override
	public final String getLatestVersionId() {
		return database.pullLatestFromRemote();
	}

	@Override
	public final Set<String> getCachedVersionList() {
		return gitCache.keySet();
	}

	@Override
	public final void clearCache() {
		log.info("Clearing all content caches.");
		gitCache.clear();
		searchProvider.expungeEntireSearchCache();
		indexProblemCache.clear();
		tagsList.clear();
		allUnits.clear();
	}

	@Override
	public final void clearCache(final String version) {
		Validate.notBlank(version);

		if (gitCache.containsKey(version)) {
			gitCache.remove(version);
			searchProvider.expungeIndexFromSearchCache(version);
			indexProblemCache.remove(version);
			tagsList.remove(version);
			allUnits.remove(version);
		}
	}

	@Override
	public final ResultsWrapper<ContentDTO> getContentByTags(
			final String version, final Set<String> tags) {
		if (null == version || null == tags) {
			return null;
		}

		if (this.ensureCache(version)) {
			ResultsWrapper<String> searchResults = this.searchProvider
					.termSearch(version, CONTENT_TYPE, tags, "tags");

			List<Content> contentResults = mapper
					.mapFromStringListToContentList(searchResults.getResults());

			List<ContentDTO> contentDTOResults = mapper
					.getDTOByDOList(contentResults);

			return new ResultsWrapper<ContentDTO>(contentDTOResults,
					searchResults.getTotalResults());
		} else {
			log.error("Cache not found. Failed to build cache with version: "
					+ version);
			return null;
		}
	}

	@Override
	public final Set<String> getTagsList(final String version) {
		Validate.notBlank(version);

		this.ensureCache(version);

		if (!tagsList.containsKey(version)) {
			log.warn("The version requested does not exist in the tag list.");
			return null;
		}

		return tagsList.get(version);
	}
	
	@Override
	public final Collection<String> getAllUnits(final String version) {
		Validate.notBlank(version);

		this.ensureCache(version);

		if (!allUnits.containsKey(version)) {
			log.warn("The version requested does not exist in the set of all units.");
			return null;
		}

		return allUnits.get(version).values();
	}

	@Override
	public final boolean ensureCache(final String version) {
		if (version == null) {
			return false;
		}

		if (!gitCache.containsKey(version)) {
			synchronized (this) {
				if (!gitCache.containsKey(version)) {
					if (database.verifyCommitExists(version)) {
						log.debug("Rebuilding cache as sha does not exist in hashmap");
						buildGitContentIndex(version);
						
						// may as well spawn a new thread to do the validation work now.
						Thread validationJob = new Thread() {
							@Override
							public void run() {
								validateReferentialIntegrity(version);
							}
						};
						validationJob.start();
						
						buildSearchIndexFromLocalGitIndex(version);
					} else {
						log.warn("Unable find the commit (" + version
								+ ") in git to ensure the cache");
						return false;
					}
				}
			}
		}

		boolean searchIndexed = searchProvider.hasIndex(version);
		if (!searchIndexed) {
			log.warn("Search does not have a valid index for the " + version
					+ " version of the content");
			synchronized (this) {
				this.buildSearchIndexFromLocalGitIndex(version);
			}
		}

		return gitCache.containsKey(version) && searchIndexed;
	}

	@Override
	public final Map<Content, List<String>> getProblemMap(final String version) {
		return indexProblemCache.get(version);
	}

	/**
	 * Augment content DTO with related content.
	 * 
	 * @param version
	 *            - the version of the content to use for the augmentation
	 *            process.
	 * 
	 * @param contentDTO
	 *            - the destination contentDTO which should have content
	 *            summaries created.
	 * @return fully populated contentDTO.
	 */
	@Override
	public ContentDTO populateContentSummaries(final String version,
			final ContentDTO contentDTO) {
		if (contentDTO.getRelatedContent() == null
				|| contentDTO.getRelatedContent().isEmpty()) {
			return contentDTO;
		}

		// build query the db to get full content information
		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMap 
			= new HashMap<Map.Entry<Constants.BooleanOperator, String>, List<String>>();

		List<String> relatedContentIds = Lists.newArrayList();
		for (ContentSummaryDTO summary : contentDTO.getRelatedContent()) {
			relatedContentIds.add(summary.getId());
		}

		fieldsToMap.put(Maps.immutableEntry(Constants.BooleanOperator.OR,
				Constants.ID_FIELDNAME + '.'
						+ Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX),
				relatedContentIds);

		ResultsWrapper<ContentDTO> results = this.findByFieldNames(version,
				fieldsToMap, 0, relatedContentIds.size());

		List<ContentSummaryDTO> relatedContentDTOs = Lists.newArrayList();

		for (ContentDTO relatedContent : results.getResults()) {
			ContentSummaryDTO summary = this.mapper.getAutoMapper().map(
					relatedContent, ContentSummaryDTO.class);
			relatedContentDTOs.add(summary);
		}

		contentDTO.setRelatedContent(relatedContentDTOs);

		return contentDTO;
	}

	/**
	 * This method will send off the information in the git cache to the search
	 * provider for indexing.
	 * 
	 * @param sha
	 *            - the version in the git cache to send to the search provider.
	 */
	private synchronized void buildSearchIndexFromLocalGitIndex(final String sha) {
		if (!gitCache.containsKey(sha)) {
			log.error("Unable to create search index as git cache does not exist locally");
			return;
		}

		if (this.searchProvider.hasIndex(sha)) {
			log.info("Search index has already been updated by"
					+ " another thread. No need to reindex. Aborting...");
			return;
		}

		log.info("Building search index for: " + sha);

		// setup object mapper to use pre-configured deserializer module.
		// Required to deal with type polymorphism
		List<Map.Entry<String, String>> thingsToIndex = Lists.newArrayList();
		ObjectMapper objectMapper = mapper.getContentObjectMapper();
		for (Content content : gitCache.get(sha).values()) {
			try {
				thingsToIndex.add(immutableEntry(content.getId(), objectMapper.writeValueAsString(content)));
			} catch (JsonProcessingException e) {
				log.error("Unable to serialize content object "
						+ "for indexing with the search provider.", e);
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
	 * This method will populate the internal gitCache based on the content
	 * object files found for a given SHA.
	 * 
	 * Currently it only looks for json files in the repository.
	 * 
	 * @param sha
	 *            - the version to index.
	 */
	private synchronized void buildGitContentIndex(final String sha) {
		// This set of code only needs to happen if we have to read from git
		// again.
		if (null == sha || gitCache.get(sha) != null) {
			return;
		}

		// iterate through them to create content objects
		Repository repository = database.getGitRepository();

		try {
			ObjectId commitId = repository.resolve(sha);

			if (null == commitId) {
				log.error("Failed to buildGitIndex - Unable to locate resource with SHA: "
						+ sha);
				return;
			}

			Map<String, Content> shaCache = new HashMap<String, Content>();

			TreeWalk treeWalk = database.getTreeWalk(sha, ".json");
			log.info("Populating git content cache based on sha " + sha
					+ " ...");

			// Traverse the git repository looking for the .json files
			while (treeWalk.next()) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
				loader.copyTo(out);

				// setup object mapper to use preconfigured deserializer
				// module. Required to deal with type polymorphism
				ObjectMapper objectMapper = mapper.getContentObjectMapper();

				Content content = null;
				try {
					content = (Content) objectMapper.readValue(out.toString(),
							ContentBase.class);

					// check if we only want to index published content
					if (indexOnlyPublishedParentContent && !content.getPublished()) {
						log.debug("Skipping unpublished content: " + content.getId());
						continue;
					}
					
					content = this.augmentChildContent(content,
							treeWalk.getPathString(), null);					
					
					if (null != content) {
						// add children (and parent) from flattened Set to
						// cache if they have ids
						for (Content flattenedContent : this
								.flattenContentObjects(content)) {
							if (flattenedContent.getId() == null) {
								continue;
							}

							// check if we have seen this key before if
							// we have then we don't want to add it
							// again
							if (!shaCache.containsKey(flattenedContent.getId())) {
								// It must be new so we can add it
								log.debug("Loading into cache: "
										+ flattenedContent.getId() + "("
										+ flattenedContent.getType() + ")"
										+ " from " + treeWalk.getPathString());
								shaCache.put(flattenedContent.getId(),
										flattenedContent);
								registerTagsWithVersion(sha,
										flattenedContent.getTags());
								
								
								// If this is a numeric question, extract any 
								// units from its answers.
								
								if (flattenedContent instanceof IsaacNumericQuestion) {
									registerUnitsWithVersion(sha, 
											(IsaacNumericQuestion) flattenedContent);									
								}
									
								continue; // our work here is done (reduces
											// nesting compared to else)
							}

							// shaCache contains key already, compare the
							// content
							if (shaCache.get(flattenedContent.getId()).equals(
									flattenedContent)) {
								// content is the same therefore it is just
								// reuse of a content object so that is
								// fine.
								log.debug("Resource (" + content.getId()
										+ ") already seen in cache. Skipping "
										+ treeWalk.getPathString());
								continue; 
							}

							// Otherwise, duplicate IDs with different content,
							// therefore log an error
							log.warn("Resource with duplicate ID ("
									+ content.getId()
									+ ") detected in cache. Skipping "
									+ treeWalk.getPathString());
							this.registerContentProblem(
									sha,
									flattenedContent,
									"Index failure - Duplicate ID found in file "
											+ treeWalk.getPathString()
											+ " and "
											+ shaCache.get(
													flattenedContent.getId())
													.getCanonicalSourceFile());
						}
					}
				} catch (JsonMappingException e) {
					log.warn(
							"Unable to parse the json file found "
									+ treeWalk.getPathString()
									+ " as a content object. Skipping file...",
							e);
					Content dummyContent = new Content();
					dummyContent.setCanonicalSourceFile(treeWalk
							.getPathString());
					this.registerContentProblem(
							sha,
							dummyContent,
							"Index failure - Unable to parse json file found - "
									+ treeWalk.getPathString()
									+ ". The following error occurred: "
									+ e.getMessage());
				}
			}

			// add all of the work we have done to the git cache.
			gitCache.put(sha, shaCache);
			repository.close();
			log.debug("Tags available " + tagsList);
			log.debug("All units: " + allUnits);
			log.info("Git content cache population for " + sha + " completed!");
		} catch (IOException e) {
			log.error("IOException while trying to access git repository. ", e);
		}
	}

	/**
	 * Augments all child objects recursively to include additional information.
	 * 
	 * This should be done before saving to the local gitCache in memory
	 * storage.
	 * 
	 * This method will also attempt to reconstruct object id's of nested
	 * content such that they are unique to the page by default.
	 * 
	 * @param content
	 *            - content to augment
	 * @param canonicalSourceFile
	 *            - source file to add to child content
	 * @param parentId
	 *            - used to construct nested ids for child elements.
	 * @return Content object with new reference
	 */
	private Content augmentChildContent(final Content content,
			final String canonicalSourceFile, @Nullable final String parentId) {
		if (null == content) {
			return null;
		}

		// If this object is of type question then we need to give it a random
		// id if it doesn't have one.
		if (content instanceof Question && content.getId() == null) {
			log.warn("Found question without id " + content.getTitle() + " "
					+ canonicalSourceFile);
		}

		// Try to figure out the parent ids.
		String newParentId = null;
		if (null == parentId && content.getId() != null) {
			newParentId = content.getId();
		} else {
			if (content.getId() != null) {
				newParentId = parentId + Constants.ID_SEPARATOR
						+ content.getId();
			} else {
				newParentId = parentId;
			}
		}

		content.setCanonicalSourceFile(canonicalSourceFile);

		if (!content.getChildren().isEmpty()) {
			for (ContentBase cb : content.getChildren()) {
				if (cb instanceof Content) {
					Content c = (Content) cb;

					this.augmentChildContent(c, canonicalSourceFile,
							newParentId);
				}
			}
		}

		// TODO: hack to get hints to apply as children
		if (content instanceof Question) {
			Question question = (Question) content;
			if (question.getHints() != null) {
				for (ContentBase cb : question.getHints()) {
					Content c = (Content) cb;
					this.augmentChildContent(c, canonicalSourceFile,
							newParentId);
				}
			}

			// Augment question answers
			if (question.getAnswer() != null) {
				Content answer = (Content) question.getAnswer();
				if (answer.getChildren() != null) {
					for (ContentBase cb : answer.getChildren()) {
						Content c = (Content) cb;
						this.augmentChildContent(c, canonicalSourceFile,
								newParentId);
					}
				}
			}
		}
		
		// TODO: we need to fix this as this is an isaac thing in segue land.
		if (content instanceof IsaacFeaturedProfile) {
			IsaacFeaturedProfile profile = (IsaacFeaturedProfile) content;
			if (profile.getImage() != null) {
				this.augmentChildContent(profile.getImage(), canonicalSourceFile, newParentId);
			}
		}

		if (content instanceof Media) {
			Media media = (Media) content;
			if (media.getSrc() != null && !media.getSrc().startsWith("http")) {
				String newPath = FilenameUtils.normalize(
						FilenameUtils.getPath(canonicalSourceFile)
								+ media.getSrc(), true);
				media.setSrc(newPath);
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
	 * This method will attempt to traverse the cache to ensure that all content
	 * references are valid.
	 * 
	 * @param sha
	 *            version to validate integrity of.
	 * @return True if we are happy with the integrity of the git repository,
	 *         False if there is something wrong.
	 */
	private boolean validateReferentialIntegrity(final String sha) {
		log.info("Starting content Validation.");
		Set<Content> allObjectsSeen = new HashSet<Content>();
		Set<String> expectedIds = new HashSet<String>();
		Set<String> definedIds = new HashSet<String>();
		Set<String> missingContent = new HashSet<String>();
		Map<String, Content> whoAmI = new HashMap<String, Content>();

		// Build up a set of all content (and content fragments for validation)
		for (Content c : gitCache.get(sha).values()) {
			if (c instanceof IsaacSymbolicQuestion) {
				// do not validate these questions for now.
				continue;
			}
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
						+ "This content will always be automatically marked as incorrect");

				log.error("Invalid content item detected: The object with ID (" + id
						+ ") has both children and a value.");
			}

			// content type specific checks
			if (c instanceof Media) {
				Media f = (Media) c;

				if (f.getSrc() != null && !f.getSrc().startsWith("http")
						&& !database.verifyGitObject(sha, f.getSrc())) {
					this.registerContentProblem(
							sha,
							c,
							"Unable to find Image: " + f.getSrc()
									+ " in Git. Could the reference be incorrect? SourceFile is "
									+ c.getCanonicalSourceFile());
				} 

				// check that there is some alt text.
				if (f.getAltText() == null || f.getAltText().isEmpty()) {
					this.registerContentProblem(sha, c,
							"No altText attribute set for media element: " + f.getSrc()
									+ " in Git source file " + c.getCanonicalSourceFile());
				}
			}
			if (c instanceof Question && c.getId() == null) {
				this.registerContentProblem(sha, c, "Question: " + c.getTitle() + " in "
						+ c.getCanonicalSourceFile() + " found without a unqiue id. "
						+ "This question cannot be logged correctly.");
			}
			
			// TODO: remove reference to isaac specific types from here.
			if (c instanceof ChoiceQuestion
					&& !(c.getType().equals("isaacQuestion") || c.getType().equals("isaacSymbolicQuestion"))) {
				ChoiceQuestion question = (ChoiceQuestion) c;

				if (question.getChoices() == null || question.getChoices().isEmpty()) {
					this.registerContentProblem(sha, question, "Question: " + question.getId() + " in "
							+ question.getCanonicalSourceFile() + " found without any choice metadata. "
							+ "This question will always be automatically " + "marked as incorrect");
				} else {
					boolean correctOptionFound = false;
					for (Choice choice : question.getChoices()) {
						if (choice.isCorrect()) {
							correctOptionFound = true;
						}
					}

					if (!correctOptionFound) {
						this.registerContentProblem(sha, question, "Question: " + question.getId() + " in "
								+ question.getCanonicalSourceFile() + " found without a correct answer. "
								+ "This question will always be automatically marked " + "as incorrect");
					}
				}
			}

			// Find quantities with values that cannot be parsed as numbers.
			if (c instanceof IsaacNumericQuestion) {
				IsaacNumericQuestion q = (IsaacNumericQuestion) c;
				for (Choice choice : q.getChoices()) {
					if (choice instanceof Quantity) {
						Quantity quantity = (Quantity) choice;

						try {
							Double.parseDouble(quantity.getValue());
						} catch (NumberFormatException e) {
							this.registerContentProblem(sha, c, "Quantity (" + quantity.getValue()
									+ ") found with value that" + " cannot be interpreted as a number in "
									+ c.getCanonicalSourceFile()
									+ ". Users will never be able to give a correct answer.");
						}
					}
				}

			}

			if (c instanceof IsaacQuestionPage && (c.getLevel() == null || c.getLevel() == 0)) {
				this.registerContentProblem(sha, c,
						"Level error! - Question: " + c.getId() + " in " + c.getCanonicalSourceFile()
								+ " has the level field set to: " + c.getLevel());
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
		log.info("Validation processing complete. There are " + this.indexProblemCache.get(sha).size()
				+ " files with content problems");
		
		return false;
	}

	/**
	 * Unpack the content objects into one big set. Useful for validation but
	 * could produce a very large set
	 * 
	 * @param content
	 *            content object to flatten
	 * @return Set of content objects comprised of all children and the parent.
	 */
	private Set<Content> flattenContentObjects(final Content content) {
		Set<Content> setOfContentObjects = new HashSet<Content>();
		if (!content.getChildren().isEmpty()) {

			List<ContentBase> children = content.getChildren();

			for (ContentBase child : children) {
				setOfContentObjects.add((Content) child);
				setOfContentObjects
						.addAll(flattenContentObjects((Content) child));
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
	private synchronized void registerTagsWithVersion(final String version,
			final Set<String> tags) {
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
	 * Helper function to accumulate the set of all units used in
	 * numeric question answers.
	 * 
	 * @param version
	 * 			- version to register the units for.
	 * @param q
	 * 			- numeric question from which to extract units.
	 */
	private synchronized void registerUnitsWithVersion(final String version, 
			final IsaacNumericQuestion q) {		
		
		HashMap<String, String> newUnits = Maps.newHashMap();
		
		for (Choice c: q.getChoices()) {
			if (c instanceof Quantity) {
				Quantity quantity = (Quantity) c;
				
				if (!quantity.getUnits().isEmpty()) {
					String units = quantity.getUnits();
					String cleanKey = units.replace("\t", "").replace("\n", "").replace(" ", "");

					// May overwrite previous entry, doesn't matter as there is no mechanism by which to choose a winner
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
	 *            - Partial content object to represent the object that has
	 *            problems.
	 * @param message
	 *            - Error message to associate with the problem file / content.
	 */
	private synchronized void registerContentProblem(final String version,
			final Content c, final String message) {
		Validate.notNull(c);

		// try and make sure each dummy content object has a title
		if (c.getTitle() == null) {
			c.setTitle(Paths.get(c.getCanonicalSourceFile()).getFileName()
					.toString());
		}

		if (!indexProblemCache.containsKey(version)) {
			indexProblemCache
					.put(version, new HashMap<Content, List<String>>());
		}

		if (!indexProblemCache.get(version).containsKey(c)) {
			indexProblemCache.get(version).put(c, new ArrayList<String>());
		}

		indexProblemCache.get(version).get(c).add(message.replace("_", "\\_"));
	}

	@Override
	public void setIndexRestriction(final boolean loadOnlyPublishedContent) {
		this.indexOnlyPublishedParentContent = loadOnlyPublishedContent;
	}
}
