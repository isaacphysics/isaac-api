package uk.ac.cam.cl.dtg.segue.database;

import java.util.HashMap;

import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.ContentManager;
import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.IRegistrationManager;
import uk.ac.cam.cl.dtg.segue.dao.LogManager;
import uk.ac.cam.cl.dtg.segue.dao.RegistrationManager;
import uk.ac.cam.cl.dtg.segue.dto.Choice;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.Question;

import com.google.inject.AbstractModule;
import com.mongodb.DB;

public class PersistenceConfigurationModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(DB.class).toInstance(Mongo.getDB());
		bind(IContentManager.class).to(ContentManager.class);
		bind(ILogManager.class).to(LogManager.class);
		bind(IRegistrationManager.class).to(RegistrationManager.class);
		bind(ContentMapper.class).toInstance(mapper);
	}
	
	private ContentMapper mapper = new ContentMapper(buildDefaultJsonTypeMap());
	
	private HashMap<String, Class<? extends Content>> buildDefaultJsonTypeMap() {
		HashMap<String, Class<? extends Content>> map = new HashMap<String, Class<? extends Content>>();
		
		map.put("choice", Choice.class);
		map.put("question", Question.class);
		return map;
	}
}
