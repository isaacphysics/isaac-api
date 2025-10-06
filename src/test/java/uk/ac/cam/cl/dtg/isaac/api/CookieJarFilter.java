package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/*
 * JAX-RS Client filter for storing and retrieving cookies across requests.
 */
public class CookieJarFilter implements ClientRequestFilter, ClientResponseFilter {
    private final Map<String, NewCookie> cookieJar = new ConcurrentHashMap<>();

    @Override
    public void filter(final ClientRequestContext requestContext) {
        if (!cookieJar.isEmpty()) {
            String header = cookieJar.values().stream()
                    .map(c -> c.getName() + "=" + c.getValue())
                    .collect(Collectors.joining("; "));
            requestContext.getHeaders().putSingle(HttpHeaders.COOKIE, header);
        }
    }

    @Override
    public void filter(final ClientRequestContext requestContext, final ClientResponseContext responseContext) {
        responseContext.getCookies().values().forEach(c -> cookieJar.put(c.getName(), c));
    }
}
