/**
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.isaac.api.managers;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.PROXY_PATH;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.Inject;

/**
 * A class responsible for managing Segue URIs.
 * 
 * @author Stephen Cummins
 */
public class URIManager {
    private static final Logger log = LoggerFactory.getLogger(URIManager.class);
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
     * @return null if we are unable to generate the URL or a string that represents the url combined with any proxypath
     *         information required.
     */
    public String generateApiUrl(final ContentDTO content) {
        String resourceUrl = null;
        try {
            // TODO fix this stuff to be less horrid
            if (content instanceof ImageDTO) {
                resourceUrl = proxyPath + "/api/images/" + URLEncoder.encode(content.getId(), "UTF-8");
            } else if (content.getType().toLowerCase().contains("question")) {
                resourceUrl = proxyPath + "/api/pages/questions/" + URLEncoder.encode(content.getId(), "UTF-8");
            } else if (content.getType().toLowerCase().contains("concept")) {
                resourceUrl = proxyPath + "/api/pages/concepts/" + URLEncoder.encode(content.getId(), "UTF-8");
            } else {
                resourceUrl = proxyPath + "/api/pages/" + URLEncoder.encode(content.getId(), "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Url generation for resource id " + content.getId() + " failed. ", e);
        }

        return resourceUrl;
    }
}