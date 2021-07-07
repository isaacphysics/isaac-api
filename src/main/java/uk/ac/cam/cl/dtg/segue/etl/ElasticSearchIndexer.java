package uk.ac.cam.cl.dtg.segue.etl;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Ian on 17/10/2016.
 */
class ElasticSearchIndexer extends ElasticSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchIndexer.class);
    private final Map<String, List<String>> rawFieldsListByType = new HashMap<>();
    private final Map<String, List<String>> nestedFieldsByType = new HashMap<>();

    /**
     * Constructor for creating an instance of the ElasticSearchProvider Object.
     *
     * @param searchClient - the client that the provider should be using.
     */
    @Inject
    public ElasticSearchIndexer(Client searchClient) {
        super(searchClient);
        rawFieldsListByType.put("content", Lists.newArrayList("id", "title"));
        rawFieldsListByType.put("school", Lists.newArrayList("urn"));
        nestedFieldsByType.put("content", Lists.newArrayList("audience"));
    }


    public void indexObject(final String indexBase, final String indexType, final String content)
            throws SegueSearchException {
        indexObject(indexBase, indexType, content, null);
    }


    void bulkIndex(final String indexBase, final String indexType, final List<Map.Entry<String, String>> dataToIndex)
            throws SegueSearchException {

        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);

        // check index already exists if not execute any initialisation steps.
        if (!this.hasIndex(indexBase, indexType)) {
            if (this.rawFieldsListByType.containsKey(indexType) || this.nestedFieldsByType.containsKey(indexType)) {
                this.sendMappingCorrections(typedIndex, indexType);
            }
        }

        // build bulk request
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (Map.Entry<String, String> itemToIndex : dataToIndex) {
            bulkRequest.add(client.prepareIndex(typedIndex, indexType, itemToIndex.getKey())
                    .setSource(itemToIndex.getValue(), XContentType.JSON));
        }

        try {
            // execute bulk request
            BulkResponse bulkResponse = bulkRequest.setTimeout("180s").setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE).execute().actionGet();
            if (bulkResponse.hasFailures()) {
                // process failures by iterating through each bulk response item
                for (BulkItemResponse itemResponse : bulkResponse.getItems()) {
                    if (itemResponse.isFailed()) {
                        log.error("Unable to index the following item: " + itemResponse.getFailureMessage());
                    }
                }
            }
        } catch (ElasticsearchException e) {
            throw new SegueSearchException("Error during bulk index operation.", e);
        }
    }


    void indexObject(final String indexBase, final String indexType, final String content, final String uniqueId)
            throws SegueSearchException {
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        // check index already exists if not execute any initialisation steps.
        if (!this.hasIndex(indexBase, indexType)) {
            if (this.rawFieldsListByType.containsKey(indexType)) {
                this.sendMappingCorrections(typedIndex, indexType);
            }
        }

        try {
            IndexResponse indexResponse = client.prepareIndex(typedIndex, indexType, uniqueId)
                    .setSource(content, XContentType.JSON)
                    .execute()
                    .actionGet();
            log.debug("Document: " + indexResponse.getId() + " indexed.");

        } catch (ElasticsearchException e) {
            throw new SegueSearchException("Error during index operation.", e);
        }
    }

    public boolean expungeEntireSearchCache() {
        return this.expungeTypedIndexFromSearchCache("_all");
    }

    boolean expungeTypedIndexFromSearchCache(final String typedIndex) {
        try {
            log.info("Sending delete request to ElasticSearch for search index: " + typedIndex);
            client.admin().indices().delete(new DeleteIndexRequest(typedIndex)).actionGet();
        } catch (ElasticsearchException e) {
            log.error("ElasticSearch exception while trying to delete index " + typedIndex, e);
            return false;
        }

        return true;
    }

    boolean expungeIndexFromSearchCache(final String indexBase, final String indexType) {
        Validate.notBlank(indexBase);
        Validate.notBlank(indexType);
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        return this.expungeTypedIndexFromSearchCache(typedIndex);
    }

    boolean addOrMoveIndexAlias(final String aliasBase, final String indexBaseTarget, final List<String> indexTypeTargets) {
        String indexWithPrevious = null; // This is the index that has the <alias>_previous alias
        String indexWithCurrent = null; // This is the index that has the <alias> alias.

        for (String indexTypeTarget : indexTypeTargets) {
            String typedAlias = ElasticSearchProvider.produceTypedIndexName(aliasBase, indexTypeTarget);
            String typedIndexTarget = ElasticSearchProvider.produceTypedIndexName(indexBaseTarget, indexTypeTarget);

            // First, find where <alias>_previous points.
            ImmutableOpenMap<String, List<AliasMetadata>> returnedPreviousAliases =
                    client.admin().indices().getAliases(new GetAliasesRequest().aliases(typedAlias + "_previous")).actionGet().getAliases();

            Iterator<String> indexIterator = returnedPreviousAliases.keysIt();
            while (indexIterator.hasNext()) {
                String indexName = indexIterator.next();
                for (AliasMetadata aliasMetaData : returnedPreviousAliases.get(indexName)) {
                    if (aliasMetaData.alias().equals(typedAlias + "_previous")) {
                        indexWithPrevious = indexName;
                    }
                }
            }

            // Now find where <alias> points
            ImmutableOpenMap<String, List<AliasMetadata>> returnedAliases =
                    client.admin().indices().getAliases(new GetAliasesRequest().aliases(typedAlias)).actionGet().getAliases();
            
            indexIterator = returnedAliases.keysIt();
            while (indexIterator.hasNext()) {
                String indexName = indexIterator.next();
                for (AliasMetadata aliasMetadata : returnedAliases.get(indexName)) {
                    if (aliasMetadata.alias().equals(typedAlias)) {
                        indexWithCurrent = indexName;
                    }
                }
            }

            if (indexWithCurrent != null && indexWithCurrent.equals(typedIndexTarget)) {
                log.info("Not moving alias '" + typedAlias + "' - it already points to the right index.");
            } else {
                IndicesAliasesRequestBuilder reqBuilder = client.admin().indices().prepareAliases();

                if (indexWithCurrent != null) {
                    // Remove the alias from the place it's currently pointing
                    reqBuilder.removeAlias(indexWithCurrent, typedAlias);
                    if (indexWithPrevious != null) {
                        // Remove <alias>_previous from wherever it's currently pointing.
                        reqBuilder.removeAlias(indexWithPrevious, typedAlias + "_previous");
                    }
                    // Move <alias>_previous to wherever <alias> was pointing.
                    reqBuilder.addAlias(indexWithCurrent, typedAlias + "_previous");
                }

                // Point <alias> to the right place.
                reqBuilder.addAlias(typedIndexTarget, typedAlias);
                reqBuilder.execute().actionGet();
            }
        }
        this.expungeOldIndices();
        return true;
    }

    private void expungeOldIndices() {
        // This deletes any indices that don't have aliases pointing to them.
        // If you want an index kept, make sure it has an alias!
        ImmutableOpenMap<String, IndexMetadata> indices = client.admin().cluster().prepareState().execute().actionGet().getState().getMetadata().indices();

        for(ObjectObjectCursor<String, IndexMetadata> c: indices) {
            if (c.value.getAliases().size() == 0) {
                log.info("Index " + c.key + " has no aliases. Removing.");
                this.expungeTypedIndexFromSearchCache(c.key);
            }
        }
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
     * @param typedIndex
     *            - type suffixed index to send the mapping corrections to.
     */
    private void sendMappingCorrections(final String typedIndex, String indexType) {
        try {
            // Specify index settings
            CreateIndexRequestBuilder indexBuilder = client.admin().indices().prepareCreate(typedIndex).setSettings(
                    XContentFactory.jsonBuilder()
                            .startObject()
                                .field("index.mapping.total_fields.limit", "9999")
//                                // To apply the english analyzer as default (performs stemming)
//                                .startObject("analysis")
//                                    .startObject("analyzer")
//                                        .startObject("default")
//                                            .field("type", "english")
//                                        .endObject()
//                                    .endObject()
//                                .endObject()
                            .endObject());

            // Add mapping to specify properties of the index
            final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()
                    .startObject().startObject(indexType).startObject("properties");

            // Add mapping to specify raw, un-analyzed fields
            for (String fieldName : this.rawFieldsListByType.getOrDefault(indexType, Collections.emptyList())) {
                log.debug("Sending raw mapping correction for " + fieldName + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX);
                mappingBuilder
                        .startObject(fieldName)
                            .field("type", "text")
                            .field("index", "true")
                            .startObject("fields")
                                .startObject(Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX)
                                    .field("type", "keyword")
                                    .field("index", "true")
                                .endObject()
                            .endObject()
                        .endObject();
            }

            // Add mapping to specify nested object fields
            for (String fieldName : this.nestedFieldsByType.getOrDefault(indexType, Collections.emptyList())) {
                log.debug("Sending mapping correction for nested field " + fieldName);
                mappingBuilder.startObject(fieldName).field("type", "nested").endObject();
            }

            mappingBuilder.endObject().endObject().endObject();
            indexBuilder.addMapping(indexType, mappingBuilder);

            indexBuilder.execute().actionGet();

        } catch (IOException e) {
            log.error("Error while sending mapping correction " + "instructions to the ElasticSearch Server", e);
        }
    }
}
