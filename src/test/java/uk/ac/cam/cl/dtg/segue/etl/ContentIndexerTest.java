package uk.ac.cam.cl.dtg.segue.etl;

/*
 * Copyright 2014 Stephen Cummins
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
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static uk.ac.cam.cl.dtg.segue.etl.ContentIndexer.FEEDBACK_QUESTION_UNUSED_DZ;

import java.util.*;

import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacDndValidatorTest;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentSubclassMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.util.mappers.ContentMapper;

/**
 * Test class for the GitContentManager class.
 *
 */
public class ContentIndexerTest {
    private GitDb database;
    private ElasticSearchIndexer searchProvider;
    private ContentMapper contentMapper;
    private ContentSubclassMapper contentSubclassMapper;

    private ContentIndexer defaultContentIndexer;

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
        this.searchProvider = createMock(ElasticSearchIndexer.class);
        this.contentMapper = createMock(ContentMapper.class);
        this.contentSubclassMapper = createMock(ContentSubclassMapper.class);
        this.defaultContentIndexer = new ContentIndexer(database, searchProvider, contentSubclassMapper);
    }

    /**
     * Test that the buildSearchIndex sends all the various different segue data
     * to the searchProvider and we haven't forgotten anything.
     *
     * @throws Exception
     */
	@Test
	public void buildSearchIndexes_sendContentToSearchProvider_checkSearchProviderIsSentAllImportantObject()
			throws Exception {
		reset(database, searchProvider);
		String uniqueObjectId = UUID.randomUUID().toString();
		String uniqueObjectHash = UUID.randomUUID().toString();

		Map<String, Content> contents = new TreeMap<>();
		Content content = new Content();
		content.setId(uniqueObjectId);
		contents.put(uniqueObjectId, content);

		Set<String> someTagsList = Sets.newHashSet();

		Map<String, String> someUnitsMap = ImmutableMap.of("N","N", "km", "km");
		Map<String, String> publishedUnitsMap = ImmutableMap.of("N","N", "km", "km");

        // This is what is sent to the search provider so needs to be mocked
        Map<String, String> someUnitsMapRaw = ImmutableMap.of("cleanKey","N", "unit", "N");
        Map<String, String> someUnitsMapRaw2 = ImmutableMap.of("cleanKey","km", "unit", "km");

        Date someCreatedDate = new Date();
        Map versionMeta = ImmutableMap.of("version", INITIAL_VERSION, "created", someCreatedDate.toString());
        Map tagsMeta = ImmutableMap.of("tags", someTagsList);

        Map<Content, List<String>> someContentProblemsMap = Maps.newHashMap();

        // assume in this case that there are no pre-existing indexes for this version
        for (Constants.CONTENT_INDEX_TYPE contentIndexType : Constants.CONTENT_INDEX_TYPE.values()) {
            expect(searchProvider.hasIndex(INITIAL_VERSION, contentIndexType.toString())).andReturn(false).once();
        }

        // prepare pre-canned responses for the object mapper
		ObjectMapper objectMapper = createMock(ObjectMapper.class);
		expect(contentSubclassMapper.generateNewPreconfiguredContentMapper()).andReturn(objectMapper)
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
        searchProvider.bulkIndex(eq(INITIAL_VERSION), eq(Constants.CONTENT_INDEX_TYPE.UNIT.toString()), anyObject());
        expectLastCall().once();
        searchProvider.bulkIndex(eq(INITIAL_VERSION), eq(Constants.CONTENT_INDEX_TYPE.PUBLISHED_UNIT.toString()), anyObject());
        expectLastCall().once();

        // Ensure content errors are indexed
        searchProvider.bulkIndex(eq(INITIAL_VERSION), eq(Constants.CONTENT_INDEX_TYPE.CONTENT_ERROR.toString()), anyObject());
        expectLastCall().once();

        // Ensure at least one bulk index for general content is requested
        searchProvider.bulkIndexWithIDs(eq(INITIAL_VERSION), eq(Constants.CONTENT_INDEX_TYPE.CONTENT.toString()), anyObject());
		expectLastCall().once();

		replay(searchProvider, contentMapper, contentSubclassMapper, objectMapper);

        ContentIndexer contentIndexer = new ContentIndexer(database, searchProvider, contentSubclassMapper);

        // Method under test
		Whitebox.invokeMethod(contentIndexer,
				"buildElasticSearchIndex",
                INITIAL_VERSION, contents, someTagsList, someUnitsMap, publishedUnitsMap, someContentProblemsMap);

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

        Set<Content> elements = new HashSet<>();
        Content rootNode = createContentHierarchy(numChildLevels, elements);

        Set<Content> contents = Whitebox.invokeMethod(
                defaultContentIndexer, "flattenContentObjects", rootNode);

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

    /**
     * Test that recordContentTypeSpecificError does not add an error message to indexProblemCache when neither
     * significant figure is set whilst disregardSignificantFigures is not set
     *
     * @throws Exception as reflection may not find method
     */
    @Test
    public void recordContentTypeSpecificError_noSigFigSet_checkNoError()
            throws Exception {
        // ARRANGE
        final Map<Content, List<String>> indexProblemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        contents.add(createIsaacNumericQuestion(false, null, null, "null"));

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(
                    defaultContentIndexer,
                    "recordContentTypeSpecificError",
                    "",
                    content,
                    indexProblemCache
            );
        }

        // ASSERT
        for (Content key : indexProblemCache.keySet()) {
            assertTrue(indexProblemCache.get(key).isEmpty());
        }
    }

    /**
     * Test that recordContentTypeSpecificError adds an error message to indexProblemCach when only one significant
     * figure is specified and the other is set to null whilst disregardSignificantFigures is not set
     *
     * @throws Exception as reflection may not find method
     */
    @Test
    public void recordContentTypeSpecificError_onlyOneSigFigSet_checkErrorIsCorrect()
            throws Exception {
        // ARRANGE
        final Map<Content, List<String>> indexProblemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        contents.add(createIsaacNumericQuestion(false, 1, null, "min_set"));
        contents.add(createIsaacNumericQuestion(false, null, 1, "max_set"));

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(
                    defaultContentIndexer,
                    "recordContentTypeSpecificError",
                    "",
                    content,
                    indexProblemCache
            );
        }

        // ASSERT
        for (Content key : indexProblemCache.keySet()) {
            for (String problem : indexProblemCache.get(key)) {
                assertTrue(problem.contains("has only one significant figure bound"));
            }
        }
    }

    /**
     * Test that recordContentTypeSpecificError adds an error message to indexProblemCache when both significant
     * figures are specified but either is less than 1 whilst disregardSignificantFigures is not set
     *
     * @throws Exception as reflection may not find method
     */
    @Test
    public void recordContentTypeSpecificError_bothSigFigSetLessThan1_checkErrorIsCorrect()
            throws Exception {
        // ARRANGE
        final Map<Content, List<String>> indexProblemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        contents.add(createIsaacNumericQuestion(false, 1, 0, "max_fault"));
        contents.add(createIsaacNumericQuestion(false, 0, 1, "min_fault"));
        contents.add(createIsaacNumericQuestion(false, 0, 0, "both_fault"));

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(
                    defaultContentIndexer,
                    "recordContentTypeSpecificError",
                    "",
                    content,
                    indexProblemCache
            );
        }

        // ASSERT
        for (Content key : indexProblemCache.keySet()) {
            for (String problem : indexProblemCache.get(key)) {
                assertTrue(problem.contains("either bound might be less than 1"));
            }
        }
    }

    /**
     * Test that recordContentTypeSpecificError adds an error message to indexProblemCache when the maximum significant
     * figure is less than the minimum significant figure (both above 1) whilst disregardSignificantFigures is not set
     *
     * @throws Exception as reflection may not find method
     */
    @Test
    public void recordContentTypeSpecificError_maxLessThanMin_checkErrorIsCorrect()
            throws Exception {
        // ARRANGE
        final Map<Content, List<String>> indexProblemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        contents.add(createIsaacNumericQuestion(false, 2, 1, "order_fault"));

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(
                    defaultContentIndexer,
                    "recordContentTypeSpecificError",
                    "",
                    content,
                    indexProblemCache
            );
        }

        // ASSERT
        for (Content key : indexProblemCache.keySet()) {
            for (String problem : indexProblemCache.get(key)) {
                assertTrue(problem.contains("The upper bound may be below the lower bound"));
            }
        }
    }

    /**
     * Test that recordContentTypeSpecificError does not add an error message to indexProblemCache when the minimum
     * significant figure is less than the maximum significant figure (both above 1) whilst disregardSignificantFigures
     * is not set
     *
     * @throws Exception as reflection may not find method
     */
    @Test
    public void recordContentTypeSpecificError_minLessThanMax_checkNoError()
            throws Exception {
        // ARRANGE
        final Map<Content, List<String>> indexProblemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        contents.add(createIsaacNumericQuestion(false, 1, 2, "order_correct"));

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(
                    defaultContentIndexer,
                    "recordContentTypeSpecificError",
                    "",
                    content,
                    indexProblemCache
            );
        }

        // ASSERT
        for (Content key : indexProblemCache.keySet()) {
            assertTrue(indexProblemCache.get(key).isEmpty());
        }
    }

    /**
     * Test that recordContentTypeSpecificError does not add an error message to indexProblemCache when
     * disregardSignificantFigures is not set
     *
     * @throws Exception as reflection may not find method
     */
    @Test
    public void recordContentTypeSpecificError_disregardSigFigsSet_checkNoError()
            throws Exception {
        // ARRANGE
        final Map<Content, List<String>> indexProblemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        contents.add(createIsaacNumericQuestion(true, null, null, "null"));
        contents.add(createIsaacNumericQuestion(true, 1, null, "min_set"));
        contents.add(createIsaacNumericQuestion(true, null, 1, "max_set"));
        contents.add(createIsaacNumericQuestion(true, 0, 1, "min_fault"));
        contents.add(createIsaacNumericQuestion(true, 1, 0, "max_fault"));
        contents.add(createIsaacNumericQuestion(true, 0, 0, "both_fault"));
        contents.add(createIsaacNumericQuestion(true, 2, 1, "order_fault"));
        contents.add(createIsaacNumericQuestion(true, 1, 2, "order_correct"));

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(
                    defaultContentIndexer,
                    "recordContentTypeSpecificError",
                    "",
                    content,
                    indexProblemCache
            );
        }

        // ASSERT
        for (Content key : indexProblemCache.keySet()) {
            assertTrue(indexProblemCache.get(key).isEmpty());
        }
    }

    @Test
    public void recordContentTypeSpecificError_dndQuestionCorrect_checkNoError() throws Exception {
        // ARRANGE
        final Map<Content, List<String>> problemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        var dndQuestion = IsaacDndValidatorTest.question(
            IsaacDndValidatorTest.correctChoice(IsaacDndValidatorTest.item(IsaacDndValidatorTest.item_3cm, "leg_1"))
        );
        dndQuestion.setChildren(List.of(new Content("[drop-zone:leg_1]")));
        contents.add(dndQuestion);

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(defaultContentIndexer, "recordContentTypeSpecificError", "", content, problemCache);
        }

        // ASSERT
        assertEquals(0, problemCache.size());
    }

    @Test
    public void recordContentTypeSpecificError_dndQuestionNoDropZones_checkErrorIsCorrect() throws Exception {
        // ARRANGE
        final Map<Content, List<String>> problemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        var dndQuestion = IsaacDndValidatorTest.question(
            IsaacDndValidatorTest.correctChoice(IsaacDndValidatorTest.item(IsaacDndValidatorTest.item_3cm, "leg_1"))
        );
        dndQuestion.setChildren(null);
        dndQuestion.setCanonicalSourceFile("");
        contents.add(dndQuestion);

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(defaultContentIndexer, "recordContentTypeSpecificError", "", content, problemCache);
        }

        // ASSERT
        Collection<List<String>> expected = List.of(List.of(
            String.format("Drag-and-drop Question: %s has a problem. This question doesn't have any drop zones.",
                          dndQuestion.getId())));
        assertEquals(expected, List.copyOf(problemCache.values()));
    }

    @Test
    public void recordContentTypeSpecificError_dndQuestionDuplicateDropZones_checkErrorIsCorrect() throws Exception {
        // ARRANGE
        final Map<Content, List<String>> problemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        var dndQuestion = IsaacDndValidatorTest.question(
                IsaacDndValidatorTest.correctChoice(IsaacDndValidatorTest.item(IsaacDndValidatorTest.item_3cm, "leg_1"))
        );
        dndQuestion.setChildren(List.of(new Content("[drop-zone:leg_1] [drop-zone:leg_1]")));
        dndQuestion.setCanonicalSourceFile("");
        contents.add(dndQuestion);

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(defaultContentIndexer, "recordContentTypeSpecificError", "", content, problemCache);
        }

        // ASSERT
        Collection<List<String>> expected = List.of(List.of(
                String.format("Drag-and-drop Question: %s has a problem. This question contains duplicate drop zones.",
                        dndQuestion.getId())));
        assertEquals(expected, List.copyOf(problemCache.values()));
    }

    @Test
    public void recordContentTypeSpecificError_dndQuestionUnusedDropZones_checkErrorIsCorrect() throws Exception {
        // ARRANGE
        final Map<Content, List<String>> problemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        var dndQuestion = IsaacDndValidatorTest.question(
                IsaacDndValidatorTest.correctChoice(IsaacDndValidatorTest.item(IsaacDndValidatorTest.item_3cm, "leg_1"))
        );
        dndQuestion.setChildren(List.of(new Content("[drop-zone:leg_1] [drop-zone:leg_2]")));
        dndQuestion.setCanonicalSourceFile("");
        contents.add(dndQuestion);

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(defaultContentIndexer, "recordContentTypeSpecificError", "", content, problemCache);
        }

        // ASSERT
        Collection<List<String>> expected = List.of(List.of(
                String.format("Drag-and-drop Question: %s has a problem. %s",
                        dndQuestion.getId(), FEEDBACK_QUESTION_UNUSED_DZ)));
        assertEquals(expected, List.copyOf(problemCache.values()));
    }

    @Test
    public void recordContentTypeSpecificError_dndQuestionUnrecognisedDropZones_checkErrorIsCorrect() throws Exception {
        // ARRANGE
        final Map<Content, List<String>> problemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        var dndQuestion = IsaacDndValidatorTest.question(
                IsaacDndValidatorTest.correctChoice(IsaacDndValidatorTest.item(IsaacDndValidatorTest.item_3cm, "leg_2"))
        );
        dndQuestion.setChildren(List.of(new Content("[drop-zone:leg_1]")));
        dndQuestion.setCanonicalSourceFile("");
        contents.add(dndQuestion);

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(defaultContentIndexer, "recordContentTypeSpecificError", "", content, problemCache);
        }

        // ASSERT
        Collection<List<String>> expected = List.of(List.of(
                String.format("Drag-and-drop Question: %s has a problem. This question contains an answer with "
                        + "unrecognised drop zones.", dndQuestion.getId())));
        assertEquals(expected, List.copyOf(problemCache.values()));
    }

    @Test
    public void recordContentTypeSpecificError_answerDuplicateDropZones_checkErrorIsCorrect() throws Exception {
        // ARRANGE
        final Map<Content, List<String>> problemCache = new HashMap<>();
        final List<Content> contents = new LinkedList<>();
        var dndQuestion = IsaacDndValidatorTest.question(
            IsaacDndValidatorTest.correctChoice(
                IsaacDndValidatorTest.item(IsaacDndValidatorTest.item_3cm, "leg_1"),
                IsaacDndValidatorTest.item(IsaacDndValidatorTest.item_3cm, "leg_1"))
        );
        dndQuestion.setChildren(List.of(new Content("[drop-zone:leg_1]")));
        dndQuestion.setCanonicalSourceFile("");
        contents.add(dndQuestion);

        // ACT
        for (Content content : contents) {
            Whitebox.invokeMethod(defaultContentIndexer, "recordContentTypeSpecificError", "", content, problemCache);
        }

        // ASSERT
        Collection<List<String>> expected = List.of(List.of(
                String.format("Drag-and-drop Question: %s has a problem. The question is invalid, because it has an"
                              + " answer with duplicate drop zones.", dndQuestion.getId())));
        assertEquals(expected, List.copyOf(problemCache.values()));
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
     * Helper method for the recordContentTypeSpecificError tests,
     * generates an IsaacNumericQuestion with provided significant figure information
     *
     * @param disregardSigFig
     *              - Whether to set the disregardSignificantFigures to true
     * @param sigFigMin
     *              - The minimum significant figures value
     * @param sigFigMax
     *              - The maximum significant figures value
     * @return The new IsaacNumericQuestion object
     */
    private IsaacNumericQuestion createIsaacNumericQuestion(final Boolean disregardSigFig,
                                                            final Integer sigFigMin,
                                                            final Integer sigFigMax,
                                                            final String id) {
        IsaacNumericQuestion question = new IsaacNumericQuestion();

        // TODO: See if there is a "better" way of initialising object fields
        question.setId(id);
        question.setTitle("");
        question.setType("isaacQuestion");
        question.setChoices(new LinkedList<>());

        question.setDisregardSignificantFigures(disregardSigFig);
        question.setSignificantFiguresMin(sigFigMin);
        question.setSignificantFiguresMax(sigFigMax);

        return question;
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

        return new GitContentManager(database, searchProvider, contentMapper, contentSubclassMapper);
    }
}
