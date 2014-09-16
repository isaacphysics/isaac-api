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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.PropertiesManager;

/**
 * ContentVersionController This Class is responsible for talking to the content
 * manager and tracking what version of content should be issued to users.
 * 
 */
public class ContentVersionController {
	private static final Logger log = LoggerFactory
			.getLogger(ContentVersionController.class);

	private static volatile String liveVersion;
	
	private final PropertiesManager versionPropertiesManager; 

	private final PropertiesLoader properties;
	private final IContentManager contentManager;

	/**
	 * Creates a content version controller. Usually you only need one.
	 * 
	 * @param generalProperties
	 *            - properties loader for segue.
	 * @param versionPropertiesManager - allows the CVM to read and write the current initial version.           
	 * @param contentManager
	 *            - content manager that knows how to retrieve content.
	 */
	@Inject
	public ContentVersionController(final PropertiesLoader generalProperties, 
			final PropertiesManager versionPropertiesManager,  
			final IContentManager contentManager) {
		this.properties = generalProperties;
		this.contentManager = contentManager;
		
		// Check on object creation if we need to clear caches.
		if (Boolean.parseBoolean(this.properties
				.getProperty(Constants.CLEAR_CACHES_ON_APP_START))) {
			contentManager.clearCache();
		}
		
		this.versionPropertiesManager = versionPropertiesManager;			
		
		// we want to make sure we have set a default liveVersion number
		if (null == liveVersion) {			
			liveVersion = versionPropertiesManager.getProperty(Constants.INITIAL_LIVE_VERSION);
			log.info("Setting live version of the site from properties file to "
					+ liveVersion);
		}
		
		boolean indexOnlyPublishedContent = Boolean.parseBoolean(this.properties
				.getProperty(Constants.SHOW_ONLY_PUBLISHED_CONTENT));

		if (indexOnlyPublishedContent) {
			log.info("Setting content index mode to: Published Content only");
			this.contentManager.setIndexRestriction(true);
		}
	}

	/**
	 * Gets the current live version of the Segue content as far as the
	 * controller is concerned.
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
	 * Trigger a sync job that will request a sync and subsequent index of the
	 * latest version of the content available
	 * 
	 * Currently this is not asynchronous as it is expected that notifiers will
	 * cope with blocking.
	 * 
	 * @return a string representation of the version that is available.
	 */
	public synchronized String triggerSyncJob() {
		return this.triggerSyncJob(null);
	}

	/**
	 * Trigger a sync job that will request a sync and subsequent index of a
	 * specific version of the content.
	 * 
	 * Currently this is not asynchronous as it is expected that notifiers will
	 * cope with blocking.
	 * 
	 * @param version
	 *            to sync
	 * @return a string representation of the version that is available.
	 */
	public synchronized String triggerSyncJob(final String version) {
		ContentSynchronisationWorker worker = new ContentSynchronisationWorker(
				this, version);

		// We can convert this to be a thread if we wish later; at the moment
		// blocking isn't a problem as github makes the sync requests as separate threads.
		worker.run();
		return version;
	}

	/**
	 * This method is intended to be used by Synchronisation jobs to inform the
	 * controller that they have completed their work.
	 * 
	 * @param version 
	 *            the version that has just been indexed.
	 * @param success - whether or not the job completed successfully.
	 */
	public synchronized void syncJobCompleteCallback(final String version,
			final boolean success) {
		// for use by ContentSynchronisationWorkers to alert the controller that
		// they have finished
		if (!success) {
			log.error("ContentSynchronisationWorker reported a failure to synchronise");
			return;
		}

		// verify that the version is indeed cached
		if (!contentManager.getCachedVersionList().contains(version)) {
			// if not just return without doing anything.
			log.error("Sync job informed version controller "
					+ "that a version was ready and it lied. The version is no longer cached. "
					+ "Terminating sync job.");
			return;
		}

		// Decide if we have to update the live version or not.
		if (Boolean.parseBoolean(properties
				.getProperty(Constants.FOLLOW_GIT_VERSION))) {

			// acquire the lock for an atomic update
			synchronized (liveVersion) {
				// set it to the live version only if it is newer than the
				// current live version.
				if (contentManager.compareTo(version, this.getLiveVersion()) > 0) {
					this.setLiveVersion(version);
				} else {
					log.info("Not changing live version as part of sync job as the "
							+ "version indexed is older than (or the same as) the new one.");
				}
			}
		} else {
			// we don't want to change the latest version until told to do so.
			log.info("New content version "
					+ version
					+ " indexed and available. Not changing liveVersion of the "
					+ "site as per configuration instruction.");
		}

		if (success) {
			this.cleanupCache(version);
		}
		
		log.info("Sync job completed - callback received and finished. ");
	}

