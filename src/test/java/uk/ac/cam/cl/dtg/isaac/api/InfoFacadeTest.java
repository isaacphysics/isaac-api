package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.isaac.IsaacE2ETest;
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
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_LINUX_CONFIG_LOCATION;

@PowerMockIgnore("javax.net.ssl.*")
// NOTE: This was a proof of concept but I'm not too sure we actually need this entire test suite.
public class InfoFacadeTest extends IsaacE2ETest {

    public InfoFacade infoFacade;

    public Request requestForCaching;

    @Before
    public void setUp() throws RuntimeException, IOException {
        String configLocation = SystemUtils.IS_OS_LINUX ? DEFAULT_LINUX_CONFIG_LOCATION : null;
        if (System.getProperty("test.config.location") != null) {
            configLocation = System.getProperty("test.config.location");
        }
        if (System.getenv("SEGUE_TEST_CONFIG_LOCATION") != null){
            configLocation = System.getenv("SEGUE_TEST_CONFIG_LOCATION");
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
        Injector injector = Guice.createInjector(testModule);
        ContentMapper mapper = injector.getInstance(ContentMapper.class);
        // Get instance of class to test
        infoFacade = injector.getInstance(InfoFacade.class);
    }

    @Test
    public void getSegueAppVersion_respondsOK() {
        // /info/segue_version
        Response response = infoFacade.getSegueAppVersion();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getSegueEnvironment_respondsOK() {
        // /info/segue_environment
        Response response = infoFacade.getSegueEnvironment(requestForCaching);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getSegueEnvironment_respondsWithDEV() {
        // /info/segue_environment
        Response response = infoFacade.getSegueEnvironment(requestForCaching);
        if (response.getEntity() instanceof ImmutableMap) {
            ImmutableMap<String, String> entity = (ImmutableMap<String, String>) response.getEntity();
            assertNotNull(entity);
            assertNotNull(entity.get("segueEnvironment"));
            assertEquals("DEV", entity.get("segueEnvironment"));
        }
    }

    @Test
    public void getLiveVersion_respondsOK() {
        // /info/content_version/live_version
        Response response = infoFacade.getLiveVersionInfo();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getLiveVersion_respondsWithCorrectVersion() {
        // /info/content_version/live_version
        Response response = infoFacade.getLiveVersionInfo();
        if (response.getEntity() instanceof ImmutableMap) {
            ImmutableMap<String, String> entity = (ImmutableMap<String, String>) response.getEntity();
            assertNotNull(entity);
            assertNotNull(entity.get("liveVersion"));
            // TODO: with a live content manager, this is going to be a fun test case to keep up to date.
            // assertEquals(entity.get("liveVersion", "someRandomStringWeMocked"));
        }
    }

    @Test
    public void etlPing_respondsOK() {
        // /info/etl/ping
        Response response = infoFacade.pingETLServer();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void elasticsearchPing_respondsOK() {
        // /info/elasticsearch/ping
        Response response = infoFacade.pingElasticSearch();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    // NOTE: The other methods are probably less useful to test unless we also bring up the checkers
}