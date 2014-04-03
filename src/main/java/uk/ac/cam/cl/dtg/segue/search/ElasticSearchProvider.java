package uk.ac.cam.cl.dtg.segue.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticSearchProvider implements ISearchProvider {

	private static final Logger log = LoggerFactory.getLogger(ElasticSearchProvider.class);
	private String clusterName;
	private InetSocketTransportAddress address;
	
	private Client client = null;

	public ElasticSearchProvider(String clusterName, InetSocketTransportAddress address){
		this.clusterName = clusterName;
		this.address = address;
		this.client = this.getTransportClient();
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
		QueryBuilder query = QueryBuilders.fuzzyLikeThisQuery(fields).likeText(searchString);
		SearchResponse response = client.prepareSearch(index)
		        .setTypes(indexType)
		        .setQuery(query)
		        .execute()
		        .actionGet();
		
		log.debug("Query: " + query);
		
		SearchHits results = response.getHits();
		
		List<SearchHit> hitAsList = Arrays.asList(results.getHits());
		log.info("Search for: "+ searchString +" TOTAL SEARCH HITS " + results.getTotalHits());
		
		List<String> resultList = new ArrayList<String>();
		
		for(SearchHit item : hitAsList){
			resultList.add(item.getSourceAsString());
		}
		return resultList; 
	}
	
	@Override
	public List<String> termSearch(final String index, final String indexType, final Collection<String> searchTerms, final String field){
		QueryBuilder query = QueryBuilders.termsQuery(field, searchTerms).minimumMatch(searchTerms.size());
		SearchResponse response = client.prepareSearch(index)
		        .setTypes(indexType)
		        .setQuery(query)
		        .execute()
		        .actionGet();
		
		log.debug("Query: " + query);
		
		SearchHits results = response.getHits();
		
		List<SearchHit> hitAsList = Arrays.asList(results.getHits());
		log.info("Search for: "+ searchTerms +" TOTAL SEARCH HITS " + results.getTotalHits());
		
		List<String> resultList = new ArrayList<String>();
		
		for(SearchHit item : hitAsList){
			resultList.add(item.getSourceAsString());
		}
		return resultList; 
	}

	@Override
	public boolean expungeEntireSearchCache() {
		return this.expungeIndexFromSearchCache("_all");
	}
	
	@Override
	public boolean expungeIndexFromSearchCache(final String index) {
		//if they don't provide an index assume we are deleting every index.
		if(null == index){
			return false;
		}
		
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
	
	private Client getTransportClient(){		
		Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build();
		TransportClient transportClient = new TransportClient(settings);
		transportClient = transportClient.addTransportAddress(address);
		return transportClient;
	}

	@Override
	public boolean hasIndex(final String index) {
		return client.admin().indices().exists(new IndicesExistsRequest(index)).actionGet().isExists();
	}
}
