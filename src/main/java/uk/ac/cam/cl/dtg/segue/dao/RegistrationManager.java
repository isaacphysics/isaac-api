package uk.ac.cam.cl.dtg.segue.dao;

import org.bson.types.ObjectId;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import com.google.inject.Inject;
import com.mongodb.DB;

import uk.ac.cam.cl.dtg.segue.dto.User;

public class RegistrationManager implements IRegistrationManager {

	private final DB database;
	
	@Inject
	public RegistrationManager(DB database) {
		this.database = database;
	}
	
	public boolean register(User user) {
		JacksonDBCollection<User, ObjectId> jc = JacksonDBCollection.wrap(database.getCollection("registrations"), User.class, ObjectId.class);
		WriteResult<User, ObjectId> r = jc.save(user);
		
		// TODO: Return something useful, like whether we succeeded.
		return true;
	}
}
