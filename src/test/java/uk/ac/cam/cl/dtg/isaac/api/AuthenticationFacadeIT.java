package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.jupiter.api.Test;

import static org.eclipse.jetty.http.HttpCookie.SAME_SITE_LAX_COMMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthenticationFacadeIT extends IsaacIntegrationTest {

    @Test
    public void login_cookie_samesite() throws Exception {
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        assertEquals(SAME_SITE_LAX_COMMENT, teacherLogin.cookie.getComment());
    }

    // See E2E for logout test - response object is mocked in IT tests, preventing retrieval of transformed logout cookie
    // Usage of actual object may be possible in future but the complexity is currently prohibitive
}
