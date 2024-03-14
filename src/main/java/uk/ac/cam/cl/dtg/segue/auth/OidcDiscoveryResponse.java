/*
 * Copyright 2023 Matthew Trew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.auth;

import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Key;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Describes a response from the OpenID Connect identity provider (AKA authorization server)'s discovery endpoint,
 * containing locations of important endpoints for the OAuth interaction.
 *
 * This implementation names only the subset of the possible fields required by our use case.
 *
 * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfig>OpenID documentation.</a>
 */
public class OidcDiscoveryResponse extends GenericJson {

    @Key("issuer")
    private String issuer;

    @Key("authorization_endpoint")
    private String authorizationEndpoint;

    @Key("token_endpoint")
    private String tokenEndpoint;

    @Key("jwks_uri")
    private String jwksUri;

    @Key("id_token_signing_alg_values_supported")
    private List<String> idTokenSigningAlgorithmsSupported;

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public List<String> getIdTokenSigningAlgorithmsSupported() {
        return idTokenSigningAlgorithmsSupported;
    }

    public String getIssuer() {
        return issuer;
    }


    /**
     * Alternatively, load saved OIDC IdP metadata from disk.
     * @param jsonFactory The JSON factory to use to load the file from disk.
     * @param reader The reader to use to load the file from disk.
     * @return A configured {@link OidcDiscoveryResponse}.
     * @throws IOException
     */
    public static OidcDiscoveryResponse load(JsonFactory jsonFactory, Reader reader)
            throws IOException {
        return jsonFactory.fromReader(reader, OidcDiscoveryResponse.class);
    }
}
