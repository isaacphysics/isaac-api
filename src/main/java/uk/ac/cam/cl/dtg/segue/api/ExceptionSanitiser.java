package uk.ac.cam.cl.dtg.segue.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;

@Provider
public class ExceptionSanitiser implements ContainerResponseFilter {
  private static final Logger log = LoggerFactory.getLogger(ExceptionSanitiser.class);

  @Override
  public void filter(final ContainerRequestContext containerRequestContext,
                     final ContainerResponseContext containerResponseContext) throws IOException {
    if (containerResponseContext.getEntityType() == SegueErrorResponse.class
        && ((SegueErrorResponse) containerResponseContext.getEntity()).getAdditionalErrorInformation() != null) {
      SegueErrorResponse error = (SegueErrorResponse) containerResponseContext.getEntity();
      UUID generatedUuid = UUID.randomUUID();
      String logMessage = String.format(
          "Unsanitised error response captured. Assigned ID: %1$s. Error content: %2$s",
          generatedUuid, error.toString()
      );
      String responseMessage = String.format(
          "%1$s\nPlease report this ID if you contact support: %2$s.",
          error.getErrorMessage(), generatedUuid
      );
      log.error(logMessage, error.getException());
      containerResponseContext.setEntity(new SegueErrorResponse(
          error.getResponseCode(),
          error.getResponseCodeType(),
          responseMessage,
          null
      ));
    }
  }
}
