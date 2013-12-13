package uk.ac.cam.cl.dtg.segue.dto;

import org.bson.types.ObjectId;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;

/**
 * Super Class for every content item in the Content Management System
 *
 */
public abstract class ContentBase {
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String save(DB db) {
		JacksonDBCollection jc = JacksonDBCollection.wrap(db.getCollection("content"), this.getClass(), ObjectId.class);
		WriteResult r = jc.save(this);
		return r.getSavedId().toString();
	}
	
	public static String getById(DB db, String id){
		BasicDBObject query = new BasicDBObject("id", id);

		String content = db.getCollection("content").findOne(query).toString();
		
		return content;
	}
}
