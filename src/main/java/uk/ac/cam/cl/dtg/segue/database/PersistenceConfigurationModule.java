package uk.ac.cam.cl.dtg.segue.database;

import java.util.HashMap;

import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.ContentPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.IContentPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dto.Choice;
import uk.ac.cam.cl.dtg.segue.dto.Content;

import com.google.inject.AbstractModule;
import com.mongodb.DB;

public class PersistenceConfigurationModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(DB.class).toInstance(Mongo.getDB());
		bind(IContentPersistenceManager.class).to(ContentPersistenceManager.class);
		bind(ContentMapper.class).toInstance(mapper);
	}
	
	private ContentMapper mapper = new ContentMapper(buildDefaultJsonTypeMap());
	
	private HashMap<String, Class<? extends Content>> buildDefaultJsonTypeMap() {
		HashMap<String, Class<? extends Content>> map = new HashMap<String, Class<? extends Content>>();
		
		map.put("choice", Choice.class);
		return map;
	}
}
