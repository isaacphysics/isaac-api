package uk.ac.cam.cl.dtg.segue.database;

import uk.ac.cam.cl.dtg.segue.dao.ContentPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.IContentPersistenceManager;

import com.google.inject.AbstractModule;
import com.mongodb.DB;

public class PersistenceConfigurationModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(DB.class).toInstance(Mongo.getDB());
		bind(IContentPersistenceManager.class).to(ContentPersistenceManager.class);
	}
}
