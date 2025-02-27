package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.api.InfoFacade;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;

import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


// NOTE: This was a proof of concept but I'm not too sure we actually need this entire test suite.
public class InfoFacadeIT extends IsaacIntegrationTest {

    public InfoFacade infoFacade;

    @BeforeEach
    public void setUp() throws RuntimeException, IOException {
        SegueJobService segueJobService = createNiceMock(SegueJobService.class); // new SegueJobService(new ArrayList<>(), postgresSqlDb);
        infoFacade = new InfoFacade(properties, segueJobService, logManager);
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