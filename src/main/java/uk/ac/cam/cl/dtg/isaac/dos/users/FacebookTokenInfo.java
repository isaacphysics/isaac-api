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

package uk.ac.cam.cl.dtg.isaac.dos.users;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FacebookTokenInfo.
 *
 */
public class FacebookTokenInfo {
  private FacebookTokenData data;

  /**
   * Constructor for FacebookTokenInfo.
   *
   * @param data a FacebookTokenData object to set
   */
  @JsonCreator
  public FacebookTokenInfo(@JsonProperty("data") final FacebookTokenData data) {
    this.data = data;
  }

  public FacebookTokenData getData() {
    return data;
  }

  public void setData(final FacebookTokenData data) {
    this.data = data;
  }
}
