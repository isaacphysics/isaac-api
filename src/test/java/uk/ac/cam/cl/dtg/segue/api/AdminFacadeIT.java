package uk.ac.cam.cl.dtg.segue.api;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.DELETION_TEST_STUDENT_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.DELETION_TEST_STUDENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_ADMIN_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_NON_EXISTENT_USER_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_PASSWORD;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.IsaacIntegrationTest;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.api.managers.IExternalAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.StatisticsManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;

class AdminFacadeIT extends IsaacIntegrationTest {
  private AdminFacade adminFacade;

  @BeforeEach
  void beforeEach() {
    // These three classes are only used in singular methods that we don't have tests for currently,
    // so just mock them for now
    StatisticsManager statisticsManager = createMock(StatisticsManager.class);
    SegueJobService segueJobService = createMock(SegueJobService.class);
    IExternalAccountManager externalAccountManager = createMock(IExternalAccountManager.class);
    this.adminFacade = new AdminFacade(properties, userAccountManager, contentManager, logManager, statisticsManager,
        userPreferenceManager, eventBookingManager, segueJobService, externalAccountManager, misuseMonitor,
        emailManager);
  }

  @Nested
  class DeleteUserAccount {
    @Test
    void piiIsCorrectlySanitised() throws SegueDatabaseException {
      HttpServletRequest mockRequest = prepareAdminRequest();

      try (Response response = adminFacade.deleteUserAccount(mockRequest, DELETION_TEST_STUDENT_ID)) {
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
      }

      RegisteredUser deletedUser = pgUsers.getById(DELETION_TEST_STUDENT_ID, true);
      assertNull(deletedUser.getFamilyName());
      assertNull(deletedUser.getGivenName());
      assertNotEquals(DELETION_TEST_STUDENT_EMAIL, deletedUser.getEmail());
      assertNull(deletedUser.getEmailVerificationToken());
      assertNull(deletedUser.getEmailToVerify());
      assertNull(deletedUser.getSchoolOther());
      assertEquals(Instant.parse("2010-04-01T00:00:00Z"), deletedUser.getDateOfBirth());
    }

    @Test
    void nonAdminUser_returnsForbidden()
        throws NoCredentialsAvailableException, SegueDatabaseException, AuthenticationProviderMappingException,
        IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
        NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
      LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
      HttpServletRequest mockRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
      replay(mockRequest);

      try (Response response = adminFacade.deleteUserAccount(mockRequest, DELETION_TEST_STUDENT_ID)) {
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
      }
    }

    @Test
    void anonymousUser_returnsUnauthorized() {
      HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
      replay(mockRequest);

      try (Response response = adminFacade.deleteUserAccount(mockRequest, DELETION_TEST_STUDENT_ID)) {
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      }
    }

    @Test
    void currentUserAsTarget_returnsBadRequest() {
      HttpServletRequest mockRequest = prepareAdminRequest();

      try (Response response = adminFacade.deleteUserAccount(mockRequest, TEST_ADMIN_ID)) {
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      }
    }

    @Test
    void unknownTargetUser_returnsNotFound() {
      HttpServletRequest mockRequest = prepareAdminRequest();

      try (Response response = adminFacade.deleteUserAccount(mockRequest, TEST_NON_EXISTENT_USER_ID)) {
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
      }
    }
  }
}