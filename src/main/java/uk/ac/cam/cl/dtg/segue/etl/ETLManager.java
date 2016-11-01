package uk.ac.cam.cl.dtg.segue.etl;

import com.google.inject.Inject;
import org.eclipse.jetty.util.ConcurrentArrayQueue;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Ian on 01/11/2016.
 */
class ETLManager {
    private static final Logger log = LoggerFactory.getLogger(ETLFacade.class);
    private final ContentIndexer indexer;

    private final ArrayBlockingQueue<String> newVersionQueue;

    @Inject
    ETLManager(final ContentIndexer indexer) {
        this.indexer = indexer;

        this.newVersionQueue = new ArrayBlockingQueue<>(1);

        Thread t = new Thread(new NewVersionIndexer());
        t.setDaemon(true);
        t.start();
    }

    void notifyNewVersion(String version) {
        log.info("Notified of new version: " + version);

        synchronized (newVersionQueue) {
            // This is the only place we write to newVersionQueue, so the offer should always succeed.
            this.newVersionQueue.clear();
            this.newVersionQueue.offer(version);
        }
    }

    void setLiveVersion(String version) {
        log.info("Requested new live version: " + version);

        try {
            indexer.loadAndIndexContent(version);
        } catch (VersionLockedException e) {
            log.info("Failed to index " + version + ": Could not acquire lock");
        } catch (Exception e) {
            e.printStackTrace();
        }

        indexer.setLiveVersion(version);
    }

    private class NewVersionIndexer implements Runnable {

        @Override
        public void run() {
            log.info("Starting new version indexer thread.");

            try {
                while(true) {
                    // Block here until there is something to index.
                    String newVersion = newVersionQueue.take();
                    log.info("Got new version " + newVersion + ". Attempting to index.");

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
