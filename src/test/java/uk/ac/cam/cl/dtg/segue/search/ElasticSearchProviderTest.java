package uk.ac.cam.cl.dtg.segue.search;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

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
