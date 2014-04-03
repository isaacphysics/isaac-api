package uk.ac.cam.cl.dtg.segue.search;

import java.util.List;

public interface ISearchProvider {
	
	public boolean indexObject(final String index, final String indexType, final String content, final String uniqueId);
	
	public boolean indexObject(final String index, final String indexType, final String content);
	
	public List<String> search(final String index, final String indexType, final String searchString, final String... fields);
	
	public boolean expungeIndexFromSearchCache(final String index);
	
	public boolean expungeEntireSearchCache();
	
	public boolean deleteById(final String index, final String indexType, final String id);
}
