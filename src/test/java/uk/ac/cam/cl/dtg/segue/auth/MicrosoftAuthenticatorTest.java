package uk.ac.cam.cl.dtg.segue.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.IncorrectClaimException;
import com.auth0.jwt.exceptions.MissingClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class MicrosoftAuthenticatorTest extends Helpers{
    @Test
    void getUserInfo_validToken_returnsUserInformation() throws Exception {
        String token = signedToken(validSigningKey, t -> t
            .withIssuedAt(oneHourAgo)
            .withNotBefore(oneHourAgo)
            .withExpiresAt(inOneHour)
            .withAudience(clientId)
            .withPayload("{\"email\": \"test@example.com\"}"));
        var userInfo = testGetUserInfo(token);
        assertEquals("test@example.com", userInfo.getEmail());
    }

    @Test
    void getUserInfo_tokenSignatureNoKeyId_throwsError() {
        String token = signedToken(validSigningKey, t -> t.withKeyId((String) null));
        var error = assertThrows(AuthenticatorSecurityException.class, () -> testGetUserInfo(token));
        assertEquals("Token verification: NO_KEY_ID", error.getMessage());
    }

    @Test
    void getUserInfo_tokenSignatureKeyNotFound_throwsError() {
        String token = signedToken(validSigningKey, t -> t.withKeyId("no-such-key"));
        var error = assertThrows(AuthenticatorSecurityException.class, () -> testGetUserInfo(token));
        assertEquals("No key found in http://localhost:8888/keys with kid no-such-key", error.getMessage());
    }

    @Test
    void getUserInfo_tokenSignatureMismatch_throwsError() {
        String token = signedToken(invalidSigningKey, t -> t.withKeyId(validSigningKey.id()));
        var error = assertThrows(SignatureVerificationException.class, () -> testGetUserInfo(token));
        assertEquals("The Token's Signature resulted invalid when verified using the Algorithm: SHA256withRSA", error.getMessage());
    }

    @Test
    void getUserInfo_tokenNoExp_throwsError() {
        String token = signedToken(validSigningKey, t -> t.withAudience(clientId)
            .withIssuedAt(oneHourAgo)
            .withNotBefore(oneHourAgo)
        );
        var error = assertThrows(AuthenticatorSecurityException.class, () -> testGetUserInfo(token));
        assertEquals("Token verification: NULL_EXPIRY", error.getMessage());
    }

    @Test
    void getUserInfo_tokenNullExp_throwsError() {
        String token = signedToken(validSigningKey, t -> t
            .withIssuedAt(oneHourAgo)
            .withNotBefore(oneHourAgo)
            .withAudience(clientId)
            .withExpiresAt((Date) null)
        );
        var error = assertThrows(AuthenticatorSecurityException.class, () -> testGetUserInfo(token));
        assertEquals("Token verification: NULL_EXPIRY", error.getMessage());
    }

    @Test
    void getUserInfo_tokenExpired_throwsError() {
        String token = signedToken(validSigningKey, t -> t
            .withIssuedAt(oneHourAgo)
            .withNotBefore(oneHourAgo)
            .withAudience(clientId)
            .withExpiresAt(oneHourAgo)
        );
        var error = assertThrows(TokenExpiredException.class, () -> testGetUserInfo(token));
        assertEquals(String.format("The Token has expired on %s.", oneHourAgo), error.getMessage());
    }

    @Test
    void getUserInfo_tokenNoIat_throwsError() {
        String token = signedToken(validSigningKey, t -> t
            .withExpiresAt(inOneHour)
            .withNotBefore(oneHourAgo)
            .withAudience(clientId)
        );
        var error = assertThrows(AuthenticatorSecurityException.class, () -> testGetUserInfo(token));
        assertEquals("Token verification: NULL_ISSUED_AT", error.getMessage());
    }

    @Test
    void getUserInfo_tokenNullIat_throwsError() {
        String token = signedToken(validSigningKey, t -> t
                .withExpiresAt(inOneHour)
                .withNotBefore(oneHourAgo)
                .withAudience(clientId)
                .withIssuedAt((Date) null)
        );
        var error = assertThrows(AuthenticatorSecurityException.class, () -> testGetUserInfo(token));
        assertEquals("Token verification: NULL_ISSUED_AT", error.getMessage());
    }

    @Test
    void getUserInfo_tokenIatFuture_throwsError() {
        String token = signedToken(validSigningKey, t -> t
                .withExpiresAt(inOneHour)
                .withNotBefore(oneHourAgo)
                .withAudience(clientId)
                .withIssuedAt(inOneHour)
        );
        var error = assertThrows(IncorrectClaimException.class, () -> testGetUserInfo(token));
        assertEquals(String.format("The Token can't be used before %s.", inOneHour), error.getMessage());
    }

    @Test
    void getUserInfo_tokenNoNbf_throwsError() {
        String token = signedToken(validSigningKey, t -> t
                .withExpiresAt(inOneHour)
                .withIssuedAt(oneHourAgo)
                .withAudience(clientId)
        );
        var error = assertThrows(AuthenticatorSecurityException.class, () -> testGetUserInfo(token));
        assertEquals("Token verification: NULL_NOT_BEFORE", error.getMessage());
    }

    @Test
    void getUserInfo_tokenNullNbf_throwsError() {
        String token = signedToken(validSigningKey, t -> t
                .withIssuedAt(oneHourAgo)
                .withExpiresAt(inOneHour)
                .withAudience(clientId)
                .withNotBefore((Date) null)
        );
        var error = assertThrows(AuthenticatorSecurityException.class, () -> testGetUserInfo(token));
        assertEquals("Token verification: NULL_NOT_BEFORE", error.getMessage());
    }

    @Test
    void getUserInfo_tokenNbfFuture_throwsError() {
        String token = signedToken(validSigningKey, t -> t
                .withExpiresAt(inOneHour)
                .withIssuedAt(oneHourAgo)
                .withAudience(clientId)
                .withNotBefore(inOneHour)
        );
        var error = assertThrows(IncorrectClaimException.class, () -> testGetUserInfo(token));
        assertEquals(String.format("The Token can't be used before %s.", inOneHour), error.getMessage());
    }

    @Test
    void getUserInfo_tokenNoAud_throwsError() {
        String token = signedToken(validSigningKey, t -> t
                .withExpiresAt(inOneHour)
                .withIssuedAt(oneHourAgo)
        );
        var error = assertThrows(MissingClaimException.class, () -> testGetUserInfo(token));
        assertEquals("The Claim 'aud' is not present in the JWT.", error.getMessage());
    }

    @Test
    void getUserInfo_tokenNullAud_throwsError() {
        String token = signedToken(validSigningKey, t -> t
                .withIssuedAt(oneHourAgo)
                .withExpiresAt(inOneHour)
                .withNotBefore(oneHourAgo)
                .withAudience((String) null)
        );
        var error = assertThrows(IncorrectClaimException.class, () -> testGetUserInfo(token));
        assertEquals("The Claim 'aud' value doesn't contain the required audience.", error.getMessage());
    }

    @Test
    void getUserInfo_tokenAudIncorrect_throwsError() {
        String token = signedToken(validSigningKey, t -> t
                .withIssuedAt(oneHourAgo)
                .withExpiresAt(inOneHour)
                .withNotBefore(oneHourAgo)
                .withAudience("intended_for_somebody_else")
        );
        var error = assertThrows(IncorrectClaimException.class, () -> testGetUserInfo(token));
        assertEquals("The Claim 'aud' value doesn't contain the required audience.", error.getMessage());
    }
}

