package uk.ac.cam.cl.dtg.segue.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;

import com.google.api.client.util.Sets;
import com.google.inject.Inject;

public class ElasticSearchProvider implements ISearchProvider {

	private static final Logger log = LoggerFactory
			.getLogger(ElasticSearchProvider.class);

	private final Client client;
	private final List<String> rawFieldsList;

	private final Random randomNumberGenerator;

	@Inject
	public ElasticSearchProvider(Client searchClient) {
		this.client = searchClient;
		rawFieldsList = new ArrayList<String>();
		this.randomNumberGenerator = new Random();
	}

	@Override
	public boolean indexObject(final String index, final String indexType,
			final String content) {
		return indexObject(index, indexType, content, null);
	}

	@Override
	public boolean indexObject(String index, final String indexType,
			String content, String uniqueId) {
		// check index already exists if not execute any initialisation steps.
		if (!this.hasIndex(index)) {
			this.sendMappingCorrections(index, indexType);
		}

		try {
			IndexResponse indexResponse = client
					.prepareIndex(index, indexType, uniqueId)
					.setSource(content).execute().actionGet();
			log.info("Document: " + indexResponse.getId() + " indexed.");

			return true;
		} catch (ElasticsearchException e) {
			log.error("Elastic Search Exception detected.");
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public ResultsWrapper<String> paginatedMatchSearch(
			final String index,
			final String indexType,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final int startIndex, int limit,
			Map<String, Constants.SortOrder> sortInstructions) {

		// build up the query from the fieldsToMatch map
		BoolQueryBuilder query = generateBoolMatchQuery(fieldsToMatch);

		log.debug("Query to be sent to elasticsearch is : " + query);

		SearchRequestBuilder searchRequest = client.prepareSearch(index)
				.setTypes(indexType).setQuery(query).setFrom(startIndex);

		if (limit > 0) {
			searchRequest.setSize(limit);
		} else {
			// if the limit is less than 0 then we want to show all - although
			// in order to do this we have to execute a search and then get the
			// total hits back.
			// this is a restriction on elastic search.
			log.info("Setting limit to be the size of the result set... Unlimited search may cause performance issues");
			limit = this.executeQuery(searchRequest).getTotalResults()
					.intValue();
			searchRequest.setSize(limit);
		}

		searchRequest = addSortInstructions(searchRequest, sortInstructions);

		return this.executeQuery(searchRequest);
	}

	@Override
	public ResultsWrapper<String> randomisedPaginatedMatchSearch(
			final String index,
			final String indexType,
			final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch,
			final int startIndex, final int limit) {
		// build up the query from the fieldsToMatch map
		QueryBuilder query = generateBoolMatchQuery(fieldsToMatch);
		Long seed = this.randomNumberGenerator.nextLong();

		log.debug("Randomised Query, with seed: " + seed
				+ ", to be sent to elasticsearch is : " + query);

		query = QueryBuilders.functionScoreQuery(query,
				ScoreFunctionBuilders.randomFunction(seed));

		SearchRequestBuilder searchRequest = client.prepareSearch(index)
				.setTypes(indexType).setQuery(query).setSize(limit)
				.setFrom(startIndex);

		return this.executeQuery(searchRequest);
	}

	@Override
	public ResultsWrapper<String> fuzzySearch(final String index,
			final String indexType, final String searchString,
			final String... fields) {
		if (null == index || null == indexType || null == searchString
				|| null == fields) {
			log.warn("A required field is missing. Unable to execute search.");
			return null;
		}

		QueryBuilder query = QueryBuilders.fuzzyLikeThisQuery(fields).likeText(
				searchString);
		ResultsWrapper<String> resultList = this.executeBasicQuery(index,
				indexType, query);
		return resultList;
	}

	@Override
	public ResultsWrapper<String> termSearch(final String index,
			final String indexType, final Collection<String> searchTerms,
			final String field) {
		if (null == index || null == indexType || null == searchTerms
				|| null == field) {
			log.warn("A required field is missing. Unable to execute search.");
			return null;
		}

		QueryBuilder query = QueryBuilders.termsQuery(field, searchTerms)
				.minimumMatch(searchTerms.size());
		ResultsWrapper<String> resultList = this.executeBasicQuery(index,
				indexType, query);
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
			log.info("Sending delete request to ElasticSearch for search index: "
					+ index);
			client.admin().indices().delete(new DeleteIndexRequest(index))
					.actionGet();
		} catch (ElasticsearchException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * This method will create a threadsafe client that can be used to talk to
	 * an Elastic Search cluster.
	 * 
	 * @param clusterName
	 * @param address
	 * @param port
	 * @return Defaults to http client creation.
	 */
	public static Client getTransportClient(String clusterName, String address,
			int port) {
		Settings settings = ImmutableSettings.settingsBuilder()
				.put("cluster.name", clusterName).build();
		TransportClient transportClient = new TransportClient(settings);
		InetSocketTransportAddress transportAddress = new InetSocketTransportAddress(
				address, port);
		transportClient = transportClient.addTransportAddress(transportAddress);
		return transportClient;
	}

	@Override
	public boolean hasIndex(final String index) {
		Validate.notNull(index);
		return client.admin().indices().exists(new IndicesExistsRequest(index))
				.actionGet().isExists();
	}

	@Override
	public void registerRawStringFields(List<String> fieldNames) {
		this.rawFieldsList.addAll(fieldNames);
	}

	private SearchRequestBuilder addSortInstructions(
			SearchRequestBuilder searchRequest,
			Map<String, Constants.SortOrder> sortInstructions) {
		// deal with sorting of results
		for (Map.Entry<String, Constants.SortOrder> entry : sortInstructions
				.entrySet()) {
			String sortField = entry.getKey();
			Constants.SortOrder sortOrder = entry.getValue();

			if (sortOrder == Constants.SortOrder.ASC)
				searchRequest.addSort(SortBuilders.fieldSort(sortField)
						.order(SortOrder.ASC).missing("_last"));

			else
				searchRequest.addSort(SortBuilders.fieldSort(sortField)
						.order(SortOrder.DESC).missing("_last"));
		}

		return searchRequest;
	}

	private BoolQueryBuilder generateBoolMatchQuery(
			Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		// This Set will allow us to calculate the minimum should match value -
		// it is assumed that for each or'd field there should be a match
		Set<String> shouldMatchSet = Sets.newHashSet();
		for (Map.Entry<Map.Entry<Constants.BooleanOperator, String>, List<String>> pair : fieldsToMatch
				.entrySet()) {
			// extract the MapEntry which contains a key value pair of the
			// operator and the list of operands to match against.
			Constants.BooleanOperator operatorForThisField = pair.getKey()
					.getKey();

			// go through each operand and add it to the query
			if (pair.getValue() != null) {
				for (String queryItem : pair.getValue()) {
					if (operatorForThisField
							.equals(Constants.BooleanOperator.OR)) {
						shouldMatchSet.add(pair.getKey().getValue());
						query.should(
								QueryBuilders.matchQuery(pair.getKey()
										.getValue(), queryItem))
								.minimumNumberShouldMatch(shouldMatchSet.size());
					} else {
						query.must(QueryBuilders.matchQuery(pair.getKey()
								.getValue(), queryItem));
					}
				}

			} else {
				log.warn("Null argument received in paginated match search... This is not usually expected. Ignoring it and continuing anyway.");
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
	 * @param indexType
	 * @param query
	 * 
	 * @return list of the search results
	 */
	private ResultsWrapper<String> executeBasicQuery(final String index,
			final String indexType, final QueryBuilder query) {
		log.debug("Building Query: " + query);

		SearchRequestBuilder configuredSearchRequestBuilder = client
				.prepareSearch(index).setTypes(indexType).setQuery(query);

		return executeQuery(configuredSearchRequestBuilder);
	}

	/**
	 * A general method for getting the results of a search
	 * 
	 * @param configuredSearchRequestBuilder
	 * @return List of the search results.
	 */
	private ResultsWrapper<String> executeQuery(
			SearchRequestBuilder configuredSearchRequestBuilder) {
		SearchResponse response = configuredSearchRequestBuilder.execute()
				.actionGet();

		List<SearchHit> hitAsList = Arrays.asList(response.getHits().getHits());
		List<String> resultList = new ArrayList<String>();

		log.info("TOTAL SEARCH HITS " + response.getHits().getTotalHits());
		log.debug("Search Request: " + configuredSearchRequestBuilder);
		for (SearchHit item : hitAsList) {
			resultList.add(item.getSourceAsString());
		}

		return new ResultsWrapper<String>(resultList, response.getHits()
				.getTotalHits());
	}

	/**
	 * This function will allow top level fields to have their contents cloned
	 * into an unanalysed field with the name {FieldName}.{raw}
	 * 
	 * This is useful if we want to query the original data without
	 * ElasticSearch having messed with it.
	 * 
	 * @param index
	 * @param indexType
	 */
	private void sendMappingCorrections(final String index,
			final String indexType) {
		try {
			CreateIndexRequestBuilder indexBuilder = client.admin().indices()
					.prepareCreate(index);

			final XContentBuilder mappingBuilder = XContentFactory
					.jsonBuilder().startObject().startObject(indexType)
					.startObject("properties");

			for (String fieldName : this.rawFieldsList) {
				log.info("Sending raw mapping correction for " + fieldName
						+ "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX);

				mappingBuilder.startObject(fieldName).field("type", "string")
						.field("index", "analyzed").startObject("fields")
						.startObject(Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX)
						.field("type", "string").field("index", "not_analyzed")
						.endObject().endObject().endObject();
			}
			// close off json structure
			mappingBuilder.endObject().endObject().endObject();
			indexBuilder.addMapping(indexType, mappingBuilder);

			// Send Mapping information
			indexBuilder.execute().actionGet();

		} catch (IOException e) {
			log.error(
					"Error while sending mapping correction instructions to the ElasticSearch Server",
					e);
		}
	}
}
