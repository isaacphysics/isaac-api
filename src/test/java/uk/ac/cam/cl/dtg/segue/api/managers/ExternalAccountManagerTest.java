package uk.ac.cam.cl.dtg.segue.api.managers;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mailjet.client.errors.MailjetException;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserExternalAccountChanges;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IExternalAccountDataManager;
import uk.ac.cam.cl.dtg.util.email.MailJetApiClientWrapper;
import uk.ac.cam.cl.dtg.util.email.MailJetSubscriptionAction;

class ExternalAccountManagerTest {

  private ExternalAccountManager externalAccountManager;
  private IExternalAccountDataManager mockDatabase;
  private MailJetApiClientWrapper mailjetApi;

  @BeforeEach
  public void setUp() {
    mockDatabase = createMock(IExternalAccountDataManager.class);
    mailjetApi = createMock(MailJetApiClientWrapper.class);
    externalAccountManager = new ExternalAccountManager(mailjetApi, mockDatabase);
  }

  @Nested
  class SynchroniseChangedUsers {
    @Test
    void synchroniseChangedUsers_newUser() throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // New user case:
      // - providerUserId (mailjetId) is null
      // - We expect to call addNewUserOrGetUserIfExists to create a new Mailjet account
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, null, "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "gcse"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mailjetApi.addNewUserOrGetUserIfExists("test@example.com")).andReturn("mailjetId");
      mailjetApi.updateUserProperties("mailjetId", "John", "STUDENT", "VERIFIED", "gcse");
      expectLastCall();
      mailjetApi.updateUserSubscriptions("mailjetId",
          MailJetSubscriptionAction.FORCE_SUBSCRIBE, MailJetSubscriptionAction.UNSUBSCRIBE);
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, "mailjetId");
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();

      replay(mockDatabase, mailjetApi);

      externalAccountManager.synchroniseChangedUsers();

      verify(mockDatabase, mailjetApi);
    }

    @Test
    void synchroniseChangedUsers_existingUser() throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      // Existing user case:
      // - providerUserId (mailjetId) is not null
      // - We expect to call getAccountByIdOrEmail to retrieve existing Mailjet account
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "existingMailjetId", "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "gcse"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      JSONObject mailjetDetails = new JSONObject();
      mailjetDetails.put("Email", "test@example.com");
      expect(mailjetApi.getAccountByIdOrEmail("existingMailjetId")).andReturn(mailjetDetails);
      mailjetApi.updateUserProperties("existingMailjetId", "John", "STUDENT", "VERIFIED", "gcse");
      expectLastCall();
      mailjetApi.updateUserSubscriptions("existingMailjetId",
          MailJetSubscriptionAction.FORCE_SUBSCRIBE, MailJetSubscriptionAction.UNSUBSCRIBE);
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, "existingMailjetId");
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();

      replay(mockDatabase, mailjetApi);

      externalAccountManager.synchroniseChangedUsers();

      verify(mockDatabase, mailjetApi);
    }

    @Test
    void synchroniseChangedUsers_deletedUser() throws SegueDatabaseException, MailjetException, ExternalAccountSynchronisationException {
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "existingMailjetId", "test@example.com", Role.STUDENT, "John", true,
          EmailVerificationStatus.VERIFIED, true, false, "gcse"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mailjetApi.getAccountByIdOrEmail("existingMailjetId")).andReturn(new JSONObject());
      mailjetApi.permanentlyDeleteAccountById("existingMailjetId");
      expectLastCall();
      mockDatabase.updateExternalAccount(1L, null);
      expectLastCall();
      mockDatabase.updateProviderLastUpdated(1L);
      expectLastCall();

      replay(mockDatabase, mailjetApi);

      externalAccountManager.synchroniseChangedUsers();

      verify(mockDatabase, mailjetApi);
    }

    @Test
    void synchroniseChangedUsers_mailjetException() throws SegueDatabaseException, MailjetException {
      UserExternalAccountChanges userChanges = new UserExternalAccountChanges(
          1L, "existingMailjetId", "test@example.com", Role.STUDENT, "John", false,
          EmailVerificationStatus.VERIFIED, true, false, "GCSE"
      );
      List<UserExternalAccountChanges> changedUsers = List.of(userChanges);

      expect(mockDatabase.getRecentlyChangedRecords()).andReturn(changedUsers);
      expect(mailjetApi.getAccountByIdOrEmail("existingMailjetId")).andThrow(new MailjetException("Mailjet error"));

      replay(mockDatabase, mailjetApi);

      assertThrows(ExternalAccountSynchronisationException.class, () -> externalAccountManager.synchroniseChangedUsers());

      verify(mockDatabase, mailjetApi);
    }
  }
}
