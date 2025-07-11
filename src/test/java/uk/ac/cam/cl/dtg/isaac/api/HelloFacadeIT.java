package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.jupiter.api.Test;
import java.util.Set;

public class HelloFacadeIT extends IsaacIntegrationTest {
    @Test
    public void shouldSayHello() throws Exception {
        var server = TestServer.start(Set.of(new HelloFacade(properties, logManager)), this);
        var response = server.request("/hello");
        response.assertEntityReturned("Hello World!");
    }
}
