package uk.ac.cam.cl.dtg.segue.api;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dao.LogManager;
import uk.ac.cam.cl.dtg.segue.dao.MongoUserDataManager;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.Mongo;
import uk.ac.cam.cl.dtg.segue.dto.content.Choice;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dto.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.content.Figure;
import uk.ac.cam.cl.dtg.segue.dto.content.Image;
import uk.ac.cam.cl.dtg.segue.dto.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dto.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.content.Video;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.mongodb.DB;

/**
 * This class is responsible for injecting configuration values for persistence
 * related classes
 * 
 */
public class SegueGuiceConfigurationModule extends AbstractModule {

	private static final Logger log = LoggerFactory
			.getLogger(SegueGuiceConfigurationModule.class);

	// TODO: These are effectively singletons...
	// we only ever want there to be one instance of each of these.
	private static ContentMapper mapper = null;
	private static Client elasticSearchClient = null;

	private PropertiesLoader globalProperties = null;

	public SegueGuiceConfigurationModule() {
		try {
			globalProperties = new PropertiesLoader(
					"/config/segue-config.properties");

		} catch (IOException e) {
			log.error("Error loading properties file.", e);
		}

		if (null == mapper) {
			mapper = new ContentMapper(buildDefaultJsonTypeMap());
		}
	}

	@Override
	protected void configure() {
		try {
			this.configureProperties();

			this.configureDataPersistence();

			this.configureSegueSearch();

			this.configureSecurity();

			this.configureApplicationManagers();

		} catch (IOException e) {
			e.printStackTrace();
			log.error("IOException during setup process.");
		}
	}

	private void configureProperties() {
		// Properties loader
		bind(PropertiesLoader.class).toInstance(globalProperties);

		this.bindConstantToProperty(Constants.SEARCH_CLUSTER_NAME,
				globalProperties);
		this.bindConstantToProperty(Constants.SEARCH_CLUSTER_ADDRESS,
				globalProperties);
		this.bindConstantToProperty(Constants.SEARCH_CLUSTER_PORT,
				globalProperties);
	}

	private void configureDataPersistence() throws IOException {
		// Setup different persistence bindings
		// MongoDb
		bind(DB.class).toInstance(Mongo.getDB());

		// GitDb
		bind(GitDb.class)
				.toInstance(
						new GitDb(
								globalProperties
										.getProperty(Constants.LOCAL_GIT_DB),
								globalProperties
										.getProperty(Constants.REMOTE_GIT_SSH_URL),
								globalProperties
										.getProperty(Constants.REMOTE_GIT_SSH_KEY_PATH)));
	}

	private void configureSegueSearch() {
		bind(ISearchProvider.class).to(ElasticSearchProvider.class);
	}

	private void configureSecurity() {
		this.bindConstantToProperty(Constants.HMAC_SALT, globalProperties);

		// Configure security providers
		this.bindConstantToProperty(Constants.GOOGLE_CLIENT_SECRET_LOCATION,
				globalProperties);
		this.bindConstantToProperty(Constants.GOOGLE_CALLBACK_URI,
				globalProperties);
		this.bindConstantToProperty(Constants.GOOGLE_OAUTH_SCOPES,
				globalProperties);

		// Register a map of security providers
		MapBinder<AuthenticationProvider, IFederatedAuthenticator> mapBinder = MapBinder
				.newMapBinder(binder(), AuthenticationProvider.class,
						IFederatedAuthenticator.class);
		mapBinder.addBinding(AuthenticationProvider.GOOGLE).to(
				GoogleAuthenticator.class);
	}

	/**
	 * Deals with application data managers
	 */
	private void configureApplicationManagers() {
		// bind(IContentManager.class).to(MongoContentManager.class); //Allows
		// Mongo take over Content Management
		bind(IContentManager.class).to(GitContentManager.class); // Allows GitDb
																	// take over
																	// Content
																	// Management

		bind(ILogManager.class).to(LogManager.class);

		bind(IUserDataManager.class).to(MongoUserDataManager.class);

		// bind to single instances mainly because caches are used
		bind(ContentMapper.class).toInstance(mapper);
	}

	/**
	 * This provides a singleton of the elasticSearch client that can be used by
	 * Guice.
	 * 
	 * The client is threadsafe so we don't need to keep creating new ones.
	 * 
	 * @param clusterName
	 * @param address
	 * @param port
	 * @return Client to be injected into ElasticSearch Provider.
	 */
	@Inject
	@Provides
	private static Client getSearchConnectionInformation(
			@Named(Constants.SEARCH_CLUSTER_NAME) String clusterName,
			@Named(Constants.SEARCH_CLUSTER_ADDRESS) String address,
			@Named(Constants.SEARCH_CLUSTER_PORT) int port) {
		if (null == elasticSearchClient) {
			elasticSearchClient = ElasticSearchProvider.getTransportClient(
					clusterName, address, port);
		}

		return elasticSearchClient;
	}

	/**
	 * This method will return you a populated Map which enables mapping to and
	 * from content objects.
	 * 
	 * It requires that the class definition has the JsonType("XYZ") annotation
	 * 
	 * @return initial segue type map.
	 */
	private Map<String, Class<? extends Content>> buildDefaultJsonTypeMap() {
		HashMap<String, Class<? extends Content>> map = new HashMap<String, Class<? extends Content>>();

		// We need to pre-register different content objects here for the
		// auto-mapping to work
		map.put("choice", Choice.class);
		map.put("quantity", Quantity.class);
		map.put("question", Question.class);
		map.put("choiceQuestion", ChoiceQuestion.class);
		map.put("image", Image.class);
		map.put("figure", Figure.class);
		map.put("video", Video.class);
		return map;
	}

	/**
	 * Utility method to make the syntax of property bindings clearer.
	 * 
	 * @param propertyLabel
	 * @param propertyLoader
	 */
	private void bindConstantToProperty(String propertyLabel,
			PropertiesLoader propertyLoader) {
		bindConstant().annotatedWith(Names.named(propertyLabel)).to(
				propertyLoader.getProperty(propertyLabel));
	}

}
