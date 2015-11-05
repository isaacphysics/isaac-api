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
package uk.ac.cam.cl.dtg.isaac.configuration;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.ASSIGNMENT_COLLECTION_NAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.GAMEBOARD_COLLECTION_NAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.USERS_GAMEBOARD_COLLECTION_NAME;

import javax.annotation.Nullable;

import ma.glasnost.orika.MapperFacade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.dao.AssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.AssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dos.UserGameboardsDO;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.configuration.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
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
    private static final Logger log = LoggerFactory.getLogger(IsaacGuiceConfigurationModule.class);

    private static SegueApiFacade segueApi = null;
    private static GameManager gameManager = null;

    private static GameboardPersistenceManager gameboardPersistenceManager = null;

    private static AssignmentPersistenceManager assignmentPersistenceManager = null;

    /**
     * Creates a new isaac guice configuration module.
     */
    public IsaacGuiceConfigurationModule() {
        
    }

    @Override
    protected void configure() {
        // Setup different persistence bindings
        // Currently all properties are being provided by the segue properties
        // file.

        bind(ISegueDTOConfigurationModule.class).toInstance(new SegueConfigurationModule());
    }

    /**
     * This provides a singleton of the segue api facade that can be used by isaac to serve api requests as a library or
     * register the endpoints with resteasy.
     * 
     * Note: A lot of the dependencies are injected from the segue project itself.
     * 
     * @param properties
     *            - the propertiesLoader instance
     * @param mapper
     *            - the content mapper
     * @param segueConfigurationModule
     *            - the Guice configuration module for segue.
     * @param versionController
     *            - the version controller that is in charge of managing content versions.
     * @param userManager
     *            - The user manager instance for segue.
     * @param questionManager
     *            - The Question Manager object for segue.
     * @param emailManager
     *            - The communication Manager object for segue.
     * @param logManager
     *            - The log Manager object for segue.
     * @return segueApi - The live instance of the segue api.
     */
    @Inject
    @Provides
    @Singleton
    private static SegueApiFacade getSegueFacadeSingleton(final PropertiesLoader properties,
            final ContentMapper mapper, @Nullable final ISegueDTOConfigurationModule segueConfigurationModule,
            final ContentVersionController versionController, final UserManager userManager,
            final QuestionManager questionManager, final EmailManager emailManager,
            final ILogManager logManager) {
        if (null == segueApi) {
            segueApi = new SegueApiFacade(properties, mapper, segueConfigurationModule, versionController, userManager,
                    questionManager, emailManager, logManager);
            log.info("Creating Singleton of Segue API");
        }

        return segueApi;
    }

    /**
     * Gets a Game manager.
     * 
     * @param userManager
     *            - so we can resolve game progress / user information.
     * @param versionManager
     *            - so we can augment game objects with actual detailed content
     * @param gameboardPersistenceManager
     *            - a persistence manager that deals with storing and retrieving gameboards.
     * @param mapper
     *            - allows mapping between DO and DTO object types.
     * @return Game manager object.
     */
    @Inject
    @Provides
    @Singleton
    private static GameManager getGameManager(final UserManager userManager,
            final ContentVersionController versionManager,
            final GameboardPersistenceManager gameboardPersistenceManager, final MapperFacade mapper) {
        if (null == gameManager) {
            gameManager = new GameManager(userManager, versionManager, gameboardPersistenceManager, mapper);
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
     * @param uriManager
     *            - so that the we can create content that is aware of its own location
     * @return Game persistence manager object.
     */
    @Inject
    @Provides
    @Singleton
    private static GameboardPersistenceManager getGameboardPersistenceManager(final SegueApiFacade api,
            final MapperFacade mapper, final URIManager uriManager) {
        if (null == gameboardPersistenceManager) {
            gameboardPersistenceManager = new GameboardPersistenceManager(api.requestAppDataManager(
                    GAMEBOARD_COLLECTION_NAME, GameboardDO.class), api.requestAppDataManager(
                    USERS_GAMEBOARD_COLLECTION_NAME, UserGameboardsDO.class), api, mapper, uriManager);
            log.info("Creating Singleton of GameboardPersistenceManager");
        }

        return gameboardPersistenceManager;
    }

    /**
     * Gets a Assignment Persistence manager.
     * 
     * @param api
     *            - api that we can use to create a AppDataManager instance
     * @param mapper
     *            - an instance of an auto mapper.
     * @return Game persistence manager object.
     */
    @Inject
    @Provides
    @Singleton
    private static AssignmentPersistenceManager getAssignmentPersistenceManager(final SegueApiFacade api,
            final MapperFacade mapper) {
        if (null == assignmentPersistenceManager) {
            assignmentPersistenceManager = new AssignmentPersistenceManager(api.requestAppDataManager(
                    ASSIGNMENT_COLLECTION_NAME, AssignmentDO.class), mapper);
            log.info("Creating Singleton of AssignmentPersistenceManager");
        }

        return assignmentPersistenceManager;
    }
}
