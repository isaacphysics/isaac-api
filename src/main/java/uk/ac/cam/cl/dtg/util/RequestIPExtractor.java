/**
 * Copyright 2017 James Sharkey
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
package uk.ac.cam.cl.dtg.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

/**
 *  A utility class to extract IP addresses from HTTP requests.
 */
public final class RequestIPExtractor {
    private static final Logger log = LoggerFactory.getLogger(RequestIPExtractor.class);

    /**
        It does not make sense to create one of these!
     */
    private RequestIPExtractor() {
    }

    /**
     * Extract client ip address.
     *
     * Solution based upon:
     * http://stackoverflow.com/questions/4678797/how-do-i-get-the-remote-address-of-a-client-in-servlet
     *
     * @param request
     *            - to attempt to extract a valid IP address from.
     * @return string representation of the client's ip address, or 0.0.0.0 in case of failure.
     */
    public static String getClientIpAddr(final HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.contains(",")) {
            // If X-Forwarded-For contains multiple comma-separated IP addresses, we want only the last one.
            log.debug("X-Forwarded-For contained multiple IP addresses, extracting last: '" + ip + "'");
            ip = ip.substring(ip.lastIndexOf(',') + 1).trim();
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // Isaac adds this custom header which could be used:
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // In production, this will usually be the router address which may be unhelpful.
            ip = request.getRemoteAddr();
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // We *must* return a *valid* inet field for postgres! Null would be
            // acceptable, but is used for internal log events; 'unknown' is not allowed!
            // So if all else fails, use the impossible source address '0.0.0.0' to mark this.
            ip = "0.0.0.0";
        }
        // Strip any IPv6 address bracketing:
        if (ip.startsWith("[") && ip.endsWith("]")) {
            ip = ip.substring(1, ip.length() - 1);
        }
        return ip;
    }
}
