package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import uk.ac.cam.cl.dtg.isaac.IsaacTest;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.services.GroupChangedService;
import uk.ac.cam.cl.dtg.segue.api.InfoFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_LINUX_CONFIG_LOCATION;

@PowerMockIgnore("javax.net.ssl.*")
public class InfoFacadeTest extends IsaacTest {

    public InfoFacade infoFacade;

    public Request requestForCaching;

    @ClassRule
    public static GenericContainer<?> postgres = new GenericContainer<>(DockerImageName.parse("postgres:12"))
            .withExposedPorts(5432)
            .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust") // Does not require password, OK for testing
            ;
    @ClassRule
    public static GenericContainer<?> elasticsearch = new GenericContainer<>(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch-oss:7.8.0"))
            .withExposedPorts(9200, 9300)
            .withEnv("cluster.name", "isaac")
            .withEnv("network.host", "0.0.0.0")
            .withEnv("node.name", "localhost")
            .withEnv("cluster.initial_master_nodes", "localhost")
            ;

    @Before
    public void setUp() throws RuntimeException, IOException {
        String configLocation = SystemUtils.IS_OS_LINUX ? DEFAULT_LINUX_CONFIG_LOCATION : null;
        if (System.getProperty("config.location") != null) {
            configLocation = System.getProperty("config.location");
        }
        if (System.getenv("SEGUE_CONFIG_LOCATION") != null){
            configLocation = System.getenv("SEGUE_CONFIG_LOCATION");
        }

        PropertiesLoader mockedProperties = new PropertiesLoader(configLocation) {
            final Map<String, String> propertyOverrides = ImmutableMap.of(
                    "SEARCH_CLUSTER_NAME", "isaac"
            );
            @Override
            public String getProperty(String key) {
                return propertyOverrides.getOrDefault(key, super.getProperty(key));
            }
        };

        UserAccountManager userAccountManager = createMock(UserAccountManager.class);
        GameManager gameManager = createMock(GameManager.class);
        GroupChangedService groupChangedService = createMock(GroupChangedService.class);

        requestForCaching = createMock(Request.class);
        expect(requestForCaching.evaluatePreconditions((EntityTag) anyObject())).andStubReturn(null);

        // Create Mocked Injector
        SegueGuiceConfigurationModule.setGlobalPropertiesIfNotSet(mockedProperties);
        Module productionModule = new SegueGuiceConfigurationModule();
        Module testModule = Modules.override(productionModule).with(new AbstractModule() {
            @Override protected void configure() {
                // ... register mocks
                bind(UserAccountManager.class).toInstance(userAccountManager);
                bind(GameManager.class).toInstance(gameManager);
                bind(GroupChangedService.class).toInstance(groupChangedService);
            }
        });
        // Register DTOs to json mapper
//        SegueConfigurationModule segueConfigurationModule = injector.getInstance(SegueConfigurationModule.class);
        Injector injector = Guice.createInjector(testModule);
        ContentMapper mapper = injector.getInstance(ContentMapper.class);
//        mapper.registerJsonTypes(segueConfigurationModule.getContentDataTransferObjectMap());
        // Get instance of class to test
        infoFacade = injector.getInstance(InfoFacade.class);
    }

    @Test
    public void getSegueAppVersion_respondsOK() {
        // /info/segue_version
        Response response = infoFacade.getSegueAppVersion();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void getSegueEnvironment_respondsOK() {
        // /info/segue_environment
        Response response = infoFacade.getSegueEnvironment(requestForCaching);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void getSegueEnvironment_respondsWithDEV() {
        // /info/segue_environment
        Response response = infoFacade.getSegueEnvironment(requestForCaching);
        if (response.getEntity() instanceof ImmutableMap) {
            ImmutableMap<String, String> entity = (ImmutableMap<String, String>) response.getEntity();
            assertNotNull(entity);
            assertNotNull(entity.get("segueEnvironment"));
            assertEquals(entity.get("segueEnvironment"), "DEV");
        }
    }

    @Test
    public void getLiveVersion_respondsOK() {
        // /info/content_version/live_version
        Response response = infoFacade.getLiveVersionInfo();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void getLiveVersion_respondsWithCorrectVersion() {
        // /info/content_version/live_version
        Response response = infoFacade.getLiveVersionInfo();
        if (response.getEntity() instanceof ImmutableMap) {
            ImmutableMap<String, String> entity = (ImmutableMap<String, String>) response.getEntity();
            assertNotNull(entity);
            assertNotNull(entity.get("liveVersion"));
            // TODO: Checking a content version probably needs to mock the Content Manager to return a fake hash and
            //  then we can check that the returned hash is the one we faked.
            // assertEquals(entity.get("liveVersion", "someRandomStringWeMocked"));
        }
    }

    @Test
    public void etlPing_respondsOK() {
        // /info/etl/ping
        Response response = infoFacade.pingETLServer();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    public void elasticsearchPing_respondsOK() {
        // /info/elasticsearch/ping
        Response response = infoFacade.pingElasticSearch();
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    // NOTE: The other methods are probably less useful to test unless we also bring up the checkers
}