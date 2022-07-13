package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.isaac.IsaacE2ETest;
import uk.ac.cam.cl.dtg.segue.api.InfoFacade;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueScheduledJob;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

@PowerMockIgnore("javax.net.ssl.*")
// NOTE: This was a proof of concept but I'm not too sure we actually need this entire test suite.
public class InfoFacadeTest extends IsaacE2ETest {

    public InfoFacade infoFacade = new InfoFacade(properties, contentManager, new SegueJobService(new ArrayList<SegueScheduledJob>(), postgresSqlDb), logManager);

    public Request requestForCaching;

    @Before
    public void setUp() throws RuntimeException, IOException {
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