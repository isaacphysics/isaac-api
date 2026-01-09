package uk.ac.cam.cl.dtg.segue.etl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetAliasResponse;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexState;
import co.elastic.clients.elasticsearch.indices.UpdateAliasesRequest;
import co.elastic.clients.elasticsearch.indices.get_alias.IndexAliases;
import co.elastic.clients.elasticsearch.indices.update_aliases.Action;
import co.elastic.clients.elasticsearch.indices.update_aliases.AddAction;
import co.elastic.clients.elasticsearch.indices.update_aliases.RemoveAction;
import co.elastic.clients.util.BinaryData;
import co.elastic.clients.util.ContentType;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by Ian on 17/10/2016.
 */
class ElasticSearchIndexer extends ElasticSearchProvider {
    private static final Integer BULK_REQUEST_BATCH_SIZE = 10000;  // Huge requests overwhelm ES, so batch!
    private static final String ALL = "_all";
    private static final String PREVIOUS = "_previous";

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
            // execute bulk request
            BulkResponse bulkResponse = client.bulk(bulkRequest);

            if (bulkResponse.errors()) {
                // process failures by iterating through each bulk response item
                for (BulkResponseItem responseItem : bulkResponse.items()) {
                    if (null != responseItem.error()) {
                        log.error("Unable to index the following item: {}", responseItem.error().reason());
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
                List<BulkOperation> ops = new java.util.ArrayList<>();
                batch.forEach(itemToIndex -> ops.add(BulkOperation.of(b -> b
                    .index(idx -> idx
                        .index(typedIndex)
                            .document(BinaryData.of(itemToIndex.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON))
                ))));

                return new BulkRequest.Builder()
                    .operations(ops)
                    .timeout(Time.of(t -> t.time("180s")))
                    .refresh(Refresh.True)
                    .build();
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
                List<BulkOperation> ops = new java.util.ArrayList<>();
                batch.forEach(itemToIndex -> ops.add(BulkOperation.of(b -> b
                    .index(idx -> idx
                        .index(typedIndex)
                            .id(itemToIndex.getKey())
                                .document(BinaryData.of(itemToIndex.getValue().getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON))
                    ))));

                return new BulkRequest.Builder()
                    .operations(ops)
                    .timeout(Time.of(t -> t.time("180s")))
                    .refresh(Refresh.True)
                    .build();
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
        return this.expungeTypedIndexFromSearchCache(ALL);
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
            try {
                GetAliasResponse previousResponse = client.indices().getAlias(g -> g.name(ALL));
                for (Map.Entry<String, IndexAliases> entry : previousResponse.result().entrySet()) {
                    if (entry.getValue().aliases() != null && entry.getValue().aliases().containsKey(typedAlias + PREVIOUS)) {
                        indexWithPrevious = entry.getKey();
                    }
                }
            } catch (IOException e) {
                log.error("Failed to retrieve existing previous alias {}, not moving alias!", typedAlias + PREVIOUS);
                continue;
            }

            // Now find where <alias> points
            try {
                GetAliasResponse aliasResponse = client.indices().getAlias(g -> g.name(ALL));
                for (Map.Entry<String, IndexAliases> entry : aliasResponse.result().entrySet()) {
                    if (entry.getValue().aliases() != null && entry.getValue().aliases().containsKey(typedAlias)) {
                        indexWithCurrent = entry.getKey();
                    }
                }
            } catch (IOException e) {
                log.error("Failed to retrieve existing alias {}, not moving alias!", typedAlias);
                continue;
            }

            if (indexWithCurrent != null && indexWithCurrent.equals(typedIndexTarget)) {
                log.info("Not moving alias '{}' - it already points to the right index.", typedAlias);
            } else {
                UpdateAliasesRequest.Builder request = new UpdateAliasesRequest.Builder();

                if (indexWithCurrent != null) {
                    // Remove the alias from the place it's currently pointing
                    String finalIndexWithCurrent = indexWithCurrent;
                    request.actions(Action.of(a -> a
                            .remove(RemoveAction.of(ra -> ra
                                    .index(finalIndexWithCurrent)
                                    .alias(typedAlias)
                            ))
                    ));

                    if (indexWithPrevious != null) {
                        // Remove <alias>_previous from wherever it's currently pointing.
                        String finalIndexWithPrevious = indexWithPrevious;
                        request.actions(Action.of(a -> a
                            .remove(RemoveAction.of(ra -> ra
                                .index(finalIndexWithPrevious)
                                .alias(typedAlias + PREVIOUS)
                            ))
                        ));
                    }

                    // Move <alias>_previous to wherever <alias> was pointing.
                    String finalIndexWithCurrent1 = indexWithCurrent;
                    request.actions(Action.of(a -> a
                            .add(AddAction.of(aa -> aa
                                    .index(finalIndexWithCurrent1)
                                    .alias(typedAlias + PREVIOUS)
                            ))
                    ));
                }

                // Point <alias> to the right place.
                request.actions(Action.of(a -> a
                        .add(AddAction.of(aa -> aa
                                .index(typedIndexTarget)
                                .alias(typedAlias)
                        ))
                ));

                try {
                    client.indices().updateAliases(request.build());
                } catch (final IOException e) {
                    log.error("Failed to update alias {}", typedAlias, e);
                }
            }
        }
        return true;
    }

    void deleteAllUnaliasedIndices() {
        // Deleting all unaliased indices is not a safe operation if alias or index updates are in-progress!
        try {
            GetIndexResponse response = client.indices().get(g -> g.index("*"));
            for (Map.Entry<String, IndexState> entry : response.result().entrySet()) {
                String index = entry.getKey();
                IndexState state = entry.getValue();
                if (null == state.aliases() || state.aliases().isEmpty()) {
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
    private void sendMappingCorrections(final String typedIndex, final String indexType) {
        try {
            Map<String, Property> properties = new HashMap<>();

            // Add mapping to specify raw, un-analyzed fields
            for (String fieldName : this.rawFieldsListByType.getOrDefault(indexType, Collections.emptyList())) {
                log.debug("Sending raw mapping correction for {}." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, fieldName);
                properties.put(fieldName, Property.of(p -> p
                                            .text(t -> t
                                                .fields(
                                                    Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
                                                    Property.of(k -> k.keyword(kf -> kf))
                                                )
                                            )
                                    )
                );
            }

            // Add mapping to specify nested object fields
            for (String fieldName : this.nestedFieldsByType.getOrDefault(indexType, Collections.emptyList())) {
                log.debug("Sending mapping correction for nested field {}", fieldName);
                properties.put(fieldName, Property.of(p -> p.nested(n -> n)));
            }

            CreateIndexRequest request = new CreateIndexRequest.Builder()
                .index(typedIndex)
                .settings(s -> s
                    .index(i -> i
                        .mapping(m -> m
                            .totalFields(t -> t.limit(9999))
                        )
                    )
                )
                .mappings(m -> m.properties(properties))
                .build();

            client.indices().create(request);

        } catch (IOException e) {
            log.error("Error while sending mapping correction " + "instructions to the ElasticSearch Server", e);
        }
    }
}
