/*
 * Copyright 2014 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.ConstantScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RandomScoreFunction;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RegexpQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.IndexState;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.base.CaseFormat;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;


/**
 * A class that works as an adapter for ElasticSearch.
 *
 * @author Stephen Cummins
 *
 */
public class ElasticSearchProvider implements ISearchProvider {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);
    private static final String ES_FIELD_CONNECTOR = ".";

    protected final ElasticsearchClient client;

    // to try and improve performance of searches with a -1 limit.
    private static final int LARGE_LIMIT = 100;

    private static final int DEFAULT_MAX_WINDOW_SIZE = 10000;

    // used to optimise index setting retrieval as these probably don't change every request.
    private final Cache<String, String> settingsCache;

    public static String produceTypedIndexName(final String indexName, final String typeName) {
        return indexName + "_" + CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, typeName);
    }

    /**
     * Constructor for creating an instance of the ElasticSearchProvider Object.
     *
     * @param searchClient
     *            - the client that the provider should be using.
     */
    @Inject
    public ElasticSearchProvider(final ElasticsearchClient searchClient) {
        this.client = searchClient;
        this.settingsCache = CacheBuilder.newBuilder().softValues().expireAfterWrite(10, TimeUnit.MINUTES).build();
    }

    @Override
    public String getNestedFieldConnector() {
        return ES_FIELD_CONNECTOR;
    }

    @Override
    public ResultsWrapper<String> nestedMatchSearch(final String indexBase, final String indexType,
                                                    final Integer startIndex, final Integer limit,
                                                    @NotNull final BooleanInstruction matchInstruction,
                                                    @Nullable final Long randomSeed,
                                                    @Nullable final Map<String, Constants.SortOrder> sortOrder
    ) throws SegueSearchException {

        if (null == indexBase || null == indexType) {
            log.warn("A required field is missing. Unable to execute search.");
            throw new SegueSearchException("A required field is missing. Unable to execute search.");
        }

        Query query = this.processMatchInstructions(matchInstruction);

        if (null == sortOrder && null != randomSeed) {
            RandomScoreFunction.Builder randomScoreFunctionBuilder = new RandomScoreFunction.Builder();
            randomScoreFunctionBuilder.seed(String.valueOf(randomSeed));
            randomScoreFunctionBuilder.field("_seq_no");

            Query matchInstructionsQuery = query;
            query = FunctionScoreQuery.of(fsq -> fsq
                    .query(matchInstructionsQuery)
                    .functions(fn -> fn.randomScore(randomScoreFunctionBuilder.build()))
                    // Don't use the base query's result ranking at all, only use this random weighting:
                    .boostMode(FunctionBoostMode.Replace)
            )._toQuery();
        }

        return this.executeBasicQuery(indexBase, indexType, query, startIndex, limit, sortOrder);
    }

    /**
     * This method will create a threadsafe client that can be used to talk to an Elastic Search cluster.
     * @param address
     *            - address of the cluster to connect to.
     * @param port
     *            - port that the cluster is running on.
     * @param username
     *            - username for cluster user.
     * @param password
     *            - password for cluster user.
     *
     * @return Defaults to http client creation.
     */
    public static ElasticsearchClient getClient(final String address, final int port, final String username,
                                                final String password) throws UnknownHostException {

        String credentials = java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        Rest5Client restClient = Rest5Client.builder(new HttpHost("http", InetAddress.getByName(address), port))
                .setDefaultHeaders(new Header[]{new BasicHeader("Authorization", "Basic " + credentials)})
                .setConnectionConfigCallback(connectConfig -> connectConfig.setSocketTimeout(Timeout.ofSeconds(360)))
                .build();

        ElasticsearchTransport transport = new Rest5ClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient client = new ElasticsearchClient(transport);

        log.info("Elastic Search client created: {}:{}", address, port);
        return client;
    }

    @Override
    public boolean hasIndex(final String indexBase, final String indexType) {
        Objects.requireNonNull(indexBase);
        Objects.requireNonNull(indexType);
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        try {
            return client.indices().exists(gr -> gr.index(typedIndex)).value();
        } catch (final IOException e) {
            log.error("Failed to check existence of index {}", typedIndex, e);
            return false;
        }
    }

    @Override
    public Collection<String> getAllIndices() {
        try {
            return client.indices()
                .get(g -> g.index("*"))
                .indices()
                .keySet();
        } catch (final IOException e) {
            log.error("Exception while retrieving all indices", e);
            return Collections.emptyList();
        }
    }

    /**
     * Utility method to convert sort instructions from external classes into something Elastic search can use.
     *
     * @param requestBuilder
     *            - the builder for the request to be augmented.
     * @param sortInstructions
     *            - the instructions to augment.
     */
    private void addSortInstructions(final SearchRequest.Builder requestBuilder,
                                      final Map<String, Constants.SortOrder> sortInstructions) {
        // deal with sorting of results
        for (Map.Entry<String, Constants.SortOrder> entry : sortInstructions.entrySet()) {
            String sortField = entry.getKey();
            Constants.SortOrder sortOrder = entry.getValue();

            // fully qualified to not conflict with Constants.SortOrder
            co.elastic.clients.elasticsearch._types.SortOrder clientOrder =
                    (sortOrder == Constants.SortOrder.ASC)
                            ? co.elastic.clients.elasticsearch._types.SortOrder.Asc
                            : co.elastic.clients.elasticsearch._types.SortOrder.Desc;

            requestBuilder.sort(SortOptions.of(s -> s.field(f -> f
                .field(sortField)
                .order(clientOrder)
                .missing("_last"))));
        }
    }

    /**
     * Provides default search execution using the fields specified.
     *
     * This method does not provide any way of controlling sort order or limiting information returned. It is most
     * useful for doing simple searches with fewer results e.g. by id.
     *
     * @param indexBase
     *            - search index base string to execute the query against.
     * @param indexType
     *            - index type to execute the query against.
     * @param query
     *            - the query to run.
     * @param startIndex
     *            - start index for results
     * @param limit
     *            - the maximum number of results to return -1 will attempt to return all results.
     * @return list of the search results
     */
    private ResultsWrapper<String> executeBasicQuery(final String indexBase, final String indexType,
                                                     final Query query, final int startIndex, final int limit,
                                                     @Nullable final Map<String, Constants.SortOrder> sortInstructions) throws SegueSearchException {
        int newLimit = limit;
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        boolean isUnlimitedSearch = limit == -1;

        if (isUnlimitedSearch) {
            newLimit = LARGE_LIMIT;
        }

        SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                .index(typedIndex)
                .query(query)
                .size(newLimit)
                .from(startIndex);

        if (sortInstructions != null) {
            this.addSortInstructions(requestBuilder, sortInstructions);
        }

        log.debug("Building Query: {}", requestBuilder);
        ResultsWrapper<String> results = executeQuery(requestBuilder.build());

        // execute another query to get all results as this is an unlimited
        // query.
        if (isUnlimitedSearch && (results.getResults().size() < results.getTotalResults())) {
            if (results.getTotalResults() > this.getMaxResultSize(indexBase, indexType)) {
                throw new SegueSearchException(String.format("The search you have requested "
                                + "exceeds the maximum number of results that can be returned at once (%s).",
                        this.getMaxResultSize(indexBase, indexType)));
            }

            SearchRequest secondRequest = new SearchRequest.Builder()
                    .index(typedIndex)
                    .query(query)
                    .size(results.getTotalResults().intValue())
                    .from(startIndex)
                    .build();

            results = executeQuery(secondRequest);

            log.debug("Unlimited Search - had to make a second round trip to elasticsearch.");
        }

        return results;
    }

    /**
     * A general method for getting the results of a search.
     * @param searchRequest
     *            - the search request to send to the cluster.
     * @return List of the search results.
     */
    private ResultsWrapper<String> executeQuery(final SearchRequest searchRequest) throws SegueSearchException {
        try {
            SearchResponse<ObjectNode> response = client.search(searchRequest, ObjectNode.class);

            List<Hit<ObjectNode>> hits = response.hits().hits();
            List<String> resultList = new ArrayList<>();

            long totalHits = null != response.hits().total()
                    ? response.hits().total().value()
                    : 0;

            for (Hit<ObjectNode> hit : hits) {
                ObjectNode src = hit.source();
                resultList.add(null != src ? src.toString() : "{}");
            }

            return new ResultsWrapper<>(resultList, totalHits);
        } catch (ElasticsearchException | IOException e) {
            throw new SegueSearchException("Error while trying to search", e);
        }
    }

    /**
     * Based on the relatively abstract {@code matchInstruction}, generates a {@code Query} which is usable by
     * Elasticsearch.
     *
     * @param matchInstruction An {@code AbstractInstruction} representing a search query.
     *
     * @return a {@code Query} reflecting the instructions in {@code matchInstruction}.
     * @throws SegueSearchException
     */
    private Query processMatchInstructions(final AbstractInstruction matchInstruction)
            throws SegueSearchException {
        if (matchInstruction instanceof BooleanInstruction booleanMatch) {
            return BoolQuery.of(b -> {
                try {
                    for (AbstractInstruction should : booleanMatch.getShoulds()) {
                        b.should(processMatchInstructions(should));
                    }
                    for (AbstractInstruction must : booleanMatch.getMusts()) {
                        b.must(processMatchInstructions(must));
                    }
                    for (AbstractInstruction mustNot : booleanMatch.getMustNots()) {
                        b.mustNot(processMatchInstructions(mustNot));
                    }
                } catch (final SegueSearchException e) {
                    throw new RuntimeException("Error processing boolean match instructions", e);
                }
                if (booleanMatch.getBoost() != null) {
                    b.boost(booleanMatch.getBoost());
                }
                b.minimumShouldMatch(String.valueOf(booleanMatch.getMinimumShouldMatch()));

                return b;
            })._toQuery();
        } else if (matchInstruction instanceof MatchInstruction shouldMatch) {
            return MatchQuery.of(m -> {
                m.field(shouldMatch.getField());
                m.query(shouldMatch.getValue());
                if (shouldMatch.getBoost() != null) {
                    m.boost((float) shouldMatch.getBoost());
                }
                if (shouldMatch.getFuzzy()) {
                    m.fuzziness("AUTO");
                }
                return m;
            })._toQuery();
        } else if (matchInstruction instanceof RangeInstruction<?> rangeMatch) {
            return RangeQuery.of(r ->
                r.date(d -> {
                    d.field(rangeMatch.getField());
                    if (rangeMatch.getGreaterThan() != null) {
                        d.gt(String.valueOf(rangeMatch.getGreaterThan()));
                    }
                    if (rangeMatch.getGreaterThanOrEqual() != null) {
                        d.gte(String.valueOf(rangeMatch.getGreaterThanOrEqual()));
                    }
                    if (rangeMatch.getLessThan() != null) {
                        d.lt(String.valueOf(rangeMatch.getLessThan()));
                    }
                    if (rangeMatch.getLessThanOrEqual() != null) {
                        d.lte(String.valueOf(rangeMatch.getLessThanOrEqual()));
                    }
                    d.boost((float) rangeMatch.getBoost());
                    return d;
                })
            )._toQuery();
        } else if (matchInstruction instanceof NestedInstruction nestedMatch) {
            return NestedQuery.of(nq -> {
                try {
                    return nq
                        .path(nestedMatch.getPath())
                        .query(processMatchInstructions(nestedMatch.getInstruction()))
                        .scoreMode(ChildScoreMode.Sum);
                } catch (final SegueSearchException e) {
                    throw new RuntimeException(e);
                }
            })._toQuery();
        } else if (matchInstruction instanceof WildcardInstruction wildcardMatch) {
            return WildcardQuery.of(wq -> wq
                .field(wildcardMatch.getField())
                .value(wildcardMatch.getValue())
                .boost(wildcardMatch.getBoost().floatValue()))
                ._toQuery();
        } else if (matchInstruction instanceof MultiMatchInstruction multiMatchInstruction) {
            return MultiMatchQuery.of(mm -> mm
                .fields(Arrays.asList(multiMatchInstruction.getFields()))
                .query(multiMatchInstruction.getTerm())
                .boost(multiMatchInstruction.getBoost().floatValue())
                .type(TextQueryType.PhrasePrefix)
                .prefixLength(2)
            )._toQuery();
        } else if (matchInstruction instanceof ExistsInstruction) {
            return ExistsQuery.of(eq -> eq
                .field(((ExistsInstruction) matchInstruction).getField()))
                ._toQuery();
        } else {
            throw new SegueSearchException(
                "Processing match instruction which is not supported: " + matchInstruction.getClass());
        }
    }

    @Override
    public GetResponse<ObjectNode> getById(final String indexBase, final String indexType, final String id) throws SegueSearchException {
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        try {
            return client.get(gr -> gr.index(typedIndex).id(id), ObjectNode.class);
        } catch (final IOException e) {
            throw new SegueSearchException(String.format("Failed to get content with ID %s from index %s", id, typedIndex), e);
        }
    }

    @Override
    public SearchResponse<ObjectNode> getAllFromIndex(final String indexBase, final String indexType) throws SegueSearchException {
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        try {
            return client.search(sr -> sr.index(typedIndex).size(10000), ObjectNode.class);
        } catch (final IOException e) {
            throw new SegueSearchException(String.format("Failed to retrieve all data from index %s", typedIndex), e);
        }
    }

    /**
     * This method returns the maximum window size. i.e. the number of results that can be returned in a single result
     * set without having to do a special scroll query.
     *
     * This is a configurable value but the default Elastic Search value is 10,000.
     *
     * TODO: we may want to selectively upgrade queries to scroll requests if exceeding this limit.
     *
     * @param indexBase - to look up
     * @return the configured index max window size or a default,
     * if a request exceeds this an error will be thrown. (or we should use the scroll api.
     */
    private int getMaxResultSize(final String indexBase, final String indexType) {
        final String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        try {
            final String MAX_WINDOW_SIZE_KEY = typedIndex + "_" + "MAX_WINDOW_SIZE";
            String max_window_size = this.settingsCache.getIfPresent(MAX_WINDOW_SIZE_KEY);
            if (null == max_window_size) {
                GetIndexResponse response = client.indices().get(g -> g.index(typedIndex));
                Map<String, IndexState> indices = response.indices();
                for (IndexState indexState : indices.values()) {
                    if (null == indexState || null == indexState.settings()) {
                        continue;
                    }
                    IndexSettings settings = indexState.settings();
                    Integer maxResultWindow = settings.maxResultWindow();
                    if (null == maxResultWindow) {
                        maxResultWindow = SEARCH_MAX_WINDOW_SIZE;
                    }
                    this.settingsCache.put(MAX_WINDOW_SIZE_KEY, maxResultWindow.toString());
                }
            }
            return Integer.parseInt(this.settingsCache.getIfPresent(MAX_WINDOW_SIZE_KEY));
        } catch (final IOException e) {
            log.error("Failed to retrieve max window size settings for index {} - defaulting to {}",
                    typedIndex, DEFAULT_MAX_WINDOW_SIZE, e);
            return DEFAULT_MAX_WINDOW_SIZE;
        }
    }
}