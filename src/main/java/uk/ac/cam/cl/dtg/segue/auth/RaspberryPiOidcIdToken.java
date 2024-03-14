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

import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.api.client.util.Key;

import java.io.IOException;

public class RaspberryPiOidcIdToken extends IdToken {

    /**
     * ID token with knowledge of Raspberry-Pi-specific fields.
     *
     * @param header             header
     * @param payload            payload
     * @param signatureBytes     bytes of the signature
     * @param signedContentBytes bytes of the signature content
     */
    public RaspberryPiOidcIdToken(Header header, Payload payload, byte[] signatureBytes, byte[] signedContentBytes) {
        super(header, payload, signatureBytes, signedContentBytes);
    }

    public static IdToken parse(JsonFactory jsonFactory, String idTokenString) throws IOException {
        JsonWebSignature jws =
                JsonWebSignature.parser(jsonFactory).setPayloadClass(RaspberryPiOidcIdTokenPayload.class).parse(idTokenString);
        return new IdToken(
                jws.getHeader(),
                (RaspberryPiOidcIdTokenPayload) jws.getPayload(),
                jws.getSignatureBytes(),
                jws.getSignedContentBytes());
    }

    public static class RaspberryPiOidcIdTokenPayload extends Payload {
        @Key("country_code")
        private String countryCode;

        public String getCountryCode() {
            return countryCode;
        }

        public RaspberryPiOidcIdTokenPayload setCountryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }
    }
}
