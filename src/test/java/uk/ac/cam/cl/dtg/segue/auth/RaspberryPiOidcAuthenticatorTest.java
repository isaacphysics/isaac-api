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

import org.junit.Test;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.OidcDiscoveryException;

import java.net.URL;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class RaspberryPiOidcAuthenticatorTest {

    @Test
    public void getAuthenticator_withOnDiskIdpMetadataDefined_UsesOnDiskMetadata() throws Exception {
        // Arrange
        URL res = getClass().getClassLoader().getResource("test-rpf-idp-metadata.json");
        String idpMetadataPath = Paths.get(res.toURI()).toFile().getAbsolutePath();

        // Act
        RaspberryPiOidcAuthenticator authenticator = new RaspberryPiOidcAuthenticator(
                "test_client_id",
                "test_client_secret",
                "http://localhost:9001",
                "openid",
                "https://not-real-auth-v1.raspberrypi.org/.well-known/openid-configuration",
                idpMetadataPath
        );

        // Assert
        assertEquals("https://notreal-auth-v1.raspberrypi.org/", authenticator.idpMetadata.getIssuer());
        assertEquals("https://notreal-auth-v1.raspberrypi.org/oauth2/auth", authenticator.idpMetadata.getAuthorizationEndpoint());
        assertEquals( "https://notreal-auth-v1.raspberrypi.org/oauth2/token", authenticator.idpMetadata.getTokenEndpoint());
        assertEquals( "https://notreal-auth-v1.raspberrypi.org/.well-known/jwks.json", authenticator.idpMetadata.getJwksUri());
    }

    @Test
    public void getAuthenticator_withoutOnDiskIdpMetadataDefined_UsesDiscoveredMetadata() throws Exception {
        // Arrange
        // (Mocking the discovery call response when instantiating the authenticator, see next step)

        // Act
        RaspberryPiOidcAuthenticator authenticator = new RaspberryPiOidcAuthenticator(
                "test_client_id",
                "test_client_secret",
                "http://localhost:9001",
                "openid",
                "https://not-real-auth-v1.raspberrypi.org/.well-known/openid-configuration",
                ""
        ){
            @Override
            public OidcDiscoveryResponse retrieveIdentityProviderMetadata(String discoveryUri) throws OidcDiscoveryException {
                OidcDiscoveryResponse mockResponse = new OidcDiscoveryResponse();
                mockResponse.set("issuer", "https://discovered-notreal-auth-v1.raspberrypi.org/");
                mockResponse.set("authorization_endpoint", "https://discovered-notreal-auth-v1.raspberrypi.org/oauth2/auth");
                mockResponse.set("token_endpoint", "https://discovered-notreal-auth-v1.raspberrypi.org/oauth2/token");
                mockResponse.set("jwks_uri", "https://discovered-notreal-auth-v1.raspberrypi.org/.well-known/jwks.json");
                return mockResponse;
            }
        };

        // Assert
        assertEquals("https://discovered-notreal-auth-v1.raspberrypi.org/", authenticator.idpMetadata.getIssuer());
        assertEquals("https://discovered-notreal-auth-v1.raspberrypi.org/oauth2/auth", authenticator.idpMetadata.getAuthorizationEndpoint());
        assertEquals( "https://discovered-notreal-auth-v1.raspberrypi.org/oauth2/token", authenticator.idpMetadata.getTokenEndpoint());
        assertEquals( "https://discovered-notreal-auth-v1.raspberrypi.org/.well-known/jwks.json", authenticator.idpMetadata.getJwksUri());
    }
}
