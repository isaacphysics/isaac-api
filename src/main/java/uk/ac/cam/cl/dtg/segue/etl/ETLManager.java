package uk.ac.cam.cl.dtg.segue.etl;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.util.PropertiesManager;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Ian on 01/11/2016.
 */
class ETLManager {
    private static final Logger log = LoggerFactory.getLogger(ETLFacade.class);
    private final ContentIndexer indexer;

    private final ArrayBlockingQueue<String> newVersionQueue;
    private final PropertiesManager liveVersionStore;

    @Inject
    ETLManager(final ContentIndexer indexer, final SchoolIndexer schoolIndexer, final GitDb database, final PropertiesManager liveVersionStore) {
        this.indexer = indexer;
        this.newVersionQueue = new ArrayBlockingQueue<>(1);
        this.liveVersionStore = liveVersionStore;

        // ON STARTUP

        // Load the current live version from file and set it.
        try {
            this.setLiveVersion(liveVersionStore.getProperty(Constants.CURRENT_LIVE_VERSION));
        } catch (Exception e) {
            log.error("Could not set live version on startup.", e);
        }


        // Make sure we have indexed the latest content.
        String latestSha = database.fetchLatestFromRemote();
        this.newVersionQueue.offer(latestSha);

        // Load the school list.
        try {
            schoolIndexer.indexSchoolsWithSearchProvider();
        } catch (UnableToIndexSchoolsException e) {
            log.error("Unable to index schools", e);
        }

        // Start the indexer that will deal with new version alerts in a thread-safe way.
        Thread t = new Thread(new NewVersionIndexer());
        t.setDaemon(true);
        t.start();

        log.info("ETL startup complete.");
    }

    void notifyNewVersion(String version) {
        log.info("Notified of new version: " + version);

        // This is the only place we write to newVersionQueue, so the offer should always succeed.
        this.newVersionQueue.clear();
        this.newVersionQueue.offer(version);
    }

    void setLiveVersion(String version) throws Exception {
        log.info("Requested new live version: " + version);

        indexer.loadAndIndexContent(version);
        log.info("Indexed version " + version + ". Setting live.");
        indexer.setLiveVersion(version);

        // Store the live version to file so that we can recover after wiping ElasticSearch.
        this.liveVersionStore.saveProperty(Constants.CURRENT_LIVE_VERSION, version);
    }

    private class NewVersionIndexer implements Runnable {

        @Override
        public void run() {
            log.info("Starting new version indexer thread.");

            try {
                while(true) {
                    // Block here until there is something to index.
                    log.info("Indexer going to sleep, waiting for new version alert.");
                    String newVersion = newVersionQueue.take();
                    log.info("Indexer got new version: " + newVersion + ". Attempting to index.");

                    try {
                        indexer.loadAndIndexContent(newVersion);
                        indexer.setLatestVersion(newVersion);
                    } catch (VersionLockedException e) {
                        log.warn("Could not index new version, someone is already indexing it. Ignoring.");
                    } catch (Exception e) {
                        log.warn("Indexing version " + newVersion + " failed for some reason. Moving on.");
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for new version. New versions will no longer be indexed.");
                e.printStackTrace();
            }
        }
    }
}
