package uk.ac.cam.cl.dtg.segue.dao;

import org.bson.types.ObjectId;
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
public class ContentManager implements IContentManager {

	private final DB database;
	private final ContentMapper mapper;
	
	@Inject
	public ContentManager(DB database) {
		this.database = database;
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		this.mapper = injector.getInstance(ContentMapper.class);
	}

	@Override
	public <T extends Content> String save(T objectToSave) throws IllegalArgumentException {
		JacksonDBCollection<T, ObjectId> jc = JacksonDBCollection.wrap(database.getCollection("content"), getContentSubclass(objectToSave), ObjectId.class);
		WriteResult<T, ObjectId> r = jc.save(objectToSave);
		return r.getSavedId().toString();
	}
	
	@Override
	public Content getById(String id) throws IllegalArgumentException{
		DBCollection dbCollection = database.getCollection("content");
		
		// Do database query using plain mongodb so we only have to read from the database once.
		DBObject node = dbCollection.findOne(new BasicDBObject("id", id));
		
		Content c =  mapper.mapDBOjectToContentDTO(node);
		
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
			Content childContent =  mapper.mapDBOjectToContentDTO(item);	
			content.getContentReferencedList().add(expandReferencedContent(childContent));
		}		
		return content;
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Content> Class<T> getContentSubclass(T obj) {
		if(obj instanceof Content)
			return (Class<T>) obj.getClass();
		
		throw new IllegalArgumentException("object is not a subtype of Content");
	}
}
