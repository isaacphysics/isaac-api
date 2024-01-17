/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.isaac.api.managers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.PROXY_PATH;

import com.google.inject.Inject;
import java.net.URLEncoder;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * A class responsible for managing Segue URIs.
 *
 * @author Stephen Cummins
 */
public class URIManager {
  private final String proxyPath;

  /**
   * URI manager.
   *
   * @param propertiesLoader
   *            - so we can lookup any proxy path information to use for augmenting URIs.
   */
  @Inject
  public URIManager(final PropertiesLoader propertiesLoader) {
    this.proxyPath = propertiesLoader.getProperty(PROXY_PATH);
  }

  /**
   * Generate a URI that will enable us to find an object again.
   *
   * @param content
   *            the content object of interest
   * @return a string that represents the URL combined with any proxypath
   */
  public String generateApiUrl(final ContentDTO content) {
    String base;
    if (content instanceof IsaacQuizDTO) {
      base = "quiz";
    } else if (content instanceof ImageDTO) {
      base = "images";
    } else if (content.getType().toLowerCase().contains("question")) {
      base = "pages/questions";
    } else if (content.getType().toLowerCase().contains("concept")) {
      base = "pages/concepts";
    } else {
      base = "pages";
    }

    return proxyPath + "/api/" + base + "/" + URLEncoder.encode(content.getId(), UTF_8);
  }
}
