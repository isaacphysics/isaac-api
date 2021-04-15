package uk.ac.cam.cl.dtg.isaac.api;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.isaac.configuration.SegueConfigurationModule;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

@PowerMockIgnore("javax.net.ssl.*")
public class EventsFacadeTest extends AbstractFacadeTest {

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
        PropertiesLoader properties = null;
        try {
            properties = new PropertiesLoader("../isaac-other-resources/segue-test-config.properties");
        } catch (IOException e) {
            throw new RuntimeException("Error loading properties file." + e);
        }
        properties.setProperty("POSTGRES_DB_URL", "jdbc:postgresql://localhost:" + postgres.getMappedPort(5432) + "/rutherford");

        // Create Mocked Injector
        Module productionModule = Modules.combine(new IsaacGuiceConfigurationModule(), new SegueGuiceConfigurationModule(properties));
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
        Response r = eventsFacade.getEvents(this.request, "", 0, 1000, "DESC", false, false, false, false);
        int status = r.getStatus();
        assertEquals(status, 200);
    }
}
