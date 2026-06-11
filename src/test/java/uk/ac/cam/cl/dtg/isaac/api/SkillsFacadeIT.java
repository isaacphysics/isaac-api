package uk.ac.cam.cl.dtg.isaac.api;

import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.managers.SkillsManager;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;

import jakarta.ws.rs.core.Response;
import java.net.UnknownHostException;

import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.REGRESSION_TEST_PAGE_ID;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class SkillsFacadeIT extends IsaacIntegrationTestWithREST {
    private static final String VALID_BODY = new JSONObject().put("payload", "some_signed_payload").toString();

    @Test
    public void notLoggedIn_Returns401() throws Exception {
        var response = testServer().client().post("/skills/unknown_app/answer", "{}");
        response.assertError("You must be logged in to access this resource.", Response.Status.UNAUTHORIZED);
    }

    @Nested
    class AppIdCheck {
        @Test
        public void elasticsearchUnavailable_Returns404() throws Exception {
            var client = testServer(brokenContentManager()).client();
            client.loginAs(integrationTestUsers.TEST_STUDENT);
            var response = client.post("/skills/unknown_app/answer", VALID_BODY);
            response.assertError("Error locating the version requested", Response.Status.NOT_FOUND);
        }

        @Test
        public void unknownApp_Returns404() throws Exception {
            var client = testServer().client();
            client.loginAs(integrationTestUsers.TEST_STUDENT);
            var response = client.post("/skills/unknown_app/answer", VALID_BODY);
            response.assertError("No app found for given id: unknown_app", Response.Status.NOT_FOUND);
        }

        @Test
        public void idMatchesNonApp_Returns404() throws Exception {
            var client = testServer().client();
            client.loginAs(integrationTestUsers.TEST_STUDENT);
            var response = client.post("/skills/" + REGRESSION_TEST_PAGE_ID + "/answer", VALID_BODY);
            response.assertError("No app found for given id: " + REGRESSION_TEST_PAGE_ID, Response.Status.NOT_FOUND);
        }
    }

    @Nested
    class RequestPayloadCheck {
        @Test
        public void missingPayload_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var response = client.post(validUrl(), "{}");
            response.assertError("Invalid JSON object submitted", Response.Status.BAD_REQUEST);
        }

        @Test
        public void numericPayload_Returns200() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var response = client.post(validUrl(), new JSONObject().put("payload", 123).toString());
            response.readEntity(String.class);
        }

        @Test
        public void extraAttribute_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var response = client.post(validUrl(), new JSONObject().put("payload", "some_signed_payload").put("extra", "value").toString());
            response.assertError("Invalid JSON object submitted", Response.Status.BAD_REQUEST);
        }
    }

    @Test
    public void happy_happy() throws Exception {
        var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
        var response = client.post(validUrl(), VALID_BODY);
        response.readEntity(String.class);
    }

    private GitContentManager brokenContentManager() throws UnknownHostException {
        var brokenClient = ElasticSearchProvider.getClient("localhost", 1, "elastic", "elastic");
        var brokenProvider = new ElasticSearchProvider(brokenClient);
        return new GitContentManager(null, brokenProvider, mainMapper, contentMapper, properties);
    }

    private String validUrl() throws Exception {
        var app = elasticHelper.persistJSON(new JSONObject().put("type", "anvilApp"));
        return "/skills/" + app.getString("id") + "/answer";
    }

    private TestServer testServer() throws Exception {
        return testServer(contentManager);
    }

    private TestServer testServer(final GitContentManager cm) throws Exception {
        return startServer(
            new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor),
            new SkillsFacade(properties, userAccountManager, logManager, cm, new SkillsManager())
        );
    }
}
