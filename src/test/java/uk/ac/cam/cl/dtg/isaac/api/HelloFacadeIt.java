package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import jakarta.ws.rs.client.ClientBuilder;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class HelloFacadeIt extends IsaacIntegrationTest {
    @RegisterExtension
    final TestServer server = new TestServer();

    @Test
    public void shouldSayHello() {
        try (var response = server.request("/hello")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals("Hello World!", response.readEntity(String.class));
        }
    }
}
