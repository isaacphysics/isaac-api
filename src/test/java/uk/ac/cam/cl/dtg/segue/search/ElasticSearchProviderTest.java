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
package uk.ac.cam.cl.dtg.segue.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ElasticSearchProviderTest {

    @Test
    public void generateFilterQuery_simpleFilterInstruction_returnsFilteredQuery() {
        // Arrange
        ElasticSearchProvider provider = new ElasticSearchProvider(null);

        Map<String, AbstractFilterInstruction> filters = new HashMap<>();
        filters.put("published", new SimpleFilterInstruction("true"));

        Query expectedQuery = BoolQuery.of(bq -> bq
            .must(Query.of(q -> q
                .bool(b -> b
                    .must(Query.of(q2 -> q2
                        .bool(b2 -> b2
                            .must(Query.of(q3 -> q3
                                .match(m -> m
                                    .field("published")
                                    .query(p -> p.stringValue("true"))
                                )
                            ))
                        )
                    ))
                )
            ))
        )._toQuery();

        // Act
        Query actualQuery = provider.generateFilterQuery(filters);

        // Assert
        assertEquals(expectedQuery.toString(), actualQuery.toString());
    }

    @Test
    public void generateFilterQuery_simpleExclusionInstruction_returnsExclusionQuery() {
        // Arrange
        ElasticSearchProvider provider = new ElasticSearchProvider(null);

        Map<String, AbstractFilterInstruction> filters = new HashMap<>();
        filters.put("published", new SimpleExclusionInstruction("true"));

        Query expectedQuery = BoolQuery.of(bq -> bq
            .mustNot(Query.of(q -> q
                .bool(b -> b
                    .must(Query.of(q2 -> q2
                        .bool(b2 -> b2
                            .must(Query.of(q3 -> q3
                                .match(m -> m
                                    .field("published")
                                    .query(p -> p.stringValue("true"))
                                )
                            ))
                        )
                    ))
                )
            ))
        )._toQuery();

        // Act
        Query actualQuery = provider.generateFilterQuery(filters);

        // Assert
        assertEquals(expectedQuery.toString(), actualQuery.toString());
    }

    @Test
    public void generateFilterQuery_simpleFilterInstructionAndExclusionInstruction_returnsFilteredAndExcludedQuery() {
        // Arrange
        ElasticSearchProvider provider = new ElasticSearchProvider(null);

        Map<String, AbstractFilterInstruction> filters = new HashMap<>();
        filters.put("published", new SimpleFilterInstruction("true"));
        filters.put("tags", new SimpleExclusionInstruction("regression_test"));

        Query expectedMustQuery = BoolQuery.of(bq -> bq
            .must(Query.of(q -> q
                .bool(b -> b
                    .must(Query.of(q2 -> q2
                        .match(m -> m
                            .field("published")
                            .query(p -> p.stringValue("true"))
                        )
                    ))
                )
            ))
        )._toQuery();

        Query expectedMustNotQuery = BoolQuery.of(bq -> bq
            .must(Query.of(q -> q
                .bool(b -> b
                    .must(Query.of(q2 -> q2
                        .match(m -> m
                            .field("tags")
                            .query(p -> p.stringValue("regression_test"))
                        )
                    ))
                )
            ))
        )._toQuery();

        Query expectedQuery = BoolQuery.of(bq -> bq
            .must(expectedMustQuery)
            .mustNot(expectedMustNotQuery)
        )._toQuery();

        // Act
        Query actualQuery = provider.generateFilterQuery(filters);

        // Assert
        assertEquals(expectedQuery.toString(), actualQuery.toString());
    }
}
