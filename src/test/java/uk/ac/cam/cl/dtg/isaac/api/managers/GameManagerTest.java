/**
 * Copyright 2022 Matthew Trew
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

package uk.ac.cam.cl.dtg.isaac.api.managers;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import ma.glasnost.orika.MapperFacade;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager.BooleanSearchClause;

public class GameManagerTest {
  private GitContentManager dummyContentManager;
  private GameboardPersistenceManager dummyGameboardPersistenceManager;
  private MapperFacade dummyMapper;
  private QuestionManager dummyQuestionManager;

  @Before
  public void setUp() {
    this.dummyContentManager = createMock(GitContentManager.class);
    this.dummyGameboardPersistenceManager = createMock(GameboardPersistenceManager.class);
    this.dummyMapper = createMock(MapperFacade.class);
    this.dummyQuestionManager = createMock(QuestionManager.class);
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
        "latest"
    );

    // configure the mock GitContentManager to record the filters that are sent to it by getNextQuestionsForFilter()
    Capture<List<BooleanSearchClause>> capturedFilters = Capture.newInstance();
    expect(dummyContentManager.findByFieldNamesRandomOrder(
        capture(capturedFilters),
        anyInt(),
        anyInt(),
        anyLong())
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
