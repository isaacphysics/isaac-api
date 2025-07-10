package uk.ac.cam.cl.dtg.isaac.api;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class HelloFacadeIt extends IsaacIntegrationTest {

    @RegisterExtension
    final TestServer<HelloFacade> server = new TestServer<>(HelloFacade.class, new HelloFacade(properties, logManager));

    @Test
    public void shouldSayHello() {
        final Response response = ClientBuilder.newClient().target(server.baseUrl()).path("hello").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Hello World!", response.readEntity(String.class));
    }
}

class TestServer<T> implements BeforeEachCallback, AfterEachCallback {
    private Server server;
    private String baseUrl;
    private Class<T> klass;
    private T instance;

    public TestServer(Class<T> klass, T instance) {
        this.klass = klass;
        this.instance = instance;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        baseUrl = String.format("http://localhost:%s", TestUtil.getNewPortNumber(HelloFacade.class));
        final JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress(baseUrl);
        factory.setResourceClasses(klass);
        factory.setResourceProvider(HelloFacade.class, new SingletonResourceProvider(instance, true));
        server = factory.create();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        server.destroy();
    }

    public String baseUrl() {
        return baseUrl;
    }
}

