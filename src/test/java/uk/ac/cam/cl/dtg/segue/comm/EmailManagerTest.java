/**
 * Copyright 2015 Alistair Stead
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.comm;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.easymock.Capture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.UserPreference;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants.SegueUserPreferences;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Test class for the EmailManager class.
 */
class EmailManagerTest {
  private static final String CONTENT_VERSION = "liveVersion";

  private EmailCommunicator emailCommunicator;
  private RegisteredUser user;
  private RegisteredUser userWithNulls;
  private RegisteredUserDTO userDTO;
  private RegisteredUserDTO userDTOWithNulls;
  private static final Logger log = LoggerFactory.getLogger(EmailManagerTest.class);
  private EmailCommunicationMessage email = null;
  private PropertiesLoader mockPropertiesLoader;
  private GitContentManager mockContentManager;
  private Capture<EmailCommunicationMessage> capturedArgument;
  private SegueLocalAuthenticator mockAuthenticator;
  private AbstractUserPreferenceManager userPreferenceManager;
  private ILogManager logManager;
  private UserAccountManager userManager;

  /**
   * Initial configuration of tests.
   *
   * @throws Exception - test exception
   */
  @BeforeEach
  public final void setUp() throws Exception {

    // Create dummy user
    user = new RegisteredUser();
    user.setId(1L);
    user.setEmail("test@test.com");
    user.setGivenName("tester");
    user.setFamilyName("McTest");
    user.setEmailVerificationToken("verificationToken");

    // Create dummy user with nulls
    userWithNulls = new RegisteredUser();
    userWithNulls.setId(1L);
    userWithNulls.setEmail("test@test.com");
    userWithNulls.setGivenName(null);
    userWithNulls.setFamilyName(null);
    userWithNulls.setEmailVerificationToken(null);

    // Create dummy userDTO
    userDTO = new RegisteredUserDTO();
    userDTO.setId(1L);
    userDTO.setEmail("test@test.com");
    userDTO.setGivenName("tester");
    userDTO.setFamilyName("McTest");

    // Create dummy userDTO with nulls
    userDTOWithNulls = new RegisteredUserDTO();
    userDTOWithNulls.setId(1L);
    userDTOWithNulls.setEmail("test@test.com");
    userDTOWithNulls.setGivenName(null);
    userDTOWithNulls.setFamilyName(null);

    // Create dummy email communicator
    emailCommunicator = createMock(EmailCommunicator.class);

    // Create dummy email preferences
    userPreferenceManager = createMock(PgUserPreferenceManager.class);

    mockPropertiesLoader = createMock(PropertiesLoader.class);
    expect(mockPropertiesLoader.getProperty("HOST_NAME")).andReturn("dev.isaaccomputerscience.org").anyTimes();
    expect(mockPropertiesLoader.getProperty("REPLY_TO_ADDRESS")).andReturn("test-reply@test.com").anyTimes();
    expect(mockPropertiesLoader.getProperty("MAIL_FROM_ADDRESS"))
        .andReturn("no-reply@isaaccomputerscience.org").anyTimes();
    expect(mockPropertiesLoader.getProperty("MAIL_NAME")).andReturn("Isaac Computer Science").anyTimes();

    replay(mockPropertiesLoader);


    // Create content manager
    mockContentManager = createMock(GitContentManager.class);

    // Create log manager
    logManager = createMock(ILogManager.class);
    logManager.logInternalEvent(null, null, null);
    expectLastCall().anyTimes();

    // Create user manager
    userManager = createMock(UserAccountManager.class);

    capturedArgument = Capture.newInstance();

    // Mock the emailCommunicator methods so we can see what is sent
    try {
      emailCommunicator.sendMessage(and(capture(capturedArgument), isA(EmailCommunicationMessage.class)));
    } catch (CommunicationException e1) {
      fail(e1);
    }

    replay(emailCommunicator);
    System.out.println("setup");

    mockAuthenticator = createMock(SegueLocalAuthenticator.class);

    expect(mockAuthenticator.createEmailVerificationTokenForUser(user, user.getEmail())).andAnswer(
        () -> {
          user.setEmailVerificationToken("emailVerificationToken");
          return user;
        }
    );

    replay(mockAuthenticator);
  }

