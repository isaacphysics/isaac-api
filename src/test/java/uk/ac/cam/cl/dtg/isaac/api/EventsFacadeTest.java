package uk.ac.cam.cl.dtg.isaac.api;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.isaac.configuration.SegueConfigurationModule;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class EventsFacadeTest {

    public EventsFacade eventsFacade;

    @Rule
    public GenericContainer postgres = new GenericContainer(DockerImageName.parse("postgres:12"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust") // Does not require password, OK for testing
            ;
    @Rule
    public GenericContainer elasticsearch = new GenericContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch-oss:7.8.0"))
            .withExposedPorts(9200, 9300)
            .withEnv("cluster.name", "isaac")
            .withEnv("network.host", "0.0.0.0")
            .withEnv("node.name", "localhost")
            .withEnv("cluster.initial_master_nodes", "localhost")
            ;

    @Before
    public void setUp() throws RuntimeException {
        PropertiesLoader mockedProperties = createMock(PropertiesLoader.class);
        expect(mockedProperties.getProperty(SEARCH_CLUSTER_NAME)).andReturn("isaac").anyTimes();
        expect(mockedProperties.getProperty(SEARCH_CLUSTER_ADDRESS)).andReturn("").anyTimes();
        expect(mockedProperties.getProperty(SEARCH_CLUSTER_PORT)).andReturn("").anyTimes();

        expect(mockedProperties.getProperty(HOST_NAME)).andReturn("").anyTimes();
        expect(mockedProperties.getProperty(MAILER_SMTP_SERVER)).andReturn("").anyTimes();
        expect(mockedProperties.getProperty(MAIL_FROM_ADDRESS)).andReturn("").anyTimes();
        expect(mockedProperties.getProperty(MAIL_NAME)).andReturn("").anyTimes();
        expect(mockedProperties.getProperty(SERVER_ADMIN_ADDRESS)).andReturn("").anyTimes();

        expect(mockedProperties.getProperty(LOGGING_ENABLED)).andReturn("").anyTimes();
        expect(mockedProperties.getProperty(IP_INFO_DB_API_KEY)).andReturn("").anyTimes();
        expect(mockedProperties.getProperty(SCHOOL_CSV_LIST_PATH)).andReturn("").anyTimes();
        expect(mockedProperties.getProperty(CONTENT_INDEX)).andReturn("").anyTimes();
        expect(mockedProperties.getProperty(API_METRICS_EXPORT_PORT)).andReturn("").anyTimes();

        replay(mockedProperties);

        // Create Mocked Injector
        SegueGuiceConfigurationModule.setGlobalPropertiesIfNotSet(mockedProperties);
        Module productionModule = Modules.combine(new IsaacGuiceConfigurationModule(), new SegueGuiceConfigurationModule());
        Module testModule = Modules.override(productionModule).with(new AbstractModule() {
            @Override protected void configure() {
                // ... register mocks
            }
        });
        Injector injector = Guice.createInjector(testModule);
        // Register DTOs to json mapper
        SegueConfigurationModule segueConfigurationModule = injector.getInstance(SegueConfigurationModule.class);
        ContentMapper mapper = injector.getInstance(ContentMapper.class);
        mapper.registerJsonTypes(segueConfigurationModule.getContentDataTransferObjectMap());
        // Get instance of class to test
        eventsFacade = injector.getInstance(EventsFacade.class);
    }

    @Test
    public void someTest() {
        assertEquals(1, 1);
    }
}
