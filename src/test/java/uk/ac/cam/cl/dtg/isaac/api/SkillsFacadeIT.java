package uk.ac.cam.cl.dtg.isaac.api;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import uk.ac.cam.cl.dtg.isaac.api.managers.SkillsAttemptManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgSkillsAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;

import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.ALICE_STUDENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.BOB_STUDENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.REGRESSION_TEST_PAGE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ID;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class SkillsFacadeIT extends IsaacIntegrationTestWithREST {
    @Nested
    class AnswerQuestion {
        private static final String HMAC_SECRET = "integration-test-anvil-hmac-secret";
        private static final String HMAC_SHA_256 = "HmacSHA256";
        private static final String VALID_URL = validUrl();
        private static final String VALID_APP_ID = VALID_URL.split("/")[2];
        private static final String VALID_PAYLOAD = validPayload(null);
        private static final String VALID_HMAC = sign(HMAC_SECRET, VALID_PAYLOAD, HMAC_SHA_256);
        private static final JSONObject VALID_BODY = new JSONObject().put("payload", VALID_PAYLOAD)
            .put("hmac", VALID_HMAC);

        @Test
        public void notLoggedIn_Returns401() throws Exception {
            var response = testServer().client().post("/skills/unknown_app/answer", VALID_BODY);
            response.assertError("You must be logged in to access this resource.", Response.Status.UNAUTHORIZED);
        }

        @Nested
        class AppIdCheck {
            @Test
            public void elasticsearchUnavailable_Returns404() throws Exception {
                var client = testServer(brokenContentManager()).client().loginAs(integrationTestUsers.TEST_STUDENT);
                var response = client.post("/skills/unknown_app/answer", VALID_BODY);
                response.assertError("Error locating the version requested", Response.Status.NOT_FOUND);
            }

            @Test
            public void unknownApp_Returns404() throws Exception {
                var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
                var response = client.post("/skills/unknown_app/answer", VALID_BODY);
                response.assertError("No skills app found for that id.", Response.Status.NOT_FOUND);
            }

            @Test
            public void idMatchesNonApp_Returns404() throws Exception {
                var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
                var response = client.post("/skills/" + REGRESSION_TEST_PAGE_ID + "/answer", VALID_BODY);
                response.assertError("No skills app found for that id.", Response.Status.NOT_FOUND);
            }
        }

        @Nested
        class RequestPayloadCheck {
            @Test
            public void missingPayload_Returns400() throws Exception {
                var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
                var response = client.post(VALID_URL, "{}");
                response.assertError("Invalid JSON provided!", Response.Status.BAD_REQUEST);
            }

            @Test
            public void numericPayload_Returns400() throws Exception {
                var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
                var body = new JSONObject().put("payload", 123).put("hmac", sign(HMAC_SECRET, "123", HMAC_SHA_256));
                var response = client.post(VALID_URL, body);
                response.assertError("Invalid payload.", Response.Status.BAD_REQUEST);
            }

            @Test
            public void extraAttribute_Returns400() throws Exception {
                var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
                var body = new JSONObject().put("payload", VALID_PAYLOAD).put("hmac", VALID_HMAC).put("extra", "value");
                var response = client.post(VALID_URL, body);
                response.assertError("Invalid JSON provided!", Response.Status.BAD_REQUEST);
            }

            @Test
            public void oversizedPayload_Returns400() throws Exception {
                var large = validPayload(p -> p.put("question_attempt", "x".repeat(30 * 1024 + 1)));
                var body = new JSONObject().put("payload", large).put("hmac", sign(HMAC_SECRET, large, HMAC_SHA_256));
                var response = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT).post(VALID_URL, body);
                response.assertError("Payload too large.", Response.Status.BAD_REQUEST);
            }
        }

        @Nested
        class HmacVerification {
            static Stream<Arguments> invalidBodies() {
                return Stream.of(
                    Arguments.of("missing hmac", new JSONObject().put("payload", VALID_PAYLOAD),
                        "Invalid JSON provided!"),
                    Arguments.of("invalid hmac", new JSONObject().put("payload", VALID_PAYLOAD)
                        .put("hmac", "not-a-valid-signature"), "Invalid HMAC signature."),
                    Arguments.of("wrong secret", new JSONObject().put("payload", VALID_PAYLOAD)
                        .put("hmac", sign("wrong-secret", VALID_PAYLOAD, HMAC_SHA_256)), "Invalid HMAC signature."),
                    Arguments.of("tampered payload", new JSONObject().put("payload", "tampered_payload")
                        .put("hmac", VALID_HMAC), "Invalid HMAC signature."),
                    Arguments.of("wrong algorithm", new JSONObject().put("payload", VALID_PAYLOAD)
                        .put("hmac", sign(HMAC_SECRET, VALID_PAYLOAD, "HmacMD5")), "Invalid HMAC signature.")
                );
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("invalidBodies")
            public void invalidBody_Returns400(
                final String name, final JSONObject body, final String expectedMessage
            ) throws Exception {
                testServer().client()
                    .loginAs(integrationTestUsers.TEST_STUDENT)
                    .post(VALID_URL, body)
                    .assertError(expectedMessage, Response.Status.BAD_REQUEST);
            }
        }

        @Nested
        class PayloadContentCheck {
            static String MSG_IP = "Invalid payload.";

            static Stream<Arguments> invalidPayloads() {
                var s = Stream.<Arguments>builder();

                // id
                addNonEmptyCasesFor("id", s);
                s.add(Arguments.of("invalid id", validPayload(p -> p.put("id", "not-uuid")), MSG_IP));

                // user_id
                addNonEmptyCasesFor("user_id", s);
                s.add(Arguments.of("non-numeric user_id", validPayload(p -> p.put("user_id", "ab")), MSG_IP));
                s.add(Arguments.of("wrong user_id", validPayload(p -> p.put("user_id", TEST_STUDENT_ID + 1)),
                    "Payload user_id does not match session"));

                // skill_assignment_id
                s.add(Arguments.of("missing skill_assignment_id", validPayload(p -> p.remove("skill_assignment_id")),
                    MSG_IP));
                s.add(Arguments.of("non-null skill_assignment_id",
                    validPayload(p -> p.put("skill_assignment_id", "some_id")), MSG_IP));

                // skill_id
                addNonEmptyCasesFor("skill_id", s);
                s.add(Arguments.of("wrong skill_id", validPayload(p -> p.put("skill_id", "wrong_skill_id")),
                    "Payload skill_id does not match app"));

                // subskill_id
                addNonEmptyCasesFor("subskill_id", s);

                // question
                addNonEmptyCasesFor("question", s);
                s.add(Arguments.of("invalid question JSON, str", validPayload(p -> p.put("question", "ab")), MSG_IP));
                s.add(Arguments.of("invalid question JSON, number", validPayload(p -> p.put("question", 1)), MSG_IP));

                // question_attempt
                addNonEmptyCasesFor("question_attempt", s);
                s.add(Arguments.of("invalid attempts JSON", validPayload(p -> p.put("question_attempt", "a")), MSG_IP));

                // marks
                addNonEmptyCasesFor("marks", s);
                s.add(Arguments.of("invalid mark 2", validPayload(p -> p.put("marks", 2)), MSG_IP));
                s.add(Arguments.of("invalid mark -0.4", validPayload(p -> p.put("marks", -0.4)), MSG_IP));

                // timestamp
                addNonEmptyCasesFor("timestamp", s);
                s.add(Arguments.of("invalid timestamp", validPayload(p -> p.put("timestamp", "n/d")), MSG_IP));
                s.add(Arguments.of("expired timestamp", validPayload(p -> p.put("timestamp", "2022-01-01T13:00:00Z")),
                    "Payload timestamp is outside the allowed window"));

                // other cases
                s.add(Arguments.of("extra value", validPayload(p -> p.put("extra", "value")), MSG_IP));

                return s.build();
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("invalidPayloads")
            public void invalidPayload_Returns400(
                final String name, final String payload, final String expectedMessage
            ) throws Exception {
                testServer().client()
                    .loginAs(integrationTestUsers.TEST_STUDENT)
                    .post(VALID_URL, new JSONObject()
                        .put("payload", payload)
                        .put("hmac", sign(HMAC_SECRET, payload, HMAC_SHA_256)))
                    .assertError(expectedMessage, Response.Status.BAD_REQUEST);
            }

            private static void addNonEmptyCasesFor(final String fieldName, final Stream.Builder<Arguments> s) {
                s.add(Arguments.of("missing " + fieldName, validPayload(p -> p.remove(fieldName)), MSG_IP));
                s.add(Arguments.of("null " + fieldName, validPayload(p -> p.put(fieldName, JSONObject.NULL)), MSG_IP));
                s.add(Arguments.of("empty " + fieldName, validPayload(p -> p.put(fieldName, "")), MSG_IP));
            }
        }

        @Test
        public void duplicateAttemptId_Returns409() throws Exception {
            var testP = validPayload(null);
            var testBody = new JSONObject().put("payload", testP).put("hmac", sign(HMAC_SECRET, testP, HMAC_SHA_256));
            var client = testServer().client().loginAs(integrationTestUsers.TEST_STUDENT);
            client.post(VALID_URL, testBody).readEntity(String.class);
            client.post(VALID_URL, testBody).assertError("Duplicate attempt ID.", Response.Status.CONFLICT);
        }

        @Test
        public void happy_happy() throws Exception {
            var expected = new JSONArray(VALID_PAYLOAD).getJSONObject(0);
            var actualReq = testServer().client()
                .loginAs(integrationTestUsers.TEST_STUDENT)
                .post(VALID_URL, VALID_BODY)
                .readEntityAsJsonArray().getJSONObject(0);

            // assert there is exactly 1 row in the database
            try (var conn = postgresSqlDb.getDatabaseConnection();
                 var pst = conn.prepareStatement("SELECT COUNT(*) FROM public.skills_question_attempts");
                 var rs = pst.executeQuery()) {
                rs.next();
                assertEquals(1, rs.getInt(1));
            }

            // assert everything has been recorded to the database
            try (var conn = postgresSqlDb.getDatabaseConnection();
                 var pst = conn.prepareStatement("""
                     SELECT id, user_id, skill_assignment_id, skill_id, subskill_id, question->>'text' AS question_text,
                            question->>'answer' AS question_answer, question_attempt->>'result' AS result, marks,
                            timestamp
                     FROM public.skills_question_attempts""");
                 var actualDb = pst.executeQuery()) {
                actualDb.next();
                assertEquals(expected.getString("id"), actualDb.getString("id"));
                assertEquals(expected.getInt("user_id"), actualDb.getInt("user_id"));
                assertNull(actualDb.getString("skill_assignment_id"));
                assertEquals(expected.getString("skill_id"), actualDb.getString("skill_id"));
                assertEquals(expected.get("subskill_id").toString(), actualDb.getString("subskill_id"));
                assertEquals(expected.getJSONObject("question").getString("text"), actualDb.getString("question_text"));
                assertEquals(expected.getJSONObject("question").getString("answer"),
                    actualDb.getString("question_answer"));
                assertEquals(expected.getJSONObject("question_attempt").getString("result"),
                    actualDb.getString("result"));
                assertEquals(expected.getInt("marks"), actualDb.getInt("marks"));
                assertThat(actualDb.getTimestamp("timestamp").toInstant())
                    .isCloseTo(expected.getString("timestamp"), within(5, ChronoUnit.SECONDS));
            }

            // assert the request echoes back the payload
            assertEquals(expected.getString("id"), actualReq.getString("id"));
            assertEquals(expected.getInt("user_id"), actualReq.getInt("user_id"));
            assertEquals(expected.getString("skill_id"), actualReq.getString("skill_id"));
            assertEquals(expected.get("subskill_id").toString(), actualReq.getString("subskill_id"));
            assertEquals(expected.getJSONObject("question").toString(), actualReq.getJSONObject("question").toString());
            assertEquals(expected.getJSONObject("question_attempt").toString(),
                actualReq.getJSONObject("question_attempt").toString());
            assertEquals(expected.getInt("marks"), actualReq.getInt("marks"));
            assertFalse(actualReq.has("skill_assignment_id"));
        }

        private static GitContentManager brokenContentManager() throws Exception {
            var brokenClient = ElasticSearchProvider.getClient("localhost", 1, "elastic", "elastic");
            var brokenProvider = new ElasticSearchProvider(brokenClient);
            return new GitContentManager(null, brokenProvider, mainMapper, contentMapper, properties);
        }

        private static String validUrl() {
            try {
                var skillsApp = elasticHelper.persistJSON(new JSONObject().put("type", "skillsApp"));
                return "/skills/" + skillsApp.getString("id") + "/answer";
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static String validPayload(final Consumer<JSONObject> op) {
            var defaultPayload = new JSONObject()
                .put("id", UUID.randomUUID().toString())
                .put("user_id", TEST_STUDENT_ID)
                .put("skill_assignment_id", JSONObject.NULL)
                .put("skill_id", VALID_APP_ID)
                .put("subskill_id", 21)
                .put("question", new JSONObject().put("text", "2+2").put("answer", "4"))
                .put("question_attempt", new JSONObject().put("result", "4"))
                .put("marks", 1)
                .put("timestamp", Instant.now().toString());
            if (op != null) {
                op.accept(defaultPayload);
            }
            return new JSONArray().put(defaultPayload).toString();
        }

        private static String sign(final String key, final String payload, final String algo) {
            try {
                var signingKey = new SecretKeySpec(key.getBytes(), algo);
                var mac = Mac.getInstance(algo);
                mac.init(signingKey);
                return new String(Base64.encodeBase64(mac.doFinal(payload.getBytes())));
            } catch (final Exception e) {
                return "NOT_SIGNED";
            }
        }
    }

    @Nested
    class GetAttempts {
        private static final String NO_PERMISSION_MSG = "You do not have the permissions to complete this action.";
        private static final List<Long> NO_ATTEMPTS_ANY_MONTH = ImmutableList.of(
            0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);

        @Nested
        class AuthorisationCheck {
            @Test
            public void notLoggedIn_Returns401() throws Exception {
                var response = testServer().client().get("/skills/attempts/30");
                response.assertError("You must be logged in to access this resource.", Response.Status.UNAUTHORIZED);
            }

            @Test void studentViewsOtherStudent_Returns403() throws Exception {
                var response = testServer().client()
                        .loginAs(integrationTestUsers.ALICE_STUDENT)
                        .get(String.format("/skills/attempts/%s", BOB_STUDENT_ID));
                response.assertError(NO_PERMISSION_MSG, Response.Status.FORBIDDEN);
            }

            @Test void teacherViewsUnlinkedStudent_Returns403() throws Exception {
                var response = testServer().client()
                    .loginAs(integrationTestUsers.DAVE_TEACHER)
                    .get(String.format("/skills/attempts/%s", ALICE_STUDENT_ID));
                response.assertError(NO_PERMISSION_MSG, Response.Status.FORBIDDEN);
            }

            @Test void studentViewsOwn_Returns200() throws Exception {
                var response = testServer().client()
                    .loginAs(integrationTestUsers.ALICE_STUDENT)
                    .get(String.format("/skills/attempts/%s", ALICE_STUDENT_ID));
                assertPastYearsMonthlyMathsResults(response, NO_ATTEMPTS_ANY_MONTH);
            }

            @Test void teacherViewsLinkedStudent_Returns200() throws Exception {
                var response = testServer().client()
                    .loginAs(integrationTestUsers.DAVE_TEACHER)
                    .get(String.format("/skills/attempts/%d", BOB_STUDENT_ID));
                assertPastYearsMonthlyMathsResults(response, NO_ATTEMPTS_ANY_MONTH);
            }
        }

        @Nested
        class UserIdCheck {
            @Test void nonNumeric_Returns400() throws Exception {
                var response = testServer().client().get("/skills/attempts/12str");
                response.assertError("Endpoint not found", Response.Status.NOT_FOUND);
            }

            @Test void float_Returns400() throws Exception {
                var response = testServer().client().get("/skills/attempts/12.23");
                response.assertError("Endpoint not found", Response.Status.NOT_FOUND);
            }

            @Test void oversized_Returns400() throws Exception {
                var response = testServer().client().get("/skills/attempts/9223372036854775808"); // Long.MAX_VALUE + 1
                response.assertError("Endpoint not found", Response.Status.NOT_FOUND);
            }

            @Test void nonExistentId_Returns403() throws Exception {
                var response = testServer().client()
                    .loginAs(integrationTestUsers.DAVE_TEACHER)
                    .get("/skills/attempts/100");
                response.assertError(NO_PERMISSION_MSG, Response.Status.FORBIDDEN);
            }
        }

        @Test
        public void noAttempts_returnsPaddedMathsPracticeForPastYear() throws Exception {
            var response = testServer().client()
                .loginAs(integrationTestUsers.TEST_STUDENT)
                .get(String.format("/skills/attempts/%s", TEST_STUDENT_ID));
            assertPastYearsMonthlyMathsResults(response, NO_ATTEMPTS_ANY_MONTH);
        }

        private void assertPastYearsMonthlyMathsResults(final TestResponse resp, final List<Long> expectations) {
            var mentalMathsResults = resp.readEntityAsJson().getJSONObject("mental_maths_overall");
            for (int i = 0; i < expectations.size(); i++) {
                var key = LocalDate.now().withDayOfMonth(1).minusMonths(i).toString();
                assertEquals(expectations.get(i), mentalMathsResults.getLong(key), key);
            }
        }
    }

    private TestServer testServer() throws Exception {
        return testServer(contentManager);
    }

    private TestServer testServer(final GitContentManager cm) throws Exception {
        var sm = new SkillsAttemptManager(properties, new PgSkillsAttemptPersistenceManager(postgresSqlDb));
        return startServer(
            new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor),
            new SkillsFacade(properties, userAccountManager, logManager, cm, sm, userAssociationManager)
        );
    }
}
