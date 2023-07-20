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

import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;

import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RaspberryPiOidcAuthenticatorTest {

    RaspberryPiOidcAuthenticator authenticator;

    @Before
    public void setUp() throws Exception{
        // Set up an authenticator with local OIDC IdP metadata
        URL res = getClass().getClassLoader().getResource("test-rpf-idp-metadata.json");
        String idpMetadataPath = Paths.get(res.toURI()).toFile().getAbsolutePath();

        authenticator = new RaspberryPiOidcAuthenticator(
                "test_client_id",
                "test_client_secret",
                "http://localhost:9001",
                "openid",
                idpMetadataPath
        );
    }

    @Test
    public void getAuthenticator_withOnDiskIdpMetadataDefined_UsesOnDiskMetadata() throws Exception {
        // Arrange & Act - done in setUp()

        // Assert
        assertEquals("https://notreal-auth-v1.raspberrypi.org/", authenticator.idpMetadata.getIssuer());
        assertEquals("https://notreal-auth-v1.raspberrypi.org/oauth2/auth", authenticator.idpMetadata.getAuthorizationEndpoint());
        assertEquals( "https://notreal-auth-v1.raspberrypi.org/oauth2/token", authenticator.idpMetadata.getTokenEndpoint());
        assertEquals( "https://notreal-auth-v1.raspberrypi.org/.well-known/jwks.json", authenticator.idpMetadata.getJwksUri());
    }

    /**
     * If an empty full name is provided, use the nickname in both fields.
     *
     * @throws Exception, not expected under test.
     */
    @Test
    public void getGivenNameFamilyName_emptyTokenisedNameProvided_returnsSensibleName() throws Exception{
        // Arrange
        String idpNickname = "John";
        String idpFullName = "";

        // Act
        List<String> givenNameFamilyName = authenticator.getGivenNameFamilyName(idpNickname, idpFullName);

        // Assert
        assertEquals("John", givenNameFamilyName.get(0));
        assertEquals("John", givenNameFamilyName.get(1));
    }

    @Test(expected = NoUserException.class)
    public void getGivenNameFamilyName_invalidNicknameProvided_throwsException() throws Exception{
        // Arrange
        String idpNickname = "*";
        String idpFullName = "John Smith";

        // Act
        authenticator.getGivenNameFamilyName(idpNickname, idpFullName);

        // Assert
        // See signature
    }

    @Test
    public void getGivenNameFamilyName_nicknameAndTokenisedNameProvided_returnsSensibleName() throws Exception {
        // Arrange
        String idpNickname = "John";
        String idpFullName = "John Smith";

        // Act
        List<String> givenNameFamilyName = authenticator.getGivenNameFamilyName(idpNickname, idpFullName);

        // Assert
        assertEquals("John", givenNameFamilyName.get(0));
        assertEquals("Smith", givenNameFamilyName.get(1));
    }

    @Test
    public void getGivenNameFamilyName_nicknameAndTokenisedNamesProvided_returnsSensibleName() throws Exception {
        // Arrange
        String idpNickname = "John";
        String idpFullName = "John Angus Smith";

        // Act
        List<String> givenNameFamilyName = authenticator.getGivenNameFamilyName(idpNickname, idpFullName);

        // Assert
        assertEquals("John", givenNameFamilyName.get(0));
        assertEquals("Smith", givenNameFamilyName.get(1));
    }

    /**
     * In some countries the first given name is not necessarily the "calling name".
     *
     * @throws Exception, not expected under test.
     */
    @Test
    public void getGivenNameFamilyName_nickNameAndUnorderedTokenisedNamesProvided_returnsSensibleName() throws Exception {
        // Arrange
        String idpNickname = "Otto";
        String idpFullName = "Arnold Stewart Otto Westland";

        // Act
        List<String> givenNameFamilyName = authenticator.getGivenNameFamilyName(idpNickname, idpFullName);

        // Assert
        assertEquals("Otto", givenNameFamilyName.get(0));
        assertEquals("Westland", givenNameFamilyName.get(1));
    }
}