class Helpers {
    public static UserFromAuthProvider testGetUserInfo(String token) throws Exception {
        var keyServer = TestKeyServer.withKey(validSigningKey).start(8888);
        Cache<String, String> store = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();
        try {
            var subject = new MicrosoftAuthenticator(
                    clientId, "", "", "http://localhost:8888/keys"
            ) {{
                MicrosoftAuthenticator.credentialStore = store;
            }};
            store.put("the_internal_id", token);
            return subject.getUserInfo("the_internal_id");
        } finally {
            keyServer.stop();
        }
    }

    public static String signedToken(TestKeyPair key, Function<JWTCreator.Builder, JWTCreator.Builder> fn) {
        var algorithm = Algorithm.RSA256(key.publicKey(), key.privateKey());
        var token = fn.apply(JWT.create().withKeyId(key.id()));
        return token.sign(algorithm);
    }

    public static TestKeyPair validSigningKey = new TestKeyPair();
    public static TestKeyPair invalidSigningKey = new TestKeyPair();
    public static Instant oneHourAgo = Instant.now().minusSeconds(60 * 60).truncatedTo(ChronoUnit.SECONDS);
    public static Instant inOneHour = Instant.now().plusSeconds(60 * 60).truncatedTo(ChronoUnit.SECONDS);
    public static String clientId = "the_client_id";
}

class TestKeyServer {
    private Server server;
    private TestKeyPair key;

    private TestKeyServer(TestKeyPair key) {
        this.key = key;
    }

    public static TestKeyServer withKey(TestKeyPair key) {
        return new TestKeyServer(key);
    }

    public TestKeyServer start(int port) throws Exception {
        server = new Server(port);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws IOException {
                resp.setContentType("application/json");
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().println(response());
            }
        }), "/keys");
        server.start();
        return this;
    }

    public void stop() throws Exception {
        server.stop();
    }

    private JSONObject response() {
        return new JSONObject().put(
            "keys", new JSONArray().put(
                new JSONObject()
                    .put("kty", "RSA")
                    .put("use", "sig")
                    .put("kid", key.id())
                    .put("n", key.modulus())
                    .put("e", key.exponent())
                    .put("cloud_instance_name", "microsoftonline.com")
// Microsoft's response also contains an X.509 certificate, which we don't test here.
// For an example, response, see: https://login.microsoftonline.com/common/discovery/keys
//    .put("x5t", key_id)
//    .put("x5c", new JSONArray("some_string"))
            )
        );
    }
}

class TestKeyPair {
    private KeyPair keyPair;

    public TestKeyPair() {
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String id() {
        return String.format("key_id_%s", keyPair.getPublic().hashCode());
    }

    public String modulus() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey().getModulus().toByteArray());
    }

    public String exponent() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey().getPublicExponent().toByteArray());
    }

    public RSAPublicKey publicKey() {
        return (RSAPublicKey) keyPair.getPublic();
    }

    public RSAPrivateKey privateKey() {
        return (RSAPrivateKey) keyPair.getPrivate();
    }
}
