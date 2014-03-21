package uk.ac.cam.cl.dtg.segue.database;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticator;
import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dao.LogManager;
import uk.ac.cam.cl.dtg.segue.dao.UserDataManager;
import uk.ac.cam.cl.dtg.segue.dto.Choice;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.Question;
import uk.ac.cam.cl.dtg.segue.dto.ChoiceQuestion;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.AbstractModule;
import com.mongodb.DB;

/**
 * This class is responsible for injecting configuration values for persistence related classes
 *
 * TODO: should this be a singleton 
 */
public class SeguePersistenceConfigurationModule extends AbstractModule {

	private static final Logger log = LoggerFactory.getLogger(SeguePersistenceConfigurationModule.class);

	private static PropertiesLoader globalProperties;

	// we only ever want there to be one instance of each of these.
	private static ContentMapper mapper;
	private static GoogleAuthenticator googleAuthenticator;

	public SeguePersistenceConfigurationModule(){
		try {
			//globalProperties = new PropertiesLoader("/config/local-segue-config.properties");
			globalProperties = new PropertiesLoader("/config/dev-segue-config.properties");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(null == mapper){
			mapper = new ContentMapper(buildDefaultJsonTypeMap());
		}

		if(null == googleAuthenticator){
			googleAuthenticator = new GoogleAuthenticator(globalProperties.getProperty(Constants.GOOGLE_CLIENT_SECRET_LOCATION), globalProperties.getProperty(Constants.GOOGLE_CALLBACK_URI), globalProperties.getProperty(Constants.GOOGLE_OAUTH_SCOPES));			
		}
	}

	@Override
	protected void configure() {
		// Setup different persistence bindings

		try {

			bind(PropertiesLoader.class).toInstance(globalProperties);

			// MongoDB
			bind(DB.class).toInstance(Mongo.getDB());

			// GitDb			
			bind(GitDb.class).toInstance(new GitDb(globalProperties.getProperty(Constants.LOCAL_GIT_DB), globalProperties.getProperty(Constants.REMOTE_GIT_SSH_URL), globalProperties.getProperty(Constants.REMOTE_GIT_SSH_KEY_PATH)));

			//bind(IContentManager.class).to(MongoContentManager.class); //Allows Mongo take over Content Management
			bind(IContentManager.class).to(GitContentManager.class); //Allows GitDb take over Content Management

		} catch (IOException e) {
			e.printStackTrace();
			log.error("IOException during setup process.");
		}

		bind(ILogManager.class).to(LogManager.class);
		bind(IUserDataManager.class).to(UserDataManager.class);

		// bind to single instances mainly because caches are used
		bind(ContentMapper.class).toInstance(mapper);
		bind(GoogleAuthenticator.class).toInstance(googleAuthenticator);

	}

	/**
	 * This method will return you a populated Map which enables mapping to and from content objects.
	 * 
	 * It requires that the class definition has the JsonType("XYZ") annotation
	 * 
	 * @return 
	 */
	private Map<String, Class<? extends Content>> buildDefaultJsonTypeMap() {
		HashMap<String, Class<? extends Content>> map = new HashMap<String, Class<? extends Content>>();

		// We need to pre-register different content objects here for the automapping to work
		map.put("choice", Choice.class);
		map.put("question", Question.class);
		map.put("choiceQuestion", ChoiceQuestion.class);
		return map;
	}	

}
