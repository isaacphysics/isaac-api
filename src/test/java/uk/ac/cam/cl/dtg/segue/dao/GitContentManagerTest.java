/*
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

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

		return new GitContentManager(database, searchProvider, contentMapper);
	}
}
