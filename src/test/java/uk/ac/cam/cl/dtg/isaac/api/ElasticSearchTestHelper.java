package uk.ac.cam.cl.dtg.isaac.api;

import org.json.JSONObject;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentSubclassMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.etl.ElasticSearchIndexer;

import java.util.List;

import static org.testcontainers.shaded.com.google.common.collect.Maps.immutableEntry;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class ElasticSearchTestHelper {
    private final ElasticSearchIndexer elasticSearchProvider;
    private final GitContentManager contentManager;
    private final ContentSubclassMapper contentMapper;

    public ElasticSearchTestHelper(final ElasticSearchIndexer elasticSearchProvider,
                                   final GitContentManager contentManager,
                                   final ContentSubclassMapper contentMapper) {
        this.elasticSearchProvider = elasticSearchProvider;
        this.contentManager = contentManager;
        this.contentMapper = contentMapper;
    }

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
