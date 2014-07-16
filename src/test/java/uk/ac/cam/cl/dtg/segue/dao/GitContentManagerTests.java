package uk.ac.cam.cl.dtg.segue.dao;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;

/**
 * Test class for the GitContentManager class.
 * 
 */
public class GitContentManagerTests {
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
	@SuppressWarnings("unchecked")
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
		assertTrue(compareTo_getResult(2010, 2000) > 0);
	}

	/**
	 * Test that the compareTo method returns the correct result when V2 is
	 * newer than V1.
	 */
	@Test
	public void compareTo_checkV2NewerThanV1_checkNegativeNumberReturned() {
		assertTrue(compareTo_getResult(2000, 2010) < 0);
	}

	/**
	 * Test that the compareTo method returns the correct result when V1 is the
	 * same age as V2.
	 */
	@Test
	public void compareTo_checkV2SameAgeAsV1_checkZeroReturned() {
		assertTrue(compareTo_getResult(2000, 2000) == 0);
	}

	/**
	 * This method will evaluate the result of the compareTo method using the
	 * years provided as the arguments for datestamps. The rest of the timestamp
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
		String v1Hash = "V1";
		String v2Hash = "V2";

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(v1Year, 0, 1, 0, 0, 0);
		int v1Date = (int) (cal.getTimeInMillis() / 1000L);
		cal.set(v2Year, 0, 1, 0, 0, 0);
		int v2Date = (int) (cal.getTimeInMillis() / 1000L);

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

		assertTrue(defaultGCM.searchForContent(version, "", null) == null);

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

		searchProvider = createMock(ISearchProvider.class);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache);

		ResultsWrapper<String> searchHits = createMock(ResultsWrapper.class);

		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(true).once();
		/*
		 * expect(searchProvider.fuzzySearch(isA(String.class),
		 * isA(String.class), isA(String.class), isA(Map.class),
		 * isA(String.class), isA(String.class), isA(String.class),
		 * isA(String.class), "")) .andReturn(searchHits).once();
		 */
		expect(
				searchProvider.fuzzySearch(anyString(), anyString(),
						anyString(), anyObject(Map.class), anyString(),
						anyString(), anyString(), anyString(), anyString()))
				.andReturn(searchHits).once();
		expect(searchHits.getResults()).andReturn(new LinkedList<String>())
				.once();
		expect(searchHits.getTotalResults()).andReturn(0L).once();
		
		ObjectMapper objectMapper = createMock(ObjectMapper.class);
		expect(contentMapper.getContentObjectMapper()).andReturn(objectMapper)
		.once();
		
		replay(database, searchProvider, searchHits, contentMapper);

		assertTrue(gitContentManager
				.searchForContent(INITIAL_VERSION, searchString,
						fieldsThatMustMatch).getResults().size() == 0);

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

		searchProvider = createMock(ISearchProvider.class);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache);

		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(true).once();
		replay(searchProvider);

		assertTrue(gitContentManager.getById(id, INITIAL_VERSION) == testContent);

		verify(searchProvider);
	}

	/**
	 * Test that the getById method returns null if it is passed a null id.
	 */
	@Test
	public void getById_invalidId_checkNullReturned() {
		String id = null;
		assertTrue(defaultGCM.getById(id, INITIAL_VERSION) == null);
	}

	/**
	 * Test that the getById method returns null if the specified version does
	 * not exist.
	 */
	@Test
	public void getById_missingVersion_checkNullReturned() {
		final String id = "test";

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();

		searchProvider = createMock(ISearchProvider.class);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache);

		expect(database.verifyCommitExists(INITIAL_VERSION)).andReturn(false)
				.once();
		replay(database);

		assertTrue(gitContentManager.getById(id, INITIAL_VERSION) == null);

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

		searchProvider = createMock(ISearchProvider.class);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache);

		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(true).once();
		replay(searchProvider);

		assertTrue(gitContentManager.getById(id, INITIAL_VERSION) == null);

		verify(searchProvider);
	}

	/**
	 * Test that the ensureCache method returns false if a null version hash is
	 * provided.
	 */
	@Test
	public void ensureCache_nullVersion_checkFalseReturned() {
		assertTrue(!defaultGCM.ensureCache(null));
	}

	/**
	 * Test that the ensureCache method returns true if a cached and indexed
	 * version is provided.
	 */
	@Test
	public void ensureCache_cachedVerion_checkTrueReturned() {
		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();
		gitCache.put(INITIAL_VERSION, new TreeMap<String, Content>());

		searchProvider = createMock(ISearchProvider.class);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache);

		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(true).once();
		replay(searchProvider);

		assertTrue(gitContentManager.ensureCache(INITIAL_VERSION));

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
		database = createMock(GitDb.class);
		searchProvider = createMock(ISearchProvider.class);

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache);

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
		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(true).times(
				2);

		replay(database, repository, treeWalk, searchProvider);

		assertTrue(gitContentManager.ensureCache(INITIAL_VERSION));
		assertTrue(gitCache.containsKey(INITIAL_VERSION));

		verify(database, repository, treeWalk, searchProvider);
	}

	/**
	 * Test that the buildSearchIndexFromLocalGitIndex sends each Content object
	 * to the searchProvider.
	 */
	@Test
	public void buildSearchIndexFromLocalGitIndex_sendContentToSearchProvider_checkSearchProviderReceivesObject()
			throws JsonProcessingException {
		database = createMock(GitDb.class);
		searchProvider = createMock(ISearchProvider.class);
		String uniqueObjectId = UUID.randomUUID().toString();
		String uniqueObjectHash = UUID.randomUUID().toString();

		Map<String, Map<String, Content>> gitCache = new ConcurrentHashMap<String, Map<String, Content>>();
		Map<String, Content> contents = new TreeMap<String, Content>();
		Content content = new Content();
		content.setId(uniqueObjectId);
		contents.put(uniqueObjectId, content);
		gitCache.put(INITIAL_VERSION, contents);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, gitCache);

		ObjectMapper objectMapper = createMock(ObjectMapper.class);

		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(false)
				.once();
		expect(contentMapper.getContentObjectMapper()).andReturn(objectMapper)
				.once();
		expect(objectMapper.writeValueAsString(content)).andReturn(
				uniqueObjectHash).once();
		expect(
				searchProvider.indexObject(eq(INITIAL_VERSION), anyString(),
						eq(uniqueObjectHash), eq(uniqueObjectId))).andReturn(
				true).once();

		replay(searchProvider, contentMapper, objectMapper);

		gitContentManager.buildSearchIndexFromLocalGitIndex(INITIAL_VERSION);

		verify(searchProvider, contentMapper, objectMapper);
	}

	/**
	 * Test the flattenContentObjects method and ensure the expected output
	 * is generated.
	 */
	@Test
	public void flattenContentObjects_flattenMultiTierObject_checkCorrectObjectReturned() {
		Content innerChild = createEmptyContentElement(new LinkedList<ContentBase>());
		List<ContentBase> innerChildren = new LinkedList<ContentBase>();
		innerChildren.add(innerChild);
		Content child = createEmptyContentElement(innerChildren);
		List<ContentBase> children = new LinkedList<ContentBase>();
		children.add(child);
		Content root = createEmptyContentElement(children);
		
		Set<Content> elements = new HashSet<Content>();
		elements.add(root);
		elements.add(child);
		elements.add(innerChild);
		
		Set<Content> contents = defaultGCM.flattenContentObjects(root);
		
		for (Content c : contents) {
			boolean containsElement = elements.contains(c);
			assertTrue(containsElement);
			if (containsElement) {
				elements.remove(c);
			}
		}
		
		assertTrue(elements.size() == 0);
	}

	/**
	 * Helper method for the flattenContentObjects_flattenMultiTierObject_checkCorrectObjectReturned
	 * test, generates a Content object with the given children.
	 * @param children - The children of the new Content object
	 * @return The new Content object
	 */
	private Content createEmptyContentElement(List<ContentBase> children) {
		return new Content("", "", "", "", "", "", "", "", "", children, "",
				"", new LinkedList<String>(), false, new HashSet<String>(), 1);
	}
}
