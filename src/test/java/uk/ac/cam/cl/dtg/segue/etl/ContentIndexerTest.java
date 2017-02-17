package uk.ac.cam.cl.dtg.segue.etl;

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
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.*;

import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.reflect.Whitebox;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Test class for the GitContentManager class.
 *
 */
@PowerMockIgnore({"javax.ws.*"})
public class ContentIndexerTest {
    private GitDb database;
    private ElasticSearchIndexer searchProvider;
    private ContentMapper contentMapper;

    private ContentIndexer defaultContentIndexer;

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
        this.searchProvider = createMock(ElasticSearchIndexer.class);
        this.contentMapper = createMock(ContentMapper.class);
        this.properties = createMock(PropertiesLoader.class);

        this.defaultContentIndexer = new ContentIndexer(database, searchProvider,
                contentMapper);
    }

    /**
     * Test that the buildSearchIndexFromLocalGitIndex sends each Content object
     * to the searchProvider.
     *
     * @throws Exception
     */
	@SuppressWarnings("unchecked")
	@Test
	public void buildElasticSearch_sendContentToSearchProvider_checkSearchProviderReceivesObject()
			throws Exception {
		reset(database, searchProvider);
		String uniqueObjectId = UUID.randomUUID().toString();
		String uniqueObjectHash = UUID.randomUUID().toString();

		Map<String, Content> contents = new TreeMap<>();
		Content content = new Content();
		content.setId(uniqueObjectId);
		contents.put(uniqueObjectId, content);

		Set<String> someTagsList = Sets.newHashSet();
		Map<String, String> someUnitsMap = Maps.newHashMap();
        Map<Content, List<String>> someContentProblemsMap = Maps.newHashMap();

        Map versionMeta = ImmutableMap.of("version", INITIAL_VERSION, "created", new Date().toString());
        Map tagsMeta = ImmutableMap.of("tags", someTagsList);

		ObjectMapper objectMapper = createMock(ObjectMapper.class);

		//TODO: Check this is done somewhere.
//		searchProvider.registerRawStringFields((List<String>) anyObject());
//		expectLastCall().once();

		expect(searchProvider.hasIndex(INITIAL_VERSION)).andReturn(false)
				.once();
		expect(contentMapper.generateNewPreconfiguredContentMapper()).andReturn(objectMapper)
				.once();
		expect(objectMapper.writeValueAsString(content)).andReturn(
				uniqueObjectHash).once();

        expect(objectMapper.writeValueAsString(
                versionMeta)).andReturn(versionMeta.toString()).once();

        expect(objectMapper.writeValueAsString(
                tagsMeta)).andReturn(tagsMeta.toString()).once();

        searchProvider.indexObject(INITIAL_VERSION, "metadata", versionMeta.toString(), "general");
        expectLastCall().once();

        searchProvider.indexObject(INITIAL_VERSION, "metadata", tagsMeta.toString(), "tags");
        expectLastCall().once();

        //TODO: add check that units are indexed

        searchProvider.bulkIndex(eq(INITIAL_VERSION), anyString(), anyObject());
		expectLastCall().once();

		replay(searchProvider, contentMapper, objectMapper);

        ContentIndexer contentIndexer = new ContentIndexer(database,
				searchProvider, contentMapper);

		Whitebox.invokeMethod(contentIndexer,
				"buildElasticSearchIndex",
                INITIAL_VERSION, contents, someTagsList, someUnitsMap, someContentProblemsMap);

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
                defaultContentIndexer, "flattenContentObjects", rootNode);

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
                "", new LinkedList<>(), false, new HashSet<>(), 1);
    }

    /**
     * Helper method to construct the tests for the validateReferentialIntegrity
     * method.
     *
     * @param content
     *            - Content object to be tested
     * @return An instance of GitContentManager
     */
    private GitContentManager validateReferentialIntegrity_setUpTest(
            Content content) {
        reset(database, searchProvider);

        Map<String, Content> contents = new TreeMap<String, Content>();
        contents.put(INITIAL_VERSION, content);

        return new GitContentManager(database, searchProvider, contentMapper);
    }
}
