package uk.ac.cam.cl.dtg.segue.etl;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.Validate;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchOperationException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by Ian on 17/10/2016.
 */
class ElasticSearchIndexer extends ElasticSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchIndexer.class);
    private final List<String> rawFieldsList = Lists.newArrayList("id", "title");

    /**
     * Constructor for creating an instance of the ElasticSearchProvider Object.
     *
     * @param searchClient - the client that the provider should be using.
     */
    public ElasticSearchIndexer(Client searchClient) {
        super(searchClient);
    }


//    public void indexObject(final String index, final String indexType, final String content)
//            throws SegueSearchOperationException {
//        indexObject(index, indexType, content, null);
//    }
//
//
    void bulkIndex(final String index, final String indexType, final List<Map.Entry<String, String>> dataToIndex)
            throws SegueSearchOperationException {

        // check index already exists if not execute any initialisation steps.
        if (!this.hasIndex(index)) {
            this.sendMappingCorrections(index, indexType);
        }

        // build bulk request
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (Map.Entry<String, String> itemToIndex : dataToIndex) {
            bulkRequest.add(client.prepareIndex(index, indexType, itemToIndex.getKey()).setSource(
                    itemToIndex.getValue()));
        }

        try {
            // execute bulk request
            BulkResponse bulkResponse = bulkRequest.setRefresh(true).execute().actionGet();
            if (bulkResponse.hasFailures()) {
                // process failures by iterating through each bulk response item
                for (BulkItemResponse itemResponse : bulkResponse.getItems()) {
                    log.error("Unable to index the following item: " + itemResponse.getFailureMessage());
                }
            }
        } catch (ElasticsearchException e) {
            throw new SegueSearchOperationException("Error during bulk index operation.", e);
        }
    }
//
//
//    public void indexObject(final String index, final String indexType, final String content, final String uniqueId)
//            throws SegueSearchOperationException {
//        // check index already exists if not execute any initialisation steps.
//        if (!this.hasIndex(index)) {
//            this.sendMappingCorrections(index, indexType);
//        }
//
//        try {
//            IndexResponse indexResponse = client.prepareIndex(index, indexType, uniqueId).setSource(content).execute()
//                    .actionGet();
//            log.debug("Document: " + indexResponse.getId() + " indexed.");
//
//        } catch (ElasticsearchException e) {
//            throw new SegueSearchOperationException("Error during index operation.", e);
//        }
//    }
//
//    public boolean expungeEntireSearchCache() {
//        return this.expungeIndexFromSearchCache("_all");
//    }
//
//
    public boolean expungeIndexFromSearchCache(final String index) {
        Validate.notBlank(index);

        try {
            log.info("Sending delete request to ElasticSearch for search index: " + index);
            client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
        } catch (ElasticsearchException e) {
            log.error("ElasticSearch exception while trying to delete index " + index, e);
            return false;
        }

        return true;
    }

//
//
//    public boolean expungeIndexTypeFromSearchCache(final String index, final String indexType) {
//        try {
//            DeleteMappingRequest deleteMapping = new DeleteMappingRequest(index).types(indexType);
//            client.admin().indices().deleteMapping(deleteMapping).actionGet();
//        } catch (ElasticsearchException e) {
//            log.error("ElasticSearch exception while trying to delete index " + index + " type " + indexType);
//            return false;
//        }
//        return true;
//    }
//
//
//  public void registerRawStringFields(final List<String> fieldNames) {
//      this.rawFieldsList.addAll(fieldNames);
//  }
//
    /**
     * This function will allow top level fields to have their contents cloned into an unanalysed field with the name
     * {FieldName}.{raw}
     *
     * This is useful if we want to query the original data without ElasticSearch having messed with it.
     *
     * @param index
     *            - index to send the mapping corrections to.
     * @param indexType
     *            - type to send the mapping corrections to.
     */
    private void sendMappingCorrections(final String index, final String indexType) {
        try {
            CreateIndexRequestBuilder indexBuilder = client.admin().indices().prepareCreate(index);

            final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject().startObject(indexType)
                    .startObject("properties");

            for (String fieldName : this.rawFieldsList) {
                log.debug("Sending raw mapping correction for " + fieldName + "."
                        + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX);

                mappingBuilder.startObject(fieldName).field("type", "string").field("index", "analyzed")
                        .startObject("fields").startObject(Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX)
                        .field("type", "string").field("index", "not_analyzed").endObject().endObject().endObject();
            }
            // close off json structure
            mappingBuilder.endObject().endObject().endObject();
            indexBuilder.addMapping(indexType, mappingBuilder);

            // Send Mapping information
            indexBuilder.execute().actionGet();

        } catch (IOException e) {
            log.error("Error while sending mapping correction " + "instructions to the ElasticSearch Server", e);
        }
    }

}
