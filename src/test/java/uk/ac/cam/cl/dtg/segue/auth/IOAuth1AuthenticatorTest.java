/**
 * Copyright 2014 Nick Rogers
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.Test;

/**
 * Test class for the facebook authenticator class.
 *
 */
public abstract class IOAuth1AuthenticatorTest extends IOAuthAuthenticatorTest {
  protected IOAuth1Authenticator oauth1Authenticator;

  /**
   * Verify that the authenticator returns a valid authorization URL.
   *
   * @throws IOException test exception
   */
  @Test
  public final void getAuthorizationUrl_returnsNonNullUrl() throws IOException {
    String someToken = "someToken";
    String someTokenSecret = "someTokenSecret";
    String urlString = oauth1Authenticator
        .getAuthorizationUrl(new OAuth1Token(someToken, someTokenSecret));
    assertNotNull(urlString);
    URL url = new URL(urlString);
    assertNotNull(url.getAuthority());
  }
}
