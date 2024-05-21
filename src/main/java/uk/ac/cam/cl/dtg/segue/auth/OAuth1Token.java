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

/**
 * An authentication token for OAuth1.
 *
 * @author Nick Rogers
 *
 */
public class OAuth1Token {
  private String token;
  private String tokenSecret;

  /**
   * Create an OAuth1Token.
   *
   * @param token the token
   * @param tokenSecret the token secret
   */
  public OAuth1Token(final String token, final String tokenSecret) {
    this.token = token;
    this.tokenSecret = tokenSecret;
  }

  public String getToken() {
    return token;
  }

  public void setToken(final String token) {
    this.token = token;
  }

  public String getTokenSecret() {
    return tokenSecret;
  }

  public void setTokenSecret(final String tokenSecret) {
    this.tokenSecret = tokenSecret;
  }
}
