/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.delete.DeleteMappingRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.PrefixQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;

import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.inject.Inject;

/**
 * A class that works as an adapter for ElasticSearch.
 * 
 * @author Stephen Cummins
 * 
 */
public class ElasticSearchProvider implements ISearchProvider {
	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);

	private final Client client;
	private final List<String> rawFieldsList;

	private final Random randomNumberGenerator;

	// to try and improve performance of searches with a -1 limit.
	private static final int LARGE_LIMIT = 100; 

	/**
	 * Constructor for creating an instance of the ElasticSearchProvider Object.
	 * 
	 * @param searchClient
	 *            - the client that the provider should be using.
	 */
	@Inject
	public ElasticSearchProvider(final Client searchClient) {
		this.client = searchClient;
		rawFieldsList = new ArrayList<String>();
		this.randomNumberGenerator = new Random();
	}

	@Override
	public void indexObject(final String index, final String indexType, final String content)
		throws SegueSearchOperationException {
		indexObject(index, indexType, content, null);
	}

	@Override
	public void bulkIndex(final String index, final String indexType,
			final List<Map.Entry<String, String>> dataToIndex) throws SegueSearchOperationException {
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
			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
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

	@Override
	public void indexObject(final String index, final String indexType, final String content,
			final String uniqueId) throws SegueSearchOperationException {
		// check index already exists if not execute any initialisation steps.
		if (!this.hasIndex(index)) {
			this.sendMappingCorrections(index, indexType);
		}

		try {
			IndexResponse indexResponse = client.prepareIndex(index, indexType, uniqueId).setSource(content)
					.execute().actionGet();
			log.debug("Document: " + indexResponse.getId() + " indexed.");

		} catch (ElasticsearchException e) {
			throw new SegueSearchOperationException("Error during index operation.", e);
		}
	}

	@Override
	public ResultsWrapper<String> matchSearch(final String index, final String indexType,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final int startIndex, final int limit, final Map<String, Constants.SortOrder> sortInstructions) {

		return this.matchSearch(index, indexType, fieldsToMatch, startIndex, limit, sortInstructions, null);
	}
	
	@Override
	public ResultsWrapper<String> matchSearch(final String index, final String indexType,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final int startIndex, final int limit, final Map<String, Constants.SortOrder> sortInstructions,
			@Nullable final Map<String, AbstractFilterInstruction> filterInstructions) {

		// build up the query from the fieldsToMatch map
		QueryBuilder query = generateBoolMatchQuery(fieldsToMatch);
	
		if (filterInstructions != null) {
			query = QueryBuilders.filteredQuery(query, generateFilterQuery(filterInstructions));
		}
		
		log.debug("Query to be sent to elasticsearch is : " + query);

		SearchRequestBuilder searchRequest = client.prepareSearch(index).setTypes(indexType).setQuery(query)
				.setFrom(startIndex);

		if (limit > 0) {
			searchRequest.setSize(limit);
		} else {
			// if the limit is less than 0 then we want to show all - although
			// in order to do this we have to execute a search and then get the
			// total hits back.
			// this is a restriction on elastic search.
			// TODO: we need to fix this to use similar logic to the execute basic query limit stuff.
			log.debug("Setting limit to be the size of the result set... "
					+ "Unlimited search may cause performance issues");
			int largerlimit = this.executeQuery(searchRequest).getTotalResults().intValue();
			searchRequest.setSize(largerlimit);
		}

		searchRequest = addSortInstructions(searchRequest, sortInstructions);

		return this.executeQuery(searchRequest);
	}

	@Override
	public ResultsWrapper<String> randomisedMatchSearch(final String index,
			final String indexType,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final int startIndex, final int limit) {
		Long seed = this.randomNumberGenerator.nextLong();
		return this.randomisedMatchSearch(index, indexType, fieldsToMatch, startIndex, limit, seed);
	}

	@Override
	public final ResultsWrapper<String> randomisedMatchSearch(final String index,
			final String indexType,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final int startIndex, final int limit, final Long randomSeed) {
		// build up the query from the fieldsToMatch map
		QueryBuilder query = generateBoolMatchQuery(fieldsToMatch);
		Long seed = randomSeed;

		query = QueryBuilders.functionScoreQuery(query, ScoreFunctionBuilders.randomFunction(seed));

		log.debug("Randomised Query, with seed: " + seed + ", to be sent to elasticsearch is : " + query);

		SearchRequestBuilder searchRequest = client.prepareSearch(index).setTypes(indexType).setQuery(query)
				.setSize(limit).setFrom(startIndex);

		return this.executeQuery(searchRequest);
	}

	@Override
	public ResultsWrapper<String> fuzzySearch(final String index, final String indexType,
			final String searchString, final Integer startIndex, final Integer limit, 
			@Nullable final Map<String, List<String>> fieldsThatMustMatch,
			final String... fields) {
		if (null == index || null == indexType || null == searchString || null == fields) {
			log.warn("A required field is missing. Unable to execute search.");
			return null;
		}

		BoolQueryBuilder masterQuery;
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		if (null != fieldsThatMustMatch) {
			masterQuery = this.generateBoolMatchQuery(this.convertToBoolMap(fieldsThatMustMatch));
		} else {
			masterQuery = QueryBuilders.boolQuery();
		}

		QueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(searchString, fields).type(
				MultiMatchQueryBuilder.Type.PHRASE_PREFIX).boost(2.0f);
		query.should(multiMatchQuery);
		
		QueryBuilder fuzzyQuery = QueryBuilders.fuzzyLikeThisQuery(fields).likeText(searchString)
				.fuzziness(Fuzziness.AUTO);
		query.should(fuzzyQuery);
		
		masterQuery.must(query);
		
		ResultsWrapper<String> resultList = this.executeBasicQuery(index, indexType, masterQuery,
				startIndex, limit);
		
		return resultList;
	}

	@Override
	public ResultsWrapper<String> basicFieldSearch(final String index, final String indexType,
			final String searchString, final Integer startIndex, final Integer limit, 
			@Nullable final Map<String, List<String>> fieldsThatMustMatch, final String... fields) {
		if (null == index || null == indexType || null == searchString || null == fields) {
			log.warn("A required field is missing. Unable to execute search.");
			return null;
		}

		BoolQueryBuilder query;
		if (null != fieldsThatMustMatch) {
			query = this.generateBoolMatchQuery(this.convertToBoolMap(fieldsThatMustMatch));
		} else {
			query = QueryBuilders.boolQuery();
		}
		
		QueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(searchString, fields).type(
				MultiMatchQueryBuilder.Type.PHRASE_PREFIX);
		query.must(multiMatchQuery);
		
		ResultsWrapper<String> resultList = this.executeBasicQuery(index, indexType,
				query, startIndex, limit);

		return resultList;
	}
	
	@Override
	public ResultsWrapper<String> termSearch(final String index, final String indexType,
			final Collection<String> searchTerms, final String field, final int startIndex, final int limit) {
		if (null == index || null == indexType || null == searchTerms || null == field) {
			log.warn("A required field is missing. Unable to execute search.");
			return null;
		}

		QueryBuilder query = QueryBuilders.termsQuery(field, searchTerms).minimumMatch(searchTerms.size());
		ResultsWrapper<String> resultList = this.executeBasicQuery(index, indexType, query, startIndex, limit);
		return resultList;
	}

	@Override
	public boolean expungeEntireSearchCache() {
		return this.expungeIndexFromSearchCache("_all");
	}

	@Override
	public boolean expungeIndexFromSearchCache(final String index) {
		Validate.notBlank(index);

		try {
			log.info("Sending delete request to ElasticSearch for search index: " + index);
			client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
		} catch (ElasticsearchException e) {
			log.error("ElasticSearch exception while trying to delete index " + index);
			return false;
		}

		return true;
	}

	@Override
	public boolean expungeIndexTypeFromSearchCache(final String index, final String indexType) {
		try {
			DeleteMappingRequest deleteMapping = new DeleteMappingRequest(index).types(indexType);
			client.admin().indices().deleteMapping(deleteMapping).actionGet();
		} catch (ElasticsearchException e) {
			log.error("ElasticSearch exception while trying to delete index " + index + " type " + indexType);
			return false;
		}
		return true;
	}

	/**
	 * This method will create a threadsafe client that can be used to talk to
	 * an Elastic Search cluster.
	 * 
	 * @param clusterName
	 *            - the name of the cluster to connect to.
	 * @param address
	 *            - address of the cluster to connect to.
	 * @param port
	 *            - port that the cluster is running on.
	 * @return Defaults to http client creation.
	 */
	public static Client getTransportClient(final String clusterName, final String address, final int port) {
		Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build();
		TransportClient transportClient = new TransportClient(settings);
		InetSocketTransportAddress transportAddress = new InetSocketTransportAddress(address, port);
		transportClient = transportClient.addTransportAddress(transportAddress);
		return transportClient;
	}

	@Override
	public boolean hasIndex(final String index) {
		Validate.notNull(index);
		return client.admin().indices().exists(new IndicesExistsRequest(index)).actionGet().isExists();
	}

	@Override
	public void registerRawStringFields(final List<String> fieldNames) {
		this.rawFieldsList.addAll(fieldNames);
	}

	@Override
	public ResultsWrapper<String> findByPrefix(final String index, final String indexType,
			final String fieldname, final String prefix, final int startIndex, final int limit) {
		ResultsWrapper<String> resultList;

		PrefixQueryBuilder query = QueryBuilders.prefixQuery(fieldname, prefix);

		resultList = this.executeBasicQuery(index, indexType, query, startIndex, limit);

		return resultList;
	}

	/**
	 * Utility method to convert sort instructions form external classes into
	 * something Elastic search can use.
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
				searchRequest
						.addSort(SortBuilders.fieldSort(sortField).order(SortOrder.ASC).missing("_last"));

			} else {
				searchRequest.addSort(SortBuilders.fieldSort(sortField).order(SortOrder.DESC)
						.missing("_last"));
			}
		}

		return searchRequest;
	}

	/**
	 * Helper method to create elastic search understandable filter instructions.
	 * @param filterInstructions - in the form "fieldName --> instruction key --> instruction value"
	 * @return filterbuilder
	 */
	private FilterBuilder generateFilterQuery(final Map<String, AbstractFilterInstruction> filterInstructions) {
		AndFilterBuilder filter = FilterBuilders.andFilter();

		for (Entry<String, AbstractFilterInstruction> fieldToFilterInstruction : filterInstructions.entrySet()) {
			// date filter logic
			if (fieldToFilterInstruction.getValue() instanceof DateRangeFilterInstruction) {
				DateRangeFilterInstruction dateRangeInstruction = (DateRangeFilterInstruction) fieldToFilterInstruction
						.getValue();
				RangeFilterBuilder rangeFilter = FilterBuilders.rangeFilter(fieldToFilterInstruction.getKey());
				// Note: assumption that dates are stored in long format.
				if (dateRangeInstruction.getFromDate() != null) {
					rangeFilter.from(dateRangeInstruction.getFromDate().getTime());
				} 
				
				if (dateRangeInstruction.getToDate() != null) {
					rangeFilter.to(dateRangeInstruction.getToDate().getTime());
				}

				filter.add(rangeFilter);
			}
		}

		return filter;
	}
	
	/**
	 * Utility method to generate a BoolMatchQuery based on the parameters
	 * provided.
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
		for (Map.Entry<Map.Entry<Constants.BooleanOperator, String>, List<String>> pair : fieldsToMatch
				.entrySet()) {
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
	 * This method does not provide any way of controlling sort order or
	 * limiting information returned. It is most useful for doing simple
	 * searches with fewer results e.g. by id.
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
			final QueryBuilder query, final int startIndex, final int limit) {
		log.debug("Building Query: " + query);
		int newLimit = limit;
		
		boolean isUnlimitedSearch = limit == -1;
		
		if (isUnlimitedSearch) {
			newLimit = LARGE_LIMIT;
		}

		SearchRequestBuilder configuredSearchRequestBuilder = client.prepareSearch(index).setTypes(indexType)
				.setQuery(query).setSize(newLimit).setFrom(startIndex);

		ResultsWrapper<String> results = executeQuery(configuredSearchRequestBuilder);

		// execute another query to get all results as this is an unlimited
		// query.
		if (isUnlimitedSearch && (results.getResults().size() < results.getTotalResults())) {
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
	private ResultsWrapper<String> executeQuery(final SearchRequestBuilder configuredSearchRequestBuilder) {
		SearchResponse response = configuredSearchRequestBuilder.execute().actionGet();

		List<SearchHit> hitAsList = Arrays.asList(response.getHits().getHits());
		List<String> resultList = new ArrayList<String>();

		log.debug("TOTAL SEARCH HITS " + response.getHits().getTotalHits());
		log.debug("Search Request: " + configuredSearchRequestBuilder);
		for (SearchHit item : hitAsList) {
			resultList.add(item.getSourceAsString());
		}

		return new ResultsWrapper<String>(resultList, response.getHits().getTotalHits());
	}

	/**
	 * This function will allow top level fields to have their contents cloned
	 * into an unanalysed field with the name {FieldName}.{raw}
	 * 
	 * This is useful if we want to query the original data without
	 * ElasticSearch having messed with it.
	 * 
	 * @param index
	 *            - index to send the mapping corrections to.
	 * @param indexType
	 *            - type to send the mapping corrections to.
	 */
	private void sendMappingCorrections(final String index, final String indexType) {
		try {
			CreateIndexRequestBuilder indexBuilder = client.admin().indices().prepareCreate(index);

			final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject()
					.startObject(indexType).startObject("properties");

			for (String fieldName : this.rawFieldsList) {
				log.debug("Sending raw mapping correction for " + fieldName + "."
						+ Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX);

				mappingBuilder.startObject(fieldName).field("type", "string").field("index", "analyzed")
						.startObject("fields").startObject(Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX)
						.field("type", "string").field("index", "not_analyzed").endObject().endObject()
						.endObject();
			}
			// close off json structure
			mappingBuilder.endObject().endObject().endObject();
			indexBuilder.addMapping(indexType, mappingBuilder);

			// Send Mapping information
			indexBuilder.execute().actionGet();

		} catch (IOException e) {
			log.error("Error while sending mapping correction " + "instructions to the ElasticSearch Server",
					e);
		}
	}

	/**
	 * Utility function to support conversion between simple field maps and bool
	 * maps.
	 * 
	 * @param fieldsThatMustMatch
	 *            - the map that should be converted into a suitable map for
	 *            querying.
	 * @return Map where each field is using the OR boolean operator.
	 */
	private Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> convertToBoolMap(
			final Map<String, List<String>> fieldsThatMustMatch) {
		if (null == fieldsThatMustMatch) {
			return null;
		}

		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> result = Maps.newHashMap();

		for (Map.Entry<String, List<String>> pair : fieldsThatMustMatch.entrySet()) {
			Map.Entry<Constants.BooleanOperator, String> mapEntry = com.google.common.collect.Maps
					.immutableEntry(Constants.BooleanOperator.OR, pair.getKey());

			result.put(mapEntry, pair.getValue());
		}

		return result;
	}
}