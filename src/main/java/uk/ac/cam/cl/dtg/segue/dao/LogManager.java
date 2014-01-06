package uk.ac.cam.cl.dtg.segue.dao;

import java.util.Date;

import com.google.inject.Inject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class LogManager implements ILogManager {

	private final DB database;
	
	@Inject
	public LogManager(DB database) {
		this.database = database;
	}
	public boolean log(String sessionId, String cookieId, String eventJSON) {
		
		DBCollection log = database.getCollection("log");
		
		DBObject dbo = (DBObject) JSON.parse(eventJSON);
		dbo.put("sessionId", sessionId);
		dbo.put("cookieId", cookieId);
		dbo.put("timestamp", new Date());
		
		log.insert(dbo);

		// TODO: Return something useful here. True on success.
		return true;
	}
}
