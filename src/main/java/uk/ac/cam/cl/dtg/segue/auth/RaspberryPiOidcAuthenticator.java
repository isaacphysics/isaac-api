/*
 * Copyright 2023 Matthew Trew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.auth;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.auth.openidconnect.IdTokenResponse;
import com.google.api.client.auth.openidconnect.IdTokenVerifier;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.CountryLookupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RaspberryPiOidcAuthenticator implements IOAuth2Authenticator {
    private static final Logger log = LoggerFactory.getLogger(RaspberryPiOidcAuthenticator.class);

    private static Cache<String, IdTokenResponse> credentialStore;

    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;

    private final String clientId;
    private final String clientSecret;
    private final String callbackUri;
    private final List<String> requestedScopes;

    private final IdTokenVerifier idTokenVerifier;

    // Identity provider (AKA authorization server) metadata, including URIs of auth and token endpoints
    public final OidcDiscoveryResponse idpMetadata;

    // Raspberry Pi login options
    public static final String LOGIN_OPTIONS_PARAM_NAME = "login_options";
    // Redirect the user back to the relying party (Isaac) after the signup flow - only relevant for Hydra v1 IdP
    public static final String LOGIN_OPTION_REDIRECT_AFTER_SIGNUP = "v1_signup";
    // Force the signup flow rather than log in/sign up
    public static final String LOGIN_OPTION_FORCE_SIGNUP = "force_signup";

    public static final String BRAND_PARAM_NAME = "brand";
    public static final String BRAND = "ada-cs";

    // Parameters for anti-CSRF state token
    private static final int SALT_SIZE_BITS = 130;
    private static final int SALT_RADIX = 32;

    private static final int CREDENTIAL_CACHE_TTL_MINUTES = 10;


    @Inject
    public RaspberryPiOidcAuthenticator(
            @Named(Constants.RASPBERRYPI_CLIENT_ID) final String clientId,
            @Named(Constants.RASPBERRYPI_CLIENT_SECRET) final String clientSecret,
            @Named(Constants.RASPBERRYPI_CALLBACK_URI) final String callbackUri,
            @Named(Constants.RASPBERRYPI_OAUTH_SCOPES) final String oauthScopes,
            @Named(Constants.RASPBERRYPI_LOCAL_IDP_METADATA_PATH) final String idpMetadataLocation
    ) throws AuthenticatorSecurityException, IOException {

        Validate.notBlank(clientId, "Missing resource %s", clientId);
        Validate.notBlank(clientSecret, "Missing resource %s", clientSecret);

        this.httpTransport = new NetHttpTransport();
        this.jsonFactory = new GsonFactory();

        try (InputStream inputStream = new FileInputStream(idpMetadataLocation);
             InputStreamReader reader = new InputStreamReader(inputStream)
        )
        {
            this.idpMetadata = OidcDiscoveryResponse.load(this.jsonFactory, reader);
        }

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.callbackUri = callbackUri;
        this.requestedScopes = Arrays.asList(oauthScopes.split(";"));

        if (null == credentialStore) {
            credentialStore = CacheBuilder.newBuilder().expireAfterAccess(CREDENTIAL_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
                    .build();
        }

        idTokenVerifier = new IdTokenVerifier.Builder()
            .setCertificatesLocation(this.idpMetadata.getJwksUri())
            .setAudience(Collections.singleton(this.clientId))
            .setIssuer(this.idpMetadata.getIssuer())
            .build();
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return AuthenticationProvider.RASPBERRYPI;
    }

    @Override
    public synchronized UserFromAuthProvider getUserInfo(String internalProviderReference) throws NoUserException, IOException, AuthenticatorSecurityException {
        IdTokenResponse response = credentialStore.getIfPresent(internalProviderReference);

        // Make sure we have an ID token
        if (null == response || null == response.getIdToken()) {
            log.error("No ID token was found in identity provider's response.");
            throw new AuthenticatorSecurityException("No ID token was found in identity provider's response.");
        }

        // Parse and verify the ID token
        IdToken idToken = RaspberryPiOidcIdToken.parse(jsonFactory, response.getIdToken());

        if (verifyIdToken(idToken)) {
            log.debug("Successful verification of ID token with provider.");
        } else {
            log.error("ID token is invalid - possible indication of tampering.");
            throw new AuthenticatorSecurityException("ID token is invalid - possible indication of tampering.");
        }

        // Read claims from the ID token - no need to go to the UserInfo endpoint, as everything is here
        String sub = (String) idToken.getPayload().get("sub");
        String fullName = (String) idToken.getPayload().get("name");
        String nickname = (String) idToken.getPayload().get("nickname");
        String email = (String) idToken.getPayload().get("email");
        String country = (String) idToken.getPayload().get("country_code");
        boolean emailVerified = (Boolean) idToken.getPayload().getOrDefault("email_verified", false);

        if (null != country && !CountryLookupManager.isKnownCountryCode(country)) {
            log.debug(String.format("Country code %s from identity provider is not known, discarding.", country));
            country = null;
        }

        if (null == nickname || null == fullName || null == email || null == sub) {
            throw new NoUserException("Required field missing from identity provider's response.");
        }
        else {
            // Build a given name/family name based on the nickname and full name fields available. This makes
            // unreasonable assumptions about the structure of names, but it's the best we can do.
            List<String> givenNameFamilyName = getGivenNameFamilyName(nickname, fullName);

            EmailVerificationStatus emailStatus = emailVerified ? EmailVerificationStatus.VERIFIED : EmailVerificationStatus.NOT_VERIFIED;

            // Use the IdP's unique ID for the user ('sub') as the unique (per identity provider) user ID.
            return new UserFromAuthProvider(
                    sub,
                    givenNameFamilyName.get(0),
                    givenNameFamilyName.get(1),
                    email,
                    emailStatus,
                    null,
                    null,
                    null,
                    country
            );
        }
    }

    @Override
    public String getAuthorizationUrl(String antiForgeryStateToken) {
        return new AuthorizationCodeRequestUrl(this.idpMetadata.getAuthorizationEndpoint(), this.clientId)
                .setScopes(requestedScopes)
                .setRedirectUri(callbackUri)
                .setState(antiForgeryStateToken)
                .set(LOGIN_OPTIONS_PARAM_NAME, LOGIN_OPTION_REDIRECT_AFTER_SIGNUP)
                .set(BRAND_PARAM_NAME, BRAND)
                .build();
    }

    @Override
    public String getAntiForgeryStateToken() {
        String antiForgerySalt = new BigInteger(SALT_SIZE_BITS, new SecureRandom()).toString(SALT_RADIX);
        return "raspberrypi" + antiForgerySalt;
    }

    @Override
    public String extractAuthCode(String url) {
        AuthorizationCodeResponseUrl authResponse = new AuthorizationCodeResponseUrl(url);

        if (authResponse.getError() == null) {
            log.debug("User granted access to our app.");
        } else {
            log.debug("User denied access to our app.");
        }

        return authResponse.getCode();
    }

    @Override
    public String exchangeCode(String authorizationCode) throws CodeExchangeException {
        try {
            // Exchange the authorization code for the ID token (and access token) from the IdP's token endpoint
            IdTokenResponse response = (IdTokenResponse) new AuthorizationCodeTokenRequest(
                    httpTransport,
                    jsonFactory,
                    new GenericUrl(this.idpMetadata.getTokenEndpoint()),
                    authorizationCode
            )
                    .setResponseClass(IdTokenResponse.class)
                    .setRedirectUri(callbackUri)
                    .setClientAuthentication(new BasicAuthentication(clientId, clientSecret)
            ).execute();

            // Store the response containing tokens and generate an ID that can be used to retrieve it later
            String internalCredentialID = UUID.randomUUID().toString();
            credentialStore.put(internalCredentialID, response);

            return internalCredentialID;

        } catch (IOException e) {
            log.error("An error occurred during code exchange: " + e);
            throw new CodeExchangeException();
        }
    }

    private boolean verifyIdToken(IdToken token) {
        if (null == token) {
            return false;
        }
        // Verify the signature using the identity provider's public key, as well as the issuer and audience.
        return this.idTokenVerifier.verify(token);
    }


    /**
     * Based on the nickname and full name from the identity provider, try to come up with something sensible that fits
     * our given name/family name format. There is no way to consistently get this right across name structures, so the
     * user should have an opportunity to correct it later.
     *
     * @param nickname The nickname from the IdP.
     * @param fullName The full name from the IdP.
     * @return A list with two elements: the derived given name and family name.
     * @throws NoUserException when the derived names are not valid.
     */
    public List<String> getGivenNameFamilyName(String nickname, String fullName) throws NoUserException {
        String givenName = nickname;
        String familyName;

        List<String> tokenisedFullName = Arrays.asList(fullName.split(" "));

        if (fullName.isBlank() || tokenisedFullName.isEmpty()) {
            // If the full name is empty, use the nickname in both fields. Validation on the IdP side should prevent this.
            familyName = nickname;
        } else {
            // Otherwise, use the last token of the full name as the family name.
            familyName = tokenisedFullName.get(tokenisedFullName.size() - 1);
        }

        // Finally, check that the name meets validation.
        if (!UserAccountManager.isUserNameValid(givenName) || !UserAccountManager.isUserNameValid(familyName)){
            throw new NoUserException("The name provided by the identity provider does not meet validation.");
        }
        return List.of(givenName, familyName);
    }
}
