package uk.ac.cam.cl.dtg.util;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANONYMOUS_USER;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class ServletTestUtils {
  public static HttpServletRequest createMockServletRequest(HttpSession mockSession) {
    HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
    expect(mockRequest.getHeader("X-Forwarded-For")).andReturn("0.0.0.0").anyTimes();
    expect(mockRequest.getSession()).andReturn(mockSession).anyTimes();
    return mockRequest;
  }

  public static HttpSession createMockSession() {
    return createMockSession("sessionId");
  }

  public static HttpSession createMockSession(String sessionId) {
    HttpSession mockSession = createNiceMock(HttpSession.class);
    expect(mockSession.getAttribute(ANONYMOUS_USER)).andReturn(null).anyTimes();
    expect(mockSession.getId()).andReturn(sessionId).anyTimes();
    return mockSession;
  }

  public static HttpServletRequest replayMockServletRequest() {
    HttpSession mockSession = createMockSession();
    replay(mockSession);
    HttpServletRequest mockRequest = createMockServletRequest(mockSession);
    replay(mockRequest);
    return mockRequest;
  }
}