	/**
	 * Change the version that the controller considers to be the live version.
	 * 
	 * This method is threadsafe.
	 * 
	 * @param newLiveVersion - the version to make live.
	 */
	public void setLiveVersion(final String newLiveVersion) {

		if (!contentManager.getCachedVersionList().contains(newLiveVersion)) {
			log.warn("New version hasn't been synced yet. Requesting sync job.");

			// trigger sync job
			this.triggerSyncJob(newLiveVersion);
		}

		synchronized (liveVersion) {
			log.info("Changing live version from " + this.getLiveVersion()
					+ " to " + newLiveVersion);

			// assume we always want to modify the initial version too.
			try {
				this.versionPropertiesManager.saveProperty(Constants.INITIAL_LIVE_VERSION, newLiveVersion);
			} catch (IOException e) {
				log.error("Unable to save new version to properties file.", e);
			}
			
			liveVersion = newLiveVersion;
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
	 * Check to see if the the version specified is in use by the controller for
	 * some reason.
	 * 
	 * @param version - find out if the version is in use.
	 * @return true if it is being used, false if not.
	 */
	public boolean isVersionInUse(final String version) {
		// This method will be used to indicate if a version is currently being
		// used in A/B testing in the future. For now it is just checking if it
		// is the live one.
		return getLiveVersion().equals(version);
	}

	/**
	 * This method should use the configuration settings to maintain the cache
	 * of the content manager object.
	 * @param versionJustIndexed - the version we just indexed.
	 */
	public synchronized void cleanupCache(final String versionJustIndexed) {
		int maxCacheSize = Integer.parseInt(properties
				.getProperty(Constants.MAX_VERSIONS_TO_CACHE));

		// first check if our cache is bigger than we want it to be
		if (contentManager.getCachedVersionList().size() > maxCacheSize) {
			log.info("Cache is too full ("
					+ contentManager.getCachedVersionList().size()
					+ ") finding and deleting old versions");
			// Now we want to decide which versions we can safely get rid of.
			List<String> allVersions = contentManager.listAvailableVersions();

			// got through all versions in reverse until you find the oldest one
			// that is also in the cached versions list and then remove it.
			for (int index = allVersions.size() - 1; contentManager
					.getCachedVersionList().size() > maxCacheSize
					&& index >= 0; index--) {

				// check if the version is cached
				if (contentManager.getCachedVersionList().contains(
						allVersions.get(index))) {
					// check we are not deleting the version that is currently
					// in use before we delete it.
					if (!isVersionInUse(allVersions.get(index))
							&& !versionJustIndexed.equals(allVersions
									.get(index))) {
						log.info("Requesting to delete the content at version "
								+ allVersions.get(index) + " from the cache.");
						contentManager.clearCache(allVersions.get(index));
					}
				}
			}

			// we couldn't free up enough space
			if (contentManager.getCachedVersionList().size() > maxCacheSize) {
				log.warn("Warning unable to reduce cache to target size: current cache size is "
						+ contentManager.getCachedVersionList().size());
			}
		} else {
			log.info("Not evicting cache as we have enough space: current cache size is "
					+ contentManager.getCachedVersionList().size() + ".");
		}
	}

	/**
	 * Instructs all content Managers to dump all cache information and any
	 * associated search indices.
	 */
	public synchronized void deleteAllCacheData() {
		log.info("Clearing all caches and search indices.");
		contentManager.clearCache();
	}
}
