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
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;

import java.io.IOException;
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
    }


    public void indexObject(final String index, final String indexType, final String content)
            throws SegueSearchException {
        indexObject(index, indexType, content, null);
    }


    void bulkIndex(final String index, final String indexType, final List<Map.Entry<String, String>> dataToIndex)
            throws SegueSearchException {

        // check index already exists if not execute any initialisation steps.
        if (!this.hasIndex(index)) {
            this.sendMappingCorrections(index);
        }

        // build bulk request
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (Map.Entry<String, String> itemToIndex : dataToIndex) {
            bulkRequest.add(client.prepareIndex(index, indexType, itemToIndex.getKey())
                    .setSource(itemToIndex.getValue(), XContentType.JSON)); // TODO MT this might not be what we want seeing as it is complaining about 1000 field limit
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


    void indexObject(final String index, final String indexType, final String content, final String uniqueId)
            throws SegueSearchException {
        // check index already exists if not execute any initialisation steps.
        if (!this.hasIndex(index)) {
            this.sendMappingCorrections(index);
        }

        try {
            IndexResponse indexResponse = client.prepareIndex(index, indexType, uniqueId)
                    .setSource(content, XContentType.JSON)
                    .execute()
                    .actionGet();
            log.debug("Document: " + indexResponse.getId() + " indexed.");

        } catch (ElasticsearchException e) {
            throw new SegueSearchException("Error during index operation.", e);
        }
    }

    public boolean expungeEntireSearchCache() {
        return this.expungeIndexFromSearchCache("_all");
    }


    boolean expungeIndexFromSearchCache(final String index) {
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

    boolean addOrMoveIndexAlias(final String alias, final String targetIndex) {


        String indexWithPrevious = null; // This is the index that has the <alias>-previous alias
        String indexWithCurrent = null; // This is the index that has the <alias> alias.

        // First, find where <alias>-previous points.
        ImmutableOpenMap<String, List<AliasMetaData>> aliasesPrev = client.admin().indices().getAliases(new GetAliasesRequest(alias + "-previous")).actionGet().getAliases();
        Iterator<String> i = aliasesPrev.keysIt();
        if (i.hasNext()) {
            indexWithPrevious = i.next();
        }

        // Now find where <alias> points
        ImmutableOpenMap<String, List<AliasMetaData>> aliases = client.admin().indices().getAliases(new GetAliasesRequest(alias)).actionGet().getAliases();
        i = aliases.keysIt();
        if (i.hasNext()) {
            indexWithCurrent = i.next();
        }


        if (indexWithCurrent != null && indexWithCurrent.equals(targetIndex)) {
            log.info("Not moving alias '" + alias + "' - it already points to the right index.");
        } else {
            IndicesAliasesRequestBuilder reqBuilder = client.admin().indices().prepareAliases();

            if (indexWithCurrent != null) {
                // Remove the alias from the place it's currently pointing
                reqBuilder.removeAlias(indexWithCurrent, alias);
                if (indexWithPrevious != null) {
                    // Remove <alias>-previous from wherever it's currently pointing.
                    reqBuilder.removeAlias(indexWithPrevious, alias + "-previous");
                }
                // Move <alias>-previous to wherever <alias> was pointing.
                reqBuilder.addAlias(indexWithCurrent, alias + "-previous");
            }

            // Point <alias> to the right place.
            reqBuilder.addAlias(targetIndex, alias);
            reqBuilder.execute().actionGet();
        }

        this.expungeOldIndices();
        return true;
    }

    private void expungeOldIndices() {
        // This deletes any indices that don't have aliases pointing to them.
        // If you want an index kept, make sure it has an alias!
        ImmutableOpenMap<String, IndexMetaData> indices = client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData().indices();

        for(ObjectObjectCursor<String, IndexMetaData> c: indices) {
            if (c.value.getAliases().size() == 0) {
                log.info("Index " + c.key + " has no aliases. Removing."); // TODO MT We need aliases for everything... maybe the new type_ field is better
                this.expungeIndexFromSearchCache(c.key);
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
     * @param index
     *            - index to send the mapping corrections to.
     */
    private void sendMappingCorrections(final String index) {// TODO MT if we are using this in any way we might have broken it by separating it into separate indices
        try {
            for (String indexType : this.rawFieldsListByType.keySet()) {
                CreateIndexRequestBuilder indexBuilder = client.admin().indices()
                        .prepareCreate(index + "_raw_" + indexType)
                        .setSettings(Settings.builder().put("index.mapping.total_fields.limit", "9999").build());

                final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject().startObject(indexType)
                        .startObject("properties");

                for (String fieldName : this.rawFieldsListByType.get(indexType)) {
                    log.debug("Sending raw mapping correction for " + fieldName + "."
                            + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX);

                    mappingBuilder.startObject(fieldName).field("type", "keyword").field("index", "true")
                            .startObject("fields").startObject(Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX)
                            .field("type", "keyword").field("index", "false").endObject().endObject().endObject();
                }
                // close off json structure
                mappingBuilder.endObject().endObject().endObject();
                indexBuilder.addMapping(indexType, mappingBuilder);

                // Send Mapping information
                indexBuilder.execute().actionGet();
            }
        } catch (IOException e) {
            log.error("Error while sending mapping correction " + "instructions to the ElasticSearch Server", e);
        }
    }

}
