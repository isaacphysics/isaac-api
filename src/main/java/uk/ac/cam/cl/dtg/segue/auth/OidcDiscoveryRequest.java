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

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.util.GenericData;
import com.google.api.client.util.Preconditions;

import java.io.IOException;

/**
 * OpenID Connect request to identity provider (AKA authorization server)'s discovery endpoint, returning metadata
 * including authorization and token endpoint URIs as {@link OidcDiscoveryResponse}.
 *
 * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfig>OpenID documentation.</a>
 */
public class OidcDiscoveryRequest extends GenericData {

    private final HttpTransport transport;
    private final JsonFactory jsonFactory;
    private GenericUrl idpServerUrl;
    protected Class<? extends OidcDiscoveryResponse> responseClass;

    public OidcDiscoveryRequest(HttpTransport transport, JsonFactory jsonFactory, GenericUrl idpServerUrl) {
        this.transport = Preconditions.checkNotNull(transport);
        this.jsonFactory = Preconditions.checkNotNull(jsonFactory);
        this.setIdpServerUrl(idpServerUrl);
    }

    public GenericUrl getIdpServerUrl() {
        return idpServerUrl;
    }

    public void setIdpServerUrl(GenericUrl idpServerUrl) {
        this.idpServerUrl = idpServerUrl;
    }

    public final OidcDiscoveryResponse execute() throws IOException {
        HttpRequestFactory requestFactory = transport.createRequestFactory();

        HttpRequest request = requestFactory.buildGetRequest(idpServerUrl);
        request.setParser(new JsonObjectParser(jsonFactory));

        HttpResponse response = request.execute();

        if (response.isSuccessStatusCode()) {
            return response.parseAs(OidcDiscoveryResponse.class);
        }
        throw new IOException(String.format("Failed to retrieve metadata from IdP - returned status code %s",
                response.getStatusCode()));
    }
}
