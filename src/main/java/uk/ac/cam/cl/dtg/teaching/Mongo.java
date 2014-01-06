package uk.ac.cam.cl.dtg.teaching;

import java.io.IOException;
import java.net.UnknownHostException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DB;
import com.mongodb.MongoClient;

public class Mongo {

	private static DB db;
	
	static {
		try {
			MongoClient client = new MongoClient("localhost", 27017);
			db = client.getDB("rutherford");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public static DB getDB() {
		return db;
	}
	

}
