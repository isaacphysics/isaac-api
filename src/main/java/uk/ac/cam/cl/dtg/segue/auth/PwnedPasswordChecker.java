package uk.ac.cam.cl.dtg.segue.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.FailedToValidatePasswordException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * A validator to check the security of passwords using the Pwned Passwords API.
 */
@Singleton
public class PwnedPasswordChecker {

    private static final Logger log = LoggerFactory.getLogger(PwnedPasswordChecker.class);
    private static final int SHA1_PREFIX_LENGTH = 5;

    private final HttpClient hibpClient;
    private final String pwnedPasswordsUrlBase;
    private final String userAgent;

    /**
     * Create a Pwned Password API client.
     *
     * @param properties - to load the Pwned Password API URL.
     */
    @Inject
    public PwnedPasswordChecker(final AbstractConfigLoader properties) {
        pwnedPasswordsUrlBase = properties.getProperty(PWNED_PASSWORDS_API_URL);
        Validate.notNull(pwnedPasswordsUrlBase, "No Pwned Passwords base URL specified!");

        HttpClient.Builder builder = HttpClient.newBuilder();
        builder.connectTimeout(Duration.ofMillis(500));
        hibpClient = builder.build();

        userAgent = String.format("%s/1.0", PwnedPasswordChecker.class.getCanonicalName());
    }

    private HttpRequest getRequest(final String hashPrefix) {
        return HttpRequest.newBuilder()
                .uri(URI.create(pwnedPasswordsUrlBase + hashPrefix))
                .header("User-Agent", userAgent)
                .header("Add-Padding", "true")  // This limits bicycle attacks.
                .build();
    }

    private Integer getSuffixCountFromResponse(final String responseBody, final String hashSuffix) {
        String[] lines = responseBody.split("\\R");

        for (String line : lines) {
            if (!line.startsWith(hashSuffix)) {
                // Short-circuit if the line cannot match.
                continue;
            }
            String lineSuffix = line.substring(0, line.indexOf(":"));
            int lineCount = Integer.parseInt(line.substring(line.indexOf(":") + 1));

            if (lineSuffix.equals(hashSuffix)) {
                return lineCount;
            }
        }
        return null;
    }

    /**
     * Get a password's breach count from the HIBP Pwned Passwords API.
     *
     * The password itself is never sent to the HIBP API, only the first
     * SHA1_PREFIX_LENGTH characters of the SHA1 hash. This provides k-anonymity;
     * there are a vast number of potential passwords which all share the same prefix.
     *
     * @param password - the plaintext password to check.
     * @return the associated breach count or null if not matched.
     * @see <a href="https://haveibeenpwned.com/API/v3#PwnedPasswords">API Documentation</a> for more detail.
     */
    public Integer getPasswordBreachCount(final String password) {

        String sha1Hash = DigestUtils.sha1Hex(password).toUpperCase();
        String hashPrefix = sha1Hash.substring(0, SHA1_PREFIX_LENGTH);
        String hashSuffix = sha1Hash.substring(SHA1_PREFIX_LENGTH);

        try {
            // The API requires the SHA-1 hash prefix:
            HttpResponse<String> res = hibpClient.send(getRequest(hashPrefix), HttpResponse.BodyHandlers.ofString());

            if (res.statusCode() != Response.Status.OK.getStatusCode()) {
                log.error(String.format("Error from PwnedPasswords API: %s %s", res.statusCode(), res.body()));
                throw new FailedToValidatePasswordException();
            }

            // We must then check if the hash suffix appears in the response:
            return getSuffixCountFromResponse(res.body(), hashSuffix);

        } catch (IOException | InterruptedException e) {
            log.error(String.format("Error accessing PwnedPasswords API: %s", e));
            throw new FailedToValidatePasswordException();
        }
    }
}
