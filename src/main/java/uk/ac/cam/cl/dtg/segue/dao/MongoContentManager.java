package uk.ac.cam.cl.dtg.segue.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import uk.ac.cam.cl.dtg.segue.dto.Content;

/**
 * Implementation that specifically works with MongoDB Content objects
 *
 */
public class MongoContentManager implements IContentManager {

	private final DB database;
	private final ContentMapper mapper;
	
	@Inject
	public MongoContentManager(DB database, ContentMapper mapper) {
		this.database = database;
		this.mapper = mapper;
	}

	@Override
	public <T extends Content> String save(T objectToSave) throws IllegalArgumentException {
		JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(database.getCollection("content"), getContentSubclass(objectToSave), String.class);
		WriteResult<T, String> r = jc.save(objectToSave);
		return r.getSavedId().toString();
	}
	
	@Override
	public Content getById(String id, String version) throws IllegalArgumentException{
		if(null == id){
			return null;
		}
		
		// version parameter is unused in this particular implementation
		DBCollection dbCollection = database.getCollection("content");
		
		// Do database query using plain mongodb so we only have to read from the database once.
		DBObject node = dbCollection.findOne(new BasicDBObject("id", id));
		
		Content c =  mapper.mapDBOjectToContentDTO(node);
		
		return c;
	}
	
	@Override
	public List<Content> findByFieldNames(String version, final Map<String,List<String>> fieldsToMatch, Integer startIndex, Integer limit){
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}
	
	@Override
	public List<String> listAvailableVersions()
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException("MongoDB Content Manager does not support this operation.");
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Content> Class<T> getContentSubclass(T obj) throws IllegalArgumentException {
		if(obj instanceof Content)
			return (Class<T>) obj.getClass();
		
		throw new IllegalArgumentException("object is not a subtype of Content");
	}

	@Override
	public ByteArrayOutputStream getFileBytes(String version, String filename)
			throws IOException {
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}

	@Override
	public String getLatestVersionId() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}

	@Override
	public void clearCache() {
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}

	@Override
	public List<Content> getContentByTags(String version, Set<String> tags) {
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}

	@Override
	public List<Content> searchForContent(String version, String searchString) {
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}

	@Override
	public Set<String> getTagsList(String version) {
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}

	@Override
	public Set<String> getCachedVersionList() {
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}
}
