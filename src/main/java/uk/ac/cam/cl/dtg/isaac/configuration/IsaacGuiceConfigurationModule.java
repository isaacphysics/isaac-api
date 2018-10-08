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

import com.google.inject.name.Named;
import com.google.inject.name.Names;
import ma.glasnost.orika.MapperFacade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.IAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacSymbolicChemistryValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacSymbolicValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacGraphSketcherValidator;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.configuration.ISegueDTOConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;


/**
 * This class is responsible for injecting configuration values using GUICE.
 * 
 */
public class IsaacGuiceConfigurationModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(IsaacGuiceConfigurationModule.class);

    private static GameboardPersistenceManager gameboardPersistenceManager = null;
    private static AssignmentManager assignmentManager = null;

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
        
        bind(IAssignmentPersistenceManager.class).to(PgAssignmentPersistenceManager.class);

    }

    /**
     * Gets a Game persistence manager.
     * 
     * This needs to be a singleton as it maintains temporary boards in memory.
     * 
     * @param database
     *            - the database that persists gameboards.
     * @param contentManager
     *            - api that the game manager can use for content resolution.
     * @param mapper
     *            - an instance of an auto mapper for translating gameboard DOs and DTOs efficiently.
     * @param objectMapper
     *            - a mapper to allow content to be resolved.
     * @param uriManager
     *            - so that the we can create content that is aware of its own location
     * @return Game persistence manager object.
     */
    @Inject
    @Provides
    @Singleton
    private static GameboardPersistenceManager getGameboardPersistenceManager(final PostgresSqlDb database,
                  final IContentManager contentManager, final MapperFacade mapper, final ObjectMapper objectMapper,
                  final URIManager uriManager, @Named(CONTENT_INDEX) final String contentIndex) {
        if (null == gameboardPersistenceManager) {
            gameboardPersistenceManager = new GameboardPersistenceManager(database, contentManager, mapper,
                    objectMapper, uriManager, contentIndex);
            log.info("Creating Singleton of GameboardPersistenceManager");
        }

        return gameboardPersistenceManager;
    }

    /**
     * Gets an assignment manager.
     *
     * This needs to be a singleton because operations like emailing are run for each IGroupObserver, the
     * assignment manager should only be one observer.
     *
     * @param assignmentPersistenceManager
     *            - to save assignments
     * @param groupManager
     *            - to allow communication with the group manager.
     * @param emailManager
     *            - email manager
     * @param userManager
     *            - the user manager object
     * @param gameManager
     *            - the game manager object
     * @param userAssociationManager
     *            - the userAssociationManager manager object
     * @param properties
     *            - properties loader for the service's hostname
     * @return Assignment manager object.
     */
    @Inject
    @Provides
    @Singleton
    private static AssignmentManager getAssignmentManager(
                final IAssignmentPersistenceManager assignmentPersistenceManager, final GroupManager groupManager,
                final EmailManager emailManager, final UserAccountManager userManager, final GameManager gameManager,
                final UserAssociationManager userAssociationManager, final PropertiesLoader properties) {
        if (null == assignmentManager) {
            assignmentManager =  new AssignmentManager(assignmentPersistenceManager, groupManager, emailManager,
                    userManager, gameManager, userAssociationManager, properties);
            log.info("Creating Singleton AssignmentManager");
        }
        return assignmentManager;
    }

    /**
     * Gets an instance of the graph sketcher question validator.
     *
     * @return IsaacGraphSketcherValidator preconfigured to work with the specified checker.
     */
    @Provides
    @Singleton
    @Inject
    private static IsaacGraphSketcherValidator getGraphSketcherValidator(PropertiesLoader properties) {

        return new IsaacGraphSketcherValidator(properties.getProperty(Constants.GRAPH_CHECKER_HOST),
                properties.getProperty(Constants.GRAPH_CHECKER_PORT));
    }

    /**
     * Gets an instance of the symbolic question validator.
     *
     * @return IsaacSymbolicValidator preconfigured to work with the specified checker.
     */
    @Provides
    @Singleton
    @Inject
    private static IsaacSymbolicValidator getSymbolicValidator(PropertiesLoader properties) {

        return new IsaacSymbolicValidator(properties.getProperty(Constants.EQUALITY_CHECKER_HOST),
                properties.getProperty(Constants.EQUALITY_CHECKER_PORT));
    }

    /**
     * Gets an instance of the chemistry question validator.
     *
     * @return IsaacSymbolicChemistryValidator preconfigured to work with the specified checker.
     */
    @Provides
    @Singleton
    @Inject
    private static IsaacSymbolicChemistryValidator getSymbolicChemistryValidator(PropertiesLoader properties) {

        return new IsaacSymbolicChemistryValidator(properties.getProperty(Constants.CHEMISTRY_CHECKER_HOST),
                properties.getProperty(Constants.CHEMISTRY_CHECKER_PORT));
    }
}
