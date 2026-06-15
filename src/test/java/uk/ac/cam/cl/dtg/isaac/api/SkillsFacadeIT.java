package uk.ac.cam.cl.dtg.isaac.api;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.managers.SkillsManager;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;

import jakarta.ws.rs.core.Response;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.time.Instant;

import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.REGRESSION_TEST_PAGE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ID;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class SkillsFacadeIT extends IsaacIntegrationTestWithREST {
    private static final String HMAC_SECRET = "integration-test-anvil-hmac-secret";
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String VALID_PAYLOAD = new JSONObject()
            .put("user_id", TEST_STUDENT_ID)
            .put("timestamp", Instant.now())
            .toString();
    private static final String VALID_HMAC = sign(HMAC_SECRET, VALID_PAYLOAD, HMAC_SHA_256);
    private static final JSONObject VALID_BODY = new JSONObject().put("payload", VALID_PAYLOAD).put("hmac", VALID_HMAC);

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
        public void numericPayload_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var body = new JSONObject().put("payload", 123).put("hmac", sign(HMAC_SECRET, "123", HMAC_SHA_256));
            var response = client.post(validUrl(), body);
            response.assertError("Invalid payload", Response.Status.BAD_REQUEST);
        }

        @Test
        public void extraAttribute_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var body = new JSONObject().put("payload", VALID_PAYLOAD).put("hmac", VALID_HMAC).put("extra", "value");
            var response = client.post(validUrl(), body);
            response.assertError("Invalid JSON object submitted", Response.Status.BAD_REQUEST);
        }
    }

    @Nested
    class HmacVerification {
        @Test
        public void missingHmac_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var response = client.post(validUrl(), new JSONObject().put("payload", VALID_PAYLOAD));
            response.assertError("Invalid JSON object submitted", Response.Status.BAD_REQUEST);
        }

        @Test
        public void invalidHmac_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var body = new JSONObject().put("payload", VALID_PAYLOAD).put("hmac", "not-a-valid-signature");
            var response = client.post(validUrl(), body);
            response.assertError("Invalid HMAC signature", Response.Status.BAD_REQUEST);
        }

        @Test
        public void wrongSecret_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var body = new JSONObject().put("payload", VALID_PAYLOAD)
                .put("hmac", sign("wrong-secret", VALID_PAYLOAD, HMAC_SHA_256));
            var response = client.post(validUrl(), body);
            response.assertError("Invalid HMAC signature", Response.Status.BAD_REQUEST);
        }

        @Test
        public void tamperedPayload_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var body = new JSONObject().put("payload", "tampered_payload").put("hmac", VALID_HMAC);
            var response = client.post(validUrl(), body);
            response.assertError("Invalid HMAC signature", Response.Status.BAD_REQUEST);
        }

        @Test
        public void wrongAlgo_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var body = new JSONObject().put("payload", VALID_PAYLOAD)
                .put("hmac", sign(HMAC_SECRET, VALID_PAYLOAD, "HmacMD5"));
            var response = client.post(validUrl(), body);
            response.assertError("Invalid HMAC signature", Response.Status.BAD_REQUEST);
        }
    }

    @Nested
    class PayloadContentCheck {
        @Test
        public void missingUserId_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var payload = new JSONObject().put("timestamp", Instant.now());
            var response = client.post(validUrl(), wrapSigned(payload));
            response.assertError("Invalid payload", Response.Status.BAD_REQUEST);
        }

        @Test
        public void nonNumericUserId_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var payload = new JSONObject().put("user_id", "not-a-number").put("timestamp", Instant.now());
            var response = client.post(validUrl(), wrapSigned(payload));
            response.assertError("Invalid payload", Response.Status.BAD_REQUEST);
        }

        @Test
        public void missingTimestamp_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var payload = new JSONObject().put("user_id", TEST_STUDENT_ID);
            var response = client.post(validUrl(), wrapSigned(payload));
            response.assertError("Invalid payload", Response.Status.BAD_REQUEST);
        }

        @Test
        public void invalidTimestamp_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var payload = new JSONObject().put("user_id", TEST_STUDENT_ID).put("timestamp", "not-a-date");
            var response = client.post(validUrl(), wrapSigned(payload));
            response.assertError("Invalid payload", Response.Status.BAD_REQUEST);
        }

        @Test
        public void wrongUserId_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var payload = new JSONObject().put("user_id", TEST_STUDENT_ID + 1).put("timestamp", Instant.now());
            var response = client.post(validUrl(), wrapSigned(payload));
            response.assertError("Payload user_id does not match session", Response.Status.BAD_REQUEST);
        }

        @Test
        public void expiredTimestamp_Returns400() throws Exception {
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            var payload = new JSONObject().put("user_id", TEST_STUDENT_ID).put("timestamp", "2022-01-01T13:00:00Z");
            var response = client.post(validUrl(), wrapSigned(payload));
            response.assertError("Payload timestamp is outside the allowed window", Response.Status.BAD_REQUEST);
        }
    }

    @Test
    public void happy_happy() throws Exception {
        var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
        var response = client.post(validUrl(), VALID_BODY);
        response.readEntity(String.class);
    }

    private GitContentManager brokenContentManager() throws Exception {
        var brokenClient = ElasticSearchProvider.getClient("localhost", 1, "elastic", "elastic");
        var brokenProvider = new ElasticSearchProvider(brokenClient);
        return new GitContentManager(null, brokenProvider, mainMapper, contentMapper, properties);
    }

    private String validUrl() throws Exception {
        var app = elasticHelper.persistJSON(new JSONObject().put("type", "anvilApp"));
        return "/skills/" + app.getString("id") + "/answer";
    }

    private JSONObject wrapSigned(JSONObject payload) {
        String data = payload.toString();
        return new JSONObject().put("payload", data).put("hmac", sign(HMAC_SECRET, data, HMAC_SHA_256));
    }

    private static String sign(final String key, final String payload, final String algo) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), algo);
            Mac mac = Mac.getInstance(algo);
            mac.init(signingKey);

            return new String(Base64.encodeBase64(mac.doFinal(payload.getBytes())));
        } catch (final Exception e) {
            return "NOT_SIGNED";
        }
    }

    private TestServer testServer() throws Exception {
        return testServer(contentManager);
    }

    private TestServer testServer(final GitContentManager cm) throws Exception {
        return startServer(
            new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor),
            new SkillsFacade(properties, userAccountManager, logManager, cm, new SkillsManager(properties))
        );
    }
}
