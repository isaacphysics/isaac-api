package uk.ac.cam.cl.dtg.isaac.api.services;

import com.google.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.exceptions.InvalidTimestampException;
import uk.ac.cam.cl.dtg.isaac.api.requests.PrivacyPolicyRequest;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

public class PrivacyPolicyService {

  private static final Logger log = LoggerFactory.getLogger(PrivacyPolicyService.class);

  private final UserAccountManager userManager;

  @Inject
  public PrivacyPolicyService(UserAccountManager userManager) {
    this.userManager = userManager;
  }

  /**
   * Accept privacy policy with timestamp validation.
   *
   * @param request HTTP request to get current user
   * @param privacyPolicyRequest Request containing the timestamp
   * @throws NoUserLoggedInException if no user is logged in
   * @throws SegueDatabaseException if database error occurs
   * @throws InvalidTimestampException if timestamp is too far from current time
   */
  public void acceptPrivacyPolicy(HttpServletRequest request, PrivacyPolicyRequest privacyPolicyRequest)
      throws NoUserLoggedInException, SegueDatabaseException, InvalidTimestampException {

    RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

    Instant providedTime = privacyPolicyRequest.getPrivacyPolicyAcceptedTimeInstant();

    Instant now = Instant.now();
    Instant privacyPolicyTime = providedTime.isAfter(now) ? now : providedTime;

    userManager.updatePrivacyPolicyAcceptedTime(user, privacyPolicyTime);

    log.info("User {} accepted privacy policy at {}", user.getEmail(), privacyPolicyTime);
  }

}