/**
 * Copyright 2015 Alistair Stead
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.comm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;

import uk.ac.cam.cl.dtg.segue.api.Constants.SegueUserPreferences;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.segue.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.segue.dos.UserPreference;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Test class for the EmailManager class.
 * 
 */
public class EmailManagerTest {
    private final String CONTENT_VERSION = "liveVersion";

    private EmailCommunicator emailCommunicator;
    private RegisteredUser user;
    private RegisteredUser userWithNulls;
    private RegisteredUserDTO userDTO;
    private RegisteredUserDTO userDTOWithNulls;
    private static final Logger log = LoggerFactory.getLogger(EmailManagerTest.class);
    private EmailCommunicationMessage email = null;
    private PropertiesLoader mockPropertiesLoader;
    private IContentManager mockContentManager;
    private Capture<EmailCommunicationMessage> capturedArgument;
    private SegueLocalAuthenticator mockAuthenticator;
    private AbstractUserPreferenceManager userPreferenceManager;
    private ILogManager logManager;
    private UserAccountManager userManager;

    /**
     * Initial configuration of tests.
     * 
     * @throws Exception
     *             - test exception
     */
    @Before
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
        emailCommunicator = EasyMock.createMock(EmailCommunicator.class);

        // Create dummy email preferences
        userPreferenceManager = EasyMock.createMock(PgUserPreferenceManager.class);

        mockPropertiesLoader = EasyMock.createMock(PropertiesLoader.class);
        EasyMock.expect(mockPropertiesLoader.getProperty("HOST_NAME")).andReturn("dev.isaacphysics.org").anyTimes();
        EasyMock.expect(mockPropertiesLoader.getProperty("REPLY_TO_ADDRESS")).andReturn("test-reply@test.com")
                .anyTimes();
        EasyMock.expect(mockPropertiesLoader.getProperty("MAIL_NAME")).andReturn("Isaac Physics")
                .anyTimes();

        EasyMock.replay(mockPropertiesLoader);


        // Create content manager
        mockContentManager = EasyMock.createMock(IContentManager.class);

        // Create log manager
        logManager = EasyMock.createMock(ILogManager.class);
        logManager.logInternalEvent(null, null, null);
        EasyMock.expectLastCall().anyTimes();

        // Create user manager
        userManager = EasyMock.createMock(UserAccountManager.class);

        capturedArgument = new Capture<EmailCommunicationMessage>();

        // Mock the emailCommunicator methods so we can see what is sent
        try {
            emailCommunicator.sendMessage(EasyMock.and(EasyMock.capture(capturedArgument),
                    EasyMock.isA(EmailCommunicationMessage.class)));
        } catch (CommunicationException e1) {
            e1.printStackTrace();
            Assert.fail();
        }

        EasyMock.replay(emailCommunicator);
        System.out.println("setup");

        mockAuthenticator = EasyMock.createMock(SegueLocalAuthenticator.class);

        EasyMock.expect(mockAuthenticator.createEmailVerificationTokenForUser(user, user.getEmail())).andAnswer(
                new IAnswer<RegisteredUser>() {

                    @Override
                    public RegisteredUser answer() throws Throwable {
                        user.setEmailVerificationToken("emailVerificationToken");
                        return user;
                    }

                });

