package uk.ac.cam.cl.dtg.isaac.api;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.api.managers.SkillsManager;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;

import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.REGRESSION_TEST_PAGE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ID;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class SkillsFacadeIT extends IsaacIntegrationTestWithREST {
    private static final String HMAC_SECRET = "integration-test-anvil-hmac-secret";
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String VALID_PAYLOAD = validPayload(p -> {});
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
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class HmacVerification {
        Stream<Arguments> invalidBodies() {
            return Stream.of(
                Arguments.of("missing hmac", new JSONObject().put("payload", VALID_PAYLOAD),
                    "Invalid JSON object submitted"),
                Arguments.of("invalid hmac", new JSONObject().put("payload", VALID_PAYLOAD)
                    .put("hmac", "not-a-valid-signature"), "Invalid HMAC signature"),
                Arguments.of("wrong secret", new JSONObject().put("payload", VALID_PAYLOAD)
                    .put("hmac", sign("wrong-secret", VALID_PAYLOAD, HMAC_SHA_256)), "Invalid HMAC signature"),
                Arguments.of("tampered payload", new JSONObject().put("payload", "tampered_payload")
                    .put("hmac", VALID_HMAC), "Invalid HMAC signature"),
                Arguments.of("wrong algorithm", new JSONObject().put("payload", VALID_PAYLOAD)
                    .put("hmac", sign(HMAC_SECRET, VALID_PAYLOAD, "HmacMD5")), "Invalid HMAC signature")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidBodies")
        public void invalidBody_Returns400(
            final String name, final JSONObject body, final String expectedMessage
        ) throws Exception {
            testServer().client().loginAs(integrationTestUsers.TEST_STUDENT)
                .post(validUrl(), body)
                .assertError(expectedMessage, Response.Status.BAD_REQUEST);
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class PayloadContentCheck {
        Stream<Arguments> invalidPayloads() {
            return Stream.of(
                Arguments.of("missing user_id", validPayload(p -> p.remove("user_id")), "Invalid payload"),
                Arguments.of("non-numeric user_id", validPayload(p -> p.put("user_id", "ab")), "Invalid payload"),
                Arguments.of("missing timestamp", validPayload(p -> p.remove("timestamp")), "Invalid payload"),
                Arguments.of("invalid timestamp", validPayload(p -> p.put("timestamp", "not-a-date")),
                    "Invalid payload"),
                Arguments.of("wrong user_id", validPayload(p -> p.put("user_id", TEST_STUDENT_ID + 1)),
                    "Payload user_id does not match session"),
                Arguments.of("expired timestamp", validPayload(p -> p.put("timestamp", "2022-01-01T13:00:00Z")),
                    "Payload timestamp is outside the allowed window")
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidPayloads")
        public void invalidPayload_Returns400(
            final String name, final JSONObject payload, final String expectedMessage
        ) throws Exception {
            testServer().client().loginAs(integrationTestUsers.TEST_STUDENT)
                .post(validUrl(), new JSONObject()
                    .put("payload", payload.toString())
                    .put("hmac", sign(HMAC_SECRET, payload.toString(), HMAC_SHA_256))
                ).assertError(expectedMessage, Response.Status.BAD_REQUEST);
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

    private static String validPayload(final Consumer<JSONObject> ops) {
        var defaultValidPayload = new JSONObject()
                .put("user_id", TEST_STUDENT_ID)
                .put("timestamp", Instant.now());
        ops.accept(defaultValidPayload);
        return defaultValidPayload.toString();
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
