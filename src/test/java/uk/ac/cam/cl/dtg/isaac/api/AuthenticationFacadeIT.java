package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;

import java.net.URI;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationFacadeIT extends IsaacIntegrationTest {

    private AuthenticationFacade authenticationFacade;

    @BeforeEach
    public void setUp() throws Exception {
        // get an instance of the facade to test
        this.authenticationFacade = new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor);
    }

    @Test
    public void registerWithRaspberryPiAuthenticator_notInitialSignup_omitsForceSignUpParameterFromRedirectURL() {
        // Arrange
        HttpSession session = createNiceMock(HttpSession.class);
        replay(session);

        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getSession()).andStubReturn(session);
        replay(request);

        // Act
        Response response = authenticationFacade.authenticate(request, "RASPBERRYPI", false);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // check force_signup parameter was not added to redirect URL
        URI uri = ((Map<String, URI>) response.getEntity()).get("redirectUrl");
        assertFalse(uri.getQuery().contains("force_signup"));
    }

    @Test
    public void registerWithRaspberryPiAuthenticator_initialSignup_addsForceSignUpParameterToRedirectURL() {
        // Arrange
        HttpSession session = createNiceMock(HttpSession.class);
        replay(session);

        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getSession()).andStubReturn(session);
        replay(request);

        // Act
        Response response = authenticationFacade.authenticate(request, "RASPBERRYPI", true);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // check force_signup parameter was added to redirect URL
        URI uri = ((Map<String, URI>) response.getEntity()).get("redirectUrl");
        assertTrue(uri.getQuery().contains("force_signup"));
    }
}