        EasyMock.replay(mockAuthenticator);
    }

    /**
     * @param template
     *            - id of the template
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
     * @param template
     *            - id of the template
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
     * 
     * @throws CommunicationException
     */
    @Test
    public final void sendTemplatedEmailToUser_checkForTemplateCompletion_emailShouldBeSentWithTemplateTagsFilledIn() {
        EasyMock.replay(userManager);

        EmailTemplateDTO template = createDummyEmailTemplate("Hi, {{givenname}}."
                + "\nThanks for registering!\nYour Isaac email address is: "
                + "</a href='mailto:{{email}}'>{{email}}<a>.\naddress</a>\n{{sig}}");

        ContentDTO htmlTemplate = createDummyContentTemplate("<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<title>Isaac Physics project</title></head><body>" + "{{content}}" + "</body></html>");

        ContentDTO asciiTemplate = createDummyContentTemplate("{{content}}");
        try {
            EasyMock.expect(
                    mockContentManager.getContentById(CONTENT_VERSION, "email-template-registration-confirmation"))
                    .andReturn(template);

            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-html")).andReturn(
                    htmlTemplate);
            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-ascii")).andReturn(
                    asciiTemplate);

            EasyMock.expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

            EasyMock.replay(mockContentManager);

        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        }

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        try {
            ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL", "https://testUrl.com");
            manager.sendTemplatedEmailToUser(userDTO,
                    manager.getEmailTemplateDTO("email-template-registration-confirmation"),
                    emailTokens, EmailType.SYSTEM);
        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
            Assert.fail();
        }

        final String expectedMessagePlainText = "Hi, tester."
                + "\nThanks for registering!\nYour Isaac email address is: "
                + "</a href='mailto:test@test.com'>test@test.com<a>.\naddress</a>\nIsaac Physics Project";

        final String expectedMessageHTML = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>Isaac "
                + "Physics project</title></head><body>"
                + "Hi, tester.\nThanks for registering!\nYour Isaac email address is: "
                + "</a href='mailto:test@test.com'>test@test.com<a>.\n" + "address</a>\nIsaac "
                + "Physics Project</body></html>";

        // Wait for the emailQueue to spin up and send our message
        int i = 0;
        while (!capturedArgument.hasCaptured() && i < 5) {
            try {
                Thread.sleep(100);
                i++;
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
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
     * 
     * @throws CommunicationException
     */
    @Test
    public final void sendFederatedPasswordReset_checkForTemplateCompletion_emailShouldBeSentWithTemplateTagsFilledIn() {

        EmailTemplateDTO template = createDummyEmailTemplate("Hello, {{givenname}}.\n\nYou requested a "
                + "password reset. However you use {{providerString}} to log in to our site. You need"
                + " to go to your authentication {{providerWord}} to reset your password.\n\nRegards,\n\n{{sig}}");

        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
        try {
            EasyMock.expect(
                    mockContentManager.getContentById(CONTENT_VERSION, "email-template-federated-password-reset"))
                    .andReturn(template);

            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-html")).andReturn(
                    htmlTemplate);

            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-ascii")).andReturn(
                    htmlTemplate);

            EasyMock.expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

            EasyMock.replay(mockContentManager);
        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        }

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        try {
            Map<String, Object> emailTokens = ImmutableMap.of("providerString", "testString", "providerWord", "testWord");
            manager.sendTemplatedEmailToUser(userDTO,
                    manager.getEmailTemplateDTO("email-template-federated-password-reset"),
                    emailTokens, EmailType.SYSTEM);

        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
            log.debug(e.getMessage());
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
            log.debug(e.getMessage());
            Assert.fail();
        }

        final String expectedMessage = "Hello, tester.\n\nYou requested a password reset. "
                + "However you use testString to log in to our site. You need to go to your "
                + "authentication testWord to reset your password.\n\nRegards,\n\nIsaac Physics Project";

        // Wait for the emailQueue to spin up and send our message
        int i = 0;
        while (!capturedArgument.hasCaptured() && i < 5) {
            try {
                Thread.sleep(100);
                i++;
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }
        }
        email = capturedArgument.getValue();
        assertNotNull(email);
        assertEquals(expectedMessage, email.getPlainTextMessage());
        System.out.println(email.getPlainTextMessage());

    }

    /**
     * Verifies that email templates are parsed and replaced correctly.
     * 
     * @throws CommunicationException
     */
    @Test
    public final void sendPasswordReset_checkForTemplateCompletion_emailShouldBeSentWithTemplateTagsFilledIn() {

        EmailTemplateDTO template = createDummyEmailTemplate("Hello, {{givenname}}.\n\nA request has been "
                + "made to reset the password for the account: </a href='mailto:{{email}}'>{{email}}<a>"
                + ".\n\nTo reset your password <a href='{{resetURL}}'>Click Here</a>\n\nRegards,\n\n{{sig}}");

        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");

        try {
            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-password-reset"))
                    .andReturn(template).once();

            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-html"))
                    .andReturn(htmlTemplate).once();

            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-ascii")).andReturn(
                    htmlTemplate);

            EasyMock.expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

            EasyMock.replay(mockContentManager);

        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        }

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        try {
            Map<String, Object> emailValues = ImmutableMap.of("resetURL",
                    "https://dev.isaacphysics.org/resetpassword/resetToken");

            manager.sendTemplatedEmailToUser(userDTO,
                    manager.getEmailTemplateDTO("email-template-password-reset"),
                    emailValues, EmailType.SYSTEM);

        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
            Assert.fail();
        }

        final String expectedMessage = "Hello, tester.\n\nA request has been "
                + "made to reset the password for the account: </a href='mailto:test@test.com'>test@test.com<a>"
                + ".\n\nTo reset your password <a href='https://dev.isaacphysics.org/resetpassword/resetToken'>"
                + "Click Here</a>\n\nRegards,\n\nIsaac Physics Project";

        // Wait for the emailQueue to spin up and send our message
        int i = 0;
        while (!capturedArgument.hasCaptured() && i < 5) {
            try {
                Thread.sleep(100);
                i++;
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }
        }
        email = capturedArgument.getValue();
        assertNotNull(email);

        assertEquals(expectedMessage, email.getPlainTextMessage());
    }

    /**
     * Verify that if there are extra tags the system doesn't recognise, there will be an exception, and the email won't
     * be sent.
     * 
     * @throws CommunicationException
     */
    @Test(expected = IllegalArgumentException.class)
    public final void sendRegistrationConfirmation_checkForInvalidTemplateTags_throwIllegalArgumentException() {
        EmailTemplateDTO template = createDummyEmailTemplate("Hi, {{givenname}} {{surname}}.\n"
                + "Thanks for registering!\nYour Isaac email address is: "
                + "</a href='mailto:{{email}}'>{{email}}<a>.\naddress</a>\n{{sig}}");

        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
        // Create content manager
        try {
            EasyMock.expect(
                    mockContentManager.getContentById(CONTENT_VERSION, "email-template-registration-confirmation"))
                    .andReturn(template).once();

            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-html"))
                    .andReturn(htmlTemplate).once();

            EasyMock.expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

            EasyMock.replay(mockContentManager);

        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        }

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        try {
            ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL", "https://testUrl.com");

            manager.sendTemplatedEmailToUser(userDTO,
                    template,
                    emailTokens, EmailType.SYSTEM);

        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    /**
     * Verify that the system responds correctly when there are fewer tags than expected in the email template.
     */
    @Test
    public final void sendRegistrationConfirmation_checkTemplatesWithNoTagsWorks_emailIsGeneratedWithoutTemplateContent() {
        EmailTemplateDTO template = createDummyEmailTemplate("this is a template with no tags");

        // Create content manager
        IContentManager mockContentManager = EasyMock.createMock(IContentManager.class);

        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
        try {
            EasyMock.expect(
                    mockContentManager.getContentById(CONTENT_VERSION, "email-template-registration-confirmation"))
                    .andReturn(template);

            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-ascii")).andReturn(
                    htmlTemplate);

            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-html")).andReturn(
                    htmlTemplate);

            EasyMock.expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

            EasyMock.replay(mockContentManager);
        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        }

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        try {
            ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL", "https://testUrl.com");

            manager.sendTemplatedEmailToUser(userDTO,
                    template,
                    emailTokens, EmailType.SYSTEM);
        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
            Assert.fail();
        }

        // Wait for the emailQueue to spin up and send our message
        int i = 0;
        while (!capturedArgument.hasCaptured() && i < 5) {
            try {
                Thread.sleep(100);
                i++;
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
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
    public void sendRegistrationConfirmation_checkNullContentDTO_exceptionThrownAndDealtWith() {
        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
        try {
            EasyMock.expect(
                    mockContentManager.getContentById(CONTENT_VERSION, "email-template-registration-confirmation"))
                    .andReturn(null);

            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-html")).andReturn(
                    htmlTemplate);

            EasyMock.expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

            EasyMock.replay(mockContentManager);
        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        }

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        try {

            ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL", "https://testUrl.com");
            manager.sendTemplatedEmailToUser(userDTO,
                    manager.getEmailTemplateDTO("email-template-registration-confirmation"),
                    emailTokens, EmailType.SYSTEM);

        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
            log.info(e.getMessage());
        }

        // Wait for the emailQueue to spin up and send our message (if it exists)
        int i = 0;
        while (!capturedArgument.hasCaptured() && i < 5) {
            try {
                Thread.sleep(100);
                i++;
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }
        }
        // We expect there to be nothing captured because the content was not returned
        assertFalse(capturedArgument.hasCaptured());
    }

    /**
     * Make sure that when the templates are published:false, that the method reacts appropriately.
     */
    @Test
    public void sendCustomEmail_checkNullProperties_replacedWithEmptyString() {

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());

        List<RegisteredUserDTO> allSelectedUsers = Lists.newArrayList();
        allSelectedUsers.add(userDTOWithNulls);
        allSelectedUsers.add(userDTOWithNulls);

        UserPreference userPreference = new UserPreference(userDTOWithNulls.getId(), SegueUserPreferences.EMAIL_PREFERENCE.name(), "ASSIGNMENTS", false);
        try {
            EasyMock.expect(userPreferenceManager.getUserPreference(SegueUserPreferences.EMAIL_PREFERENCE.name(), "ASSIGNMENTS", userDTOWithNulls.getId())).andReturn(userPreference);
            EasyMock.expect(userPreferenceManager.getUserPreference(SegueUserPreferences.EMAIL_PREFERENCE.name(), "ASSIGNMENTS", userDTOWithNulls.getId())).andReturn(userPreference);
        } catch (SegueDatabaseException e1) {
            e1.printStackTrace();
            Assert.fail();
        }
        EasyMock.replay(userPreferenceManager);

        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
        EmailTemplateDTO emailTemplate = createDummyEmailTemplate("Hello {{givenname}}, "
                + "how are you {{familyname}}? {{sig}}");
        String contentObjectId = "test-email-template";

        try {
            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, contentObjectId)).andReturn(
                    emailTemplate);

            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-html"))
                    .andReturn(htmlTemplate).times(allSelectedUsers.size());

            EasyMock.expect(mockContentManager.getContentById(CONTENT_VERSION, "email-template-ascii"))
                    .andReturn(htmlTemplate).times(allSelectedUsers.size());

            EasyMock.expect(mockContentManager.getCurrentContentSHA()).andReturn(CONTENT_VERSION).atLeastOnce();

            EasyMock.replay(mockContentManager);
        } catch (ContentManagerException e) {
            e.printStackTrace();
            Assert.fail();
        }

        try {
            manager.sendCustomEmail(userDTOWithNulls, contentObjectId, allSelectedUsers, EmailType.ASSIGNMENTS);
        } catch (SegueDatabaseException e) {
            Assert.fail();
        } catch (ContentManagerException e) {
            Assert.fail();
        }

    }

    /**
     * Make sure that when the templates are published:false, that the method reacts appropriately.
     */
    @Test
    public void flattenTokenMap_checkTemplateReplacement_successfulReplacement() {
        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        Date someDate = new Date();

        Map<String, Object> inputMap = Maps.newHashMap();
        inputMap.put("test", "test2");
        inputMap.put("address", ImmutableMap.of("line1", "Computer Laboratory"));
        inputMap.put("date", someDate);
        Map<String, String> mapUnderTest = manager.flattenTokenMap(inputMap, Maps.newHashMap(), "");

        assert(mapUnderTest.get("address.line1").equals("Computer Laboratory"));
    }

    private Map generateGlobalTokenMap() {
        Map<String, String> globalTokens = Maps.newHashMap();
        globalTokens.put("sig", "Isaac Physics Project");
        globalTokens.put("emailPreferencesURL", "https://test/assignments");

        return globalTokens;
    }

}
