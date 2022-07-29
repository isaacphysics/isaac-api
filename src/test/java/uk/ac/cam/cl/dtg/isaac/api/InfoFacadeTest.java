package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.isaac.IsaacIntegrationTest;
import uk.ac.cam.cl.dtg.segue.api.InfoFacade;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.easymock.EasyMock.createNiceMock;

@PowerMockIgnore("javax.net.ssl.*")
// NOTE: This was a proof of concept but I'm not too sure we actually need this entire test suite.
public class InfoFacadeTest extends IsaacIntegrationTest {

    public InfoFacade infoFacade;

    @Before
    public void setUp() throws RuntimeException, IOException {
        SegueJobService segueJobService = createNiceMock(SegueJobService.class); // new SegueJobService(new ArrayList<>(), postgresSqlDb);
        infoFacade = new InfoFacade(properties, contentManager, segueJobService, logManager);
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
        Request request = createNiceMock(Request.class);
        Response response = infoFacade.getSegueEnvironment(request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void getSegueEnvironment_respondsWithDEV() {
        // /info/segue_environment
        Request request = createNiceMock(Request.class);
        Response response = infoFacade.getSegueEnvironment(request);
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