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
package uk.ac.cam.cl.dtg.segue.api.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.PropertiesManager;

import com.google.api.client.util.Lists;
import com.google.common.collect.Queues;
import com.google.inject.Inject;

/**
 * ContentVersionController This Class is responsible for talking to the content manager and tracking what version of
 * content should be issued to users.
 * 
 */
public class ContentVersionController implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(ContentVersionController.class);

    private static volatile String liveVersion;

    private final PropertiesManager versionPropertiesManager;

    private final PropertiesLoader properties;
    private final IContentManager contentManager;

    private final ExecutorService indexer;
    private final Queue<Future<String>> indexQueue;

    /**
     * Creates a content version controller. Usually you only need one.
     * 
     * @param generalProperties
     *            - properties loader for segue.
     * @param versionPropertiesManager
     *            - allows the CVM to read and write the current initial version.
     * @param contentManager
     *            - content manager that knows how to retrieve content.
     */
    @Inject
    public ContentVersionController(final PropertiesLoader generalProperties,
            final PropertiesManager versionPropertiesManager, final IContentManager contentManager) {
        this.properties = generalProperties;
        this.contentManager = contentManager;
        this.indexer = Executors.newSingleThreadExecutor();
        this.indexQueue = Queues.newConcurrentLinkedQueue();

        this.versionPropertiesManager = versionPropertiesManager;

        // we want to make sure we have set a default liveVersion number
        if (null == liveVersion) {
            liveVersion = versionPropertiesManager.getProperty(Constants.INITIAL_LIVE_VERSION);
            log.info("Setting live version of the site from properties file to " + liveVersion);
        }

    }

    /**
     * Gets the current live version of the Segue content as far as the controller is concerned.
     * 
     * This method is threadsafe.
     * 
     * @return a version id
     */
    public String getLiveVersion() {
        synchronized (liveVersion) {
            return liveVersion;
        }
    }


    /**
     * Utility method to allow classes to query the content Manager directly.
     * 
     * @return content manager.
     */
    public IContentManager getContentManager() {
        return contentManager;
    }

    /**
     * Check to see if the the version specified is in use by the controller for some reason.
     * 
     * @param version
     *            - find out if the version is in use.
     * @return true if it is being used, false if not.
     */
    public boolean isVersionInUse(final String version) {
        // This method will be used to indicate if a version is currently being
        // used in A/B testing in the future. For now it is just checking if it
        // is the live one.
        // TODO The current live version should be stored in the database in the future not a conf file.
        
        return getLiveVersion().equals(version);
    }



    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        // we can't do anything until something initialises us anyway.
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        log.info("Informed of imminent context destruction.");
    }
}
