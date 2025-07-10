package uk.ac.cam.cl.dtg.isaac.api;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.jupiter.api.Test;
import jakarta.ws.rs.core.Response;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class HelloFacadeIt extends IsaacIntegrationTest {
    final TestServer server = new TestServer(List.of(new HelloFacade(properties, logManager)));

    @Test
    public void shouldSayHello() throws Exception {
        var response = server.execute(MockHttpRequest.get("/hello"));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Hello World!", response.getContentAsString());
    }
}
