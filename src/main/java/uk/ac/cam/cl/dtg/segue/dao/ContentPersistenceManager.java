package uk.ac.cam.cl.dtg.segue.dao;

import org.bson.types.ObjectId;
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
public class ContentPersistenceManager implements IContentPersistenceManager {

	private final DB database;
	
	@Inject
	public ContentPersistenceManager(DB database) {
		this.database = database;
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
		
		Content c =  ContentMapper.mapDBOjectToContentDTO(node);
		
		// TODO: Move somewhere else. Currently this is here just for testing. We may want to have the non-augmented objects too.
		this.expandReferencedContent(c);
		
		return c;
	}

	@Override
	public Content expandReferencedContent(Content content) {		
		// TODO: This should be improved. At the moment there is one query per content object that we see. It doesn't feel very elegant either
		
		if(null == content || null == content.getContentReferenced()){
			return content;
		}
		
		// build up query for database everytime we see an object so we don't have to do as many round trips to the database as might be necessary
		BasicDBObject query = new BasicDBObject();
		query.put("id", new BasicDBObject("$in", content.getContentReferenced()));
	
		DBCursor cursor = database.getCollection("content").find(query);
		while(cursor.hasNext()){
			DBObject item = cursor.next();
			Content childContent =  ContentMapper.mapDBOjectToContentDTO(item);	
			content.getContentReferencedList().add(expandReferencedContent(childContent));
		}		
		return content;
	}
}
