/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.etl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

/**
 * Test class for the GitContentManager class.
 */
public class ContentIndexerTest {
  private GitDb database;
  private ElasticSearchIndexer searchProvider;
  private ContentMapper contentMapper;

  private ContentIndexer defaultContentIndexer;

  private static final String INITIAL_VERSION = "0b72984c5eff4f53604fe9f1c724d3f387799db9";

  /**
   * Initial configuration of tests.
   *
   * @throws Exception - test exception
   */
  @BeforeEach
  public final void setUp() throws Exception {
    this.database = createMock(GitDb.class);
    this.searchProvider = createMock(ElasticSearchIndexer.class);
    this.contentMapper = createMock(ContentMapper.class);
    this.defaultContentIndexer = new ContentIndexer(database, searchProvider,
        contentMapper);
  }

  /**
   * Test that the buildSearchIndex sends all of the various different segue data
   * to the searchProvider and we haven't forgotten anything.
   *
   * @throws JsonProcessingException if an error occurs during object mapping
   * @throws SegueSearchException if an error occurs during content indexing
   */
  @Test
  public void buildSearchIndexes_sendContentToSearchProvider_checkSearchProviderIsSentAllImportantObject()
      throws JsonProcessingException, SegueSearchException {
    reset(database, searchProvider);
    String uniqueObjectId = UUID.randomUUID().toString();
    String uniqueObjectHash = UUID.randomUUID().toString();

    Map<String, Content> contents = new TreeMap<>();
    Content content = new Content();
    content.setId(uniqueObjectId);
    contents.put(uniqueObjectId, content);

    Set<String> someTagsList = new HashSet<>();

    Map<String, String> someUnitsMap = Map.of("N", "N", "km", "km");
    Map<String, String> publishedUnitsMap = Map.of("N", "N", "km", "km");

    // This is what is sent to the search provider so needs to be mocked
    Map<String, String> someUnitsMapRaw = Map.of("cleanKey", "N", "unit", "N");
    Map<String, String> someUnitsMapRaw2 = Map.of("cleanKey", "km", "unit", "km");

    Date someCreatedDate = new Date();
    Map<String, String> versionMeta = Map.of("version", INITIAL_VERSION, "created", someCreatedDate.toString());
    Map<String, Set<String>> tagsMeta = Map.of("tags", someTagsList);

    Map<Content, List<String>> someContentProblemsMap = new HashMap<>();

    // assume in this case that there are no pre-existing indexes for this version
    for (Constants.ContentIndextype contentIndexType : Constants.ContentIndextype.values()) {
      expect(searchProvider.hasIndex(INITIAL_VERSION, contentIndexType.toString())).andReturn(false).once();
    }

    // prepare pre-canned responses for the object mapper
    ObjectMapper objectMapper = createMock(ObjectMapper.class);
    expect(contentMapper.generateNewPreconfiguredContentMapper()).andReturn(objectMapper)
        .once();
    expect(objectMapper.writeValueAsString(content)).andReturn(
        uniqueObjectHash).once();
    expect(objectMapper.writeValueAsString(
        anyObject())).andReturn(versionMeta.toString()).once(); // expects versionMeta - possibly differing date
    expect(objectMapper.writeValueAsString(
        tagsMeta)).andReturn(tagsMeta.toString()).once();

    // populate all units and published units - order matters for the below
    expect(objectMapper.writeValueAsString(
        someUnitsMapRaw)).andReturn(someUnitsMap.toString()).once();
    expect(objectMapper.writeValueAsString(
        someUnitsMapRaw2)).andReturn(someUnitsMap.toString()).once();
    expect(objectMapper.writeValueAsString(
        someUnitsMapRaw)).andReturn(someUnitsMap.toString()).once();
    expect(objectMapper.writeValueAsString(
        someUnitsMapRaw2)).andReturn(someUnitsMap.toString()).once();

    // Important Items for test - start here

    // Ensure general metadata (version) is indexed
    searchProvider.indexObject(INITIAL_VERSION, "metadata", versionMeta.toString(), "general");
    expectLastCall().atLeastOnce();

    // Ensure tags are indexed
    searchProvider.indexObject(INITIAL_VERSION, "metadata", tagsMeta.toString(), "tags");
    expectLastCall().atLeastOnce();

    // Ensure units are indexed
    searchProvider.bulkIndex(eq(INITIAL_VERSION), eq(Constants.ContentIndextype.UNIT.toString()), anyObject());
    expectLastCall().once();
    searchProvider.bulkIndex(eq(INITIAL_VERSION), eq(Constants.ContentIndextype.PUBLISHED_UNIT.toString()),
        anyObject());
    expectLastCall().once();

    // Ensure content errors are indexed
    searchProvider.bulkIndex(eq(INITIAL_VERSION), eq(Constants.ContentIndextype.CONTENT_ERROR.toString()), anyObject());
    expectLastCall().once();

    // Ensure at least one bulk index for general content is requested
    searchProvider.bulkIndexWithIds(eq(INITIAL_VERSION), eq(Constants.ContentIndextype.CONTENT.toString()),
        anyObject());
    expectLastCall().once();

    replay(searchProvider, contentMapper, objectMapper);

    ContentIndexer contentIndexer = new ContentIndexer(database,
        searchProvider, contentMapper);

    // Method under test
    contentIndexer.buildElasticSearchIndex(INITIAL_VERSION, contents, someTagsList, someUnitsMap, publishedUnitsMap,
        someContentProblemsMap);

    verify(searchProvider, contentMapper, objectMapper);
  }

  /**
   * Test the flattenContentObjects method and ensure the expected output is
   * generated.
   */
  @Test
  public void flattenContentObjects_flattenMultiTierObject_checkCorrectObjectReturned() {
    final int numChildLevels = 5;
    final int numNodes = numChildLevels + 1;

    Set<Content> elements = new HashSet<>();
    Content rootNode = createContentHierarchy(numChildLevels, elements);

    Set<Content> contents = defaultContentIndexer.flattenContentObjects(rootNode);

    assertEquals(numNodes, contents.size());

    for (Content c : contents) {
      boolean containsElement = elements.contains(c);
      assertTrue(containsElement);
      if (containsElement) {
        elements.remove(c);
      }
    }

    assertEquals(0, elements.size());
  }

  private Content createContentHierarchy(final int numLevels,
                                         final Set<Content> flatSet) {
    List<ContentBase> children = new LinkedList<>();

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
   * @param children - The children of the new Content object
   * @param id       - The id of the content element
   * @return The new Content object
   */
  private Content createEmptyContentElement(final List<ContentBase> children,
                                            final String id) {
    return new Content(id, "", "", "", "", "", "", "", children, "",
        "", new LinkedList<>(), false, false, new HashSet<>(), 1);
  }

}