  /**
   * Helper method to create test email template objects.
   *
   * @param template - id of the template
   * @return - SegueDTO object
   */
  public EmailTemplateDTO createDummyEmailTemplate(final String template) {

    EmailTemplateDTO emailTemplateDTO = new EmailTemplateDTO();
    emailTemplateDTO.setSubject("title");
    emailTemplateDTO.setHtmlContent(template);
    emailTemplateDTO.setPlainTextContent(template);
    emailTemplateDTO.setAuthor("ags46");
    emailTemplateDTO.setId("email-template-registration-confirmation");
    emailTemplateDTO.setPublished(true);

    return emailTemplateDTO;
  }

  /**
   * Helper method to create test content objects.
   *
   * @param template - id of the template
   * @return - SegueDTO object
   */
  public ContentDTO createDummyContentTemplate(final String template) {

    ContentDTO contentDTO = new ContentDTO();
    contentDTO.setValue(template);
    contentDTO.setAuthor("ags46");
    contentDTO.setId("email-template-registration-confirmation");
    contentDTO.setPublished(true);

    return contentDTO;
  }

  /**
   * Verifies that email templates are parsed and replaced correctly.
   */
  @Test
  final void sendTemplatedEmailToUser_checkForTemplateCompletion_emailShouldBeSentWithTemplateTagsFilledIn() {
    replay(userManager);

    EmailTemplateDTO template = createDummyEmailTemplate("Hi, {{givenName}}."
        + "\nThanks for registering!\nYour Isaac email address is: "
        + "</a href='mailto:{{email}}'>{{email}}<a>.\naddress</a>\n{{sig}}");

    ContentDTO htmlTemplate = createDummyContentTemplate("<!DOCTYPE html><html><head><meta charset='utf-8'>"
        + "<title>Isaac Computer Science project</title></head><body>" + "{{content}}" + "</body></html>");

    ContentDTO asciiTemplate = createDummyContentTemplate("{{content}}");
    try {
      expect(mockContentManager.getContentById("email-template-registration-confirmation")).andReturn(template);

      expect(mockContentManager.getContentById("email-template-html")).andReturn(htmlTemplate);
      expect(mockContentManager.getContentById("email-template-ascii")).andReturn(asciiTemplate);

      expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

      replay(mockContentManager);

    } catch (ContentManagerException e) {
      fail(e);
    }

    EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
        mockContentManager, logManager, generateGlobalTokenMap());
    try {
      ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL", "https://testUrl.com");
      manager.sendTemplatedEmailToUser(userDTO,
          manager.getEmailTemplateDTO("email-template-registration-confirmation"),
          emailTokens, EmailType.SYSTEM);
    } catch (ContentManagerException | SegueDatabaseException e) {
      fail(e);
    }

    final String expectedMessagePlainText = "Hi, tester."
        + "\nThanks for registering!\nYour Isaac email address is: "
        + "</a href='mailto:test@test.com'>test@test.com<a>.\naddress</a>\nIsaac Computer Science Project";

    final String expectedMessageHTML = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>Isaac "
        + "Computer Science project</title></head><body>"
        + "Hi, tester.\nThanks for registering!\nYour Isaac email address is: "
        + "</a href='mailto:test@test.com'>test@test.com<a>.\n" + "address</a>\nIsaac "
        + "Computer Science Project</body></html>";

