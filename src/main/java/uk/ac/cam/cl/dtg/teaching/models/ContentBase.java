package uk.ac.cam.cl.dtg.teaching.models;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.bson.types.ObjectId;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mongodb.DB;

public abstract class ContentBase {
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String save(DB db) {
		JacksonDBCollection jc = JacksonDBCollection.wrap(db.getCollection("content"), this.getClass(), ObjectId.class);
		WriteResult r = jc.save(this);
		return r.getSavedId().toString();
	}
	
	

}
