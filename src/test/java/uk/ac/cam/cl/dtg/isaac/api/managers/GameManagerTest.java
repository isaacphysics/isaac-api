/*
 * Copyright 2022 Matthew Trew
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

package uk.ac.cam.cl.dtg.isaac.api.managers;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager.BooleanSearchClause;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;
import uk.ac.cam.cl.dtg.util.YamlLoader;
import uk.ac.cam.cl.dtg.util.mappers.MainMapper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class GameManagerTest {

    private GitContentManager dummyContentManager;
    private GameboardPersistenceManager dummyGameboardPersistenceManager;
    private MainMapper dummyMapper;
    private QuestionManager dummyQuestionManager;
    private AbstractConfigLoader dummyConfigLoader;

    @BeforeEach
    public void setUp() {
        this.dummyContentManager = EasyMock.createMock(GitContentManager.class);
        this.dummyGameboardPersistenceManager = EasyMock.createMock(GameboardPersistenceManager.class);
        this.dummyMapper = EasyMock.createMock(MainMapper.class);
        this.dummyQuestionManager = EasyMock.createMock(QuestionManager.class);
        this.dummyConfigLoader = EasyMock.createMock(YamlLoader.class);

        EasyMock.expect(dummyConfigLoader.getProperty(GAMEBOARD_QUESTION_LIMIT)).andStubReturn("30");
        replay(dummyConfigLoader);
    }

    @Test
    public void getNextQuestionsForFilter_appliesExclusionFilterForDeprecatedQuestions() throws
            ContentManagerException {

        // Arrange
        GameManager gameManager = new GameManager(
                this.dummyContentManager,
                this.dummyGameboardPersistenceManager,
                this.dummyMapper,
                this.dummyQuestionManager,
                this.dummyConfigLoader
        );

        // configure the mock GitContentManager to record the filters that are sent to it by getNextQuestionsForFilter()
        Capture<List<BooleanSearchClause>> capturedFilters = Capture.newInstance();
        EasyMock.expect(dummyContentManager.findByFieldNamesRandomOrder(
                EasyMock.capture(capturedFilters),
                EasyMock.anyInt(),
                EasyMock.anyInt(),
                EasyMock.anyLong())
        ).andStubReturn(new ResultsWrapper<>());
        replay(dummyContentManager);

        // Act
        gameManager.getNextQuestionsForFilter(new GameFilter(), 1, 1L);

        // Assert
        // check that one of the filters sent to GitContentManager was the deprecated question exclusion filter
        List<BooleanSearchClause> filters = capturedFilters.getValues().get(0);
        BooleanSearchClause deprecatedFilter = filters.stream()
                .filter(f -> Objects.equals(f.getField(), "deprecated")).collect(Collectors.toList()).get(0);

        assertNotNull(deprecatedFilter);
        assertEquals(deprecatedFilter.getOperator(), Constants.BooleanOperator.NOT);
        assertEquals(deprecatedFilter.getValues(), Collections.singletonList("true"));
    }
}
