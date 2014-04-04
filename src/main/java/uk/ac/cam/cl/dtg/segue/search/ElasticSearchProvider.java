package uk.ac.cam.cl.dtg.segue.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public List<String> fuzzySearch(final String index, final String indexType, final String searchString, final String... fields) {
		if(null == index || null == indexType || null == searchString || null == fields){
			log.warn("A required field is missing. Unable to execute search.");
			return null;
		}
		
		QueryBuilder query = QueryBuilders.fuzzyLikeThisQuery(fields).likeText(searchString);
		List<String> resultList = this.executeQuery(index, indexType, query);
		return resultList; 
	}
	
	@Override
	public List<String> termSearch(final String index, final String indexType, final Collection<String> searchTerms, final String field){
		if(null == index || null == indexType || null == searchTerms || null == field){
			log.warn("A required field is missing. Unable to execute search.");
			return null;
		}
		
		QueryBuilder query = QueryBuilders.termsQuery(field, searchTerms).minimumMatch(searchTerms.size());
		List<String> resultList = this.executeQuery(index, indexType, query);
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
	
	private List<String> executeQuery(final String index, final String indexType, final QueryBuilder query){		
		log.debug("Executing Query: " + query);
		
		SearchResponse response = client.prepareSearch(index)
		        .setTypes(indexType)
		        .setQuery(query)
		        .execute()
		        .actionGet();
		
		List<SearchHit> hitAsList = Arrays.asList(response.getHits().getHits());
		List<String> resultList = new ArrayList<String>();
		
		log.info("TOTAL SEARCH HITS " + response.getHits().getTotalHits());
		
		for(SearchHit item : hitAsList){
			resultList.add(item.getSourceAsString());
		}
		
		return resultList; 
	}
}
