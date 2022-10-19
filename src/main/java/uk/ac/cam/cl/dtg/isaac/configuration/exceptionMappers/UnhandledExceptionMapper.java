package uk.ac.cam.cl.dtg.isaac.configuration.exceptionMappers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;

@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger log = LoggerFactory.getLogger(UnhandledExceptionMapper.class);

    @Context
    private HttpServletRequest request;

    @Override
    public Response toResponse(final Exception e) {
        String message = String.format("%s on %s %s", e.getClass().getSimpleName(), request.getMethod(),
                request.getRequestURI());
        log.error(message, e);
        return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "An unhandled error occurred!", e).toResponse();
    }
}
