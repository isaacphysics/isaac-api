package uk.ac.cam.cl.dtg.segue.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.Media;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;

/**
 * Implementation that specifically works with Content objects.
 * 
 */
public class GitContentManager implements IContentManager {
	private static final Logger log = LoggerFactory
			.getLogger(GitContentManager.class);

	private static final String CONTENT_TYPE = "content";

	private static Map<String, Map<String, Content>> gitCache 
		= new ConcurrentHashMap<String, Map<String, Content>>();
	private static final Map<String, Map<Content, List<String>>> indexProblemCache 
		= new ConcurrentHashMap<String, Map<Content, List<String>>>();
	private static final Map<String, Set<String>> tagsList 
		= new ConcurrentHashMap<String, Set<String>>();

	private final GitDb database;
	private final ContentMapper mapper;
	private final ISearchProvider searchProvider;

	/**
	 * Constructor for instanciating a new Git Content Manager Object. 
	 * @param database - that the content Manager manages.
	 * @param searchProvider - search provider that the content manager manages and controls.
	 * @param contentMapper - The utility class for mapping content objects.
	 */
	@Inject
	public GitContentManager(final GitDb database, final ISearchProvider searchProvider,
			final ContentMapper contentMapper) {
		this.database = database;
		this.mapper = contentMapper;
		this.searchProvider = searchProvider;

		searchProvider.registerRawStringFields(Lists.newArrayList(
				Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME));
	}

