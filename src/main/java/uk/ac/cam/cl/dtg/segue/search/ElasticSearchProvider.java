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

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.base.CaseFormat;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;

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
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

    protected final RestHighLevelClient client;

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
    public ElasticSearchProvider(final RestHighLevelClient searchClient) {
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
        // build up the query from the fieldsToMatch map
        QueryBuilder query = generateBoolMatchQuery(fieldsToMatch);

        if (filterInstructions != null) {
            query = QueryBuilders.boolQuery().must(query).filter(generateFilterQuery(filterInstructions));
        }

        return this.executeBasicQuery(indexBase, indexType, query, startIndex, limit, sortInstructions);
    }

    @Override
    public final ResultsWrapper<String> randomisedMatchSearch(final String indexBase, final String indexType,
                                                              final List<GitContentManager.BooleanSearchClause> fieldsToMatch, final int startIndex, final int limit,
                                                              final Long randomSeed, final Map<String, AbstractFilterInstruction> filterInstructions)
            throws SegueSearchException {
        // build up the query from the fieldsToMatch map
        QueryBuilder query = QueryBuilders.constantScoreQuery(generateBoolMatchQuery(fieldsToMatch));

        RandomScoreFunctionBuilder randomScoreFunctionBuilder;
        if (null != randomSeed) {
            randomScoreFunctionBuilder = new RandomScoreFunctionBuilder();
            randomScoreFunctionBuilder.seed(randomSeed);
            randomScoreFunctionBuilder.setField("_seq_no");
        } else {
            randomScoreFunctionBuilder = ScoreFunctionBuilders.randomFunction();
        }
        query = QueryBuilders.functionScoreQuery(query, randomScoreFunctionBuilder);

        if (filterInstructions != null) {
            query = QueryBuilders.boolQuery().must(query).filter(generateFilterQuery(filterInstructions));
        }

        log.debug("Randomised Query, to be sent to elasticsearch is : " + query);

        return this.executeBasicQuery(indexBase, indexType, query, startIndex, limit);
    }

    @Override
    public ResultsWrapper<String> nestedMatchSearch(
            final String indexBase, final String indexType, final Integer startIndex, final Integer limit,
            final String searchString, @NotNull final BooleanMatchInstruction matchInstruction,
            @Nullable final Map<String, AbstractFilterInstruction> filterInstructions
    ) throws SegueSearchException {
        if (null == indexBase || null == indexType || null == searchString) {
            log.warn("A required field is missing. Unable to execute search.");
            throw new SegueSearchException("A required field is missing. Unable to execute search.");
        }

        BoolQueryBuilder query = (BoolQueryBuilder) this.processMatchInstructions(matchInstruction);
        if (filterInstructions != null) {
            query.filter(generateFilterQuery(filterInstructions));
        }
        query.minimumShouldMatch(1);
        return this.executeBasicQuery(indexBase, indexType, query, startIndex, limit);
    }

    @Override
    public ResultsWrapper<String> fuzzySearch(final String indexBase, final String indexType, final String searchString,
                                              final Integer startIndex, final Integer limit,
                                              @Nullable final Map<String, List<String>> fieldsThatMustMatch,
                                              @Nullable final Map<String, AbstractFilterInstruction> filterInstructions,
                                              final String... fields) throws SegueSearchException {
        if (null == indexBase || null == indexType || null == searchString || null == fields) {
            log.warn("A required field is missing. Unable to execute search.");
            return null;
        }

        BoolQueryBuilder masterQuery;
        if (null != fieldsThatMustMatch) {
            masterQuery = this.generateBoolMatchQuery(this.convertToBoolMap(fieldsThatMustMatch));
        } else {
            masterQuery = QueryBuilders.boolQuery();
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        Set boostFields = ImmutableSet.builder().add("id").add("title").add("tags").build();

        List<String> searchTerms = Lists.newArrayList();
        searchTerms.addAll(Arrays.asList(searchString.split(" ")));
        if (searchTerms.size() > 1) {
            searchTerms.add(searchString);
        }

        for (String f : fields) {
            float boost = boostFields.contains(f) ? 2f : 1f;

            for (String searchTerm : searchTerms) {
                QueryBuilder initialFuzzySearch = QueryBuilders.matchQuery(f, searchTerm)
                        .fuzziness(Fuzziness.AUTO)
                        .prefixLength(0)
                        .boost(boost);
                query.should(initialFuzzySearch);
                QueryBuilder regexSearch =
                        QueryBuilders.wildcardQuery(f, "*" + searchTerm + "*").boost(boost);
                query.should(regexSearch);
            }
        }

        // this query is just a bit smarter than the regex search above.
        QueryBuilder multiMatchPrefixQuery = QueryBuilders.multiMatchQuery(searchString, fields)
                .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX).prefixLength(2).boost(2.0f);
        query.should(multiMatchPrefixQuery);

        masterQuery.must(query);

        if (filterInstructions != null) {
            masterQuery.filter(generateFilterQuery(filterInstructions));
        }

        return this.executeBasicQuery(indexBase, indexType, masterQuery, startIndex, limit);
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

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (searchTerm != null) {
            query.must(QueryBuilders.termQuery(field, searchTerm));
        }

        if (filterInstructions != null) {
            query.filter(generateFilterQuery(filterInstructions));
        }

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
    public static RestHighLevelClient getClient(final String address, final int port, final String username,
                                                final String password) throws UnknownHostException {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(InetAddress.getByName(address), port, "http")
                )
                .setHttpClientConfigCallback(
                        httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                )
        );

        log.info("Elastic Search client created: " + address + ":" + port);
        return client;
    }

    @Override
    public boolean hasIndex(final String indexBase, final String indexType) {
        Validate.notNull(indexBase);
        Validate.notNull(indexType);
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        try {
            return client.indices().exists(new GetIndexRequest(typedIndex), RequestOptions.DEFAULT);
        } catch (IOException e) {
            log.error(String.format("Failed to check existence of index %s", typedIndex), e);
            return false;
        }
    }

    @Override
    public Collection<String> getAllIndices() {
        try {
            return List.of(client.indices().get(new GetIndexRequest("*"), RequestOptions.DEFAULT).getIndices());
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

        QueryBuilder query = QueryBuilders.matchQuery(fieldname, needle);

        if (filterInstructions != null) {
            query = QueryBuilders.boolQuery().must(query).filter(generateFilterQuery(filterInstructions));
        }

        resultList = this.executeBasicQuery(indexBase, indexType, query, startIndex, limit);

        return resultList;
    }

    @Override
    public ResultsWrapper<String> findByPrefix(final String indexBase, final String indexType, final String fieldname,
                                               final String prefix, final int startIndex, final int limit, final Map<String, AbstractFilterInstruction> filterInstructions)
            throws SegueSearchException {
        ResultsWrapper<String> resultList;

        QueryBuilder query = QueryBuilders.prefixQuery(fieldname, prefix);

        if (filterInstructions != null) {
            query = QueryBuilders.boolQuery().must(query).filter(generateFilterQuery(filterInstructions));
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

        QueryBuilder query = QueryBuilders.regexpQuery(fieldname, regex);

        if (filterInstructions != null) {
            query = QueryBuilders.boolQuery().must(query).filter(generateFilterQuery(filterInstructions));
        }

        resultList = this.executeBasicQuery(indexBase, indexType, query, startIndex, limit);

        return resultList;
    }

    /**
     * Utility method to convert sort instructions form external classes into something Elastic search can use.
     *
     * @param searchRequest
     *            - the request to be augmented.
     * @param sortInstructions
     *            - the instructions to augment.
     * @return the augmented search request with sort instructions included.
     */
    private SearchSourceBuilder addSortInstructions(final SearchSourceBuilder searchRequest,
                                                     final Map<String, Constants.SortOrder> sortInstructions) {
        // deal with sorting of results
        for (Map.Entry<String, Constants.SortOrder> entry : sortInstructions.entrySet()) {
            String sortField = entry.getKey();
            Constants.SortOrder sortOrder = entry.getValue();

            if (sortOrder == Constants.SortOrder.ASC) {
                searchRequest.sort(SortBuilders.fieldSort(sortField).order(SortOrder.ASC).missing("_last"));

            } else {
                searchRequest.sort(SortBuilders.fieldSort(sortField).order(SortOrder.DESC).missing("_last"));
            }
        }

        return searchRequest;
    }

    /**
     * Helper method to create elastic search understandable filter instructions.
     *
     * @param filterInstructions
     *            - in the form "fieldName --> instruction key --> instruction value"
     * @return filterbuilder
     */
    public QueryBuilder generateFilterQuery(final Map<String, AbstractFilterInstruction> filterInstructions) {
        BoolQueryBuilder filter = QueryBuilders.boolQuery();
        for (Entry<String, AbstractFilterInstruction> fieldToFilterInstruction : filterInstructions.entrySet()) {
            // date filter logic
            if (fieldToFilterInstruction.getValue() instanceof DateRangeFilterInstruction) {
                DateRangeFilterInstruction dateRangeInstruction = (DateRangeFilterInstruction) fieldToFilterInstruction
                        .getValue();
                RangeQueryBuilder rangeFilter = QueryBuilders.rangeQuery(fieldToFilterInstruction.getKey());
                // Note: assumption that dates are stored in long format.
                if (dateRangeInstruction.getFromDate() != null) {
                    rangeFilter.from(dateRangeInstruction.getFromDate().getTime());
                }

                if (dateRangeInstruction.getToDate() != null) {
                    rangeFilter.to(dateRangeInstruction.getToDate().getTime());
                }

                filter.must(rangeFilter);
            }

            if (fieldToFilterInstruction.getValue() instanceof SimpleFilterInstruction) {
                SimpleFilterInstruction sfi = (SimpleFilterInstruction) fieldToFilterInstruction.getValue();

                List<GitContentManager.BooleanSearchClause> fieldsToMatch = Lists.newArrayList();
                fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
                        fieldToFilterInstruction.getKey(), Constants.BooleanOperator.AND,
                        Collections.singletonList(sfi.getMustMatchValue())));

                filter.must(this.generateBoolMatchQuery(fieldsToMatch));
            }

            if (fieldToFilterInstruction.getValue() instanceof TermsFilterInstruction) {
                TermsFilterInstruction sfi = (TermsFilterInstruction) fieldToFilterInstruction.getValue();
                filter.must(QueryBuilders.termsQuery(fieldToFilterInstruction.getKey(), sfi.getMatchValues()));
            }

            if (fieldToFilterInstruction.getValue() instanceof SimpleExclusionInstruction) {
                SimpleExclusionInstruction sfi = (SimpleExclusionInstruction) fieldToFilterInstruction.getValue();

                List<GitContentManager.BooleanSearchClause> fieldsToMatch = Lists.newArrayList();
                fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
                        fieldToFilterInstruction.getKey(), Constants.BooleanOperator.AND,
                        Collections.singletonList(sfi.getMustNotMatchValue())));

                filter.mustNot(this.generateBoolMatchQuery(fieldsToMatch));
            }
        }

        return filter;
    }

    /**
     * Utility method to generate a BoolMatchQuery based on the parameters provided.
     *
     * @param fieldsToMatch
     *            - the fields that the bool query should match.
     * @return a bool query configured to match the fields to match.
     */
    private BoolQueryBuilder generateBoolMatchQuery(final List<GitContentManager.BooleanSearchClause> fieldsToMatch) {
        BoolQueryBuilder masterQuery = QueryBuilders.boolQuery();
        Map<String, BoolQueryBuilder> nestedQueriesByPath = Maps.newHashMap();

        for (GitContentManager.BooleanSearchClause searchClause : fieldsToMatch) {
            // Each search clause is its own boolean query that gets added to the master query as a must match clause
            BoolQueryBuilder query = QueryBuilders.boolQuery();

            // Add the clause to the query value by value
            for (String value : searchClause.getValues()) {
                if (Constants.BooleanOperator.OR.equals(searchClause.getOperator())) {
                    query.should(QueryBuilders.matchQuery(searchClause.getField(), value));
                } else if (Constants.BooleanOperator.AND.equals(searchClause.getOperator())) {
                    query.must(QueryBuilders.matchQuery(searchClause.getField(), value));
                } else if (Constants.BooleanOperator.NOT.equals(searchClause.getOperator())) {
                    query.mustNot(QueryBuilders.matchQuery(searchClause.getField(), value));
                } else {
                    log.warn("Null argument received in paginated match search... "
                            + "This is not usually expected. Ignoring it and continuing anyway.");
                }
            }

            // The way we're using this query, if we have a "should" the document needs to match at least one of the options.
            if (Constants.BooleanOperator.OR.equals(searchClause.getOperator())) {
                query.minimumShouldMatch(1);
            }

            if (!Constants.NESTED_FIELDS.contains(searchClause.getField())) {
                masterQuery.must(query);
            } else {
                // Nested fields need to use a nested query which specifies the path of the nested field.
                String nestedPath = searchClause.getField().split("\\.")[0];
                nestedQueriesByPath.putIfAbsent(nestedPath, QueryBuilders.boolQuery());
                nestedQueriesByPath.get(nestedPath).must(query);
            }
        }

        // Nested queries are grouped so that queries on the same nested path are not queried independently.
        for (Entry<String, BoolQueryBuilder> entry : nestedQueriesByPath.entrySet()) {
            masterQuery.must(QueryBuilders.nestedQuery(entry.getKey(), entry.getValue(), ScoreMode.Total));
        }

        return masterQuery;
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
                                                     final QueryBuilder query, final int startIndex, final int limit)
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
                                                     final QueryBuilder query, final int startIndex, final int limit,
                                                     @Nullable final Map<String, Constants.SortOrder> sortInstructions) throws SegueSearchException {
        int newLimit = limit;
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        boolean isUnlimitedSearch = limit == -1;

        if (isUnlimitedSearch) {
            newLimit = LARGE_LIMIT;
        }

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(query).size(newLimit).from(startIndex);

        if (sortInstructions != null) {
            this.addSortInstructions(sourceBuilder, sortInstructions);
        }

        log.debug("Building Query: " + sourceBuilder);
        ResultsWrapper<String> results = executeQuery(typedIndex, sourceBuilder);

        // execute another query to get all results as this is an unlimited
        // query.
        if (isUnlimitedSearch && (results.getResults().size() < results.getTotalResults())) {
            if (results.getTotalResults() > this.getMaxResultSize(indexBase, indexType)) {
                throw new SegueSearchException(String.format("The search you have requested " +
                        "exceeds the maximum number of results that can be returned at once (%s).",
                        this.getMaxResultSize(indexBase, indexType)));
            }

            sourceBuilder = new SearchSourceBuilder().query(query).size(results.getTotalResults().intValue()).from(startIndex);
            results = executeQuery(typedIndex, sourceBuilder);

            log.debug("Unlimited Search - had to make a second round trip to elasticsearch.");
        }

        return results;
    }

    /**
     * A general method for getting the results of a search.
     * @param typedIndex
     *            - the index within which to search
     * @param searchSourceBuilder
     *            - the search request to send to the cluster.
     * @return List of the search results.
     */
    private ResultsWrapper<String> executeQuery(final String typedIndex, final SearchSourceBuilder searchSourceBuilder)
            throws SegueSearchException {
        try {
            SearchResponse response = client.search(new SearchRequest(typedIndex).source(searchSourceBuilder), RequestOptions.DEFAULT);

            List<SearchHit> hitAsList = Arrays.asList(response.getHits().getHits());
            List<String> resultList = new ArrayList<>();

            log.debug("TOTAL SEARCH HITS " + response.getHits().getTotalHits());
            log.debug("Search Request: " + searchSourceBuilder);
            for (SearchHit item : hitAsList) {
                resultList.add(item.getSourceAsString());
            }

            return new ResultsWrapper<>(resultList, response.getHits().getTotalHits().value);
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
     */
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

    private QueryBuilder processMatchInstructions(final AbstractMatchInstruction matchInstruction)
            throws SegueSearchException {
        if (matchInstruction instanceof BooleanMatchInstruction) {
            BooleanMatchInstruction booleanMatch = (BooleanMatchInstruction) matchInstruction;
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            for (AbstractMatchInstruction should : booleanMatch.getShoulds()){
                query.should(processMatchInstructions(should));
            }
            for (AbstractMatchInstruction must : booleanMatch.getMusts()) {
                query.must(processMatchInstructions(must));
            }
            for (AbstractMatchInstruction mustNot : booleanMatch.getMustNots()) {
                query.mustNot(processMatchInstructions(mustNot));
            }
            query.minimumShouldMatch(booleanMatch.getMinimumShouldMatch());
            if (booleanMatch.getBoost() != null) {
                query.boost(booleanMatch.getBoost());
            }
            return query;
        }

        else if (matchInstruction instanceof ShouldMatchInstruction) {
            ShouldMatchInstruction shouldMatch = (ShouldMatchInstruction) matchInstruction;
            MatchQueryBuilder matchQuery = QueryBuilders
                    .matchQuery(shouldMatch.getField(), shouldMatch.getValue()).boost(shouldMatch.getBoost());
            if (shouldMatch.getFuzzy()) {
                matchQuery.fuzziness(Fuzziness.AUTO);
            }
            return matchQuery;
        }

        else if (matchInstruction instanceof MustMatchInstruction) {
            MustMatchInstruction mustMatch = (MustMatchInstruction) matchInstruction;
            return QueryBuilders.matchQuery(mustMatch.getField(), mustMatch.getValue());
        }

        else if (matchInstruction instanceof RangeMatchInstruction) {
            RangeMatchInstruction rangeMatch = (RangeMatchInstruction) matchInstruction;
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(rangeMatch.getField()).boost(rangeMatch.getBoost());
            if (rangeMatch.getGreaterThan() != null) {
                rangeQuery.gt(rangeMatch.getGreaterThan());
            }
            if (rangeMatch.getGreaterThanOrEqual() != null) {
                rangeQuery.gte(rangeMatch.getGreaterThanOrEqual());
            }
            if (rangeMatch.getLessThan() != null) {
                rangeQuery.lt(rangeMatch.getLessThan());
            }
            if (rangeMatch.getLessThanOrEqual() != null) {
                rangeQuery.gt(rangeMatch.getLessThan());
            }
            return rangeQuery;
        }

        else {
            throw new SegueSearchException(
                    "Processing match instruction which is not supported: " + matchInstruction.getClass());
        }
    }

    public GetResponse getById(final String indexBase, final String indexType, final String id) throws SegueSearchException {
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        try {
            return client.get(new GetRequest(typedIndex, id).fetchSourceContext(FetchSourceContext.FETCH_SOURCE),
                    RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new SegueSearchException(String.format("Failed to get content with ID %s from index %s", id, typedIndex), e);
        }
    }

    public SearchResponse getAllFromIndex(final String indexBase, final String indexType) throws SegueSearchException {
        String typedIndex = ElasticSearchProvider.produceTypedIndexName(indexBase, indexType);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().size(10000).fetchSource(true);
        try {
            return client.search(new SearchRequest(typedIndex).source(sourceBuilder), RequestOptions.DEFAULT);
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
                Map<String, Settings> response = client.indices().get(new GetIndexRequest(typedIndex), RequestOptions.DEFAULT).getSettings();
                for (Settings settings : response.values()) {
                    if (null == settings) {
                        continue;
                    }
                    this.settingsCache.put(MAX_WINDOW_SIZE_KEY, settings.get(typedIndex + ".max_result_window",
                            Integer.toString(SEARCH_MAX_WINDOW_SIZE)));
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