package uk.ac.cam.cl.dtg.segue.dao;


import org.mongojack.JacksonDBCollection;
import org.mongojack.ObjectId;
import org.mongojack.WriteResult;

import com.google.inject.Inject;
import com.mongodb.DB;
import uk.ac.cam.cl.dtg.segue.dto.User;

public class UserDataManager implements IUserDataManager {

	private final DB database;
	private static final String USER_COLLECTION_NAME = "users";
	
	@Inject
	public UserDataManager(DB database) {
		this.database = database;
	}
	
	public String register(User user) {
		JacksonDBCollection<User, ObjectId> jc = JacksonDBCollection.wrap(database.getCollection(USER_COLLECTION_NAME), User.class, ObjectId.class);
		WriteResult<User, ObjectId> r = jc.save(user);
		
		return r.getDbObject().get("_id").toString();
	}
	
	public User getById(String id) throws IllegalArgumentException{
		if(null == id){
			return null;
		}
		
		JacksonDBCollection<User, String> jc = JacksonDBCollection.wrap(database.getCollection(USER_COLLECTION_NAME), User.class, String.class);
		
		// Do database query using plain mongodb so we only have to read from the database once.
		User user = jc.findOneById(id);
		
		return user;
	}
}
