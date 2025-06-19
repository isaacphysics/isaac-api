package uk.ac.cam.cl.dtg.segue.auth.microsoft;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;

import static java.lang.String.format;

public class Token {
    public static Instant oneHourAgo = Instant.now().minusSeconds(60 * 60).truncatedTo(ChronoUnit.SECONDS);
    public static Instant inOneHour = Instant.now().plusSeconds(60 * 60).truncatedTo(ChronoUnit.SECONDS);
    public static String msTenantId = "9188040d-6c67-4c5b-b112-36a304b66dad"; // MS uses this for personal accounts

    public static String signed(KeyPair key, SystemFieldsFn fn) {
        var algorithm = Algorithm.RSA256(key.getPublic().toRSA(), key.getPrivate());
        var token = fn.apply(JWT.create().withKeyId(key.getPublic().id()));
        return token.sign(algorithm);
    }

    static UserFields userFields(UserFieldsFn customisePayload) {
        var payload = new UserFields();
        payload.put("oid", UUID.randomUUID().toString());
        payload.put("email", "test@example.com");
        payload.put("family_name", "Family");
        payload.put("given_name", "Given");
        payload.put("tid", msTenantId);
        customisePayload.apply(payload);
        return payload;
    }

    public static String expectedIssuer(String tid) {
        return format("https://login.microsoftonline.com/%s/v2.0", tid);
    }

    String clientId;
    KeyPair signingKey;

    public Token(String clientId, KeyPair signingKey) {
        this.clientId = clientId;
        this.signingKey = signingKey;
    }

    public String valid(SystemFieldsFn setSystemFields, UserFieldsFn setUserFields) {
        SystemFieldsFn defaultSys = t -> t.withIssuedAt(oneHourAgo)
                .withNotBefore(oneHourAgo)
                .withAudience(clientId)
                .withIssuer(expectedIssuer(msTenantId))
                .withExpiresAt(inOneHour)
                .withPayload(userFields(setUserFields));

        return signed(signingKey, t -> setSystemFields.apply(defaultSys.apply(t)));
    }


    public interface SystemFieldsFn extends Function<JWTCreator.Builder, JWTCreator.Builder> {
    }

    public interface UserFieldsFn extends Function<UserFields, Object> {
    }

    public static class UserFields extends HashMap<String, String> {
        public UserFields setName(String givenName, String familyName, String name) {
            setOrRemove("given_name", givenName);
            setOrRemove("family_name", familyName);
            setOrRemove("name", name);
            return this;
        }

        private void setOrRemove(String key, String value) {
            if (value == null) {
                this.remove(key);
            } else {
                this.put(key, value);
            }
        }
    }
}


