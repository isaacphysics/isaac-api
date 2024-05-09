package uk.ac.cam.cl.dtg.isaac.api;

import org.easymock.Capture;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.GroupStatus;
import uk.ac.cam.cl.dtg.isaac.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.EmailFacade;
import uk.ac.cam.cl.dtg.segue.api.GroupsFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;
import uk.ac.cam.cl.dtg.util.YamlLoader;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_UNVERIFIED_CAVEAT_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_UNVERIFIED_CAVEAT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_UNVERIFIED_CAVEAT_PASSWORD;


public class SignupFlowIT extends IsaacIntegrationTest {
    @Test
    public void signUpFlow_emailVerificationRequiredCaveatSet_removesCaveatAfterVerifyingEmail() throws Exception {
        // Arrange
        // inject config with feature flag enabled
        AbstractConfigLoader propertiesForTest = new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml,"
                        + "src/test/resources/segue-integration-test-teacher-signup-override.yaml"
        );

        // set up email facade
        UserAccountManager userAccountManagerForTest = new UserAccountManager(pgUsers, questionManager,
                propertiesForTest, providersToRegister, mapperFacade, emailManager, pgAnonymousUsers, logManager,
                userAuthenticationManager, secondFactorManager, userPreferenceManager);

        EmailFacade emailFacade = new EmailFacade(propertiesForTest, logManager, emailManager,
                userAccountManagerForTest, contentManager, misuseMonitor);

        // log in as unverified teacher
        LoginResult caveatTeacherLogin = loginAs(httpSession, TEST_UNVERIFIED_CAVEAT_EMAIL,
                TEST_UNVERIFIED_CAVEAT_PASSWORD);
        HttpServletRequest verificationRequest = createRequestWithCookies(new Cookie[]{caveatTeacherLogin.cookie});
        replay(verificationRequest);

        // prepare to capture verification response cookie
        Capture<Cookie> responseCookieCapture = Capture.newInstance();
        HttpServletResponse response = createResponseAndCaptureCookies(responseCookieCapture);
        replay(response);

        // Act
        // make request
        Response verifyEmailResponse = emailFacade.validateEmailVerificationRequest(verificationRequest, response,
                TEST_UNVERIFIED_CAVEAT_ID, integrationTestUsers.TEST_UNVERIFIED_CAVEAT.getEmailVerificationToken()
                        .substring(0, Constants.TRUNCATED_TOKEN_LENGTH));

        // Assert
        // check initial login held a caveat cookie
        assertEquals(List.of(Constants.AuthenticationCaveat.INCOMPLETE_MANDATORY_EMAIL_VERIFICATION.name()),
                getCaveatsFromCookie(caveatTeacherLogin.cookie));
        // check verification was successful
        assertEquals(Response.Status.OK.getStatusCode(), verifyEmailResponse.getStatus());
        // check new cookie set by post-verification response has no caveats
        assertEquals(List.of(), getCaveatsFromCookie(responseCookieCapture.getValue()));
    }

    @Test
    public void signUpFlow_emailVerificationRequiredCaveatSet_otherEndpointsReturnUnauthorised() throws Exception {
        // Arrange
        // create instance of groups facade
        GroupsFacade groupsFacade = new GroupsFacade(properties, userAccountManager, logManager, assignmentManager,
                groupManager, userAssociationManager, misuseMonitor);

        // log in as unverified teacher
        LoginResult caveatTeacherLogin = loginAs(httpSession, TEST_UNVERIFIED_CAVEAT_EMAIL,
                TEST_UNVERIFIED_CAVEAT_PASSWORD);
        HttpServletRequest createGroupRequest = createRequestWithCookies(new Cookie[]{caveatTeacherLogin.cookie});
        replay(createGroupRequest);

        UserGroup userGroup = new UserGroup(null, "Unverified Teacher's Group", TEST_UNVERIFIED_CAVEAT_ID,
                GroupStatus.ACTIVE, new Date(), false, false, new Date(), false);

        // Act
        // make request - this requires a caveat-free login
        Response createGroupResponse = groupsFacade.createGroup(createGroupRequest, userGroup);

        // Assert
        // check status code is unauthorised
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), createGroupResponse.getStatus());
    }
}
