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
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.base.CaseFormat;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
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
    public ResultsWrapper<String> matchSearch(final String indexBase, final String indexType,
                                              final List<GitContentManager.BooleanSearchClause> fieldsToMatch, final int startIndex,
                                              final int limit, final Map<String, Constants.SortOrder> sortInstructions,
                                              @Nullable final Map<String, AbstractFilterInstruction> filterInstructions) throws SegueSearchException {
        Query query = generateBoolMatchQuery(fieldsToMatch)._toQuery();

        if (filterInstructions != null) {
            Query finalQuery = query;
            query = BoolQuery.of(bq -> bq
                .must(finalQuery)
                .filter(generateFilterQuery(filterInstructions))
            )._toQuery();
        }

        return this.executeBasicQuery(indexBase, indexType, query, startIndex, limit, sortInstructions);
    }

    @Override
    public final ResultsWrapper<String> randomisedMatchSearch(final String indexBase, final String indexType,
                                                              final List<GitContentManager.BooleanSearchClause> fieldsToMatch, final int startIndex, final int limit,
                                                              final Long randomSeed, final Map<String, AbstractFilterInstruction> filterInstructions)
            throws SegueSearchException {
        // build up the query from the fieldsToMatch map
        Query query = ConstantScoreQuery.of(cs -> cs
            .filter(generateBoolMatchQuery(fieldsToMatch)._toQuery())
        )._toQuery();

        RandomScoreFunction randomScoreFunction;

        if (null != randomSeed) {
            randomScoreFunction = RandomScoreFunction.of(r -> r
                .seed(String.valueOf(randomSeed))
                .field("_seq_no")
            );
        } else {
            randomScoreFunction = RandomScoreFunction.of(r -> r);
        }

        Query constantScoreQuery = query;
        query = FunctionScoreQuery.of(fsq -> fsq
            .query(constantScoreQuery)
            .functions(fn -> fn
                .randomScore(randomScoreFunction)
            )
        )._toQuery();

        if (filterInstructions != null) {
            Query functionScoreQuery = query;
            query = BoolQuery.of(bq -> bq
                .must(functionScoreQuery)
                .filter(generateFilterQuery(filterInstructions))
            )._toQuery();
        }

        log.debug("Randomised Query, to be sent to elasticsearch is : " + query);

        return this.executeBasicQuery(indexBase, indexType, query, startIndex, limit);
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

    @Override
    @Deprecated
    public ResultsWrapper<String> fuzzySearch(final String indexBase, final String indexType, final String searchString,
                                              final Integer startIndex, final Integer limit,
                                              @Nullable final Map<String, List<String>> fieldsThatMustMatch,
                                              @Nullable final Map<String, AbstractFilterInstruction> filterInstructions,
                                              final String... fields) throws SegueSearchException {
        if (null == indexBase || null == indexType || null == searchString || null == fields) {
            log.warn("A required field is missing. Unable to execute search.");
            return null;
        }

        BoolQuery.Builder masterQuery;
        if (null != fieldsThatMustMatch) {
            masterQuery = new BoolQuery.Builder().must(this.generateBoolMatchQuery(this.convertToBoolMap(fieldsThatMustMatch))._toQuery());
        } else {
            masterQuery = new BoolQuery.Builder();
        }

        BoolQuery.Builder query = new BoolQuery.Builder();
        Set<String> boostFields = ImmutableSet.of("id", "title", "tags");

        List<String> searchTerms = Lists.newArrayList();
        searchTerms.addAll(Arrays.asList(searchString.split(" ")));
        if (searchTerms.size() > 1) {
            searchTerms.add(searchString);
        }

        for (String f : fields) {
            float boost = boostFields.contains(f) ? 2f : 1f;

            for (String searchTerm : searchTerms) {
                Query initialFuzzySearch = MatchQuery.of(m -> m
                        .field(f)
                        .query(searchTerm)
                        .fuzziness("AUTO")
                        .prefixLength(0)
                        .boost(boost)
                )._toQuery();
                query.should(initialFuzzySearch);

                Query regexSearch = WildcardQuery.of(r -> r
                        .field(f)
                        .value("*" + searchTerm + "*")
                        .boost(boost)
                )._toQuery();
                query.should(regexSearch);
            }
        }

        // this query is just a bit smarter than the regex search above.
        Query multiMatchPrefixQuery = MultiMatchQuery.of(mm -> mm
                .query(searchString)
                .fields(Arrays.asList(fields))
                .type(TextQueryType.PhrasePrefix)
                .prefixLength(2)
                .boost(2.0f)
        )._toQuery();
        query.should(multiMatchPrefixQuery);

        masterQuery.must(query.build()._toQuery());

        if (filterInstructions != null) {
            masterQuery.filter(generateFilterQuery(filterInstructions));
        }

        Query finalQuery = masterQuery.build()._toQuery();
        return this.executeBasicQuery(indexBase, indexType, finalQuery, startIndex, limit);
    }

    @Override
    public ResultsWrapper<String> termSearch(final String indexBase, final String indexType,
                                             final String searchTerm, final String field, final int startIndex, final int limit,
                                             @Nullable final Map<String, AbstractFilterInstruction> filterInstructions)
            throws SegueSearchException {
        if (null == indexBase || null == indexType || (null == searchTerm && null != field)) {
            log.error("A required field or field combination is missing. Unable to execute search.");
            return null;
        }

        Query query = BoolQuery.of(bq -> {
            if (searchTerm != null) {
                Query termsQuery = TermsQuery.of(t -> t.field(field).terms(ts -> ts.value(List.of(FieldValue.of(searchTerm)))))._toQuery();
                bq.must(termsQuery);
            }
            if (filterInstructions != null) {
                bq.filter(generateFilterQuery(filterInstructions));
            }
            return bq;
        })._toQuery();

        if (null == searchTerm && null == filterInstructions) {
            throw new SegueSearchException("This method requires either searchTerm or filter instructions.");
        }

        return this.executeBasicQuery(indexBase, indexType, query, startIndex, limit);
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
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        RestClient restClient = RestClient.builder(new HttpHost(InetAddress.getByName(address), port, "http"))
                .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder.setSocketTimeout(60000))
                .build();

        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient client = new ElasticsearchClient(transport);

        log.info("Elastic Search client created: " + address + ":" + port);
        return client;
    }

    @Override
    public boolean hasIndex(final String indexBase, final String indexType) {
        Objects.requireNonNull(indexBase);
        Objects.requireNonNull(indexType);
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        try {
            return client.indices().exists(gr -> gr.index(typedIndex)).value();
        } catch (IOException e) {
            log.error(String.format("Failed to check existence of index %s", typedIndex), e);
            return false;
        }
    }

    @Override
    public Collection<String> getAllIndices() {
        try {
            return client.indices()
                .get(g -> g.index("*"))
                .result()
                .keySet();
        } catch (IOException e) {
            log.error("Exception while retrieving all indices", e);
            return Collections.emptyList();
        }
    }

    @Override
    public ResultsWrapper<String> findByExactMatch(final String indexBase, final String indexType,
                                                   final String fieldname, final String needle, final int startIndex,
                                                   final int limit,
                                                   final Map<String, AbstractFilterInstruction> filterInstructions)
            throws SegueSearchException {
        ResultsWrapper<String> resultList;

        Query query = MatchQuery.of(m -> m
                .field(fieldname)
                .query(needle)
        )._toQuery();

        if (filterInstructions != null) {
            Query matchQuery = query;
            query = BoolQuery.of(bq -> bq
                    .must(matchQuery)
                    .filter(generateFilterQuery(filterInstructions))
            )._toQuery();
        }

        resultList = this.executeBasicQuery(indexBase, indexType, query, startIndex, limit);

        return resultList;
    }

    @Override
    public ResultsWrapper<String> findByPrefix(final String indexBase, final String indexType, final String fieldname,
                                               final String prefix, final int startIndex, final int limit, final Map<String, AbstractFilterInstruction> filterInstructions)
            throws SegueSearchException {
        ResultsWrapper<String> resultList;

        Query query = WildcardQuery.of(w -> w
                .field(fieldname)
                .value(prefix + "*")
        )._toQuery();

        if (filterInstructions != null) {
            Query prefixQuery = query;
            query = BoolQuery.of(bq -> bq
                    .must(prefixQuery)
                    .filter(generateFilterQuery(filterInstructions))
            )._toQuery();
        }

        resultList = this.executeBasicQuery(indexBase, indexType, query, startIndex, limit);

        return resultList;
    }

    @Override
    public ResultsWrapper<String> findByRegEx(final String indexBase, final String indexType, final String fieldname,
                                              final String regex, final int startIndex, final int limit,
                                              final Map<String, AbstractFilterInstruction> filterInstructions)
            throws SegueSearchException {
        ResultsWrapper<String> resultList;

        Query query = RegexpQuery.of(r -> r
                .field(fieldname)
                .value(regex)
        )._toQuery();

        if (filterInstructions != null) {
            Query regExpQuery = query;
            query = BoolQuery.of(bq -> bq
                    .must(regExpQuery)
                    .filter(generateFilterQuery(filterInstructions))
            )._toQuery();
        }

        resultList = this.executeBasicQuery(indexBase, indexType, query, startIndex, limit);

        return resultList;
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
     * Helper method to create elastic search understandable filter instructions.
     *
     * @param filterInstructions
     *            - in the form "fieldName --> instruction key --> instruction value"
     * @return filter query
     */
    public Query generateFilterQuery(final Map<String, AbstractFilterInstruction> filterInstructions) {
        BoolQuery.Builder filter = new BoolQuery.Builder();
        for (Entry<String, AbstractFilterInstruction> fieldToFilterInstruction : filterInstructions.entrySet()) {
            // date filter logic
            if (fieldToFilterInstruction.getValue() instanceof DateRangeFilterInstruction) {
                DateRangeFilterInstruction dateRangeInstruction = (DateRangeFilterInstruction) fieldToFilterInstruction
                        .getValue();
                // Note: assumption that dates are stored in long format.
                filter.must(RangeQuery.of(r ->
                    r.date(d -> {
                        d.field(fieldToFilterInstruction.getKey());
                        if (dateRangeInstruction.getFromDate() != null) {
                            d.gte(String.valueOf(dateRangeInstruction.getFromDate().getTime()));
                        }
                        if (dateRangeInstruction.getToDate() != null) {
                            d.lte(String.valueOf(dateRangeInstruction.getToDate().getTime()));
                        }
                        return d;
                    })
                )._toQuery());
            }

            if (fieldToFilterInstruction.getValue() instanceof SimpleFilterInstruction) {
                SimpleFilterInstruction sfi = (SimpleFilterInstruction) fieldToFilterInstruction.getValue();

                List<GitContentManager.BooleanSearchClause> fieldsToMatch = Lists.newArrayList();
                fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
                        fieldToFilterInstruction.getKey(), Constants.BooleanOperator.AND,
                        Collections.singletonList(sfi.getMustMatchValue())));

                filter.must(this.generateBoolMatchQuery(fieldsToMatch)._toQuery());
            }

            if (fieldToFilterInstruction.getValue() instanceof TermsFilterInstruction) {
                TermsFilterInstruction sfi = (TermsFilterInstruction) fieldToFilterInstruction.getValue();
                filter.must(
                    TermsQuery.of(t -> t
                        .field(fieldToFilterInstruction.getKey())
                        .terms(ts -> ts
                            .value(
                                sfi.getMatchValues().stream()
                                    .map(FieldValue::of)
                                    .collect(Collectors.toList())
                            )
                        )
                    )._toQuery()
                );
            }

            if (fieldToFilterInstruction.getValue() instanceof SimpleExclusionInstruction) {
                SimpleExclusionInstruction sfi = (SimpleExclusionInstruction) fieldToFilterInstruction.getValue();

                List<GitContentManager.BooleanSearchClause> fieldsToMatch = Lists.newArrayList();
                fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
                        fieldToFilterInstruction.getKey(), Constants.BooleanOperator.AND,
                        Collections.singletonList(sfi.getMustNotMatchValue())));

                filter.mustNot(this.generateBoolMatchQuery(fieldsToMatch)._toQuery());
            }
        }

        return filter.build()._toQuery();
    }

    /**
     * Utility method to generate a BoolMatchQuery based on the parameters provided.
     *
     * @param fieldsToMatch
     *            - the fields that the bool query should match.
     * @return a bool query configured to match the fields to match.
     * @deprecated as {@code AbstractInstruction}-based instructions should be preferred over
     * {@code BooleanSearchClause}, which are instead processed by {@code processMatchInstructions()}.
     */
    @Deprecated
    private BoolQuery generateBoolMatchQuery(final List<GitContentManager.BooleanSearchClause> fieldsToMatch) {
        BoolQuery.Builder masterQuery = new BoolQuery.Builder();
        Map<String, BoolQuery.Builder> nestedQueriesByPath = Maps.newHashMap();

        for (GitContentManager.BooleanSearchClause searchClause : fieldsToMatch) {
            // Each search clause is its own boolean query that gets added to the master query as a must match clause
            BoolQuery.Builder query = new BoolQuery.Builder();

            // Add the clause to the query value by value
            for (String value : searchClause.getValues()) {
                Query matchQuery = MatchQuery.of(m -> m
                        .field(searchClause.getField())
                        .query(value)
                )._toQuery();

                if (Constants.BooleanOperator.OR.equals(searchClause.getOperator())) {
                    query.should(matchQuery);
                } else if (Constants.BooleanOperator.AND.equals(searchClause.getOperator())) {
                    query.must(matchQuery);
                } else if (Constants.BooleanOperator.NOT.equals(searchClause.getOperator())) {
                    query.mustNot(matchQuery);
                } else {
                    log.warn("Null argument received in paginated match search... "
                            + "This is not usually expected. Ignoring it and continuing anyway.");
                }
            }

            // The way we're using this query, if we have a "should" the document needs to match at least one of the options.
            if (Constants.BooleanOperator.OR.equals(searchClause.getOperator())) {
                query.minimumShouldMatch("1");
            }

            if (!Constants.NESTED_QUERY_FIELDS.contains(searchClause.getField())) {
                masterQuery.must(query.build()._toQuery());
            } else {
                // Nested fields need to use a nested query which specifies the path of the nested field.
                String nestedPath = searchClause.getField().split("\\.")[0];
                nestedQueriesByPath.putIfAbsent(nestedPath, new BoolQuery.Builder());
                nestedQueriesByPath.get(nestedPath).must(query.build()._toQuery());
            }
        }

        // Nested queries are grouped so that queries on the same nested path are not queried independently.
        for (Entry<String, BoolQuery.Builder> entry : nestedQueriesByPath.entrySet()) {
            Query nestedQuery = NestedQuery.of(nq -> nq
                    .path(entry.getKey())
                    .query(entry.getValue().build()._toQuery())
                    .scoreMode(ChildScoreMode.Sum)
            )._toQuery();
            masterQuery.must(nestedQuery);
        }

        return masterQuery.build();
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
                                                     final Query query, final int startIndex, final int limit)
            throws SegueSearchException {
        return this.executeBasicQuery(indexBase, indexType, query, startIndex, limit, null);
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

        log.debug("Building Query: " + requestBuilder);
        ResultsWrapper<String> results = executeQuery(requestBuilder.build());

        // execute another query to get all results as this is an unlimited
        // query.
        if (isUnlimitedSearch && (results.getResults().size() < results.getTotalResults())) {
            if (results.getTotalResults() > this.getMaxResultSize(indexBase, indexType)) {
                throw new SegueSearchException(String.format("The search you have requested " +
                        "exceeds the maximum number of results that can be returned at once (%s).",
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

            log.debug("TOTAL SEARCH HITS " + totalHits);
            log.debug("Search Request: " + searchRequest);

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
     * Utility function to support conversion between simple field maps and bool maps.
     *
     * @param fieldsThatMustMatch
     *            - the map that should be converted into a suitable map for querying.
     * @return Map where each field is using the OR boolean operator.
     *
     * @deprecated as {@code AbstractInstruction}-based instructions should be preferred over
     * {@code fieldsToMatch}-style instructions.
     */
    @Deprecated
    private List<GitContentManager.BooleanSearchClause> convertToBoolMap(final Map<String, List<String>> fieldsThatMustMatch) {
        if (null == fieldsThatMustMatch) {
            return null;
        }

        List<GitContentManager.BooleanSearchClause> result = Lists.newArrayList();

        for (Map.Entry<String, List<String>> pair : fieldsThatMustMatch.entrySet()) {
            result.add(new GitContentManager.BooleanSearchClause(
                    pair.getKey(), Constants.BooleanOperator.OR, pair.getValue()));
        }

        return result;
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
        if (matchInstruction instanceof BooleanInstruction) {
            BooleanInstruction booleanMatch = (BooleanInstruction) matchInstruction;
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
                } catch (SegueSearchException e) {
                    throw new RuntimeException("Error processing boolean match instructions", e);
                }
                if (booleanMatch.getBoost() != null) {
                    b.boost(booleanMatch.getBoost());
                }
                b.minimumShouldMatch(String.valueOf(booleanMatch.getMinimumShouldMatch()));

                return b;
            })._toQuery();
        } else if (matchInstruction instanceof MatchInstruction) {
            MatchInstruction shouldMatch = (MatchInstruction) matchInstruction;
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
        } else if (matchInstruction instanceof RangeInstruction) {
            RangeInstruction<?> rangeMatch = (RangeInstruction<?>) matchInstruction;
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
        } else if (matchInstruction instanceof NestedInstruction) {
            NestedInstruction nestedMatch = (NestedInstruction) matchInstruction;
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
        } else if (matchInstruction instanceof WildcardInstruction) {
            WildcardInstruction wildcardMatch = (WildcardInstruction) matchInstruction;
            return WildcardQuery.of(wq -> wq
                .field(wildcardMatch.getField())
                .value(wildcardMatch.getValue())
                .boost(wildcardMatch.getBoost().floatValue()))
                ._toQuery();
        } else if (matchInstruction instanceof MultiMatchInstruction) {
            MultiMatchInstruction multiMatchInstruction = (MultiMatchInstruction) matchInstruction;
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
        } catch (IOException e) {
            throw new SegueSearchException(String.format("Failed to get content with ID %s from index %s", id, typedIndex), e);
        }
    }

    @Override
    public SearchResponse<ObjectNode> getAllFromIndex(final String indexBase, final String indexType) throws SegueSearchException {
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        try {
            return client.search(sr -> sr.index(typedIndex).size(10000), ObjectNode.class);
        } catch (IOException e) {
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
                Map<String, IndexState> indices = response.result();
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
        } catch (IOException e) {
            log.error(String.format("Failed to retrieve max window size settings for index %s - defaulting to %d",
                    typedIndex, DEFAULT_MAX_WINDOW_SIZE), e);
            return DEFAULT_MAX_WINDOW_SIZE;
        }
    }
}