package uk.ac.cam.cl.dtg.segue.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;

import com.google.inject.Inject;

public class ElasticSearchProvider implements ISearchProvider {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);	
	
	private final Client client;
	
	@Inject
	public ElasticSearchProvider(Client searchClient){
		this.client = searchClient;
	}
	
	@Override
	public boolean indexObject(final String index, final String indexType, final String content) {		
		return indexObject(index, indexType, content, null);
	}
	
	@Override
	public boolean indexObject(String index, final String indexType, String content, String uniqueId) {
		// check index already exists if not execute any initialisation steps.
		if(!this.hasIndex(index)){
			this.sendMappingCorrections(index, indexType);
		}
		
		try{						
			IndexResponse indexResponse = client.prepareIndex(index, indexType, uniqueId).setSource(content).execute().actionGet(); 
			log.info("Document: " + indexResponse.getId() + " indexed.");
			
			return true;
    	}
    	catch(ElasticsearchException e){
    		log.error("Elastic Search Exception detected.");
    		e.printStackTrace();
    		return false;
    	}
	}

	@Override
	public List<String> paginatedMatchSearch(final String index, final String indexType, final Map<String,List<String>> fieldsToMatch, 
			final int startIndex, final int limit, final Map<String, Constants.SortOrder> sortInstructions){		

		// build up the query from the fieldsToMatch map
		BoolQueryBuilder query = QueryBuilders.boolQuery();

		for(Map.Entry<String, List<String>> pair : fieldsToMatch.entrySet()){
			if(pair.getValue() != null){
				// If it is a list of only one thing just put it in the query.
				if(pair.getValue().size() == 1){
					query.must(QueryBuilders.matchQuery(pair.getKey(), pair.getValue().get(0)));	
				}
				// If not it is an AND query and should be split into separate constraints.
				else if(pair.getValue().size() > 1)
				{
					for(String queryItem : pair.getValue()){
						query.must(QueryBuilders.matchQuery(pair.getKey(), queryItem));
					}
				}
				else{
					// this shouldn't happen unless we are given an empty list in which case we can skip it
				}				
			}
			else{
				log.warn("Null argument received in paginated match search... This is not usually expected. Ignoring it and continuing anyway.");
			}
		}
		
		log.debug("Query to be sent to elasticsearch is : " + query);
		
		SearchRequestBuilder searchRequest = client.prepareSearch(index)
		        .setTypes(indexType)
		        .setQuery(query)
		        .setSize(limit)
		        .setFrom(startIndex);
		        
		searchRequest = addSortInstructions(searchRequest, sortInstructions);

		return this.executeQuery(searchRequest);
	}
	
	@Override
	public List<String> fuzzySearch(final String index, final String indexType, final String searchString, final String... fields) {
		if(null == index || null == indexType || null == searchString || null == fields){
			log.warn("A required field is missing. Unable to execute search.");
			return null;
		}
		
		QueryBuilder query = QueryBuilders.fuzzyLikeThisQuery(fields).likeText(searchString);
		List<String> resultList = this.executeBasicQuery(index, indexType, query);
		return resultList; 
	}
	
	@Override
	public List<String> termSearch(final String index, final String indexType, final Collection<String> searchTerms, final String field){
		if(null == index || null == indexType || null == searchTerms || null == field){
			log.warn("A required field is missing. Unable to execute search.");
			return null;
		}
		
		QueryBuilder query = QueryBuilders.termsQuery(field, searchTerms).minimumMatch(searchTerms.size());
		List<String> resultList = this.executeBasicQuery(index, indexType, query);
		return resultList; 
	}

	@Override
	public boolean expungeEntireSearchCache() {
		return this.expungeIndexFromSearchCache("_all");
	}
	
	@Override
	public boolean expungeIndexFromSearchCache(final String index) {
		Validate.notBlank(index);
		
		try{
			log.info("Sending delete request to ElasticSearch for search index: " + index);
			client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
		}
		catch(ElasticsearchException e){
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	/**
	 * This method will create a threadsafe client that can be used to talk to an Elastic Search cluster.
	 *  
	 * @param clusterName
	 * @param address
	 * @param port
	 * @return Defaults to http client creation.
	 */
	public static Client getTransportClient(String clusterName, String address, int port){		
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
	
	private SearchRequestBuilder addSortInstructions(SearchRequestBuilder searchRequest, Map<String, Constants.SortOrder> sortInstructions){
		// deal with sorting of results
		for (Map.Entry<String, Constants.SortOrder> entry : sortInstructions.entrySet()) {
		    String sortField = entry.getKey();
		    Constants.SortOrder sortOrder = entry.getValue();
		    
		    if(sortOrder == Constants.SortOrder.ASC)
		    	searchRequest.addSort(SortBuilders.fieldSort(sortField).order(SortOrder.ASC).missing("_last"));
		    	
		    else
		    	searchRequest.addSort(SortBuilders.fieldSort(sortField).order(SortOrder.DESC).missing("_last"));
		}
		
		return searchRequest;
	}
	
	/**
	 * Provides default search execution using the fields specified.
	 * 
	 * This method does not provide any way of controlling sort order or limiting information returned. It is most useful for doing simple searches with fewer results e.g. by id.
	 * 
	 * @param index
	 * @param indexType
	 * @param query
	 * 
	 * @return list of the search results
	 */
	private List<String> executeBasicQuery(final String index, final String indexType, final QueryBuilder query){		
		log.debug("Building Query: " + query);
		
		SearchRequestBuilder configuredSearchRequestBuilder = client.prepareSearch(index)
		        .setTypes(indexType)
		        .setQuery(query);
		        
		return executeQuery(configuredSearchRequestBuilder);
	}
	
	/**
	 * A general method for getting the results of a search
	 * 
	 * @param configuredSearchRequestBuilder
	 * @return List of the search results.
	 */
	private List<String> executeQuery(SearchRequestBuilder configuredSearchRequestBuilder){
		SearchResponse response = configuredSearchRequestBuilder.execute().actionGet();
		
		List<SearchHit> hitAsList = Arrays.asList(response.getHits().getHits());
		List<String> resultList = new ArrayList<String>();
		
		log.info("TOTAL SEARCH HITS " + response.getHits().getTotalHits());
		
		for(SearchHit item : hitAsList){
			resultList.add(item.getSourceAsString());
		}
		
		return resultList;
	}
	
	private void sendMappingCorrections(final String index, final String indexType){
		try {
			log.info("Sending mapping correction for title.raw");
			
			CreateIndexRequestBuilder indexBuilder = client.admin().indices().prepareCreate(index);
			// TODO: This needs to turn into a generic function so that we can make other fields searchable. 
			final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder().startObject().startObject(indexType)
					.startObject("properties").startObject(Constants.TITLE_FIELDNAME).field("type","string")
					.field("index","analyzed").startObject("fields").startObject(Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX)
					.field("type","string").field("index","not_analyzed").endObject()
					.endObject().endObject().endObject().endObject().endObject();

			indexBuilder.addMapping(indexType, mappingBuilder);
			
	        // MAPPING DONE
			indexBuilder.execute().actionGet();
			
		} catch (IOException e) {
			log.error("Error while sending mapping correction instructions to the ElasticSearch Server", e);
		}
	}
}
