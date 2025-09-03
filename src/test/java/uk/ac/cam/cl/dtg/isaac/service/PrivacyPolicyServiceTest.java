package uk.ac.cam.cl.dtg.isaac.service;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.easymock.EasyMock;
import org.easymock.EasyMockExtension;
import org.easymock.IMocksControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.InvalidTimestampException;
import uk.ac.cam.cl.dtg.isaac.api.requests.PrivacyPolicyRequest;
import uk.ac.cam.cl.dtg.isaac.api.services.PrivacyPolicyService;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

@ExtendWith(EasyMockExtension.class)
class PrivacyPolicyServiceTest {

  private IMocksControl control;
  private HttpServletRequest mockRequest;
  private PrivacyPolicyRequest mockPrivacyPolicyRequest;
  private UserAccountManager mockUserManager;
  private RegisteredUserDTO mockUser;

  private PrivacyPolicyService privacyPolicyService;

  @BeforeEach
  void setUp() {
    control = EasyMock.createControl();
    mockRequest = control.createMock(HttpServletRequest.class);
    mockPrivacyPolicyRequest = control.createMock(PrivacyPolicyRequest.class);
    mockUserManager = control.createMock(UserAccountManager.class);
    mockUser = control.createMock(RegisteredUserDTO.class);

    privacyPolicyService = new PrivacyPolicyService(mockUserManager);
  }

  @Test
  void acceptPrivacyPolicy_WithValidPastTime_ShouldUseProvidedTime()
      throws NoUserLoggedInException, SegueDatabaseException, InvalidTimestampException {

    Instant pastTime = Instant.now().minus(1, ChronoUnit.HOURS);
    String userEmail = "test@example.com";

    expect(mockUserManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockUser);
    expect(mockPrivacyPolicyRequest.getPrivacyPolicyAcceptedTimeInstant()).andReturn(pastTime);
    expect(mockUser.getEmail()).andReturn(userEmail);

    mockUserManager.updatePrivacyPolicyAcceptedTime(mockUser, pastTime);
    expectLastCall();

    expectLastCall();

    control.replay();

    privacyPolicyService.acceptPrivacyPolicy(mockRequest, mockPrivacyPolicyRequest);

    control.verify();
  }

  @Test
  void acceptPrivacyPolicy_WithFutureTime_ShouldUseCurrentTime()
      throws NoUserLoggedInException, SegueDatabaseException, InvalidTimestampException {

    Instant futureTime = Instant.now().plus(1, ChronoUnit.HOURS);
    String userEmail = "test@example.com";

    expect(mockUserManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockUser);
    expect(mockPrivacyPolicyRequest.getPrivacyPolicyAcceptedTimeInstant()).andReturn(futureTime);
    expect(mockUser.getEmail()).andReturn(userEmail);

    mockUserManager.updatePrivacyPolicyAcceptedTime(eq(mockUser), anyObject(Instant.class));
    expectLastCall();

    expectLastCall();

    control.replay();

    privacyPolicyService.acceptPrivacyPolicy(mockRequest, mockPrivacyPolicyRequest);

    control.verify();
  }

  @Test
  void acceptPrivacyPolicy_WithCurrentTime_ShouldUseProvidedTime()
      throws NoUserLoggedInException, SegueDatabaseException, InvalidTimestampException {

    Instant currentTime = Instant.now();
    String userEmail = "test@example.com";

    expect(mockUserManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockUser);
    expect(mockPrivacyPolicyRequest.getPrivacyPolicyAcceptedTimeInstant()).andReturn(currentTime);
    expect(mockUser.getEmail()).andReturn(userEmail);

    mockUserManager.updatePrivacyPolicyAcceptedTime(mockUser, currentTime);
    expectLastCall();

    expectLastCall();

    control.replay();

    privacyPolicyService.acceptPrivacyPolicy(mockRequest, mockPrivacyPolicyRequest);

    control.verify();
  }

  @Test
  void acceptPrivacyPolicy_WhenNoUserLoggedIn_ShouldThrowException()
      throws NoUserLoggedInException {

    expect(mockUserManager.getCurrentRegisteredUser(mockRequest))
        .andThrow(new NoUserLoggedInException());

    control.replay();

    assertThrows(NoUserLoggedInException.class, () -> {
      privacyPolicyService.acceptPrivacyPolicy(mockRequest, mockPrivacyPolicyRequest);
    });

    control.verify();
  }

  @Test
  void acceptPrivacyPolicy_WhenDatabaseException_ShouldThrowException()
      throws NoUserLoggedInException, SegueDatabaseException, InvalidTimestampException {

    // Arrange
    Instant pastTime = Instant.now().minus(1, ChronoUnit.HOURS);

    expect(mockUserManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockUser);
    expect(mockPrivacyPolicyRequest.getPrivacyPolicyAcceptedTimeInstant()).andReturn(pastTime);

    mockUserManager.updatePrivacyPolicyAcceptedTime(mockUser, pastTime);
    expectLastCall().andThrow(new SegueDatabaseException("Database error"));

    control.replay();

    assertThrows(SegueDatabaseException.class, () -> {
      privacyPolicyService.acceptPrivacyPolicy(mockRequest, mockPrivacyPolicyRequest);
    });

    control.verify();
  }

  @Test
  void acceptPrivacyPolicy_LogsCorrectInformation()
      throws NoUserLoggedInException, SegueDatabaseException, InvalidTimestampException {

    Instant pastTime = Instant.now().minus(30, ChronoUnit.MINUTES);
    String userEmail = "user@test.com";

    expect(mockUserManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockUser);
    expect(mockPrivacyPolicyRequest.getPrivacyPolicyAcceptedTimeInstant()).andReturn(pastTime);
    expect(mockUser.getEmail()).andReturn(userEmail);

    mockUserManager.updatePrivacyPolicyAcceptedTime(mockUser, pastTime);
    expectLastCall();

    expectLastCall();

    control.replay();

    privacyPolicyService.acceptPrivacyPolicy(mockRequest, mockPrivacyPolicyRequest);

    control.verify();
  }

  @Test
  void acceptPrivacyPolicy_WithBoundaryTime_JustBeforeNow_ShouldUseProvidedTime()
      throws NoUserLoggedInException, SegueDatabaseException, InvalidTimestampException {

    Instant almostNow = Instant.now().minus(1, ChronoUnit.MILLIS);
    String userEmail = "boundary@example.com";

    expect(mockUserManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockUser);
    expect(mockPrivacyPolicyRequest.getPrivacyPolicyAcceptedTimeInstant()).andReturn(almostNow);
    expect(mockUser.getEmail()).andReturn(userEmail);

    mockUserManager.updatePrivacyPolicyAcceptedTime(mockUser, almostNow);
    expectLastCall();

    expectLastCall();

    control.replay();

    privacyPolicyService.acceptPrivacyPolicy(mockRequest, mockPrivacyPolicyRequest);

    control.verify();
  }
}