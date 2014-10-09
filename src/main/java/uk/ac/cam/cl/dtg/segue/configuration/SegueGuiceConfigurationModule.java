/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContextListener;

import ma.glasnost.orika.MapperFacade;

import org.apache.commons.lang3.Validate;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.FacebookAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.TwitterAuthenticator;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicator;
import uk.ac.cam.cl.dtg.segue.comm.ICommunicator;
import uk.ac.cam.cl.dtg.segue.dao.FileAppDataManager;
import uk.ac.cam.cl.dtg.segue.dao.IAppDatabaseManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.MongoLogManager;
import uk.ac.cam.cl.dtg.segue.dao.MongoAppDataManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.content.MathsContentManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.MongoUserDataManager;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.MongoDb;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Figure;
import uk.ac.cam.cl.dtg.segue.dos.content.Image;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.segue.dos.content.Video;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.PropertiesManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.mongodb.DB;
import com.mongodb.MongoClient;

/**
 * This class is responsible for injecting configuration values for persistence
 * related classes.
 * 
 */
public class SegueGuiceConfigurationModule extends AbstractModule {
	private static final Logger log = LoggerFactory
			.getLogger(SegueGuiceConfigurationModule.class);

	// we only ever want there to be one instance of each of these.
	private static MongoDb mongoDB = null;
	
	private static ContentMapper mapper = null;
	private static ContentVersionController contentVersionController = null;
	
	private static GitContentManager contentManager = null;
	private static Client elasticSearchClient = null;
	
	private static UserManager userManager = null;
	private static IUserDataManager userDataManager = null;
	private static GoogleClientSecrets googleClientSecrets = null;

	private static ICommunicator communicator = null;

	private PropertiesLoader globalProperties = null;

	private static ILogManager logManager;

	/**
	 * Create a SegueGuiceConfigurationModule.
	 */
	public SegueGuiceConfigurationModule() {
		try {
			globalProperties = new PropertiesLoader(
					"/config/segue-config.properties");

		} catch (IOException e) {
			log.error("Error loading properties file.", e);
		}
	}

	@Override
	protected void configure() {
		try {
			this.configureProperties();
			this.configureDataPersistence();
			this.configureSegueSearch();
			this.configureAuthenticationProviders();
			this.configureApplicationManagers();

		} catch (IOException e) {
			e.printStackTrace();
			log.error("IOException during setup process.");
		}
	}

	/**
	 * Extract properties and bind them to constants.
	 */
	private void configureProperties() {
		// Properties loader
		bind(PropertiesLoader.class).toInstance(globalProperties);

		this.bindConstantToProperty(Constants.SEARCH_CLUSTER_NAME,
				globalProperties);
		this.bindConstantToProperty(Constants.SEARCH_CLUSTER_ADDRESS,
				globalProperties);
		this.bindConstantToProperty(Constants.SEARCH_CLUSTER_PORT,
				globalProperties);

		this.bindConstantToProperty(Constants.HOST_NAME, globalProperties);
		this.bindConstantToProperty(Constants.MAILER_SMTP_SERVER, globalProperties);
		this.bindConstantToProperty(Constants.MAIL_FROM_ADDRESS, globalProperties);
		
		this.bindConstantToProperty(Constants.LOGGING_ENABLED, globalProperties);
	}

