package uk.ac.cam.cl.dtg.isaac.configuration;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.GAMEBOARD_COLLECTION_NAME;

import java.io.IOException;

import javax.annotation.Nullable;

import ma.glasnost.orika.MapperFacade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.api.GameManager;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.segue.api.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.api.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.UserManager;
import uk.ac.cam.cl.dtg.segue.configuration.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * This class is responsible for injecting configuration values using GUICE.
 * 
 */
public class IsaacGuiceConfigurationModule extends AbstractModule {

	private static final Logger log = LoggerFactory
			.getLogger(IsaacGuiceConfigurationModule.class);

	private static PropertiesLoader globalProperties;

	private static SegueApiFacade segueApi = null;
	private static GameManager gameManager = null;

	private static GameboardPersistenceManager gameboardPersistenceManager = null;

	/**
	 * Creates a new isaac guice configuration module.
	 */
	public IsaacGuiceConfigurationModule() {
		try {
			if (null == globalProperties) {
				final String propertiesFileLocation = "/config/segue-config.properties";
				globalProperties = new PropertiesLoader(propertiesFileLocation);
				log.info("Loading properties file from "
						+ propertiesFileLocation);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void configure() {
		// Setup different persistence bindings
		// Currently all properties are being provided by the segue properties
		// file.
		// bind(PropertiesLoader.class).toInstance(globalProperties);

		bind(ISegueDTOConfigurationModule.class).toInstance(
				new SegueConfigurationModule());
	}

	/**
	 * This provides a singleton of the segue api facade that can be used by
	 * isaac to serve api requests as a library or register the endpoints with
	 * resteasy.
	 * 
	 * Note: A lot of the dependencies are injected from the segue project
	 * itself.
	 * 
	 * @param properties
	 *            - the propertiesLoader instance
	 * @param mapper
	 *            - the content mapper
	 * @param segueConfigurationModule
	 *            - the Guice configuration module for segue.
	 * @param versionController
	 *            - the version controller that is in charge of managing content
	 *            versions.
	 * @param userManager
	 *            - The user manager instance for segue.
	 * @return segueApi - The live instance of the segue api.
	 */
	@Inject
	@Provides
	@Singleton
	private static SegueApiFacade getSegueFacadeSingleton(
			final PropertiesLoader properties,
			final ContentMapper mapper,
			@Nullable final ISegueDTOConfigurationModule segueConfigurationModule,
			final ContentVersionController versionController,
			final UserManager userManager,
			final QuestionManager questionManager) {
		if (null == segueApi) {
			segueApi = new SegueApiFacade(properties, mapper,
					segueConfigurationModule, versionController, userManager, questionManager);
			log.info("Creating Singleton of Segue API");
		}

		return segueApi;
	}

	/**
	 * Gets a Game manager.
	 * 
	 * @param api
	 *            - api that the game manager can use for content resolution.
	 * @param gameboardPersistenceManager
	 *            - a persistence manager that deals with storing and retrieving
	 *            gameboards.
	 * @return Game manager object.
	 */
	@Inject
	@Provides
	@Singleton
	private static GameManager getGameManager(final SegueApiFacade api,
			final GameboardPersistenceManager gameboardPersistenceManager) {
		if (null == gameManager) {
			gameManager = new GameManager(api, gameboardPersistenceManager);
			log.info("Creating Singleton of Game Manager");
		}

		return gameManager;
	}

	/**
	 * Gets a Game persistence manager.
	 * 
	 * @param api
	 *            - api that the game manager can use for content resolution.
	 * @param mapper
	 *            - an instance of an auto mapper.
	 * @return Game persistence manager object.
	 */
	@Inject
	@Provides
	@Singleton
	private static GameboardPersistenceManager getGameboardPersistenceManager(
			final SegueApiFacade api, final MapperFacade mapper) {
		if (null == gameboardPersistenceManager) {
			gameboardPersistenceManager = new GameboardPersistenceManager(
					api.requestAppDataManager(GAMEBOARD_COLLECTION_NAME,
							GameboardDO.class), api, mapper);
			log.info("Creating Singleton of GameboardPersistenceManager");
		}

		return gameboardPersistenceManager;
	}

}
