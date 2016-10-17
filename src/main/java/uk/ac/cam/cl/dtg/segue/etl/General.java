package uk.ac.cam.cl.dtg.segue.etl;

/**
 * Created by Ian on 17/10/2016.
 */
public class General {


    // ContentVersionController

    //    /**
//     * Trigger a sync job that will request a sync and subsequent index of the latest version of the content available
//     *
//     * This method will cause a new job to be added to the indexer queue.
//     *
//     * This method is asynchronous and can be made to block by invoking the get method on the object returned.
//     *
//     * @return a future containing a string which is the version id.
//     */
//    public Future<String> triggerSyncJob() {
//        return this.triggerSyncJob(null);
//    }
//
//    /**
//     * Trigger a sync job that will request a sync and subsequent index of a specific version of the content.
//     *
//     * This method will cause a new job to be added to the indexer queue.
//     *
//     * This method is asynchronous and can be made to block by invoking the get method on the object returned.
//     *
//     * @param version
//     *            to sync
//     * @return a future containing a string which is the version id.
//     */
//    public Future<String> triggerSyncJob(final String version) {
//        ContentSynchronisationWorker worker = new ContentSynchronisationWorker(this, version);
//
//        log.info("Adding sync job for version " + version + " to the queue (" + this.indexQueue.size() + ")");
//        // add the job to the indexers internal queue
//        Future<String> future = indexer.submit(worker);
//
//        // add it to our queue so that we have the option of cancelling it if necessary
//        this.indexQueue.add(future);
//
//        return future;
//    }
//
//    /**
//     * This method is intended to be used by Synchronisation jobs to inform the controller that they have completed
//     * their work.
//     *
//     * @param version
//     *            the version that has just been indexed.
//     * @param success
//     *            - whether or not the job completed successfully.
//     */
//    public synchronized void syncJobCompleteCallback(final String version, final boolean success) {
//        // this job is about to complete so remove it from the queue.
//        this.indexQueue.remove();
//
//        // for use by ContentSynchronisationWorkers to alert the controller that
//        // they have finished
//        if (!success) {
//            log.error(String.format("ContentSynchronisationWorker reported a failure to synchronise %s. Giving up...",
//                    version));
//            return;
//        }
//
//        // verify that the version is indeed cached
//        if (!contentManager.getCachedVersionList().contains(version)) {
//            // if not just return without doing anything.
//            log.error("Sync job informed version controller "
//                    + "that a version was ready and it lied. The version is no longer cached. "
//                    + "Terminating sync job.");
//            return;
//        }
//
//        // Decide if we have to update the live version or not.
//        if (Boolean.parseBoolean(properties.getProperty(Constants.FOLLOW_GIT_VERSION))) {
//
//            // if we are in FOLLOW_GIT_VERSION mode then we do have to try to update.
//            // acquire the lock for an atomic update
//            synchronized (liveVersion) {
//                // set it to the live version only if it is newer than the
//                // current live version OR if the current live version no-longer
//                // exists (a rebase might have happened, for example).
//
//                boolean newer;
//                try {
//                    newer = contentManager.compareTo(version, this.getLiveVersion()) > 0;
//                } catch (NotFoundException e) {
//                    // The current live version was not found. A rebase probably happened underneath us.
//                    log.info("Failed to find current live version, someone probably rebased and force-pushed. Tut tut.");
//                    newer = true;
//                }
//
//                if (newer) {
//                    this.setLiveVersion(version);
//                } else {
//                    log.info("Not changing live version as part of sync job as the " + "version (" + version
//                            + ") just indexed is older than (or the same as) the current one (" + this.getLiveVersion()
//                            + ").");
//                }
//            }
//
//            cleanUpTheIndexQueue();
//
//        } else {
//            // we don't want to change the latest version until told to do so.
//            log.info("New content version " + version + " indexed and available. Not changing liveVersion of the "
//                    + "site until told to do so.");
//        }
//
//        if (success) {
//            this.cleanupCache(version);
//        }
//
//        log.debug("Sync job completed - callback received and finished.");
//    }
//
//    /**
//     * Change the version that the controller considers to be the live version.
//     *
//     * This method is threadsafe.
//     *
//     * @param newLiveVersion
//     *            - the version to make live.
//     */
//    public void setLiveVersion(final String newLiveVersion) {
//        if (!contentManager.getCachedVersionList().contains(newLiveVersion)) {
//            log.warn("New version hasn't been synced yet. Requesting sync job.");
//
//            // trigger sync job
//            try {
//                this.triggerSyncJob(newLiveVersion).get(); // we want this to block.
//            } catch (InterruptedException | ExecutionException e) {
//                log.error("Unable to complete sync job");
//            }
//        }
//
//        synchronized (liveVersion) {
//            log.info("Changing live version from " + this.getLiveVersion() + " to " + newLiveVersion);
//
//            // assume we always want to modify the initial version too.
//            try {
//                this.versionPropertiesManager.saveProperty(Constants.INITIAL_LIVE_VERSION, newLiveVersion);
//            } catch (IOException e) {
//                log.error("Unable to save new version to properties file.", e);
//            }
//
//            liveVersion = newLiveVersion;
//        }
//    }
//    /**
//     * This method should use the configuration settings to maintain the cache of the content manager object.
//     *
//     * @param versionJustIndexed
//     *            - the version we just indexed.
//     */
//    public synchronized void cleanupCache(final String versionJustIndexed) {
//        int maxCacheSize = Integer.parseInt(properties.getProperty(Constants.MAX_VERSIONS_TO_CACHE));
//
//        // clean up task queue
//        for (Future<?> future : this.indexQueue) {
//            if (future.isDone() || future.isCancelled()) {
//                this.indexQueue.remove(future);
//            }
//        }
//        log.info("Index job queue currently of size (" + this.indexQueue.size() + ")");
//
//        // first check if our cache is bigger than we want it to be
//        if (contentManager.getCachedVersionList().size() > maxCacheSize) {
//            log.info("Cache is too full (" + contentManager.getCachedVersionList().size()
//                    + ") finding and deleting old versions");
//
//
//            // Now we want to decide which versions we can safely get rid of.
//            List<String> allCachedVersions = Lists.newArrayList(contentManager.getCachedVersionList());
//            // sort them so they are in ascending order with the oldest version first.
//            Collections.sort(allCachedVersions, new Comparator<String>() {
//                @Override
//                public int compare(final String arg0, final String arg1) {
//                    return contentManager.compareTo(arg0, arg1);
//                }
//            });
//
//            for (String version : allCachedVersions) {
//                // we want to stop when we have deleted enough.
//                if (contentManager.getCachedVersionList().size() <= maxCacheSize) {
//                    log.info("Cache clear complete");
//                    break;
//                }
//
//                // check we are not deleting the version that is currently
//                // in use before we delete it.
//                if (!isVersionInUse(version) && !versionJustIndexed.equals(version)) {
//                    log.info("Requesting to delete the content at version " + version
//                            + " from the cache.");
//                    contentManager.clearCache(version);
//                }
//            }
//
//            // we couldn't free up enough space
//            if (contentManager.getCachedVersionList().size() > maxCacheSize) {
//                log.warn("Warning unable to reduce cache to target size: current cache size is "
//                        + contentManager.getCachedVersionList().size());
//            }
//        } else {
//            log.info("Not evicting cache as we have enough space: current cache size is "
//                    + contentManager.getCachedVersionList().size() + ".");
//        }
//    }
//
//    /**
//     * Instructs all content Managers to dump all cache information and any associated search indices.
//     */
//    public synchronized void deleteAllCacheData() {
//        log.info("Clearing all caches and search indices.");
//        contentManager.clearCache();
//    }
//
//    /**
//     * get a string representation of what is in the to IndexQueue.
//     *
//     * @return a list of tasks in the index queue.
//     */
//    public Collection<String> getToIndexQueue() {
//        ArrayList<String> newArrayList = Lists.newArrayList();
//        for (Future<String> f : this.indexQueue) {
//            newArrayList.add(f.toString());
//        }
//
//        return newArrayList;
//    }
//
//    /**
//     * Utility method that will empty the to index queue of any unstarted jobs.
//     */
//    public void cleanUpTheIndexQueue() {
//        // remove all but the latest one as the chances are the others are old requests
//        while (this.indexQueue.size() > 1) {
//            Future<String> f = this.indexQueue.remove();
//            f.cancel(false);
//            log.info("Cancelling pending (old) index operations as we are in follow git mode. Queue is currently: ("
//                    + this.indexQueue.size() + ")");
//        }
//    }




}
