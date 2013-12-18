package uk.ac.cam.cl.dtg.segue.dao;

import org.bson.types.ObjectId;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import uk.ac.cam.cl.dtg.segue.dto.Content;

/**
 * Implementation that specifically works with MongoDB objects
 *
 */
public class ContentPersistenceManager implements IContentPersistenceManager {

	private DB database;
	
	@Inject
	public ContentPersistenceManager(DB db) {
		// TODO Auto-generated constructor stub
		this.database = db;
	}

	@Override
	public String save(Content objectToSave) throws IllegalArgumentException {
		JacksonDBCollection jc = JacksonDBCollection.wrap(database.getCollection("content"), objectToSave.getClass(), ObjectId.class);
		WriteResult r = jc.save(objectToSave);
		return r.getSavedId().toString();
	}

	@Override
	public Content getById(String id) throws IllegalArgumentException{
		DBCollection dbCollection = database.getCollection("content");
		
		// Do database query using plain mongodb so we only have to read from the database once.
		DBObject node = dbCollection.findOne(new BasicDBObject("id", id));
		
		Content c = null;
		
		// Deserialize object into POJO of specified type, providing one exists. 
		c = ContentMapper.mapDBOjectToContentDTO(node);
		
		return c;
	}



}
