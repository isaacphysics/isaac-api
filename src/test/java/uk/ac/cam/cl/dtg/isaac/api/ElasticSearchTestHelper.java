package uk.ac.cam.cl.dtg.isaac.api;

import org.json.JSONObject;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentSubclassMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.etl.ElasticSearchIndexer;

import java.util.List;

import static org.testcontainers.shaded.com.google.common.collect.Maps.immutableEntry;

/**
 * Test utility for indexing content directly into Elasticsearch, bypassing the Git content pipeline.
 * Use instead of text fixtures for more self-contained tests and to avoid needing to use the same
 * database state across all tests.
 * */
public class ElasticSearchTestHelper {
    private final ElasticSearchIndexer elasticSearchProvider;
    private final GitContentManager contentManager;
    private final ContentSubclassMapper contentMapper;

    /** Constructor.*/
    public ElasticSearchTestHelper(final ElasticSearchIndexer elasticSearchProvider,
                                   final GitContentManager contentManager,
                                   final ContentSubclassMapper contentMapper) {
        this.elasticSearchProvider = elasticSearchProvider;
        this.contentManager = contentManager;
        this.contentMapper = contentMapper;
    }

    /** Indexes a typed content object and returns it. */
    public <T extends Content> T persist(final T content) throws Exception {
        elasticSearchProvider.bulkIndexWithIDs(
            contentManager.getCurrentContentSHA(),
            "content",
            List.of(immutableEntry(
                content.getId(), contentMapper.getSharedContentObjectMapper().writeValueAsString(content))
            )
        );
        return content;
    }

    /** Indexes a raw JSON content object with a fixed ID of {@code "i1"} and returns it. */
    public JSONObject persistJSON(final JSONObject contentJSON) throws Exception {
        contentJSON.put("id", "i1");
        elasticSearchProvider.bulkIndexWithIDs(
            contentManager.getCurrentContentSHA(),
            "content",
            List.of(immutableEntry("i1", contentJSON.toString()))
        );
        return contentJSON;
    }
}
