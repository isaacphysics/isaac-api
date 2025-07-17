package uk.ac.cam.cl.dtg.isaac.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.easymock.Capture;
import org.easymock.EasyMock;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Use this as opposed to IsaacIntegrationTestWithREST when you'd like to execute the test subject in isolation, without
 * the context of an HTTP framework.
 */
public abstract class IsaacIntegrationTest extends AbstractIsaacIntegrationTest {
    protected class LoginResult {
        public RegisteredUserDTO user;
        public Cookie cookie;

        public LoginResult(final RegisteredUserDTO user, final Cookie cookie) {
            this.user = user;
            this.cookie = cookie;
        }
    }

    private final ObjectMapper serializationMapper = new ObjectMapper();

    protected LoginResult loginAs(final HttpSession httpSession, final String username, final String password) throws Exception {
        Capture<Cookie> capturedUserCookie = Capture.newInstance(); // new Capture<Cookie>(); seems deprecated

        HttpServletRequest userLoginRequest = createNiceMock(HttpServletRequest.class);
        expect(userLoginRequest.getSession()).andReturn(httpSession).atLeastOnce();
        replay(userLoginRequest);

        HttpServletResponse userLoginResponse = createNiceMock(HttpServletResponse.class);
        userLoginResponse.addCookie(and(capture(capturedUserCookie), isA(Cookie.class)));
        expectLastCall().atLeastOnce(); // This is how you expect void methods, apparently...
        replay(userLoginResponse);

        RegisteredUserDTO user;
        try {
            user = userAccountManager.authenticateWithCredentials(userLoginRequest, userLoginResponse, AuthenticationProvider.SEGUE.toString(), username, password, false);
        } catch (AdditionalAuthenticationRequiredException | EmailMustBeVerifiedException e) {
            // In this case, we won't get a user object but the cookies have still been set.
            user = null;
        }

        return new LoginResult(user, capturedUserCookie.getValue());
    }

    protected HttpServletRequest createRequestWithCookies(final Cookie[] cookies) {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        HttpSession session = createNiceMock(HttpSession.class);
        expect(request.getCookies()).andReturn(cookies).anyTimes();
        expect(request.getSession()).andReturn(session).anyTimes();
        return request;
    }

    protected HttpServletRequest createRequestWithSession() {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getSession()).andReturn(httpSession).anyTimes();
        return request;
    }

    protected HttpServletResponse createResponseAndCaptureCookies(final Capture<Cookie> cookieToCapture) {
        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        response.addCookie(capture(cookieToCapture));
        EasyMock.expectLastCall();
        return response;
    }

    protected HashMap<String, String> getSessionInformationFromCookie(final Cookie cookie) throws Exception {
        return this.serializationMapper.readValue(Base64.decodeBase64(cookie.getValue()), HashMap.class);
    }

    protected List<String> getCaveatsFromCookie(final Cookie cookie) throws Exception {
        return serializationMapper.readValue(getSessionInformationFromCookie(cookie)
                        .get(SESSION_CAVEATS), new TypeReference<ArrayList<String>>(){});
    }

    static Set<RegisteredUser> allTestUsersProvider() {
        return ITUsers.ALL;
    }
}
