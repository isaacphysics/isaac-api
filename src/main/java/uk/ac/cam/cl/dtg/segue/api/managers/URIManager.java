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
package uk.ac.cam.cl.dtg.segue.api.managers;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.PROXY_PATH;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.api.client.util.Maps;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * A class responsible for managing Segue URIs.
 * 
 * @author Stephen Cummins
 */
public class URIManager {
    private static final Logger log = LoggerFactory.getLogger(URIManager.class);

    private final Map<Class<?>, String> typeToURLPrefixMapping;

    /**
     * Default constructor for URI manager.
     */
    public URIManager() {
        typeToURLPrefixMapping = Maps.newHashMap();
    }

    /**
     * Injectable constructor.
     * 
     * @param typeToURLPrefixMapping
     *            - A map that maps type information to a URL prefix.
     */
    @Inject
    public URIManager(final Map<Class<?>, String> typeToURLPrefixMapping) {
        this.typeToURLPrefixMapping = typeToURLPrefixMapping;
    }

    /**
     * Register a new url mapping.
     * 
     * @param type
     *            - Type of object that this mapping relates.
     * @param uriPrefix
     *            - the URI prefix to prepend.
     */
    public void registerKnownURLMapping(final Class<?> type, final String uriPrefix) {
        this.typeToURLPrefixMapping.put(type, uriPrefix);
    }

    /**
     * Get a url prefix by the type information provided.
     * 
     * @param typeInformation
     *            - the class representing the type of interest.
     * @return the url prefix to prepend with the object id.
     */
    public String getURLPrefix(final Class<?> typeInformation) {
        return this.typeToURLPrefixMapping.get(typeInformation);
    }

    /**
     * Generate a URI that will enable us to find an object again.
     * 
     * @param content
     *            the content object of interest
     * @return null if we are unable to generate the URL or a string that represents the url combined with any proxypath
     *         information required.
     */
    public static String generateApiUrl(final ContentDTO content) {
        Injector injector = Guice.createInjector(new IsaacGuiceConfigurationModule(),
                new SegueGuiceConfigurationModule());
        String proxyPath = injector.getInstance(PropertiesLoader.class).getProperty(PROXY_PATH);

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

    /**
     * Helper method to store redirect urls for a user going through external authentication. This is particularly
     * useful for when the user returns from 3rd party authentication.
     * 
     * @param request
     *            - the request to store the session variable in.
     * @param urls
     *            - A map containing the possible redirect locations.
     */
    public static void storeRedirectUrl(final HttpServletRequest request, final Map<String, String> urls) {
        request.getSession().setAttribute(Constants.REDIRECT_URL_PARAM_NAME, urls);
    }

    /**
     * Helper method to retrieve the users redirect URL from their session.
     * 
     * Note: Using this method will clear the session variable as it is equivalent to a stack.
     * 
     * This is particularly useful for when the user returns from 3rd party authentication.
     * 
     * @param request
     *            - the request where the redirect url is stored (session variable).
     * @param urlToRetrieve
     *            - the key for the url you wish to retrieve.
     * @return the URI containing the users desired uri. If URL is null then returns /
     * @throws URISyntaxException
     *             - if the session retrieved is an invalid URI.
     */
    public static URI loadRedirectUrl(final HttpServletRequest request, final String urlToRetrieve)
            throws URISyntaxException {
        @SuppressWarnings("unchecked")
        Map<String, String> urlMap = (Map<String, String>) request.getSession().getAttribute(
                Constants.REDIRECT_URL_PARAM_NAME);

        if (null == urlMap) {
            log.warn("No redirect url has been set for this session. Returning URI with root path. / ");
            return new URI("/");
        }

        String url = urlMap.get(urlToRetrieve);

        request.getSession().removeAttribute(Constants.REDIRECT_URL_PARAM_NAME);

        if (null == url) {
            return new URI("/");
        }

        return new URI(url);
    }
}
