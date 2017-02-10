package uk.ac.cam.cl.dtg.segue.etl;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.elasticsearch.client.Client;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.configuration.SegueConfigurationModule;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.PropertiesManager;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Created by Ian on 21/10/2016.
 */
class ETLConfigurationModule extends AbstractModule {
    private static final Logger log = LoggerFactory.getLogger(ETLConfigurationModule.class);
    private static PropertiesLoader globalProperties = null;
    private static ContentMapper mapper = null;
    private static Client elasticSearchClient = null;
    private static SchoolIndexer schoolIndexer = null;
    private static ETLManager etlManager = null;

    ETLConfigurationModule() {
        if (globalProperties == null) {
            try {
                globalProperties = new PropertiesLoader(System.getProperty("config.location"));
            } catch (IOException e) {
                log.error("Error loading properties file.", e);
            }
        }
    }
    /**
     * Utility method to make the syntax of property bindings clearer.
     *
     * @param propertyLabel
     *            - Key for a given property
     * @param propertyLoader
     *            - property loader to use
     */
    private void bindConstantToProperty(final String propertyLabel, final PropertiesLoader propertyLoader) {
        bindConstant().annotatedWith(Names.named(propertyLabel)).to(propertyLoader.getProperty(propertyLabel));
    }

    @Override
    protected void configure() {
        try {

            bind(PropertiesLoader.class).toInstance(globalProperties);

            this.bindConstantToProperty(Constants.SEARCH_CLUSTER_NAME, globalProperties);
            this.bindConstantToProperty(Constants.SEARCH_CLUSTER_ADDRESS, globalProperties);
            this.bindConstantToProperty(Constants.SEARCH_CLUSTER_PORT, globalProperties);
            this.bindConstantToProperty(Constants.SCHOOL_CSV_LIST_PATH, globalProperties);

            // GitDb
            bind(GitDb.class).toInstance(
                    new GitDb(globalProperties.getProperty(Constants.LOCAL_GIT_DB), globalProperties
                            .getProperty(Constants.REMOTE_GIT_SSH_URL), globalProperties
                            .getProperty(Constants.REMOTE_GIT_SSH_KEY_PATH)));

        } catch (IOException e) {
            e.printStackTrace();
            log.error("IOException during setup process.");
        }


    }


    /**
     * This provides a singleton of the contentVersionController for the segue facade.
     * Note: This is a singleton because this content mapper has to use reflection to register all content classes.
     *
     * @return Content version controller with associated dependencies.
     */
    @Inject
    @Provides
    @Singleton
    private static ContentMapper getContentMapper() {
        if (null == mapper) {
            Reflections r = new Reflections("uk.ac.cam.cl.dtg.segue");
            mapper = new ContentMapper(r);

            mapper.registerJsonTypes(new SegueConfigurationModule().getContentDataTransferObjectMap());

        }
        return mapper;
    }

    private static PropertiesManager getContentIndicesStore() throws IOException {
        return new PropertiesManager(globalProperties.getProperty(Constants.CONTENT_INDICES_LOCATION));
    }

    @Inject
    @Provides
    @Singleton
    private static ETLManager getETLManager(ContentIndexer contentIndexer, SchoolIndexer schoolIndexer, GitDb db) throws IOException {
        if (null == etlManager) {
            PropertiesManager contentIndicesStore = getContentIndicesStore();
            etlManager = new ETLManager(contentIndexer, schoolIndexer, db, contentIndicesStore);
        }
        return etlManager;
    }

    /**
     * This provides a singleton of the elasticSearch client that can be used by Guice.
     *
     * The client is threadsafe so we don't need to keep creating new ones.
     *
     * @param clusterName
     *            - The name of the cluster to create.
     * @param address
     *            - address of the cluster to create.
     * @param port
     *            - port of the custer to create.
     * @return Client to be injected into ElasticSearch Provider.
     */
    @Inject
    @Provides
    @Singleton
    private static Client getSearchConnectionInformation(
            @Named(Constants.SEARCH_CLUSTER_NAME) final String clusterName,
            @Named(Constants.SEARCH_CLUSTER_ADDRESS) final String address,
            @Named(Constants.SEARCH_CLUSTER_PORT) final int port) {

        if (null == elasticSearchClient) {
            try {
                elasticSearchClient = ElasticSearchIndexer.getTransportClient(clusterName, address, port);
                log.info("Creating singleton of ElasticSearchIndexer");
            } catch (UnknownHostException e) {
                log.error("Could not create ElasticSearchIndexer");
                return null;
            }
        }

        return elasticSearchClient;
    }

    @Inject
    @Provides
    @Singleton
    private SchoolIndexer getSchoolListIndexer(@Named(Constants.SCHOOL_CSV_LIST_PATH) final String schoolListPath,
                                                 final ElasticSearchIndexer es,
                                                 final ContentMapper mapper) throws IOException {
        if (null == schoolIndexer) {
            schoolIndexer = new SchoolIndexer(es, mapper, schoolListPath);
            log.info("Creating singleton of SchoolListReader");
        }

        return schoolIndexer;
    }
}
