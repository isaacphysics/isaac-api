/**
 * Copyright 2014 Nick Rogers
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
package uk.ac.cam.cl.dtg.segue.dao;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Response.Status;

import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.reflect.Whitebox;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.Media;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

/**
 * Test class for the GitContentManager class.
 * 
 */
@PowerMockIgnore({"javax.ws.*"})
public class GitContentManagerTest {
	private GitDb database;
	private ISearchProvider searchProvider;
	private ContentMapper contentMapper;

	private GitContentManager defaultGCM;

	private static final String INITIAL_VERSION = "0b72984c5eff4f53604fe9f1c724d3f387799db9";

	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		this.database = createMock(GitDb.class);
		this.searchProvider = createMock(ISearchProvider.class);
		this.contentMapper = createMock(ContentMapper.class);

		this.defaultGCM = new GitContentManager(database, searchProvider,
				contentMapper);
	}

	/**
	 * Test that the compareTo method returns the correct result when V1 is
	 * newer than V2.
	 */
	@Test
	public void compareTo_checkV1NewerThanV2_checkPositiveNumberReturned() {
		final int v1Year = 2010;
		final int v2Year = 2000;
		assertTrue(compareTo_getResult(v1Year, v2Year) > 0);
	}

	/**
	 * Test that the compareTo method returns the correct result when V2 is
	 * newer than V1.
	 */
	@Test
	public void compareTo_checkV2NewerThanV1_checkNegativeNumberReturned() {
		final int v1Year = 2000;
		final int v2Year = 2010;
		assertTrue(compareTo_getResult(v1Year, v2Year) < 0);
	}

	/**
	 * Test that the compareTo method returns the correct result when V1 is the
	 * same age as V2.
	 */
	@Test
	public void compareTo_checkV2SameAgeAsV1_checkZeroReturned() {
		final int v1Year = 2000;
		final int v2Year = 2000;
		assertTrue(compareTo_getResult(v1Year, v2Year) == 0);
	}

	/**
	 * This method will evaluate the result of the compareTo method using the
	 * years provided as the arguments for timestamps. The rest of the timestamp
	 * will read 1st January 00:00:00
	 * 
	 * @param v1Year
	 *            - The year for v1
	 * @param v2Year
	 *            - The year for v2
	 * @return the result of comparing the two dates using the
	 *         GitContentManager.compareTo method
	 */
	private int compareTo_getResult(final int v1Year, final int v2Year) {
		final long millisecondsPerSecond = 1000L;
		final String v1Hash = "V1";
		final String v2Hash = "V2";

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(v1Year, 0, 1, 0, 0, 0);
		int v1Date = (int) (cal.getTimeInMillis() / millisecondsPerSecond);
		cal.set(v2Year, 0, 1, 0, 0, 0);
		int v2Date = (int) (cal.getTimeInMillis() / millisecondsPerSecond);

		expect(database.getCommitTime(v1Hash)).andReturn(v1Date).once();
		expect(database.getCommitTime(v2Hash)).andReturn(v2Date).once();

		replay(database);

		int result = defaultGCM.compareTo(v1Hash, v2Hash);

		verify(database);

		return result;
	}

	/**
	 * Test that the searchForContent method returns null if an invalid version
	 * hash is given.
	 */
	@Test
	public void searchForContent_handleBogusVersion_checkNullReturned() {
		final String version = "";

		expect(database.verifyCommitExists(version)).andReturn(false).once();
		replay(database);

		try {
			defaultGCM.searchForContent(version, "", null,0, Constants.DEFAULT_RESULTS_LIMIT);
			fail("exception should have been thrown.");
		} catch (ContentManagerException e1) {
			// this should pass.
		}
		
		verify(database);
	}

	/**
	 * Test that the searchForContent method returns an empty ResultsWrapper if
	 * no results are found.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void searchForContent_handleNoResults_checkEmptyResultsWrapperReturned() {
		final String searchString = "";
		final Map<String, List<String>> fieldsThatMustMatch = null;

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();
		gitCache.put(INITIAL_VERSION, new ConcurrentHashMap<String, Content>());

		reset(database, searchProvider, contentMapper);

		searchProvider.registerRawStringFields((List<String>) anyObject());
		expectLastCall().once();

		ResultsWrapper<String> searchHits = createMock(ResultsWrapper.class);
		
		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(true).once();
		expect(
				searchProvider.fuzzySearch(anyString(), anyString(),
						anyString(), anyInt(), anyInt(), anyObject(Map.class), anyString(),
						anyString(), anyString(), anyString(), anyString()))
				.andReturn(searchHits).once();

		expect(searchHits.getResults()).andReturn(new LinkedList<String>())
				.once();
		expect(searchHits.getTotalResults()).andReturn(0L).once();

		expect(contentMapper.getDTOByDOList((List<Content>) anyObject()))
				.andReturn(new ArrayList<ContentDTO>()).once();

		expect(
				contentMapper
						.mapFromStringListToContentList((List<String>) anyObject()))
				.andReturn(new ArrayList<Content>()).once();

		replay(database, searchProvider, searchHits, contentMapper);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache, new ConcurrentHashMap<String, Map<Content, List<String>>>());
		
		try {
			assertTrue(gitContentManager
					.searchForContent(INITIAL_VERSION, searchString,
							fieldsThatMustMatch, 0, Constants.DEFAULT_RESULTS_LIMIT).getResults().size() == 0);
		} catch (ContentManagerException e) {
			fail("An empty results wrapper should be returned");
		}

		verify(database, searchProvider, searchHits, contentMapper);
	}

	/**
	 * Test that the getById method returns the correct object.
	 */
	@Test
	public void getById_retrieveObject_checkCorrectObjectReturned() {
		final String id = "test";

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();
		Map<String, Content> contentMap = new TreeMap<String, Content>();
		Content testContent = new Content();
		contentMap.put(id, testContent);
		gitCache.put(INITIAL_VERSION, contentMap);

		reset(searchProvider);
		
		searchProvider.registerRawStringFields((List<String>) anyObject());
		expectLastCall().once();
		
		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(true).once();
		replay(searchProvider);
		
		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache, new ConcurrentHashMap<String, Map<Content, List<String>>>());

		try {
			assertTrue(gitContentManager.getById(id, INITIAL_VERSION) == testContent);
		} catch (ContentManagerException e) {
			fail("correct object should be returned");
			e.printStackTrace();
		}

		verify(searchProvider);
	}

	/**
	 * Test that the getById method returns null if it is passed a null id.
	 */
	@Test
	public void getById_invalidId_checkNullReturned() {
		String id = null;
		try {
			assertTrue(defaultGCM.getById(id, INITIAL_VERSION) == null);
		} catch (ContentManagerException e) {
			fail("Null should be returned");
		}
	}

	/**
	 * Test that the getById method returns null if the specified version does
	 * not exist.
	 */
	@Test
	public void getById_missingVersion_checkExceptionReturned() {
		final String id = "test";

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();

		reset(database, searchProvider);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache, new ConcurrentHashMap<String, Map<Content, List<String>>>());

		expect(database.verifyCommitExists(INITIAL_VERSION)).andReturn(false)
				.once();
		replay(database);

		try {
			gitContentManager.getById(id, INITIAL_VERSION);
			fail("an exception should be returned");
		} catch (ContentManagerException e) {
			// pass
		}

		verify(database);
	}

	/**
	 * Test that the getById method returns null if the specified object does
	 * not exist the specified version.
	 */
	@Test
	public void getById_missingKey_checkNullReturned() {
		final String id = "test";

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();

		// Create a version containing an empty TreeMap of Content
		gitCache.put(INITIAL_VERSION, new TreeMap<String, Content>());

		reset(searchProvider);

		searchProvider.registerRawStringFields((List<String>) anyObject());
		expectLastCall().once();
		
		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(true).once();
		replay(searchProvider);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache, new ConcurrentHashMap<String, Map<Content, List<String>>>());
		
		try {
			assertTrue(gitContentManager.getById(id, INITIAL_VERSION) == null);
		} catch (ContentManagerException e) {
			fail("Null should be returned");
		}

		verify(searchProvider);
	}

	/**
	 * Test that the ensureCache method returns an exception if a null version hash is
	 * provided.
	 */
	@Test
	public void ensureCache_nullVersion_checkExceptionReturned() {
		try {
			defaultGCM.ensureCache(null);
			fail("Expected exception");
		} catch (ContentManagerException e) {
			// pass
		}
	}

	/**
	 * Test that the ensureCache method returns without exception if a cached and indexed
	 * version is provided.
	 */
	@Test
	public void ensureCache_cachedVerion_checkNoExceptionReturned() {
		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();
		gitCache.put(INITIAL_VERSION, new TreeMap<String, Content>());

		reset(searchProvider);

		searchProvider.registerRawStringFields((List<String>) anyObject());
		expectLastCall().once();
		
		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(true).once();
		replay(searchProvider);
		
		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache, new ConcurrentHashMap<String, Map<Content, List<String>>>());

		try {
			gitContentManager.ensureCache(INITIAL_VERSION);
		} catch (ContentManagerException e) {
			e.printStackTrace();
			fail("No exception should be returned");
		}

		verify(searchProvider);
	}

	/**
	 * Test that the ensureCache method rebuilds the cache when a version that
	 * exists in the database is not found in the cache.
	 */
	@Test
	public void ensureCache_uncachedVersion_checkGitContentIndexBuilt()
			throws RevisionSyntaxException, AmbiguousObjectException,
			IncorrectObjectTypeException, IOException {
		reset(database, searchProvider);

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();



		Repository repository = createMock(Repository.class);
		ObjectId commitId = createMock(ObjectId.class);
		TreeWalk treeWalk = createMock(TreeWalk.class);
		
		expect(database.verifyCommitExists(INITIAL_VERSION)).andReturn(true)
				.once();
		expect(database.getGitRepository()).andReturn(repository).once();
		expect(repository.resolve(INITIAL_VERSION)).andReturn(commitId).once();
		expect(database.getTreeWalk(anyString(), anyString())).andReturn(
				treeWalk).once();
		expect(treeWalk.next()).andReturn(false).once();
		repository.close();
		expectLastCall().once();
		
		searchProvider.registerRawStringFields((List<String>) anyObject());
		expectLastCall().once();
		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(true).times(
				2);

		replay(database, repository, treeWalk, searchProvider);
		
		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache, new ConcurrentHashMap<String, Map<Content, List<String>>>());

		try {
			gitContentManager.ensureCache(INITIAL_VERSION);
		} catch (ContentManagerException e) {
			e.printStackTrace();
			fail("Index should build");
		}
		assertTrue(gitCache.containsKey(INITIAL_VERSION));

		verify(database, repository, treeWalk, searchProvider);
	}

	/**
	 * Test that the buildSearchIndexFromLocalGitIndex sends each Content object
	 * to the searchProvider.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void buildSearchIndexFromLocalGitIndex_sendContentToSearchProvider_checkSearchProviderReceivesObject()
			throws Exception {
		reset(database, searchProvider);
		String uniqueObjectId = UUID.randomUUID().toString();
		String uniqueObjectHash = UUID.randomUUID().toString();

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();
		Map<String, Content> contents = new TreeMap<String, Content>();
		Content content = new Content();
		content.setId(uniqueObjectId);
		contents.put(uniqueObjectId, content);
		gitCache.put(INITIAL_VERSION, contents);

		ObjectMapper objectMapper = createMock(ObjectMapper.class);
		
		searchProvider.registerRawStringFields((List<String>) anyObject());
		expectLastCall().once();
		
		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(false)
				.once();
		expect(contentMapper.getContentObjectMapper()).andReturn(objectMapper)
				.once();
		expect(objectMapper.writeValueAsString(content)).andReturn(
				uniqueObjectHash).once();
	
		searchProvider.bulkIndex(eq(INITIAL_VERSION), anyString(), (List<Map.Entry<String, String>>) anyObject());
		expectLastCall().once();
		
		replay(searchProvider, contentMapper, objectMapper);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache, new ConcurrentHashMap<String, Map<Content, List<String>>>());
		
		Whitebox.invokeMethod(gitContentManager,
				"buildSearchIndexFromLocalGitIndex", INITIAL_VERSION);

		verify(searchProvider, contentMapper, objectMapper);
	}

	/**
	 * Test the flattenContentObjects method and ensure the expected output is
	 * generated.
	 * 
	 * @throws Exception
	 */
	@Test
	public void flattenContentObjects_flattenMultiTierObject_checkCorrectObjectReturned()
			throws Exception {
		final int numChildLevels = 5;
		final int numNodes = numChildLevels + 1;

		Set<Content> elements = new HashSet<Content>();
		Content rootNode = createContentHierarchy(numChildLevels, elements);

		Set<Content> contents = Whitebox.<Set<Content>> invokeMethod(
				defaultGCM, "flattenContentObjects", rootNode);

		assertTrue(contents.size() == numNodes);

		for (Content c : contents) {
			boolean containsElement = elements.contains(c);
			assertTrue(containsElement);
			if (containsElement) {
				elements.remove(c);
			}
		}

		assertTrue(elements.size() == 0);
	}

	private Content createContentHierarchy(final int numLevels,
			final Set<Content> flatSet) {
		List<ContentBase> children = new LinkedList<ContentBase>();

		if (numLevels > 0) {
			Content child = createContentHierarchy(numLevels - 1, flatSet);
			children.add(child);
		}

		Content content = createEmptyContentElement(children,
				String.format("%d", numLevels));
		flatSet.add(content);
		return content;
	}

	/**
	 * Helper method for the
	 * flattenContentObjects_flattenMultiTierObject_checkCorrectObjectReturned
	 * test, generates a Content object with the given children.
	 * 
	 * @param children
	 *            - The children of the new Content object
	 * @param id
	 *            - The id of the content element
	 * @return The new Content object
	 */
	private Content createEmptyContentElement(final List<ContentBase> children,
			final String id) {
		return new Content("", id, "", "", "", "", "", "", "", children, "",
				"", new LinkedList<String>(), false, new HashSet<String>(), 1);
	}

	/**
	 * Test the validateReferentialIntegrity method to ensure it handles web
	 * based media correctly and does not attempt to search for it in the
	 * database.
	 * 
	 * @throws Exception
	 */
	@Test
	public void validateReferentialIntegrity_handlesWebMedia_trueReturned()
			throws Exception {
		Media content = createMock(Media.class);
		Map<String, Map<Content, List<String>>> indexProblemCache = new ConcurrentHashMap<String, Map<Content, List<String>>>();
		GitContentManager gitContentManager = validateReferentialIntegrity_setUpTest(
				content, indexProblemCache);

		String uniqueObjectId = UUID.randomUUID().toString();

		// Self reference for the purpose of passing the test
		List<String> relatedContent = new LinkedList<String>();
		relatedContent.add(uniqueObjectId);

		expect(content.getId()).andReturn(uniqueObjectId).atLeastOnce();
		expect(content.getChildren()).andReturn(new LinkedList<ContentBase>())
				.once();
		expect(content.getAltText()).andReturn("Test Alt Text").anyTimes();
		expect(content.getRelatedContent()).andReturn(relatedContent)
				.atLeastOnce();
		expect(content.getValue()).andReturn(null).once();
		expect(content.getSrc())
				.andReturn("http://www.website.com/media.media").atLeastOnce();
		replay(content, database);

		boolean result = Whitebox.<Boolean> invokeMethod(gitContentManager,
				"checkForContentErrors", INITIAL_VERSION);

		assertTrue(result);
		assertTrue(indexProblemCache.size() == 0);

		verify(content, database);
	}

	/**
	 * Test the validateReferentialIntegrity method to ensure it searches the
	 * database for nonweb media content and returns true if found.
	 * 
	 * @throws Exception
	 */
	@Test
	public void validateReferentialIntegrity_storedMediaDatabaseLookup_trueReturned()
			throws Exception {
		Media content = createMock(Media.class);
		Map<String, Map<Content, List<String>>> indexProblemCache = new ConcurrentHashMap<String, Map<Content, List<String>>>();
		GitContentManager gitContentManager = validateReferentialIntegrity_setUpTest(
				content, indexProblemCache);

		String uniqueObjectId = UUID.randomUUID().toString();

		// Self reference for the purpose of passing the test
		List<String> relatedContent = new LinkedList<String>();
		relatedContent.add(uniqueObjectId);

		String src = "media.media";

		expect(content.getId()).andReturn(uniqueObjectId).atLeastOnce();
		expect(content.getChildren()).andReturn(new LinkedList<ContentBase>())
				.once();
		expect(content.getAltText()).andReturn("Test Alt Text").anyTimes();
		expect(content.getRelatedContent()).andReturn(relatedContent)
				.atLeastOnce();
		expect(content.getValue()).andReturn(null).once();
		expect(content.getSrc()).andReturn(src).atLeastOnce();
		expect(database.verifyGitObject(INITIAL_VERSION, src)).andReturn(true)
				.once();
		replay(content, database);

		boolean result = Whitebox.<Boolean> invokeMethod(gitContentManager,
				"checkForContentErrors", INITIAL_VERSION);

		assertTrue(result);
		assertTrue(indexProblemCache.size() == 0);

		verify(content, database);
	}

	/**
	 * Test the validateReferentialIntegrity method to ensure it searches the
	 * database for nonweb media content and when not found, returns true but
	 * registers a content error if not found.
	 * 
	 * @throws Exception
	 */
	@Test
	public void validateReferentialIntegrity_missingMediaDatabaseLookup_trueReturned()
			throws Exception {
		Media content = createMock(Media.class);
		Map<String, Map<Content, List<String>>> indexProblemCache = new ConcurrentHashMap<String, Map<Content, List<String>>>();
		GitContentManager gitContentManager = validateReferentialIntegrity_setUpTest(
				content, indexProblemCache);

		String uniqueObjectId = UUID.randomUUID().toString();

		// Self reference for the purpose of passing the test
		List<String> relatedContent = new LinkedList<String>();
		relatedContent.add(uniqueObjectId);

		String src = "media.media";

		expect(content.getId()).andReturn(uniqueObjectId).atLeastOnce();
		expect(content.getAltText()).andReturn("Test Alt Text").anyTimes();
		expect(content.getChildren()).andReturn(new LinkedList<ContentBase>())
				.once();
		expect(content.getRelatedContent()).andReturn(relatedContent)
				.atLeastOnce();
		expect(content.getValue()).andReturn(null).once();
		expect(content.getSrc()).andReturn(src).atLeastOnce();
		expect(database.verifyGitObject(INITIAL_VERSION, src)).andReturn(false)
				.once();
		expect(content.getCanonicalSourceFile()).andReturn("").anyTimes();
		expect(content.getTitle()).andReturn("").anyTimes();
		replay(content, database);

		boolean result = Whitebox.<Boolean> invokeMethod(gitContentManager,
				"checkForContentErrors", INITIAL_VERSION);

		assertTrue(result);
		assertTrue(indexProblemCache.size() == 1);
		assertTrue(indexProblemCache.get(INITIAL_VERSION).size() == 1);

		verify(content, database);
	}

	/**
	 * Test the validateReferentialIntegrity method to ensure it reports a
	 * content fault if related content is not found in the cache.
	 * 
	 * @throws Exception
	 */
	@Test
	public void validateReferentialIntegrity_missingRelatedContent_trueReturned()
			throws Exception {
		Content content = createMock(Content.class);
		Map<String, Map<Content, List<String>>> indexProblemCache = new ConcurrentHashMap<String, Map<Content, List<String>>>();
		GitContentManager gitContentManager = validateReferentialIntegrity_setUpTest(
				content, indexProblemCache);

		String uniqueObjectId = UUID.randomUUID().toString();

		// Reference a non-existent object
		List<String> relatedContent = new LinkedList<String>();
		relatedContent.add(UUID.randomUUID().toString());

		expect(content.getId()).andReturn(uniqueObjectId).atLeastOnce();
		expect(content.getChildren()).andReturn(new LinkedList<ContentBase>())
				.once();
		expect(content.getRelatedContent()).andReturn(relatedContent)
				.atLeastOnce();
		expect(content.getValue()).andReturn(null).once();
		expect(content.getCanonicalSourceFile()).andReturn("").anyTimes();
		expect(content.getTitle()).andReturn("").anyTimes();
		replay(content);

		boolean result = Whitebox.<Boolean> invokeMethod(gitContentManager,
				"checkForContentErrors", INITIAL_VERSION);

		assertTrue(!result);
		assertTrue(indexProblemCache.size() == 1);
		assertTrue(indexProblemCache.get(INITIAL_VERSION).size() == 1);

		verify(content);
	}

	/**
	 * Test the validateReferentialIntegrity method to ensure it reports a
	 * content fault if a content object has both children and a value.
	 * 
	 * @throws Exception
	 */
	@Test
	public void validateReferentialIntegrity_contentWithValueAndChildren_trueReturned()
			throws Exception {
		Content content = createMock(Content.class);
		Map<String, Map<Content, List<String>>> indexProblemCache = new ConcurrentHashMap<String, Map<Content, List<String>>>();
		GitContentManager gitContentManager = validateReferentialIntegrity_setUpTest(
				content, indexProblemCache);

		String uniqueObjectId = UUID.randomUUID().toString();

		// Self reference for the purpose of passing the test
		List<String> relatedContent = new LinkedList<String>();
		relatedContent.add(uniqueObjectId);

		List<ContentBase> children = new LinkedList<ContentBase>();
		Content child = createMock(Content.class);
		expect(child.getId()).andReturn(null).once();
		expect(child.getRelatedContent()).andReturn(null).once();
		expect(child.getValue()).andReturn(null).once();
		expect(child.getChildren()).andReturn(new LinkedList<ContentBase>())
				.atLeastOnce();
		children.add(child);

		expect(content.getId()).andReturn(uniqueObjectId).atLeastOnce();
		expect(content.getChildren()).andReturn(children).atLeastOnce();
		expect(content.getRelatedContent()).andReturn(relatedContent)
				.atLeastOnce();
		expect(content.getValue()).andReturn(new String()).once();
		expect(content.getCanonicalSourceFile()).andReturn("").anyTimes();
		expect(content.getTitle()).andReturn("").anyTimes();
		replay(child, content);

		boolean result = Whitebox.<Boolean> invokeMethod(gitContentManager,
				"checkForContentErrors", INITIAL_VERSION);

		assertTrue(result);
		assertTrue(indexProblemCache.size() == 1);
		assertTrue(indexProblemCache.get(INITIAL_VERSION).size() == 1);

		verify(child, content);
	}

	/**
	 * Helper method to construct the tests for the validateReferentialIntegrity
	 * method.
	 * 
	 * @param content
	 *            - Content object to be tested
	 * @param indexProblemCache
	 *            - Externally provided indexProblemCache for GitContentManager
	 *            so that it can be inspected during the test
	 * @return An instance of GitContentManager
	 */
	private GitContentManager validateReferentialIntegrity_setUpTest(
			Content content,
			Map<String, Map<Content, List<String>>> indexProblemCache) {
		reset(database, searchProvider);

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();
		Map<String, Content> contents = new TreeMap<String, Content>();
		contents.put(INITIAL_VERSION, content);
		gitCache.put(INITIAL_VERSION, contents);

		return new GitContentManager(database, searchProvider, contentMapper,
				gitCache, indexProblemCache);
	}

	/**
	 * Test the buildGitContentIndex method and ensure it adds content objects
	 * to the cache.
	 * 
	 * @throws Exception
	 */
	@Test
	public void buildGitContentIndex_addObject_objectAddedToCache()
			throws Exception {
		reset(database, searchProvider);

		final String pathToContent = "/path/to/content/";

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();
		Map<String, Map<Content, List<String>>> indexProblemCache = new ConcurrentHashMap<String, Map<Content, List<String>>>();

		searchProvider.registerRawStringFields((List<String>) anyObject());
		expectLastCall().once();
		
		Repository repository = createMock(Repository.class);
		ObjectId commitId = createMock(ObjectId.class);
		TreeWalk treeWalk = createMock(TreeWalk.class);
		ObjectLoader loader = createMock(ObjectLoader.class);
		ObjectMapper objectMapper = createMock(ObjectMapper.class);

		Content content = new Content();
		content.setId(UUID.randomUUID().toString());

		expect(database.getGitRepository()).andReturn(repository).once();
		expect(repository.resolve(INITIAL_VERSION)).andReturn(commitId).once();
		expect(database.getTreeWalk(eq(INITIAL_VERSION), anyString()))
				.andReturn(treeWalk).once();
		expect(treeWalk.next()).andReturn(true).once();
		expect(treeWalk.getObjectId(0)).andReturn(null);
		expect(repository.open(null)).andReturn(loader).once();
		loader.copyTo(anyObject(ByteArrayOutputStream.class));
		expectLastCall().once();
		expect(contentMapper.getContentObjectMapper()).andReturn(objectMapper)
				.once();
		expect(objectMapper.readValue(anyString(), eq(ContentBase.class)))
				.andReturn(content).once();
		expect(treeWalk.getPathString()).andReturn(pathToContent).atLeastOnce();
		expect(treeWalk.next()).andReturn(false).once();
		repository.close();
		expectLastCall().once();

		replay(database, repository, treeWalk, contentMapper, objectMapper,
				searchProvider);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache, indexProblemCache);
		
		Whitebox.invokeMethod(gitContentManager, "buildGitContentIndex",
				INITIAL_VERSION);

		assertTrue(gitCache.containsKey(INITIAL_VERSION));
		assertTrue(gitCache.get(INITIAL_VERSION).containsKey(content.getId()));

		verify(database, repository, treeWalk, contentMapper, objectMapper,
				searchProvider);
	}

	/**
	 * Test the buildGitContentIndex method to ensure it reports a content fault
	 * if different content with the same id is attempted to be added to the
	 * cache.
	 * 
	 * @throws Exception
	 */
	@Test
	public void buildGitContentIndex_addDuplicateObject_contentErrorLogged()
		throws Exception {
		reset(database, searchProvider);

		final String pathToContent = "/path/to/content/";

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();
		Map<String, Map<Content, List<String>>> indexProblemCache = new ConcurrentHashMap<String, Map<Content, List<String>>>();

		searchProvider.registerRawStringFields((List<String>) anyObject());
		expectLastCall().once();
		
		Repository repository = createMock(Repository.class);
		ObjectId commitId = createMock(ObjectId.class);
		TreeWalk treeWalk = createMock(TreeWalk.class);
		ObjectLoader loader = createMock(ObjectLoader.class);
		ObjectMapper objectMapper = createMock(ObjectMapper.class);

		String id = UUID.randomUUID().toString();
		Content content = new Content();
		content.setId(id);
		content.setTitle("CONTENT1");
		Content content2 = new Content();
		content2.setId(id);
		content2.setTitle("CONTENT2");

		expect(database.getGitRepository()).andReturn(repository).once();
		expect(repository.resolve(INITIAL_VERSION)).andReturn(commitId).once();
		expect(database.getTreeWalk(eq(INITIAL_VERSION), anyString()))
				.andReturn(treeWalk).once();
		expect(treeWalk.next()).andReturn(true).times(2);
		expect(treeWalk.getObjectId(0)).andReturn(null).times(2);
		expect(repository.open(null)).andReturn(loader).times(2);
		loader.copyTo(anyObject(ByteArrayOutputStream.class));
		expectLastCall().times(2);
		expect(contentMapper.getContentObjectMapper()).andReturn(objectMapper)
				.times(2);
		expect(objectMapper.readValue(anyString(), eq(ContentBase.class)))
				.andReturn(content).once().andReturn(content2).once();
		expect(treeWalk.getPathString()).andReturn(pathToContent).atLeastOnce();
		expect(treeWalk.next()).andReturn(false).once();
		repository.close();
		expectLastCall().once();

		replay(database, repository, treeWalk, contentMapper, objectMapper,
				searchProvider);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache, indexProblemCache);
		
		Whitebox.invokeMethod(gitContentManager, "buildGitContentIndex",
				INITIAL_VERSION);

		assertTrue(gitCache.containsKey(INITIAL_VERSION));
		assertTrue(gitCache.get(INITIAL_VERSION).containsKey(id));
		assertTrue(indexProblemCache.size() == 1);

		verify(database, repository, treeWalk, contentMapper, objectMapper,
				searchProvider);
	}
}
