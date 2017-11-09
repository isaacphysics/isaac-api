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

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsRequest;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.SEARCH_MAX_WINDOW_SIZE;

/**
 * A class that works as an adapter for ElasticSearch.
 *
 * @author Stephen Cummins
 *
 */
public class ElasticSearchProvider implements ISearchProvider {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);

    protected final Client client;

    // to try and improve performance of searches with a -1 limit.
    private static final int LARGE_LIMIT = 100;

    // used to optimise index setting retrieval as these probably don't change every request.
    private final Cache<String, String> settingsCache;


    /**
     * Constructor for creating an instance of the ElasticSearchProvider Object.
     *
     * @param searchClient
     *            - the client that the provider should be using.
     */
    @Inject
    public ElasticSearchProvider(final Client searchClient) {
        this.client = searchClient;
        this.settingsCache = CacheBuilder.newBuilder().softValues().expireAfterWrite(10, TimeUnit.MINUTES).build();
    }

    @Override
    public ResultsWrapper<String> matchSearch(final String index, final String indexType,
                                              final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch, final int startIndex,
                                              final int limit, final Map<String, Constants.SortOrder> sortInstructions,
                                              @Nullable final Map<String, AbstractFilterInstruction> filterInstructions) throws SegueSearchException {
        // build up the query from the fieldsToMatch map
        QueryBuilder query = generateBoolMatchQuery(fieldsToMatch);

        if (filterInstructions != null) {
            query = QueryBuilders.boolQuery().must(query).filter(generateFilterQuery(filterInstructions));
        }

        return this.executeBasicQuery(index, indexType, query, startIndex, limit, sortInstructions);
    }

    @Override
    public final ResultsWrapper<String> randomisedMatchSearch(final String index, final String indexType,
                                                              final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch, final int startIndex,
                                                              final int limit, final Long randomSeed, final Map<String, AbstractFilterInstruction> filterInstructions)
            throws SegueSearchException {
        // build up the query from the fieldsToMatch map
        QueryBuilder query = generateBoolMatchQuery(fieldsToMatch);

        query = QueryBuilders.functionScoreQuery(query, ScoreFunctionBuilders.randomFunction(randomSeed));

        if (filterInstructions != null) {
            query = QueryBuilders.boolQuery().must(query).filter(generateFilterQuery(filterInstructions));
        }

        log.debug("Randomised Query, with seed: " + randomSeed + ", to be sent to elasticsearch is : " + query);

        return this.executeBasicQuery(index, indexType, query, startIndex, limit);
    }

    @Override
    public ResultsWrapper<String> fuzzySearch(final String index, final String indexType, final String searchString,
                                              final Integer startIndex, final Integer limit,
                                              @Nullable final Map<String, List<String>> fieldsThatMustMatch,
                                              @Nullable final Map<String, AbstractFilterInstruction> filterInstructions,
                                              final String... fields) throws SegueSearchException {
        if (null == index || null == indexType || null == searchString || null == fields) {
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

        for (String f : fields) {
            float boost = boostFields.contains(f) ? 2f : 1f;

            QueryBuilder initialFuzzySearch = QueryBuilders.matchQuery(f, searchString)
                    .fuzziness(Fuzziness.AUTO)
                    .prefixLength(0)
                    .boost(boost);
            query.should(initialFuzzySearch);

            QueryBuilder regexSearch = QueryBuilders.wildcardQuery(f, "*" + searchString + "*").boost(boost);
            query.should(regexSearch);
        }

        // this query is just a bit smarter than the regex search above.
        QueryBuilder multiMatchPrefixQuery = QueryBuilders.multiMatchQuery(searchString, fields)
                .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX).prefixLength(2).boost(2.0f);
        query.should(multiMatchPrefixQuery);

        masterQuery.must(query);

        if (filterInstructions != null) {
            masterQuery.filter(generateFilterQuery(filterInstructions));
        }

        return this.executeBasicQuery(index, indexType, masterQuery, startIndex, limit);
    }

    @Override
    public ResultsWrapper<String> termSearch(final String index, final String indexType,
                                             final String searchTerm, final String field, final int startIndex, final int limit,
                                             @Nullable final Map<String, AbstractFilterInstruction> filterInstructions)
            throws SegueSearchException {
        if (null == index || null == indexType || (null == searchTerm && null != field)) {
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

        return this.executeBasicQuery(index, indexType, query, startIndex, limit);
    }

    /**
     * This method will create a threadsafe client that can be used to talk to an Elastic Search cluster.
     *
     * @param clusterName
     *            - the name of the cluster to connect to.
     * @param address
     *            - address of the cluster to connect to.
     * @param port
     *            - port that the cluster is running on.
     * @return Defaults to http client creation.
     */
    public static Client getTransportClient(final String clusterName, final String address, final int port) throws UnknownHostException {
        TransportClient client = new PreBuiltTransportClient(Settings.builder().put("cluster.name", clusterName)
                .put("client.transport.ping_timeout", "180s").build())
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(address), port));

        log.info("Elastic Search Transport client created: " + address + ":" + port);
        return client;
    }

    @Override
    public boolean hasIndex(final String index) {
        Validate.notNull(index);
        return client.admin().indices().exists(new IndicesExistsRequest(index)).actionGet().isExists();
    }

    @Override
    public Collection<String> getAllIndices() {
        return client.admin().indices().stats(new IndicesStatsRequest()).actionGet().getIndices().keySet();
    }

    @Override
    public ResultsWrapper<String> findByPrefix(final String index, final String indexType, final String fieldname,
                                               final String prefix, final int startIndex, final int limit, final Map<String, AbstractFilterInstruction> filterInstructions)
            throws SegueSearchException {
        ResultsWrapper<String> resultList;

        QueryBuilder query = QueryBuilders.prefixQuery(fieldname, prefix);

        if (filterInstructions != null) {
            query = QueryBuilders.boolQuery().must(query).filter(generateFilterQuery(filterInstructions));
        }

        resultList = this.executeBasicQuery(index, indexType, query, startIndex, limit);

        return resultList;
    }

    @Override
    public ResultsWrapper<String> findByRegEx(final String index, final String indexType, final String fieldname,
                                              final String regex, final int startIndex, final int limit,
                                              final Map<String, AbstractFilterInstruction> filterInstructions)
            throws SegueSearchException {
        ResultsWrapper<String> resultList;

        QueryBuilder query = QueryBuilders.regexpQuery(fieldname, regex);

        if (filterInstructions != null) {
            query = QueryBuilders.boolQuery().must(query).filter(generateFilterQuery(filterInstructions));
        }

        resultList = this.executeBasicQuery(index, indexType, query, startIndex, limit);

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
    private SearchRequestBuilder addSortInstructions(final SearchRequestBuilder searchRequest,
                                                     final Map<String, Constants.SortOrder> sortInstructions) {
        // deal with sorting of results
        for (Map.Entry<String, Constants.SortOrder> entry : sortInstructions.entrySet()) {
            String sortField = entry.getKey();
            Constants.SortOrder sortOrder = entry.getValue();

            if (sortOrder == Constants.SortOrder.ASC) {
                searchRequest.addSort(SortBuilders.fieldSort(sortField).order(SortOrder.ASC).missing("_last"));

            } else {
                searchRequest.addSort(SortBuilders.fieldSort(sortField).order(SortOrder.DESC).missing("_last"));
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
    private QueryBuilder generateFilterQuery(final Map<String, AbstractFilterInstruction> filterInstructions) {
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

                Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch = Maps.newHashMap();

                fieldsToMatch.put(immutableEntry(Constants.BooleanOperator.AND, fieldToFilterInstruction.getKey()),
                        Arrays.asList(sfi.getMustMatchValue()));

                filter.must(this.generateBoolMatchQuery(fieldsToMatch));
            }

            if (fieldToFilterInstruction.getValue() instanceof TermsFilterInstruction) {
                TermsFilterInstruction sfi = (TermsFilterInstruction) fieldToFilterInstruction.getValue();
                filter.must(QueryBuilders.termsQuery(fieldToFilterInstruction.getKey(), sfi.getMatchValues()));
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
    private BoolQueryBuilder generateBoolMatchQuery(
            final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch) {
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        // This Set will allow us to calculate the minimum should match value -
        // it is assumed that for each or'd field there should be a match
        Set<String> shouldMatchSet = Sets.newHashSet();
        for (Map.Entry<Map.Entry<Constants.BooleanOperator, String>, List<String>> pair : fieldsToMatch.entrySet()) {
            // extract the MapEntry which contains a key value pair of the
            // operator and the list of operands to match against.
            Constants.BooleanOperator operatorForThisField = pair.getKey().getKey();

            // go through each operand and add it to the query
            if (pair.getValue() != null) {
                for (String queryItem : pair.getValue()) {
                    if (operatorForThisField.equals(Constants.BooleanOperator.OR)) {
                        shouldMatchSet.add(pair.getKey().getValue());
                        query.should(QueryBuilders.matchQuery(pair.getKey().getValue(), queryItem))
                                .minimumNumberShouldMatch(shouldMatchSet.size());
                    } else {
                        query.must(QueryBuilders.matchQuery(pair.getKey().getValue(), queryItem));
                    }
                }

            } else {
                log.warn("Null argument received in paginated match search... "
                        + "This is not usually expected. Ignoring it and continuing anyway.");
            }
        }
        return query;
    }

    /**
     * Provides default search execution using the fields specified.
     *
     * This method does not provide any way of controlling sort order or limiting information returned. It is most
     * useful for doing simple searches with fewer results e.g. by id.
     *
     * @param index
     *            - search index to execute the query against.
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
    private ResultsWrapper<String> executeBasicQuery(final String index, final String indexType,
                                                     final QueryBuilder query, final int startIndex, final int limit)
            throws SegueSearchException {
        return this.executeBasicQuery(index, indexType, query, startIndex, limit, null);
    }

    /**
     * Provides default search execution using the fields specified.
     *
     * This method does not provide any way of controlling sort order or limiting information returned. It is most
     * useful for doing simple searches with fewer results e.g. by id.
     *
     * @param index
     *            - search index to execute the query against.
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
    private ResultsWrapper<String> executeBasicQuery(final String index, final String indexType,
                                                     final QueryBuilder query, final int startIndex, final int limit,
                                                     @Nullable final Map<String, Constants.SortOrder> sortInstructions) throws SegueSearchException {
        int newLimit = limit;
        boolean isUnlimitedSearch = limit == -1;

        if (isUnlimitedSearch) {
            newLimit = LARGE_LIMIT;
        }

        SearchRequestBuilder configuredSearchRequestBuilder = client.prepareSearch(index).setTypes(indexType)
                .setQuery(query).setSize(newLimit).setFrom(startIndex);

        if (sortInstructions != null) {
            this.addSortInstructions(configuredSearchRequestBuilder, sortInstructions);
        }

        log.debug("Building Query: " + configuredSearchRequestBuilder);
        ResultsWrapper<String> results = executeQuery(configuredSearchRequestBuilder);

        // execute another query to get all results as this is an unlimited
        // query.
        if (isUnlimitedSearch && (results.getResults().size() < results.getTotalResults())) {
            if (results.getTotalResults() > this.getMaxResultSize(index)) {
                throw new SegueSearchException(String.format("The search you have requested " +
                        "exceeds the maximum number of results that can be returned at once (%s).",
                        this.getMaxResultSize(index)));
            }

            configuredSearchRequestBuilder = client.prepareSearch(index).setTypes(indexType).setQuery(query)
                    .setSize(results.getTotalResults().intValue()).setFrom(startIndex);

            results = executeQuery(configuredSearchRequestBuilder);

            log.debug("Unlimited Search - had to make a second round trip to elasticsearch.");
        }

        return results;
    }

    /**
     * A general method for getting the results of a search.
     *
     * @param configuredSearchRequestBuilder
     *            - the search request to send to the cluster.
     * @return List of the search results.
     */
    private ResultsWrapper<String> executeQuery(final SearchRequestBuilder configuredSearchRequestBuilder)
            throws SegueSearchException{
        try {
            SearchResponse response = configuredSearchRequestBuilder.execute().actionGet();

            List<SearchHit> hitAsList = Arrays.asList(response.getHits().getHits());
            List<String> resultList = new ArrayList<>();

            log.debug("TOTAL SEARCH HITS " + response.getHits().getTotalHits());
            log.debug("Search Request: " + configuredSearchRequestBuilder);
            for (SearchHit item : hitAsList) {
                resultList.add(item.getSourceAsString());
            }

            return new ResultsWrapper<>(resultList, response.getHits().getTotalHits());
        } catch (ElasticsearchException e) {
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
    private Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> convertToBoolMap(
            final Map<String, List<String>> fieldsThatMustMatch) {
        if (null == fieldsThatMustMatch) {
            return null;
        }

        Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> result = Maps.newHashMap();

        for (Map.Entry<String, List<String>> pair : fieldsThatMustMatch.entrySet()) {
            Map.Entry<Constants.BooleanOperator, String> mapEntry = com.google.common.collect.Maps.immutableEntry(
                    Constants.BooleanOperator.OR, pair.getKey());

            result.put(mapEntry, pair.getValue());
        }

        return result;
    }

    public GetResponse getById(String index, String type, String id) {
        GetRequestBuilder grb = client.prepareGet(index, type, id).setFetchSource(true);
        return grb.execute().actionGet();
    }

    public SearchResponse getAllByType(String index, String type) {
        return client.prepareSearch(index).setTypes(type).setSize(10000).setFetchSource(true).execute().actionGet();
    }

    /**
     * This method returns the maximum window size. i.e. the number of results that can be returned in a single result
     * set without having to do a special scroll query.
     *
     * This is a configurable value but the default Elastic Search value is 10,000.
     *
     * TODO: we may want to selectively upgrade queries to scroll requests if exceeding this limit.
     *
     * @param index - to look up
     * @return the configured index max window size or a default,
     * if a request exceeds this an error will be thrown. (or we should use the scroll api.
     */
    private int getMaxResultSize(final String index) {
        final String MAX_WINDOW_SIZE_KEY = index + "_" + "MAX_WINDOW_SIZE";
        String max_window_size = this.settingsCache.getIfPresent(MAX_WINDOW_SIZE_KEY);
        if (null == max_window_size) {
            GetSettingsResponse response = client.admin().indices()
                    .prepareGetSettings(index).get();
            for (ObjectObjectCursor<String, Settings> cursor : response.getIndexToSettings()) {
                Settings settings = cursor.value;
                if (null == settings) {
                    continue;
                }

                this.settingsCache.put(MAX_WINDOW_SIZE_KEY, settings.get(index + ".max_result_window",
                        Integer.toString(SEARCH_MAX_WINDOW_SIZE)));
            }
        }
        return Integer.parseInt(this.settingsCache.getIfPresent(MAX_WINDOW_SIZE_KEY));
    }
}