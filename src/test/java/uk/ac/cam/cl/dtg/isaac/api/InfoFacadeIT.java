package uk.ac.cam.cl.dtg.isaac.api;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.collect.ImmutableMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.api.InfoFacade;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;

// NOTE: This was a proof of concept but I'm not too sure we actually need this entire test suite.
class InfoFacadeIT extends IsaacIntegrationTest {

  public InfoFacade infoFacade;

  @BeforeEach
  public void setUp() throws RuntimeException, IOException {
    SegueJobService segueJobService =
        createNiceMock(SegueJobService.class); // new SegueJobService(new ArrayList<>(), postgresSqlDb);
    infoFacade = new InfoFacade(properties, contentManager, segueJobService, logManager);
  }

  @Test
  void getSegueAppVersion_respondsOk() {
    // /info/segue_version
    Response response = infoFacade.getSegueAppVersion();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void getSegueEnvironment_respondsOk() {
    // /info/segue_environment
    Request request = createNiceMock(Request.class);
    Response response = infoFacade.getSegueEnvironment(request);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void getSegueEnvironment_respondsWithDEV() {
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
  void getLiveVersion_respondsOk() {
    // /info/content_version/live_version
    Response response = infoFacade.getLiveVersionInfo();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void getLiveVersion_respondsWithCorrectVersion() {
    // /info/content_version/live_version
    Response response = infoFacade.getLiveVersionInfo();
    if (response.getEntity() instanceof ImmutableMap) {
      ImmutableMap<String, String> entity = (ImmutableMap<String, String>) response.getEntity();
      assertNotNull(entity);
      assertNotNull(entity.get("liveVersion"));
    }
  }

  @Test
  void etlPing_respondsOk() {
    // /info/etl/ping
    Response response = infoFacade.pingETLServer();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  void elasticsearchPing_respondsOk() {
    // /info/elasticsearch/ping
    Response response = infoFacade.pingElasticSearch();
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  // NOTE: The other methods are probably less useful to test unless we also bring up the checkers
}