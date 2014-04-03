package uk.ac.cam.cl.dtg.segue.database;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.common.transport.InetSocketTransportAddress;
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
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.AbstractModule;
import com.mongodb.DB;

/**
 * This class is responsible for injecting configuration values for persistence related classes
 *
 */
public class SeguePersistenceConfigurationModule extends AbstractModule {

	private static final Logger log = LoggerFactory.getLogger(SeguePersistenceConfigurationModule.class);

	private static PropertiesLoader globalProperties;

	// we only ever want there to be one instance of each of these.
	private static ContentMapper mapper;
	private static GoogleAuthenticator googleAuthenticator;
	private static ElasticSearchProvider elasticSearchProvider;

	// TODO: These are all singletons... Maybe we should change this if possible?
	public SeguePersistenceConfigurationModule(){
		try {
			globalProperties = new PropertiesLoader("/config/segue-config.properties");
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
		
		if(null == elasticSearchProvider){
			elasticSearchProvider = new ElasticSearchProvider("elasticsearch", new InetSocketTransportAddress("localhost", 9300));
		}
	}

	@Override
	protected void configure() {
		try {
			// Properties loader
			bind(PropertiesLoader.class).toInstance(globalProperties);

			this.configureDataPersistence();
			
			this.configureSegueSearch();
			
			this.configureApplicationManagers();

		} catch (IOException e) {
			e.printStackTrace();
			log.error("IOException during setup process.");
		}

		this.configureSecurity();
	}
	
	private void configureDataPersistence() throws IOException{
		// Setup different persistence bindings
		// MongoDb
		bind(DB.class).toInstance(Mongo.getDB());

		// GitDb			
		bind(GitDb.class).toInstance(new GitDb(globalProperties.getProperty(Constants.LOCAL_GIT_DB), globalProperties.getProperty(Constants.REMOTE_GIT_SSH_URL), globalProperties.getProperty(Constants.REMOTE_GIT_SSH_KEY_PATH)));
	}
	
	private void configureSegueSearch(){
		bind(ElasticSearchProvider.class).toInstance(elasticSearchProvider);
		bind(ISearchProvider.class).to(ElasticSearchProvider.class);
	}
	
	/**
	 * Deals with application data managers
	 */
	private void configureApplicationManagers(){
		//bind(IContentManager.class).to(MongoContentManager.class); //Allows Mongo take over Content Management
		bind(IContentManager.class).to(GitContentManager.class); //Allows GitDb take over Content Management
		
		bind(ILogManager.class).to(LogManager.class);
		
		bind(IUserDataManager.class).to(UserDataManager.class);

		// bind to single instances mainly because caches are used
		bind(ContentMapper.class).toInstance(mapper);
	}
	
	private void configureSecurity(){
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
