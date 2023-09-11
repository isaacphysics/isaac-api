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

package uk.ac.cam.cl.dtg.segue.search;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

public class ElasticSearchProviderTest {

  @Test
  public void generateFilterQuery_simpleFilterInstruction_returnsFilteredQuery() {
    // Arrange
    ElasticSearchProvider provider = new ElasticSearchProvider(null);

    Map<String, AbstractFilterInstruction> filters = new HashMap<>();
    filters.put("published", new SimpleFilterInstruction("true"));

    BoolQueryBuilder expectedQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.boolQuery()
            .must(QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("published", "true"))));

    // Act
    QueryBuilder actualQuery = provider.generateFilterQuery(filters);

    // Assert
    assertEquals(expectedQuery, actualQuery);
  }

  @Test
  public void generateFilterQuery_simpleExclusionInstruction_returnsExclusionQuery() {
    // Arrange
    ElasticSearchProvider provider = new ElasticSearchProvider(null);

    Map<String, AbstractFilterInstruction> filters = new HashMap<>();
    filters.put("published", new SimpleExclusionInstruction("true"));

    BoolQueryBuilder expectedQuery = QueryBuilders.boolQuery()
        .mustNot(QueryBuilders.boolQuery()
            .must(QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("published", "true"))));

    // Act
    QueryBuilder actualQuery = provider.generateFilterQuery(filters);

    // Assert
    assertEquals(expectedQuery, actualQuery);
  }

  @Test
  public void generateFilterQuery_simpleFilterInstructionAndExclusionInstruction_returnsFilteredAndExcludedQuery() {
    // Arrange
    ElasticSearchProvider provider = new ElasticSearchProvider(null);

    Map<String, AbstractFilterInstruction> filters = new HashMap<>();
    filters.put("published", new SimpleFilterInstruction("true"));
    filters.put("tags", new SimpleExclusionInstruction("regression_test"));

    BoolQueryBuilder expectedMustQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.boolQuery()
            .must(QueryBuilders.matchQuery("published", "true")));

    BoolQueryBuilder expectedMustNotQuery = QueryBuilders.boolQuery()
        .must(QueryBuilders.boolQuery()
            .must(QueryBuilders.matchQuery("tags", "regression_test")));

    BoolQueryBuilder expectedQuery = QueryBuilders.boolQuery().must(expectedMustQuery);
    expectedQuery.mustNot(expectedMustNotQuery);

    // Act
    QueryBuilder actualQuery = provider.generateFilterQuery(filters);

    // Assert
    assertEquals(expectedQuery, actualQuery);
  }
}
