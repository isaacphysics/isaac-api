package uk.ac.cam.cl.dtg.segue.etl;

/**
 * Created by Ian on 17/10/2016.
 */
public class Schools {


    // //SchoolListReader init
    //searchProvider.registerRawStringFields(Arrays.asList(SCHOOL_URN_FIELDNAME.toLowerCase()));


//    /**
//     * Trigger a thread to index the schools list. If needed.
//     */
//    public synchronized void prepareSchoolList() {
//
//        // We mustn't throw any exceptions here, as this is called from the constructor of SchoolLookupServiceFacade,
//        // called by Guice. And if anything dies while Guice is working, we never recover.
//
//        Thread thread = new Thread() {
//            public void run() {
//                log.info("Starting a new thread to index schools list.");
//                try {
//                    indexSchoolsWithSearchProvider();
//                } catch (UnableToIndexSchoolsException e) {
//                    log.error("Unable to index the schools list.");
//                }
//            }
//        };
//        thread.setDaemon(true);
//        thread.start();
//    }

//    /**
//     * Build the index for the search schools provider.
//     *
//     * @throws UnableToIndexSchoolsException
//     *             - when there is a problem building the index of schools.
//     */
//    private synchronized void indexSchoolsWithSearchProvider() throws UnableToIndexSchoolsException {
//        if (!searchProvider.hasIndex(SCHOOLS_SEARCH_INDEX)) {
//            log.info("Creating schools index with search provider.");
//            List<School> schoolList = this.loadAndBuildSchoolList();
//            List<Map.Entry<String, String>> indexList = Lists.newArrayList();
//
//            for (School school : schoolList) {
//                try {
//                    indexList.add(immutableEntry(school.getUrn().toString(), mapper.writeValueAsString(school)));
//                } catch (JsonProcessingException e) {
//                    log.error("Unable to serialize the school object into json.", e);
//                }
//            }
//
//            try {
//                searchProvider.bulkIndex(SCHOOLS_SEARCH_INDEX, SCHOOLS_SEARCH_TYPE, indexList);
//                log.info("School list index request complete.");
//            } catch (SegueSearchOperationException e) {
//                log.error("Unable to complete bulk index operation for schools list.", e);
//            }
//        } else {
//            log.info("Cancelling school search index operation as another thread has already done it.");
//        }
//    }
}
