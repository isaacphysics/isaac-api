package uk.ac.cam.cl.dtg.segue.etl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.client.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by Ian on 17/10/2016.
 */
class ElasticSearchIndexer extends ElasticSearchProvider {
    private static final Integer BULK_REQUEST_BATCH_SIZE = 10000;  // Huge requests overwhelm ES, so batch!

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchIndexer.class);
    private final Map<String, List<String>> rawFieldsListByType = new HashMap<>();
    private final Map<String, List<String>> nestedFieldsByType = new HashMap<>();

    /**
     * Constructor for creating an instance of the ElasticSearchProvider Object.
     *
     * @param searchClient - the client that the provider should be using.
     */
    @Inject
    public ElasticSearchIndexer(final ElasticsearchClient searchClient) {
        super(searchClient);
        rawFieldsListByType.put("content", Lists.newArrayList("id", "title", "subtitle"));
        rawFieldsListByType.put("school", Lists.newArrayList("urn"));
        nestedFieldsByType.put("content", Lists.newArrayList("audience"));
    }


    public void indexObject(final String indexBase, final String indexType, final String content)
            throws SegueSearchException {
        indexObject(indexBase, indexType, content, null);
    }

    /**
     *
     * @param buildBulkRequest a function that takes an elasticsearch typed index name, and produces a (populated) BulkRequestBuilder
     * @throws SegueSearchException
     */
    private void executeBulkIndexRequest(final String indexBase, final String indexType, final Function<String, BulkRequest> buildBulkRequest)
            throws SegueSearchException {
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);

        // check index already exists if not execute any initialisation steps.
        if (!this.hasIndex(indexBase, indexType)) {
            if (this.rawFieldsListByType.containsKey(indexType) || this.nestedFieldsByType.containsKey(indexType)) {
                this.sendMappingCorrections(typedIndex, indexType);
            }
        }

        // execute bulk request builder function
        BulkRequest bulkRequest = buildBulkRequest.apply(typedIndex);

        try {
            // increase default timeouts
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(360000)
                    .setSocketTimeout(360000)
                    .build();
            RequestOptions options = RequestOptions.DEFAULT.toBuilder()
                    .setRequestConfig(requestConfig)
                    .build();

            // execute bulk request
            BulkResponse bulkResponse = client.bulk(bulkRequest.timeout("180s").setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE), options);
            if (bulkResponse.hasFailures()) {
                // process failures by iterating through each bulk response item
                for (BulkItemResponse itemResponse : bulkResponse.getItems()) {
                    if (itemResponse.isFailed()) {
                        log.error("Unable to index the following item: {}", itemResponse.getFailureMessage());
                    }
                }
                throw new SegueSearchException("Error during bulk index operation, some items failed!");
            }
        } catch (ElasticsearchException | IOException e) {
            throw new SegueSearchException("Error during bulk index operation.", e);
        }
    }

    void bulkIndex(final String indexBase, final String indexType, final List<String> dataToIndex)
            throws SegueSearchException {

        Iterable<List<String>> partitions = Iterables.partition(dataToIndex, BULK_REQUEST_BATCH_SIZE);
        // For loop not lambda in forEach, to allow checked exceptions to propagate correctly.
        for (List<String> batch : partitions) {
            executeBulkIndexRequest(indexBase, indexType, typedIndex -> {
                // build bulk request, these items don't have ids
                BulkRequest request = new BulkRequest();
                batch.forEach(itemToIndex -> request.add(
                    new IndexRequest(typedIndex).source(itemToIndex, XContentType.JSON)
                ));
                return request;
            });
        }
    }

    void bulkIndexWithIDs(final String indexBase, final String indexType, final List<Map.Entry<String, String>> dataToIndex)
            throws SegueSearchException {

        Iterable<List<Map.Entry<String, String>>> partitions = Iterables.partition(dataToIndex, BULK_REQUEST_BATCH_SIZE);
        // For loop not lambda in forEach, to allow checked exceptions to propagate correctly.
        for (List<Map.Entry<String, String>> batch : partitions) {
            executeBulkIndexRequest(indexBase, indexType, typedIndex -> {
                // build bulk request, ids of data items are specified by their keys
                BulkRequest bulkRequest = new BulkRequest();
                batch.forEach(itemToIndex -> bulkRequest.add(
                        new IndexRequest(typedIndex).id(itemToIndex.getKey()).source(itemToIndex.getValue(), XContentType.JSON)
                ));
                return bulkRequest;
            });
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
            IndexResponse indexResponse = client.index(ir -> ir.index(typedIndex).id(uniqueId).withJson(new StringReader(content)));
            log.debug("Document: {} indexed.", indexResponse.id());

        } catch (ElasticsearchException | IOException e) {
            throw new SegueSearchException("Error during index operation.", e);
        }
    }

    public boolean expungeEntireSearchCache() {
        return this.expungeTypedIndexFromSearchCache("_all");
    }

    boolean expungeTypedIndexFromSearchCache(final String typedIndex) {
        try {
            log.info("Sending delete request to ElasticSearch for search index: {}", typedIndex);
            client.indices().delete(dir -> dir.index(typedIndex));
        } catch (ElasticsearchException | IOException e) {
            log.error("ElasticSearch exception while trying to delete index {}, it might not have existed.", typedIndex, e);
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
            ImmutableMap<String, Set<AliasMetadata>> returnedPreviousAliases = null;
            try {
                returnedPreviousAliases = ImmutableMap.copyOf(
                        client.indices().getAlias(new GetAliasesRequest().aliases(typedAlias + "_previous"), RequestOptions.DEFAULT)
                        .getAliases());
            } catch (IOException e) {
                log.error("Failed to retrieve existing previous alias {}, not moving alias!", typedAlias + "_previous");
                continue;
            }

            Iterator<String> indexIterator = returnedPreviousAliases.keySet().iterator();
            while (indexIterator.hasNext()) {
                String indexName = indexIterator.next();
                for (AliasMetadata aliasMetaData : returnedPreviousAliases.get(indexName)) {
                    if (aliasMetaData.alias().equals(typedAlias + "_previous")) {
                        indexWithPrevious = indexName;
                    }
                }
            }

            // Now find where <alias> points
            ImmutableMap<String, Set<AliasMetadata>> returnedAliases = null;
            try {
                returnedAliases = ImmutableMap.copyOf(
                        client.indices().getAlias(new GetAliasesRequest().aliases(typedAlias), RequestOptions.DEFAULT)
                        .getAliases());
            } catch (IOException e) {
                log.error("Failed to retrieve existing alias {}, not moving alias!", typedAlias);
                continue;
            }

            indexIterator = returnedAliases.keySet().iterator();
            while (indexIterator.hasNext()) {
                String indexName = indexIterator.next();
                for (AliasMetadata aliasMetadata : returnedAliases.get(indexName)) {
                    if (aliasMetadata.alias().equals(typedAlias)) {
                        indexWithCurrent = indexName;
                    }
                }
            }

            if (indexWithCurrent != null && indexWithCurrent.equals(typedIndexTarget)) {
                log.info("Not moving alias '{}' - it already points to the right index.", typedAlias);
            } else {
                IndicesAliasesRequest request = new IndicesAliasesRequest();

                if (indexWithCurrent != null) {
                    // Remove the alias from the place it's currently pointing
                    request.addAliasAction(
                            new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                                    .index(indexWithCurrent)
                                    .alias(typedAlias)
                    );
                    if (indexWithPrevious != null) {
                        // Remove <alias>_previous from wherever it's currently pointing.
                        request.addAliasAction(
                                new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                                .index(indexWithPrevious)
                                .alias(typedAlias + "_previous")
                        );
                    }
                    // Move <alias>_previous to wherever <alias> was pointing.
                    request.addAliasAction(
                            new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                                    .index(indexWithCurrent)
                                    .alias(typedAlias + "_previous")
                    );
                }

                // Point <alias> to the right place.
                request.addAliasAction(
                        new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                                .index(typedIndexTarget)
                                .alias(typedAlias)
                );
                try {
                    client.indices().updateAliases(request, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    log.error("Failed to update alias {}", typedAlias, e);
                    continue;
                }
            }

        }
        return true;
    }

    void deleteAllUnaliasedIndices() {
        // Deleting all unalisaed indices is not a safe operation if alias or index updates are in-progress!
        try {
            GetIndexResponse indices = client.indices().get(new GetIndexRequest("*"), RequestOptions.DEFAULT);
            ImmutableMap<String, List<AliasMetadata>> aliases = ImmutableMap.copyOf(indices.getAliases());
            for (String index : indices.getIndices()) {
                if (!aliases.containsKey(index) || aliases.get(index).isEmpty()) {
                    log.info("Index {} has no aliases. Removing.", index);
                    this.expungeTypedIndexFromSearchCache(index);
                }
            }
        } catch (IOException e) {
            log.error("Failed to expunge old indices", e);
        }
    }

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
            CreateIndexRequest indexRequest = new CreateIndexRequest(typedIndex).settings(
                    XContentFactory.jsonBuilder()
                            .startObject()
                                .field("index.mapping.total_fields.limit", "9999")
                            .endObject()
            );

            // Add mapping to specify properties of the index
            final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject().startObject("properties");

            // Add mapping to specify raw, un-analyzed fields
            for (String fieldName : this.rawFieldsListByType.getOrDefault(indexType, Collections.emptyList())) {
                log.debug("Sending raw mapping correction for {}." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, fieldName);
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
                log.debug("Sending mapping correction for nested field {}", fieldName);
                mappingBuilder.startObject(fieldName).field("type", "nested").endObject();
            }

            mappingBuilder.endObject().endObject();
            indexRequest.mapping(mappingBuilder);

            client.indices().create(indexRequest, RequestOptions.DEFAULT);

        } catch (IOException e) {
            log.error("Error while sending mapping correction " + "instructions to the ElasticSearch Server", e);
        }
    }
}
