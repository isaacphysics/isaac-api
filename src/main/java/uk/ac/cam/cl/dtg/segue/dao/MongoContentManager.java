package uk.ac.cam.cl.dtg.segue.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import uk.ac.cam.cl.dtg.segue.database.PersistenceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.Content;

/**
 * Implementation that specifically works with MongoDB Content objects
 *
 */
public class MongoContentManager implements IContentManager {

	private final DB database;
	private final ContentMapper mapper;
	
	@Inject
	public MongoContentManager(DB database) {
		this.database = database;
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		this.mapper = injector.getInstance(ContentMapper.class);
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
	public List<Content> findAllByType(String type, String version, Integer limit){
		if(null == limit)
			limit = 0;

		DBCollection dbCollection = database.getCollection("content");
		
		BasicDBObject query = new BasicDBObject("type", type);
		
		// Do database query using plain mongodb so we only have to read from the database once.
		DBCursor cursor = dbCollection.find(query).limit(limit);
		
		List<Content> listOfContent = new ArrayList<Content>();
		
		for(DBObject node : cursor){
			Content c =  mapper.mapDBOjectToContentDTO(node);
			listOfContent.add(c);
		}		
		
		return listOfContent;
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
	
	/**
	 * Wrapper method that converts a list of string representations of object ids into a list of objectids
	 * 
	 * This is needed for querying mongodb
	 * @deprecated not using mongo
	 * @param List of string object Ids
	 * @return List of object ids 
	 */
	private List<ObjectId> wrapObjectIds(List<String> stringIds){
		List<ObjectId> newList = new ArrayList<ObjectId>();
		for(String objectString : stringIds){
			newList.add(new ObjectId(objectString));
		}
		
		return newList;
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
}