	/**
	 * Configure all things persistency.
	 * 
	 * @throws IOException
	 *             - when we cannot load the database.
	 */
	private void configureDataPersistence() throws IOException {
		// Setup different persistence bindings
		// MongoDb
		this.bindConstantToProperty(Constants.MONGO_DB_HOSTNAME,
				globalProperties);
		this.bindConstantToProperty(Constants.MONGO_DB_PORT, globalProperties);
		this.bindConstantToProperty(Constants.SEGUE_DB_NAME, globalProperties);
		
		this.bindConstantToProperty(Constants.MATHS_CACHE_LOCATION, globalProperties);

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

	/**
	 * Configure segue search classes.
	 */
	private void configureSegueSearch() {
		bind(ISearchProvider.class).to(ElasticSearchProvider.class);
	}

	/**
	 * Configure user security related classes.
	 */
	private void configureAuthenticationProviders() {
		this.bindConstantToProperty(Constants.HMAC_SALT, globalProperties);

		// Configure security providers
		// Google
		this.bindConstantToProperty(Constants.GOOGLE_CLIENT_SECRET_LOCATION,
				globalProperties);
		this.bindConstantToProperty(Constants.GOOGLE_CALLBACK_URI,
				globalProperties);
		this.bindConstantToProperty(Constants.GOOGLE_OAUTH_SCOPES,
				globalProperties);

		// Facebook
		this.bindConstantToProperty(Constants.FACEBOOK_SECRET, globalProperties);
		this.bindConstantToProperty(Constants.FACEBOOK_CLIENT_ID,
				globalProperties);
		this.bindConstantToProperty(Constants.FACEBOOK_CALLBACK_URI,
				globalProperties);
		this.bindConstantToProperty(Constants.FACEBOOK_OAUTH_SCOPES,
				globalProperties);

		// Twitter
		this.bindConstantToProperty(Constants.TWITTER_SECRET, globalProperties);
		this.bindConstantToProperty(Constants.TWITTER_CLIENT_ID,
				globalProperties);
		this.bindConstantToProperty(Constants.TWITTER_CALLBACK_URI,
				globalProperties);

		// Register a map of security providers
		MapBinder<AuthenticationProvider, IAuthenticator> mapBinder = MapBinder
				.newMapBinder(binder(), AuthenticationProvider.class,
						IAuthenticator.class);
		mapBinder.addBinding(AuthenticationProvider.GOOGLE).to(
				GoogleAuthenticator.class);
		mapBinder.addBinding(AuthenticationProvider.FACEBOOK).to(
				FacebookAuthenticator.class);
		mapBinder.addBinding(AuthenticationProvider.TWITTER).to(
				TwitterAuthenticator.class);
		mapBinder.addBinding(AuthenticationProvider.SEGUE).to(
				SegueLocalAuthenticator.class);

	}

	/**
	 * Deals with application data managers.
	 */
	private void configureApplicationManagers() {
		// Allows Mongo to take over Content Management
		// bind(IContentManager.class).to(MongoContentManager.class);

		// Allows GitDb to take over content Management
		bind(IContentManager.class).to(GitContentManager.class);

		//bind(ILogManager.class).to(MongoLogManager.class);
	}

	/**
	 * This provides a singleton of the elasticSearch client that can be used by
	 * Guice.
	 * 
	 * The client is threadsafe so we don't need to keep creating new ones.
	 * 
	 * @param clusterName
	 *            - The name of the cluster to create.
	 * @param address
	 *            - address of the cluster to create.
	 * @param port
	 *            - port of the custer to create.
	 * @return Client to be injected into ElasticSearch Provider.
	 */
	@Inject
	@Provides
	@Singleton
	private static Client getSearchConnectionInformation(
			@Named(Constants.SEARCH_CLUSTER_NAME) final String clusterName,
			@Named(Constants.SEARCH_CLUSTER_ADDRESS) final String address,
			@Named(Constants.SEARCH_CLUSTER_PORT) final int port) {
		if (null == elasticSearchClient) {
			elasticSearchClient = ElasticSearchProvider.getTransportClient(
					clusterName, address, port);
			log.info("Creating singleton of ElasticSearchProvider");
		}

		return elasticSearchClient;
	}

	/**
	 * This provides a singleton of the contentVersionController for the segue
	 * facade.
	 * 
	 * @param generalProperties
	 *            - properties loader
	 * @param contentManager
	 *            - content manager (with associated persistence links).
	 * @return Content version controller with associated dependencies.
	 * @throws IOException - if we can't load the properties file for live version.
	 */
	@Inject
	@Provides
	@Singleton
	private static ContentVersionController getContentVersionController(
			final PropertiesLoader generalProperties,
			final IContentManager contentManager) throws IOException {
		
		PropertiesManager versionPropertiesLoader = new PropertiesManager(
				generalProperties.getProperty(Constants.LIVE_VERSION_CONFIG_LOCATION));
		
		if (null == contentVersionController) {
			contentVersionController = new ContentVersionController(generalProperties, versionPropertiesLoader,
					contentManager);
			log.info("Creating singleton of ContentVersionController");
		}
		return contentVersionController;
	}

	/**
	 * This provides a singleton of the git content manager for the segue
	 * facade.
	 * 
	 * @param database
	 *            - database reference
	 * @param searchProvider
	 *            - search provider to use
	 * @param contentMapper
	 *            - content mapper to use.
	 * @return a fully configured content Manager.
	 */
	@Inject
	@Provides
	@Singleton
	private GitContentManager getContentManager(final GitDb database,
			final ISearchProvider searchProvider,
			final ContentMapper contentMapper) {
		if (null == contentManager) {
			contentManager = new GitContentManager(database, searchProvider,
					contentMapper);
			log.info("Creating singleton of ContentManager");
		}

		return contentManager;
	}
	
	/**
	 * This provides a singleton of the LogManager for the Segue
	 * facade.
	 * 
	 * @param database
	 *            - database reference
	 * @param objectMapper - A configured object mapper so that we can serialize objects logged.
	 * @param loggingEnabled - boolean to determine if we should persist log messages.
	 * @return A fully configured LogManager
	 */
	@Inject
	@Provides
	@Singleton
	private ILogManager getLogManager(final DB database, final ObjectMapper objectMapper,
			@Named(Constants.LOGGING_ENABLED) final boolean loggingEnabled) {
		if (null == logManager) {
			logManager = new MongoLogManager(database,
					objectMapper, loggingEnabled);
			log.info("Creating singleton of LogManager");
			if (loggingEnabled) {
				log.info("Log manager configured to record logging.");
			} else {
				log.info("Log manager configured NOT to record logging.");
			}
		}

		return logManager;
	}

	/**
	 * This provides a singleton of the contentVersionController for the segue
	 * facade.
	 * 
	 * @return Content version controller with associated dependencies.
	 */
	@Inject
	@Provides
	@Singleton
	private ContentMapper getContentMapper() {
		if (null == mapper) {
			mapper = new ContentMapper();
			this.buildDefaultJsonTypeMap();
			log.info("Initialising Content Mapper");
		}

		return mapper;
	}

	/**
	 * This provides a singleton of the contentVersionController for the segue
	 * facade.
	 * 
	 * @param database
	 *            - IUserManager
	 * @param properties
	 *            - properties loader
	 * @param providersToRegister
	 *            - list of known providers.
	 * @param communicator - so that we can send e-mails.
	 * @param logManager - so that we can log interesting user based events.
	 * @return Content version controller with associated dependencies.
	 */
	@Inject
	@Provides
	@Singleton
	private UserManager getUserManager(
			final IUserDataManager database,
			final PropertiesLoader properties,
			final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
			final ICommunicator communicator, final ILogManager logManager) {

		if (null == userManager) {
			userManager = new UserManager(database, properties, providersToRegister,
					this.getDOtoDTOMapper(), communicator, logManager);
			log.info("Creating singleton of UserManager");
		}

		return userManager;
	}

	/**
	 * This provides a singleton of the contentVersionController for the segue
	 * facade.
	 * 
	 * @param database
	 *            - to use for persistence.
	 * @param contentMapper
	 *            - the instance of a content mapper to use.
	 * @return Content version controller with associated dependencies.
	 */
	@Inject
	@Provides
	@Singleton
	private static IUserDataManager getUserDataManager(
			final DB database, final ContentMapper contentMapper) {
		if (null == userDataManager) {
			userDataManager = new MongoUserDataManager(database, contentMapper);
			log.info("Creating singleton of MongoUserDataManager");
		}

		return userDataManager;
	}


	/**
	 * Gets the instance of the dozer mapper object.
	 * 
	 * @return a preconfigured instance of an Auto Mapper. This is specialised
	 *         for mapping SegueObjects.
	 */
	@Provides
	@Singleton
	@Inject
	public MapperFacade getDOtoDTOMapper() {
		return this.getContentMapper().getAutoMapper();
	}

	/**
	 * Gets the instance of the GoogleClientSecrets object.
	 * 
	 * @param clientSecretLocation
	 *            - The path to the client secrets json file
	 * @return GoogleClientSecrets
	 * @throws IOException
	 *             - when we are unable to access the google client file.
	 */
	@Provides
	@Singleton
	@Inject
	private static GoogleClientSecrets getGoogleClientSecrets(
			@Named(Constants.GOOGLE_CLIENT_SECRET_LOCATION) final String clientSecretLocation)
		throws IOException {
		if (null == googleClientSecrets) {
			Validate.notNull(clientSecretLocation, "Missing resource %s",
					clientSecretLocation);

			// load up the client secrets from the file system.
			InputStream inputStream = new FileInputStream(clientSecretLocation);
			InputStreamReader isr = new InputStreamReader(inputStream);

			googleClientSecrets = GoogleClientSecrets.load(
					new JacksonFactory(), isr);
		}

		return googleClientSecrets;
	}

	/**
	 * Gets the instance of the mongodb client object.
	 * 
	 * @param host
	 *            - database host to connect to.
	 * @param port
	 *            - port that the mongodb service is running on.
	 * @param dbName
	 *            - the name of the database to configure the wrapper to use.
	 * @return MongoDB db object preconfigured to work with the segue database.
	 * @throws UnknownHostException
	 *             - when we are unable to access the host.
	 */
	@Provides
	@Singleton
	@Inject
	private static DB getMongoDB(
			@Named(Constants.MONGO_DB_HOSTNAME) final String host,
			@Named(Constants.MONGO_DB_PORT) final String port,
			@Named(Constants.SEGUE_DB_NAME) final String dbName)
		throws UnknownHostException {

		if (null == mongoDB) {
			MongoClient client = new MongoClient(host, Integer.parseInt(port));
			MongoDb newMongoDB = new MongoDb(client, dbName);
			mongoDB = newMongoDB;
			log.info("Created Singleton of MongoDb wrapper");
		}

		return mongoDB.getDB();
	}


	/**
	 * This provides a singleton of the ICommunicator for the segue
	 * facade.
	 *
	 * @param smtpServer
	 *            - SMTP Server Address
	 * @param mailFromAddress
	 *            - The default address emails should be sent from
	 * @return The singleton instance of EmailCommunicator
	 */
	@Inject
	@Provides
	@Singleton
	private ICommunicator getCommunicator(
			@Named(Constants.MAILER_SMTP_SERVER) final String smtpServer,
			@Named(Constants.MAIL_FROM_ADDRESS) final String mailFromAddress) {

		if (null == communicator) {
			communicator = new EmailCommunicator(smtpServer, mailFromAddress);
			log.info("Creating singleton of EmailCommunicator");
		}

		return communicator;
	}


	/**
	 * This provides a singleton of the MathsContent Manager for the segue.
	 *
	 * @param fileCacheLocation - the location that cached math images should be stored.
	 * @return the content manager that knows how to render / cache maths content.
	 * @throws IOException - when we can't read from the file cache location.
	 */
	@Inject
	@Provides
	private MathsContentManager getMathsContentManager(
			@Named(Constants.MATHS_CACHE_LOCATION) final String fileCacheLocation) throws IOException {
		MathsContentManager mcm;

		mcm = new MathsContentManager(new FileAppDataManager(fileCacheLocation));

		return mcm;
	}


	/**
	 * This method will pre-register the mapper class so that content objects
	 * can be mapped.
	 * 
	 * It requires that the class definition has the JsonType("XYZ") annotation
	 */
	private void buildDefaultJsonTypeMap() {
		// We need to pre-register different content objects here for the
		// auto-mapping to work
		mapper.registerJsonTypeAndDTOMapping(Content.class);
		mapper.registerJsonTypeAndDTOMapping(SeguePage.class);
		mapper.registerJsonTypeAndDTOMapping(Choice.class);
		mapper.registerJsonTypeAndDTOMapping(Quantity.class);
		mapper.registerJsonTypeAndDTOMapping(Question.class);
		mapper.registerJsonTypeAndDTOMapping(ChoiceQuestion.class);
		mapper.registerJsonTypeAndDTOMapping(Image.class);
		mapper.registerJsonTypeAndDTOMapping(Figure.class);
		mapper.registerJsonTypeAndDTOMapping(Video.class);
	}

	/**
	 * Utility method to make the syntax of property bindings clearer.
	 * 
	 * @param propertyLabel
	 *            - Key for a given property
	 * @param propertyLoader
	 *            - property loader to use
	 */
	private void bindConstantToProperty(final String propertyLabel,
			final PropertiesLoader propertyLoader) {
		bindConstant().annotatedWith(Names.named(propertyLabel)).to(
				propertyLoader.getProperty(propertyLabel));
	}

	/**
	 * Gets the segue classes that should be registered as context listeners.
	 * 
	 * TODO: we probably want to make this so that apps can register things too.
	 * @return the list of context listener classes (these should all be singletons).
	 */
	public static List<Class <? extends ServletContextListener>> getRegisteredContextListenerClasses() {
		List<Class <? extends ServletContextListener>> contextListeners = Lists.newArrayList();
		contextListeners.add(ContentVersionController.class);
		return contextListeners;
	}
	
	/**
	 * Segue utility method for providing a new instance of an application
	 * manager.
	 * 
	 * @param databaseName
	 *            - the database / table name - should be unique.
	 * @param classType
	 *            - the class type that represents what can be managed by this
	 *            app manager.
	 * @param <T>
	 *            the type that can be managed by this App Manager.
	 * @return the application manager ready to use.
	 */
	public static <T> IAppDatabaseManager<T> getAppDataManager(
			final String databaseName, final Class<T> classType) {
		Validate.notNull(mongoDB, "Error: mongoDB has not yet been initialised.");

		return new MongoAppDataManager<T>(mongoDB.getDB(), databaseName,
				classType);
	}
}
