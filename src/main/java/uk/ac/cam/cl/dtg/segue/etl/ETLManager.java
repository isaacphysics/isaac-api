package uk.ac.cam.cl.dtg.segue.etl;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.util.PropertiesManager;
import java.util.concurrent.*;
import java.util.*;

/**
 * Created by Ian on 01/11/2016.
 */
class ETLManager {
    private static final Logger log = LoggerFactory.getLogger(ETLFacade.class);
    private static final String LATEST_INDEX_ALIAS = "latest";

    private final ContentIndexer indexer;
    private final SchoolIndexer schoolIndexer;
    private final GitDb database;
    private final PropertiesManager contentIndicesStore;
    private final ScheduledExecutorService scheduler;


    @Inject
    ETLManager(final ContentIndexer indexer, final SchoolIndexer schoolIndexer, final GitDb database, final PropertiesManager contentIndicesStore) {
        this.indexer = indexer;
        this.schoolIndexer = schoolIndexer;
        this.database = database;
        this.contentIndicesStore = contentIndicesStore;
        this.scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(new ContentIndexerTask(), 0, 300, TimeUnit.SECONDS);

        log.info("ETL startup complete.");
    }

    void setNamedVersion(final String alias, final String version) throws Exception {
        log.info("Requested aliased version: " + alias + " - " + version);
        indexer.loadAndIndexContent(version);
        indexer.setNamedVersion(alias, version);
        log.info("Version " + version + " with alias '" + alias + "' is successfully indexed.");
    }

    // Indexes all content in idempotent fashion. If the content is already indexed no action is taken.
    void indexContent() {
        // Load the current version aliases from config file, as well as latest, and set them.
        Map<String, String> aliasVersions = new HashMap<>();
        String latestSha = database.fetchLatestFromRemote();
        aliasVersions.put(LATEST_INDEX_ALIAS, latestSha);
        for (String alias: contentIndicesStore.stringPropertyNames()) {
          aliasVersions.put(alias, contentIndicesStore.getProperty(alias));
        }

        for (var entry : aliasVersions.entrySet()) {
            try {
                this.setNamedVersion(entry.getKey(), entry.getValue());
            } catch (VersionLockedException e) {
                log.warn("Could not index new version, lock is already held by another thread.");
            } catch (Exception e) {
                log.error("Indexing version " + entry.getKey() + " failed.");
                e.printStackTrace();
            }
        }

        // Load the school list.
        try {
            schoolIndexer.indexSchoolsWithSearchProvider();
        } catch (UnableToIndexSchoolsException e) {
            log.error("Unable to index schools", e);
        }
    }

    private class ContentIndexerTask implements Runnable {

        @Override
        public void run() {
            log.info("Starting content indexer thread.");
            indexContent();
            log.info("Content indexer thread complete, waiting for next scheduled run.");
        }
    }
}
