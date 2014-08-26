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
package uk.ac.cam.cl.dtg.segue.api;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * A class responsible for managing Segue URIs.
 * 
 * @author Stephen Cummins
 */
public class URIManager {
	
	/**
	 * Default constructor for URI manager.
	 */
	public URIManager() {
		
	}
	
	/**
	 * Register a new url mapping.
	 * 
	 * @param type - Type of object that this mapping relates.
	 * @param uriPrefix - the URI prefix to prepend.
	 */
	public void registerKnownURLMapping(final String type, final String uriPrefix) {
		// TODO: write this.
	}
	
	/**
	 * Helper method to store redirect urls for a user going through external
	 * authentication. This is particularly useful for when the user returns from 3rd party authentication.
	 * 
	 * @param request
	 *            - the request to store the session variable in.
	 * @param urls
	 *            - A map containing the possible redirect locations.
	 */
	public static void storeRedirectUrl(final HttpServletRequest request,
			final Map<String, String> urls) {
		request.getSession().setAttribute(Constants.REDIRECT_URL_PARAM_NAME,
				urls);
	}

	/**
	 * Helper method to retrieve the users redirect URL from their session.
	 * 
	 * Note: Using this method will clear the session variable as it is equivalent to a stack.
	 * 
	 * This is particularly useful for when the user returns from 3rd party authentication.
	 * 
	 * @param request
	 *            - the request where the redirect url is stored (session
	 *            variable).
	 * @param urlToRetrieve - the key for the url you wish to retrieve.
	 * @return the URI containing the users desired uri. If URL is null then
	 *         returns /
	 * @throws URISyntaxException
	 *             - if the session retrieved is an invalid URI.
	 */
	public static URI loadRedirectUrl(final HttpServletRequest request, final String urlToRetrieve)
		throws URISyntaxException {
		@SuppressWarnings("unchecked")
		Map<String, String> urlMap = (Map<String, String>) request.getSession().getAttribute(
				Constants.REDIRECT_URL_PARAM_NAME);
		
		String url = urlMap.get(urlToRetrieve);
		
		request.getSession().removeAttribute(Constants.REDIRECT_URL_PARAM_NAME);
		
		if (null == url) {
			return new URI("/");
		}

		return new URI(url);
	}
}
