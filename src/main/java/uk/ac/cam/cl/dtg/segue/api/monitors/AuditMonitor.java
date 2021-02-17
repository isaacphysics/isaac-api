package uk.ac.cam.cl.dtg.segue.api.monitors;

import com.google.inject.Inject;
import org.jboss.resteasy.spi.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.api.services.MonitorService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
public class AuditMonitor implements ContainerResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(AuditMonitor.class);

    private final UserAuthenticationManager userAuthenticationManager;
    private final MonitorService monitorService;

    @Context private HttpRequest request;
    @Context private HttpServletRequest httpServletRequest;

    @Inject
    public AuditMonitor(final MonitorService monitorService, final UserAuthenticationManager userAuthenticationManager) {
        this.monitorService = monitorService;
        this.userAuthenticationManager = userAuthenticationManager;
    }

    /**
     * A filter to record an audit log of endpoint access by user.
     *
     * NOTE: User identifiers are taken from the request, we do not checked these against the database as that would add
     * too great an overhead.
     *
     * @param containerRequestContext - http request received by the user.
     * @param containerResponseContext - http response to return to the user.
     */
    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        // As of 16/02/2021 the CSV will be in the following machine-readable format (without the spaces):
        // date_and_time, ip_address, jsessionid, segue_user_id, session_token, is_valid_hmac, http_method, canonical_path, request_path, response_code
        log.trace(String.format("%s,%s,%s,%s,%d",
                userAuthenticationManager.getUserIdentifierCsv(httpServletRequest),
                containerRequestContext.getMethod(), // http_method
                monitorService.getPathWithoutPathParamValues(request.getUri()), // canonical_path
                request.getUri().getPath(), // request_path
                containerResponseContext.getStatus() // response_code
        ));
    }
}
