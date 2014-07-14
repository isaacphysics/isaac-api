package uk.ac.cam.cl.dtg.isaac.configuration;

import java.io.IOException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.app.GameManager;
import uk.ac.cam.cl.dtg.segue.api.ContentVersionController;
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
			final UserManager userManager) {
		if (null == segueApi) {
			segueApi = new SegueApiFacade(properties, mapper,
					segueConfigurationModule, versionController, userManager);
		}

		return segueApi;
	}

	/**
	 * Gets a Game manager.
	 * @param api - api that the game manager can use for content resolution.
	 * @return Game manager object.
	 */
	@Inject
	@Provides
	@Singleton
	private static GameManager getGameManager(final SegueApiFacade api) {
		if (null == gameManager) {
			gameManager = new GameManager(api);
		}

		return gameManager;
	}
}