	/**
	 * FOR TESTING PURPOSES ONLY - Constructor for instanciating a new Git Content Manager Object. 
	 * @param database - that the content Manager manages.
	 * @param searchProvider - search provider that the content manager manages and controls.
	 * @param contentMapper - The utility class for mapping content objects.
	 * @param gitCache - A manually constructed gitCache for testing purposes.
	 */
	public GitContentManager(final GitDb database, final ISearchProvider searchProvider,
			final ContentMapper contentMapper, Map<String, Map<String, Content>> gitCache) {
		this.database = database;
		this.mapper = contentMapper;
		this.searchProvider = searchProvider;
		GitContentManager.gitCache = gitCache;
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
				log.info("Failed to locate the content (" + id
						+ ") in the cache for version " + version);
			} else {
				log.info("Loading content from cache: " + id);
			}
			return result;
		} else {
			return null;
		}
	}

	@Override
	public final ResultsWrapper<Content> searchForContent(final String version,
			final String searchString,
			@Nullable final Map<String, List<String>> fieldsThatMustMatch) {
		if (this.ensureCache(version)) {
			ResultsWrapper<String> searchHits = searchProvider.fuzzySearch(
					version, CONTENT_TYPE, searchString, fieldsThatMustMatch,
					Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME,
					Constants.TAGS_FIELDNAME, Constants.VALUE_FIELDNAME,
					Constants.CHILDREN_FIELDNAME);

			// setup object mapper to use preconfigured deserializer module.
			// Required to deal with type polymorphism
			ObjectMapper objectMapper = mapper.getContentObjectMapper();

			List<Content> searchResults = new ArrayList<Content>();
			for (String hit : searchHits.getResults()) {
				try {
					searchResults.add((Content) objectMapper.readValue(hit,
							ContentBase.class));
				} catch (IOException e) {
					log.error("Error while trying to search for "
							+ searchString + " in version " + version, e);
				}
			}
			return new ResultsWrapper<Content>(searchResults,
					searchHits.getTotalResults());
		} else {
			log.error("Unable to ensure cache for requested version" + version);
			return null;
		}

	}

	@Override
	public final ResultsWrapper<Content> findByFieldNames(
			final String version,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final Integer startIndex, final Integer limit) {
		ResultsWrapper<Content> finalResults = new ResultsWrapper<Content>();

		if (this.ensureCache(version)) {
			// TODO: Fix to allow sort order to be changed, currently it is hard
			// coded to sort ASC by title..
			Map<String, Constants.SortOrder> sortInstructions
				= new HashMap<String, Constants.SortOrder>();

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
			
			finalResults = new ResultsWrapper<Content>(result,
					searchHits.getTotalResults());
		}

		return finalResults;
	}

	@Override
	public final ResultsWrapper<Content> findByFieldNamesRandomOrder(
			final String version,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final Integer startIndex, final Integer limit) {
		ResultsWrapper<Content> finalResults = new ResultsWrapper<Content>();

		if (this.ensureCache(version)) {
			ResultsWrapper<String> searchHits = searchProvider
					.randomisedPaginatedMatchSearch(version, CONTENT_TYPE,
							fieldsToMatch, startIndex, limit);

			// setup object mapper to use preconfigured deserializer module.
			// Required to deal with type polymorphism
			List<Content> result = mapper
					.mapFromStringListToContentList(searchHits.getResults());
			finalResults = new ResultsWrapper<Content>(result,
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
		log.info("Clearing Git content cache.");
		gitCache.clear();
		searchProvider.expungeEntireSearchCache();
		indexProblemCache.clear();
		tagsList.clear();
	}

	@Override
	public final void clearCache(final String version) {
		Validate.notBlank(version);

		if (gitCache.containsKey(version)) {
			gitCache.remove(version);
			searchProvider.expungeIndexFromSearchCache(version);
			indexProblemCache.remove(version);
			tagsList.remove(version);
		}
	}

	@Override
	public final ResultsWrapper<Content> getContentByTags(final String version,
			final Set<String> tags) {
		if (null == version || null == tags) {
			return null;
		}

		if (this.ensureCache(version)) {
			ResultsWrapper<String> searchResults = this.searchProvider
					.termSearch(version, CONTENT_TYPE, tags, "tags");

			List<Content> contentResults = mapper
					.mapFromStringListToContentList(searchResults.getResults());

			return new ResultsWrapper<Content>(contentResults,
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
	public final boolean ensureCache(final String version) {
		if (version == null) {
			return false;
		}
		
		if (!gitCache.containsKey(version)) {
			if (database.verifyCommitExists(version)) {
				log.info("Rebuilding cache as sha does not exist in hashmap");
				buildGitContentIndex(version);
				buildSearchIndexFromLocalGitIndex(version);
				validateReferentialIntegrity(version);
			} else {
				log.warn("Unable find the commit (" + version
						+ ") in git to ensure the cache");
				return false;
			}
		}

		boolean searchIndexed = searchProvider.hasIndex(version);

		if (!searchIndexed) {
			log.warn("Search does not have a valid index for the " + version
					+ " version of the content");
			this.buildSearchIndexFromLocalGitIndex(version);
		}

		return gitCache.containsKey(version) && searchIndexed;
	}

	@Override
	public final Map<Content, List<String>> getProblemMap(final String version) {
		return indexProblemCache.get(version);
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
		for (Content content : gitCache.get(sha).values()) {
			// setup object mapper to use pre-configured deserializer module.
			// Required to deal with type polymorphism
			ObjectMapper objectMapper = mapper.getContentObjectMapper();

			try {
				this.searchProvider.indexObject(sha, CONTENT_TYPE,
						objectMapper.writeValueAsString(content),
						content.getId());
			} catch (JsonProcessingException e) {
				log.error("Unable to serialize content object "
						+ "for indexing with the search provider.");
				e.printStackTrace();
			}
		}
		log.info("Search index built for: " + sha);
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
				ObjectLoader loader = repository.open(treeWalk
						.getObjectId(0));
				loader.copyTo(out);

				// setup object mapper to use preconfigured deserializer
				// module. Required to deal with type polymorphism
				ObjectMapper objectMapper = mapper.getContentObjectMapper();

				Content content = null;
				try {
					content = (Content) objectMapper.readValue(
							out.toString(), ContentBase.class);
					content = this.augmentChildContent(content,
							treeWalk.getPathString(), null);

					if (null != content) {
						// add children (and parent) from flattened Set to
						// cache if they have ids
						for (Content flattenedContent : this
								.flattenContentObjects(content)) {
							if (flattenedContent.getId() != null) {
								// check if we have seen this key before if
								// we have then we don't want to add it
								// again
								if (shaCache.containsKey(flattenedContent
										.getId())) {
									// if the key is the same but the
									// content is different then something
									// has gone wrong - log an error
									if (!shaCache.get(
											flattenedContent.getId())
											.equals(flattenedContent)) {
										// log an error if we find that
										// there are duplicate ids and the
										// content is different.
										log.warn("Resource with duplicate ID ("
												+ content.getId()
												+ ") detected in cache. Skipping "
												+ treeWalk.getPathString());
										this.registerContentProblem(
												sha,
												flattenedContent,
												"Index failure - Duplicate ID found in file "
														+ treeWalk
																.getPathString()
														+ " and "
														+ shaCache
																.get(flattenedContent
																		.getId())
																.getCanonicalSourceFile());
									}
									// if the content is the same then it is
									// just reuse of a content object so
									// that is fine.
									else {
										log.info("Resource ("
												+ content.getId()
												+ ") already seen in cache. Skipping "
												+ treeWalk.getPathString());
									}
								}
								// It must be new so we can add it
								else {
									log.debug("Loading into cache: "
											+ flattenedContent.getId()
											+ "("
											+ flattenedContent.getType()
											+ ")" + " from "
											+ treeWalk.getPathString());
									shaCache.put(flattenedContent.getId(),
											flattenedContent);
									registerTagsWithVersion(sha,
											flattenedContent.getTags());
								}
							}
						}

					}
				} catch (JsonMappingException e) {
					log.warn("Unable to parse the json file found "
							+ treeWalk.getPathString()
							+ " as a content object. Skipping file...", e);
					Content dummyContent = new Content();
					dummyContent.setCanonicalSourceFile(treeWalk
							.getPathString());
					this.registerContentProblem(sha, dummyContent,
							"Index failure - Unable to parse json file found - "
									+ treeWalk.getPathString()
									+ ". The following error occurred: "
									+ e.getMessage());
				}
			}

			// add all of the work we have done to the git cache.
			gitCache.put(sha, shaCache);
			repository.close();
			log.info("Tags available " + tagsList);
			log.info("Git content cache population for " + sha
					+ " completed!");
		} catch (IOException e) {
			log.error(
					"IOException while trying to access git repository. ",
					e);
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
		if (null == parentId) {
			if (content.getId() != null) {
				newParentId = content.getId();
			}
		} else {
			newParentId = parentId + '.' + content.getId();
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
		}

		// TODO Improve Hack to convert image source into something that the api
		// can use to locate the specific image in the repository.
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
			content.setId(parentId + '.' + content.getId());
		}

		return content;
	}

	/**
	 * This method will attempt to traverse the cache to ensure that all content
	 * references are valid. TODO: Convert this into a more useful method.
	 * Currently it is a hack to flag bad references.
	 * 
	 * @param sha version to validate integrity of.
	 * @return True if we are happy with the integrity of the git repository,
	 *         False if there is something wrong.
	 */
	private boolean validateReferentialIntegrity(final String sha) {
		Set<Content> allObjectsSeen = new HashSet<Content>();

		Set<String> expectedIds = new HashSet<String>();
		Set<String> definedIds = new HashSet<String>();
		Set<String> missingContent = new HashSet<String>();

		Map<String, Content> whoAmI = new HashMap<String, Content>();

		// Build up a set of all content (and content fragments for validation)
		for (Content c : gitCache.get(sha).values()) {
			allObjectsSeen.addAll(this.flattenContentObjects(c));
		}

		// Start looking for issues in the flattened content data
		for (Content c : allObjectsSeen) {
			// add the id to the list of defined ids if one is set for this
			// content object
			if (c.getId() != null) {
				definedIds.add(c.getId());
			}

			// add the ids to the list of expected ids if we see a list of
			// referenced content
			if (c.getRelatedContent() != null) {
				expectedIds.addAll(c.getRelatedContent());
				// record which content object was referencing which ID
				for (String id : c.getRelatedContent()) {
					whoAmI.put(id, c);
				}
			}

			// content type specific checks
			if (c instanceof Media) {
				Media f = (Media) c;

				if (f.getSrc() != null && !f.getSrc().startsWith("http")
						&& !database.verifyGitObject(sha, f.getSrc())) {
					log.warn("Unable to find Image: "
							+ f.getSrc()
							+ " in Git. Could the reference be incorrect? SourceFile is "
							+ c.getCanonicalSourceFile());
					this.registerContentProblem(
							sha,
							c,
							"Unable to find Image: "
									+ f.getSrc()
									+ " in Git. Could the reference be incorrect? SourceFile is "
									+ c.getCanonicalSourceFile());
				} else {
					log.debug("Verified image " + f.getSrc()
							+ " exists in git.");
				}
			}

			// TODO: remove reference to isaac specific type from here.
			if (c instanceof ChoiceQuestion && !(c.getType().equals("isaacQuestion"))) {
				ChoiceQuestion question = (ChoiceQuestion) c;
				if (question.getChoices() == null || question.getChoices().isEmpty()) {
					log.warn("Choice question: " + question.getId() + " in " 
							+ question.getCanonicalSourceFile() + " found without any choices - "
							+ "this will mean users will always get it wrong.");
					this.registerContentProblem(sha, question, "Question: " + question.getId() 
							+ " in " + question.getCanonicalSourceFile() 
							+ " found without any choice metadata. "
							+ "This question will always be automatically marked as incorrect");
				} else {
					boolean correctOptionFound = false;
					for (Choice choice : question.getChoices()) {
						if (choice.isCorrect()) {
							correctOptionFound = true;
						}
					}
					
					if (!correctOptionFound) {
						this.registerContentProblem(sha, question, "Question: " + question.getId() 
								+ " in " + question.getCanonicalSourceFile() 
								+ " found without a correct answer. "
								+ "This question will always be automatically marked as incorrect");
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
				this.registerContentProblem(sha, whoAmI.get(id),
						"This id (" + id + ") was referenced by "
								+ whoAmI.get(id).getCanonicalSourceFile()
								+ " but the content with that "
								+ "ID cannot be found.");
			}

			log.error("Referential integrity broken for (" + expectedIds.size()
					+ ") related Content items. "
					+ "The following ids are referenced but do not exist: "
					+ expectedIds.toString());
			return false;
		}
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
	private void registerTagsWithVersion(final String version,
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
	private void registerContentProblem(final String version, final Content c,
			final String message) {
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

		indexProblemCache.get(version).get(c).add(message);
	}
}
