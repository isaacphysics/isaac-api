/*
 * Copyright 2014 Nick Rogers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentSubclassMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.util.mappers.ContentMapper;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test class for the GitContentManager class.
 * 
 */
public class GitContentManagerTest {
	private GitDb database;
	private ISearchProvider searchProvider;
	private ContentMapper contentMapper;
	private ContentSubclassMapper contentSubclassMapper;

	private GitContentManager defaultGCM;

	private static final String INITIAL_VERSION = "0b72984c5eff4f53604fe9f1c724d3f387799db9";

	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@BeforeEach
	public final void setUp() throws Exception {
		this.database = createMock(GitDb.class);
		this.searchProvider = createMock(ISearchProvider.class);
		this.contentMapper = createMock(ContentMapper.class);
		this.contentSubclassMapper = createMock(ContentSubclassMapper.class);

		this.defaultGCM = new GitContentManager(database, searchProvider, contentMapper, contentSubclassMapper);
	}
	/**
	 * Test that the getById method returns null if it is passed a null id.
	 */
	@Test
	public void getById_invalidId_checkNullReturned() {
		String id = null;
		try {
			assertTrue(defaultGCM.getContentDOById(id) == null);
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

		return new GitContentManager(database, searchProvider, contentMapper, contentSubclassMapper);
	}
}
