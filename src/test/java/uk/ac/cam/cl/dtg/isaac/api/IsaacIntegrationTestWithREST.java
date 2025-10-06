package uk.ac.cam.cl.dtg.isaac.api;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.json.JSONObject;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.AfterEach;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dto.LocalAuthDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Abstract superclass for integration test. Use when testing in the context of a REST application. This lets you
 * start a real server that will serve just the endpoints from a given facade, and also provides a client for
 * interacting with the server. As the server runs in-process, mocking and debugging still work.
 */
public class IsaacIntegrationTestWithREST extends AbstractIsaacIntegrationTest {
    private final HashSet<ThrowingRunnable> cleanups = new HashSet<>();

    @SuppressWarnings({"checkstyle:EmptyCatchBlock", "checkstyle:MissingJavadocMethod"})
    @AfterEach
    public void doCleanup() {
        for (var cleanup : cleanups) {
            try {
                cleanup.run();
            } catch (final Throwable ignored) {

            }
        }
    }

    public void registerCleanup(final ThrowingRunnable cleanup) {
        this.cleanups.add(cleanup);
    }

    TestServer startServer(final Object... facades) throws Exception {
        return TestServer.start(Set.of(facades), this::registerCleanup);
    }

    static class TestServer {
        private String sessionId;
        private final Server server;
        private final ServletContextHandler ctx;
        private final Consumer<ThrowingRunnable> registerCleanup;

        private TestServer(
            final Server server, final ServletContextHandler ctx, final Consumer<ThrowingRunnable> registerCleanup
        ) {
            this.server = server;
            this.ctx = ctx;
            this.registerCleanup = registerCleanup;
        }

        public static TestServer start(
            final Set<Object> facades, final Consumer<ThrowingRunnable> registerCleanup
        ) throws Exception {
            TestApp.facades = facades;

            var server = new Server(0);
            var ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
            ctx.setContextPath("/");
            server.setHandler(ctx);

            var servlet = new ServletHolder(new HttpServletDispatcher());
            servlet.setInitParameter("jakarta.ws.rs.Application", TestApp.class.getName());
            ctx.addServlet(servlet, "/*");

            server.start();
            registerCleanup.accept(server::stop);
            return new TestServer(server, ctx, registerCleanup);
        }

        public TestServer setSessionAttributes(final Map<String, String> attributes) {
            var session = ctx.getSessionHandler().newHttpSession(new Request(null, null));
            attributes.keySet().forEach(k -> session.setAttribute(k, attributes.get(k)));
            sessionId = session.getId();
            return this;
        }

        public TestClient client() {
            var baseUrl = "http://localhost:" + server.getURI().getPort();
            RequestBuilder builder = (null == this.sessionId) ? r -> r : r -> r.cookie("JSESSIONID", sessionId);
            return new TestClient(baseUrl, registerCleanup, builder);
        }

        public static class TestApp extends Application {
            static Set<Object> facades = new HashSet<>();

            @Override
            public Set<Object> getSingletons() {
                return TestApp.facades;
            }
        }
    }

    static class TestClient {
        String baseUrl;
        Consumer<ThrowingRunnable> registerCleanup;
        RequestBuilder builder;
        RegisteredUserDTO currentUser;
        Client client;

        TestClient(final String baseUrl, final Consumer<ThrowingRunnable> registerCleanup, final RequestBuilder builder) {
            this.baseUrl = baseUrl;
            this.registerCleanup = registerCleanup;
            this.builder = builder;
            this.client =  ClientBuilder.newClient().register(new CookieJarFilter());
        }

        public TestResponse get(final String url) {
            var request = client.target(baseUrl + url).request(MediaType.APPLICATION_JSON);
            var response = builder.apply(request).get();
            registerCleanup.accept(response::close);
            return new TestResponse(response);
        }

        public TestResponse post(final String url, final Object body) {
            var request = client.target(baseUrl + url).request(MediaType.APPLICATION_JSON);
            var response = builder.apply(request).post(Entity.json(body));
            registerCleanup.accept(response::close);
            return new TestResponse(response);
        }

        public void loginAs(final RegisteredUser user) {
            var request = client.target(baseUrl + "/auth/SEGUE/authenticate").request(MediaType.APPLICATION_JSON);
            var body = new LocalAuthDTO();
            body.setEmail(user.getEmail());
            body.setPassword("test1234");
            this.currentUser = builder.apply(request).post(Entity.json(body), RegisteredUserDTO.class);
        }
    }

    static class TestResponse {
        Response response;

        TestResponse(final Response response) {
            this.response = response;
        }

        void assertError(final String message, final Response.Status status) {
            assertEquals(message, response.readEntity(Map.class).get("errorMessage"));
            assertEquals(status.getStatusCode(), response.getStatus());
        }

        void assertNoUserLoggedIn() {
            assertThat(this.response.getCookies()).doesNotContainKey("SEGUE_AUTH_COOKIE");
        }

        void assertUserLoggedIn(final Number userId) {
            var base64Cookie = this.response.getCookies().get("SEGUE_AUTH_COOKIE").getValue();
            var cookieBytes = java.util.Base64.getDecoder().decode(base64Cookie);
            var cookie = new JSONObject(new String(cookieBytes));
            assertEquals(userId, cookie.getLong("id"));
        }

        <T> void assertEntityReturned(final T entity) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            assertEquals(entity, response.readEntity(entity.getClass()));
        }

        <T> T readEntity(final Class<T> klass) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            return response.readEntity(klass);
        }
    }

    interface RequestBuilder extends Function<Invocation.Builder, Invocation.Builder> {}
}