    // Wait for the emailQueue to spin up and send our message
    int i = 0;
    while (!capturedArgument.hasCaptured() && i < 5) {
      try {
        Thread.sleep(100);
        i++;
      } catch (InterruptedException e) {
        fail(e);
      }
    }
    email = capturedArgument.getValue();
    assertNotNull(email);
    assertEquals(expectedMessagePlainText, email.getPlainTextMessage());
    assertEquals(expectedMessageHTML, email.getHTMLMessage());
    System.out.println(email.getPlainTextMessage());
  }

  /**
   * Verifies that email templates are parsed and replaced correctly.
   */
  @Test
  final void sendFederatedPasswordReset_checkForTemplateCompletion_emailShouldBeSentWithTemplateTagsFilledIn() {

    EmailTemplateDTO template = createDummyEmailTemplate("Hello, {{givenName}}.\n\nYou requested a "
        + "password reset. However you use {{providerString}} to log in to our site. You need"
        + " to go to your authentication {{providerWord}} to reset your password.\n\nRegards,\n\n{{sig}}");

    ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
    try {
      expect(mockContentManager.getContentById("email-template-federated-password-reset")).andReturn(template);

      expect(mockContentManager.getContentById("email-template-html")).andReturn(htmlTemplate);

      expect(mockContentManager.getContentById("email-template-ascii")).andReturn(htmlTemplate);

      expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

      replay(mockContentManager);
    } catch (ContentManagerException e) {
      fail(e);
    }

    EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
        mockContentManager, logManager, generateGlobalTokenMap());
    try {
      Map<String, Object> emailTokens = ImmutableMap.of("providerString", "testString", "providerWord", "testWord");
      manager.sendTemplatedEmailToUser(userDTO,
          manager.getEmailTemplateDTO("email-template-federated-password-reset"),
          emailTokens, EmailType.SYSTEM);

    } catch (ContentManagerException | SegueDatabaseException e) {
      fail(e);
    }

    final String expectedMessage = "Hello, tester.\n\nYou requested a password reset. "
        + "However you use testString to log in to our site. You need to go to your "
        + "authentication testWord to reset your password.\n\nRegards,\n\nIsaac Computer Science Project";

    // Wait for the emailQueue to spin up and send our message
    int i = 0;
    while (!capturedArgument.hasCaptured() && i < 5) {
      try {
        Thread.sleep(100);
        i++;
      } catch (InterruptedException e) {
        fail(e);
      }
    }
    email = capturedArgument.getValue();
    assertNotNull(email);
    assertEquals(expectedMessage, email.getPlainTextMessage());
    System.out.println(email.getPlainTextMessage());

  }

  /**
   * Verifies that email templates are parsed and replaced correctly.
   */
  @Test
  final void sendPasswordReset_checkForTemplateCompletion_emailShouldBeSentWithTemplateTagsFilledIn() {

    EmailTemplateDTO template = createDummyEmailTemplate("Hello, {{givenName}}.\n\nA request has been "
        + "made to reset the password for the account: </a href='mailto:{{email}}'>{{email}}<a>"
        + ".\n\nTo reset your password <a href='{{resetURL}}'>Click Here</a>\n\nRegards,\n\n{{sig}}");

    ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");

    try {
      expect(mockContentManager.getContentById("email-template-password-reset")).andReturn(template).once();

      expect(mockContentManager.getContentById("email-template-html")).andReturn(htmlTemplate).once();

      expect(mockContentManager.getContentById("email-template-ascii")).andReturn(htmlTemplate);

      expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

      replay(mockContentManager);

    } catch (ContentManagerException e) {
      fail(e);
    }

    EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
        mockContentManager, logManager, generateGlobalTokenMap());
    try {
      Map<String, Object> emailValues = ImmutableMap.of("resetURL",
          "https://dev.isaaccomputerscience.org/resetpassword/resetToken");

      manager.sendTemplatedEmailToUser(userDTO,
          manager.getEmailTemplateDTO("email-template-password-reset"),
          emailValues, EmailType.SYSTEM);

    } catch (ContentManagerException | SegueDatabaseException e) {
      fail(e);
    }

    final String expectedMessage = "Hello, tester.\n\nA request has been "
        + "made to reset the password for the account: </a href='mailto:test@test.com'>test@test.com<a>"
        + ".\n\nTo reset your password <a href='https://dev.isaaccomputerscience.org/resetpassword/resetToken'>"
        + "Click Here</a>\n\nRegards,\n\nIsaac Computer Science Project";

    // Wait for the emailQueue to spin up and send our message
    int i = 0;
    while (!capturedArgument.hasCaptured() && i < 5) {
      try {
        Thread.sleep(100);
        i++;
      } catch (InterruptedException e) {
        fail(e);
      }
    }
    email = capturedArgument.getValue();
    assertNotNull(email);

    assertEquals(expectedMessage, email.getPlainTextMessage());
  }

  /**
   * Verify that if there are extra tags the system doesn't recognise, there will be an exception, and the email won't
   * be sent.
   */
  @Test
  void sendRegistrationConfirmation_checkForInvalidTemplateTags_throwIllegalArgumentException() {
    EmailTemplateDTO template = createDummyEmailTemplate("Hi, {{givenName}} {{surname}}.\n"
        + "Thanks for registering!\nYour Isaac email address is: "
        + "</a href='mailto:{{email}}'>{{email}}<a>.\naddress</a>\n{{sig}}");

    ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
    // Create content manager
    try {
      expect(mockContentManager.getContentById("email-template-registration-confirmation")).andReturn(template).once();

      expect(mockContentManager.getContentById("email-template-html")).andReturn(htmlTemplate).once();

      expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

      replay(mockContentManager);

    } catch (ContentManagerException e) {
      fail(e);
    }

    EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
        mockContentManager, logManager, generateGlobalTokenMap());

    Map<String, Object> emailTokens = Map.of("verificationURL", "https://testUrl.com");

    assertThrows(IllegalArgumentException.class, () -> {
      manager.sendTemplatedEmailToUser(userDTO,
          template,
          emailTokens, EmailType.SYSTEM);
    });
  }

  /**
   * Verify that the system responds correctly when there are fewer tags than expected in the email template.
   */
  @Test
  final void sendRegistrationConfirmation_checkTemplatesWithNoTagsWorks_emailIsGeneratedWithoutTemplateContent() {
    EmailTemplateDTO template = createDummyEmailTemplate("this is a template with no tags");

    // Create content manager
    GitContentManager mockContentManager = createMock(GitContentManager.class);

    ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
    try {
      expect(mockContentManager.getContentById("email-template-registration-confirmation")).andReturn(template);

      expect(mockContentManager.getContentById("email-template-ascii")).andReturn(htmlTemplate);

      expect(mockContentManager.getContentById("email-template-html")).andReturn(htmlTemplate);

      expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

      replay(mockContentManager);
    } catch (ContentManagerException e) {
      fail(e);
    }

    EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
        mockContentManager, logManager, generateGlobalTokenMap());
    try {
      ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL", "https://testUrl.com");

      manager.sendTemplatedEmailToUser(userDTO,
          template,
          emailTokens, EmailType.SYSTEM);
    } catch (ContentManagerException | SegueDatabaseException e) {
      fail(e);
    }

    // Wait for the emailQueue to spin up and send our message
    int i = 0;
    while (!capturedArgument.hasCaptured() && i < 5) {
      try {
        Thread.sleep(100);
        i++;
      } catch (InterruptedException e) {
        fail(e);
      }
    }
    email = capturedArgument.getValue();
    assertNotNull(email);
    assertEquals("this is a template with no tags", email.getPlainTextMessage());
    System.out.println(email.getPlainTextMessage());
  }

  /**
   * Make sure that when the templates are published:false, that the method reacts appropriately.
   */
  @Test
  void sendRegistrationConfirmation_checkNullContentDTO_exceptionThrownAndDealtWith() {
    ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
    try {
      expect(mockContentManager.getContentById("email-template-registration-confirmation")).andReturn(null);

      expect(mockContentManager.getContentById("email-template-html")).andReturn(htmlTemplate);

      expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

      replay(mockContentManager);
    } catch (ContentManagerException e) {
      fail(e);
    }

    EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
        mockContentManager, logManager, generateGlobalTokenMap());
    try {

      ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL", "https://testUrl.com");
      manager.sendTemplatedEmailToUser(userDTO,
          manager.getEmailTemplateDTO("email-template-registration-confirmation"),
          emailTokens, EmailType.SYSTEM);

    } catch (ContentManagerException e) {
      fail(e);
    } catch (SegueDatabaseException e) {
      log.info(e.getMessage());
    }

    // Wait for the emailQueue to spin up and send our message (if it exists)
    int i = 0;
    while (!capturedArgument.hasCaptured() && i < 5) {
      try {
        Thread.sleep(100);
        i++;
      } catch (InterruptedException e) {
        fail(e);
      }
    }
    // We expect there to be nothing captured because the content was not returned
    assertFalse(capturedArgument.hasCaptured());
  }

  /**
   * Make sure that when the templates are published:false, that the method reacts appropriately.
   */
  @Test
  void sendCustomEmail_checkNullProperties_replacedWithEmptyString() {

    EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
        mockContentManager, logManager, generateGlobalTokenMap());

    List<RegisteredUserDTO> allSelectedUsers = Lists.newArrayList();
    allSelectedUsers.add(userDTOWithNulls);
    allSelectedUsers.add(userDTOWithNulls);

    UserPreference userPreference =
        new UserPreference(userDTOWithNulls.getId(), SegueUserPreferences.EMAIL_PREFERENCE.name(), "ASSIGNMENTS",
            false);
    try {
      expect(
          userPreferenceManager.getUserPreference(SegueUserPreferences.EMAIL_PREFERENCE.name(), "ASSIGNMENTS",
              userDTOWithNulls.getId())).andReturn(userPreference);
      expect(
          userPreferenceManager.getUserPreference(SegueUserPreferences.EMAIL_PREFERENCE.name(), "ASSIGNMENTS",
              userDTOWithNulls.getId())).andReturn(userPreference);
    } catch (SegueDatabaseException e1) {
      fail(e1);
    }
    replay(userPreferenceManager);

    ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
    EmailTemplateDTO emailTemplate = createDummyEmailTemplate("Hello {{givenName}}, "
        + "how are you {{familyName}}? {{sig}}");
    String contentObjectId = "test-email-template";

    try {
      expect(mockContentManager.getContentById(contentObjectId)).andReturn(emailTemplate);

      expect(mockContentManager.getContentById("email-template-html"))
          .andReturn(htmlTemplate).times(allSelectedUsers.size());

      expect(mockContentManager.getContentById("email-template-ascii"))
          .andReturn(htmlTemplate).times(allSelectedUsers.size());

      expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

      replay(mockContentManager);
    } catch (ContentManagerException e) {
      fail(e);
    }

    try {
      manager.sendCustomEmail(userDTOWithNulls, contentObjectId, allSelectedUsers, EmailType.ASSIGNMENTS);
    } catch (SegueDatabaseException | ContentManagerException e) {
      fail(e);
    }

  }

  /**
   * Check we don't send custom content emails to users with null / preference.
   */
  @Test
  void sendCustomContentEmail_checkNullProperties_replacedWithEmptyString() {

    EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
        mockContentManager, logManager, generateGlobalTokenMap());

    List<RegisteredUserDTO> allSelectedUsers = Lists.newArrayList();
    allSelectedUsers.add(userDTOWithNulls);
    allSelectedUsers.add(userDTOWithNulls);

    UserPreference userPreference =
        new UserPreference(userDTOWithNulls.getId(), SegueUserPreferences.EMAIL_PREFERENCE.name(), "ASSIGNMENTS",
            false);
    try {
      expect(
          userPreferenceManager.getUserPreference(SegueUserPreferences.EMAIL_PREFERENCE.name(), "ASSIGNMENTS",
              userDTOWithNulls.getId())).andReturn(userPreference);
      expect(
          userPreferenceManager.getUserPreference(SegueUserPreferences.EMAIL_PREFERENCE.name(), "ASSIGNMENTS",
              userDTOWithNulls.getId())).andReturn(userPreference);
    } catch (SegueDatabaseException e1) {
      fail(e1);
    }
    replay(userPreferenceManager);

    ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
    String htmlContent = "hi {{givenName}}<br><br>This is a test";
    String plainTextContent = "hi {{givenName}}\n\nThis is a test";
    String subject = "Test email";

    EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
    emailTemplate.setHtmlContent(htmlContent);
    emailTemplate.setPlainTextContent(plainTextContent);
    emailTemplate.setSubject(subject);

    try {

      expect(mockContentManager.getContentById("email-template-html"))
          .andReturn(htmlTemplate).times(allSelectedUsers.size());

      expect(mockContentManager.getContentById("email-template-ascii"))
          .andReturn(htmlTemplate).times(allSelectedUsers.size());

      expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

      replay(mockContentManager);
    } catch (ContentManagerException e) {
      fail(e);
    }

    try {
      manager.sendCustomContentEmail(userDTOWithNulls, emailTemplate, allSelectedUsers, EmailType.ASSIGNMENTS);
    } catch (SegueDatabaseException | ContentManagerException e) {
      fail(e);
    }

  }

  /**
   * Make sure that when the templates are published:false, that the method reacts appropriately.
   */
  @Test
  void flattenTokenMap_checkTemplateReplacement_successfulReplacement() {
    EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
        mockContentManager, logManager, generateGlobalTokenMap());
    Instant someDate = Instant.now();

    Map<String, Object> inputMap = Maps.newHashMap();
    inputMap.put("test", "test2");
    inputMap.put("address", ImmutableMap.of("line1", "Computer Laboratory"));
    inputMap.put("date", someDate);
    Map<String, String> mapUnderTest = manager.flattenTokenMap(inputMap, Maps.newHashMap(), "");

    assertEquals("Computer Laboratory", mapUnderTest.get("address.line1"));
  }

  private Map<String, String> generateGlobalTokenMap() {
    Map<String, String> globalTokens = Maps.newHashMap();
    globalTokens.put("sig", "Isaac Computer Science Project");
    globalTokens.put("emailPreferencesURL", "https://test/assignments");

    return globalTokens;
  }

}
