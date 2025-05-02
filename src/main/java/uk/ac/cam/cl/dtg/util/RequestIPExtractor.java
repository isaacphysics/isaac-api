/*
 * Copyright 2017 James Sharkey
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
package uk.ac.cam.cl.dtg.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

/**
 *  A utility class to extract IP addresses from HTTP requests.
 */
public final class RequestIPExtractor {
    private static final Logger log = LoggerFactory.getLogger(RequestIPExtractor.class);

    private static boolean remoteIpWarningShown = false;

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
            log.debug("X-Forwarded-For contained multiple IP addresses, extracting last: '{}'", ip);
            ip = ip.substring(ip.lastIndexOf(',') + 1).trim();
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            // A reverse proxy can add this custom header which could be used:
            ip = request.getHeader("X-Real-IP");
        }

        // If we have previously used the remote IP directly, but later see proxy headers, warn about danger:
        if (remoteIpWarningShown && ip != null && !ip.isEmpty()) {
            log.warn("X-Real-IP or X-Forwarded-For set unexpectedly on request. The logged IP address may be untrusted!");
        }

        // If no IP address extracted, use the remote IP address directly (useful for local dev):
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            // Behind a reverse proxy, this will usually be the address of the proxy, which will hide the true client!
            ip = request.getRemoteAddr();
            if (!remoteIpWarningShown) {
                log.warn("API appears to be running without a reverse proxy. Using raw remote IP addresses for logging!");
                remoteIpWarningShown = true;
            }
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
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
