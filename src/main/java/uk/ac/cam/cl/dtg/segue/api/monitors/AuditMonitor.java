package uk.ac.cam.cl.dtg.segue.api.monitors;

import com.google.inject.Inject;
import org.jboss.resteasy.spi.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class AuditMonitor implements ContainerResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(AuditMonitor.class);

    private final UserAuthenticationManager userAuthenticationManager;

    @Context private HttpRequest request;
    @Context private HttpServletRequest httpServletRequest;

    @Inject
    public AuditMonitor(final UserAuthenticationManager userAuthenticationManager) {
        this.userAuthenticationManager = userAuthenticationManager;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        String plausibleUserId = userAuthenticationManager.getPlausibleUserIdentifierFromCookie(httpServletRequest);
        log.trace(String.format("User (%s) requested %s \"%s\" with response: %d [%s]",
                plausibleUserId, containerRequestContext.getMethod(), request.getUri().getPath(),
                containerResponseContext.getStatus(), containerResponseContext.getStatusInfo()));
    }
}
