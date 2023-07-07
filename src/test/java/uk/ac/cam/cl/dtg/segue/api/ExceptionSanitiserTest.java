package uk.ac.cam.cl.dtg.segue.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Response;
import org.easymock.Capture;
import org.jboss.resteasy.core.interception.jaxrs.ContainerResponseContextImpl;
import org.jboss.resteasy.core.interception.jaxrs.ResponseContainerRequestContext;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.builder.EqualsBuilder;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class ExceptionSanitiserTest {

    ExceptionSanitiser exceptionSanitiser  = new ExceptionSanitiser();

    @Test
    public void filter_sanitiseError() throws IOException {
        SegueErrorResponse preFilterError = new SegueErrorResponse(Response.Status.BAD_REQUEST, "Test message", new Exception("Extra detail"));

        Capture<SegueErrorResponse> replacementErrorCapture = newCapture();
        ContainerRequestContext mockRequestContext = createMock(ResponseContainerRequestContext.class);
        ContainerResponseContext mockResponseContext = createMock(ContainerResponseContextImpl.class);
        expect(mockResponseContext.getEntityType()).andReturn(SegueErrorResponse.class).times(1);
        expect(mockResponseContext.getEntity()).andReturn(preFilterError).times(2);
        mockResponseContext.setEntity(capture(replacementErrorCapture));
        expectLastCall();
        replay(mockRequestContext, mockResponseContext);

        exceptionSanitiser.filter(mockRequestContext, mockResponseContext);

        String generatedUUID = replacementErrorCapture.getValue().getErrorMessage().substring(59, 95);
        SegueErrorResponse expectedPostFilterError = new SegueErrorResponse(Response.Status.BAD_REQUEST, "Test message\nPlease report this ID if you contact support: " + generatedUUID + ".", null);
        SegueErrorResponse actualPostFilterError = replacementErrorCapture.getValue();

        verify(mockResponseContext);
        assertEquals(expectedPostFilterError.getResponseCode(), actualPostFilterError.getResponseCode());
        assertEquals(expectedPostFilterError.getResponseCodeType(), actualPostFilterError.getResponseCodeType());
        assertEquals(expectedPostFilterError.getErrorMessage(), actualPostFilterError.getErrorMessage());
        assertEquals(expectedPostFilterError.getException(), actualPostFilterError.getException());
    }

    @Test
    public void filter_cleanError() throws IOException {
        SegueErrorResponse preFilterError = new SegueErrorResponse(Response.Status.BAD_REQUEST, "Test message");

        ContainerRequestContext mockRequestContext = createMock(ResponseContainerRequestContext.class);
        ContainerResponseContext mockResponseContext = createMock(ContainerResponseContextImpl.class);
        expect(mockResponseContext.getEntityType()).andReturn(SegueErrorResponse.class).times(1);
        expect(mockResponseContext.getEntity()).andReturn(preFilterError).times(1);
        replay(mockRequestContext, mockResponseContext);

        exceptionSanitiser.filter(mockRequestContext, mockResponseContext);

        verify(mockResponseContext);

    }
}