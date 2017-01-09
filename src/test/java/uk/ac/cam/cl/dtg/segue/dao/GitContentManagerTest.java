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

import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.reflect.Whitebox;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

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
	private PropertiesLoader properties;

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
		this.properties = createMock(PropertiesLoader.class);

		this.defaultGCM = new GitContentManager(database, searchProvider,
				contentMapper, properties);
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
	 * Test that the getById method returns null if it is passed a null id.
	 */
	@Test
	public void getById_invalidId_checkNullReturned() {
		String id = null;
		try {
			assertTrue(defaultGCM.getContentDOById(INITIAL_VERSION, id) == null);
		} catch (ContentManagerException e) {
			fail("Null should be returned");
		}
	}

	/**
	 * Test that the ensureCache method returns an exception if a null version hash is
	 * provided.
	 *//*
	@Test
	public void ensureCache_nullVersion_checkExceptionReturned() {
		try {
			defaultGCM.ensureCache(null);
			fail("Expected exception");
		} catch (ContentManagerException e) {
			// pass
		}
	}*/

	/**
	 * Test that the buildSearchIndexFromLocalGitIndex sends each Content object
	 * to the searchProvider.
	 * 
	 * @throws Exception
	 *//*
	@SuppressWarnings("unchecked")
	@Test
	public void buildSearchIndexFromLocalGitIndex_sendContentToSearchProvider_checkSearchProviderReceivesObject()
			throws Exception {
		reset(database, searchProvider);
		String uniqueObjectId = UUID.randomUUID().toString();
		String uniqueObjectHash = UUID.randomUUID().toString();
        
		Map<String, Content> contents = new TreeMap<String, Content>();
		Content content = new Content();
		content.setId(uniqueObjectId);
		contents.put(uniqueObjectId, content);

		ObjectMapper objectMapper = createMock(ObjectMapper.class);
		
		searchProvider.registerRawStringFields((List<String>) anyObject());
		expectLastCall().once();
		
		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(false)
				.once();
		expect(contentMapper.generateNewPreconfiguredContentMapper()).andReturn(objectMapper)
				.once();
		expect(objectMapper.writeValueAsString(content)).andReturn(
				uniqueObjectHash).once();
	
		searchProvider.bulkIndex(eq(INITIAL_VERSION), anyString(), (List<Map.Entry<String, String>>) anyObject());
		expectLastCall().once();
		
		replay(searchProvider, contentMapper, objectMapper);

		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper, new ConcurrentHashMap<String, Map<Content, List<String>>>());
		
		Whitebox.invokeMethod(gitContentManager,
				"buildSearchIndexFromLocalGitIndex", INITIAL_VERSION, contents);

		verify(searchProvider, contentMapper, objectMapper);
	}
*/
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
		return new Content(id, "", "", "", "", "", "", "", children, "",
				"", new LinkedList<String>(), false, new HashSet<String>(), 1);
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
        
		Map<String, Content> contents = new TreeMap<String, Content>();
		contents.put(INITIAL_VERSION, content);

		return new GitContentManager(database, searchProvider, contentMapper,
				indexProblemCache);
	}
}
