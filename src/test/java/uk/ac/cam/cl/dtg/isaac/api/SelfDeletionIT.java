package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.collect.ImmutableMap;
import org.easymock.Capture;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.segue.api.EmailFacade;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;

import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.ALICE_STUDENT_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.ALICE_STUDENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.ALICE_STUDENT_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.ALICE_STUDENT_VALID_DELETION_TOKEN;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.BOB_STUDENT_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.BOB_STUDENT_EXPIRED_DELETION_TOKEN;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.BOB_STUDENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.BOB_STUDENT_PASSWORD;

public class SelfDeletionIT extends IsaacIntegrationTest {


    @Test
    public void selfDeletion_existingTokenWorksToConfirmDeletion() throws Exception {

        // Create student login and request:
        LoginResult studentLogin = loginAs(httpSession, ALICE_STUDENT_EMAIL, ALICE_STUDENT_PASSWORD);
        HttpServletRequest deletionConfirmationRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(deletionConfirmationRequest);

        // Prepare response:
        Capture<Cookie> responseCookieCapture = Capture.newInstance();
        HttpServletResponse response = createResponseAndCaptureCookies(responseCookieCapture);
        replay(response);

        // Make the Facade to test:
        EmailFacade emailFacade = new EmailFacade(properties, logManager, emailManager, userAccountManager,
                contentManager, misuseMonitor);

        // Attempt to use a valid token:
        Response apiResponse = emailFacade.completeAccountDeletion(deletionConfirmationRequest, response, ALICE_STUDENT_VALID_DELETION_TOKEN, ImmutableMap.of("reason", "other"));

        // Check request succeeded:
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), apiResponse.getStatus());
        Cookie cookieInResponse = responseCookieCapture.getValue();
        assertEquals(cookieInResponse.getValue(), "");  // Deletion should clear the cookie too.

        // Check old cookie no longer functions and user is deleted:
        try {
            userAccountManager.getCurrentRegisteredUser(createRequestWithCookies(new Cookie[]{studentLogin.cookie}));
            fail("Existing cookie should no longer be valid!");
        } catch (NoUserLoggedInException e) {
            // expected!
        }
        try {
            userAccountManager.getUserDTOById(ALICE_STUDENT_ID);
            fail("User should have been deleted!");
        } catch (NoUserException e) {
            // expected!
        }
    }

    @Test
    public void selfDeletion_expiredTokenErrors() throws Exception {

        // Create student login and request:
        LoginResult studentLogin = loginAs(httpSession, BOB_STUDENT_EMAIL, BOB_STUDENT_PASSWORD);
        HttpServletRequest deletionConfirmationRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(deletionConfirmationRequest);

        // Prepare response:
        Capture<Cookie> responseCookieCapture = Capture.newInstance();
        HttpServletResponse response = createResponseAndCaptureCookies(responseCookieCapture);
        replay(response);

        // Make the Facade to test:
        EmailFacade emailFacade = new EmailFacade(properties, logManager, emailManager, userAccountManager,
                contentManager, misuseMonitor);

        // Attempt to use a expired token:
        Response apiResponse = emailFacade.completeAccountDeletion(deletionConfirmationRequest, response, BOB_STUDENT_EXPIRED_DELETION_TOKEN, ImmutableMap.of("reason", "other"));

        // Check request did nothing:
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), apiResponse.getStatus());
        try {
            userAccountManager.getUserDTOById(BOB_STUDENT_ID);
        } catch (NoUserException e) {
            fail("User should not have been deleted!");
        }
    }

    @Test
    public void selfDeletion_validTokenOfOtherUserErrors() throws Exception {

        // Create student login and request:
        LoginResult studentLogin = loginAs(httpSession, BOB_STUDENT_EMAIL, BOB_STUDENT_PASSWORD);
        HttpServletRequest deletionConfirmationRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(deletionConfirmationRequest);

        // Prepare response:
        Capture<Cookie> responseCookieCapture = Capture.newInstance();
        HttpServletResponse response = createResponseAndCaptureCookies(responseCookieCapture);
        replay(response);

        // Make the Facade to test:
        EmailFacade emailFacade = new EmailFacade(properties, logManager, emailManager, userAccountManager,
                contentManager, misuseMonitor);

        // Attempt to use someone else's valid token:
        Response apiResponse = emailFacade.completeAccountDeletion(deletionConfirmationRequest, response, ALICE_STUDENT_VALID_DELETION_TOKEN, ImmutableMap.of("reason", "other"));

        // Check request did nothing:
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), apiResponse.getStatus());
        try {
            userAccountManager.getUserDTOById(BOB_STUDENT_ID);
        } catch (NoUserException e) {
            fail("User should not have been deleted!");
        }
    }
}
