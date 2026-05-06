/*
 * Copyright 2015 Alistair Stead
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.comm;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for the EmailManager class.
 * 
 */
public class EmailManagerTest {
    private final String CONTENT_VERSION = "liveVersion";

    private static final Logger log = LoggerFactory.getLogger(EmailManagerTest.class);
    private EmailCommunicator emailCommunicator;
    private RegisteredUser user;
    private RegisteredUserDTO userDTO;
    private RegisteredUserDTO userDTOWithNulls;
    private EmailCommunicationMessage email = null;
    private AbstractConfigLoader mockPropertiesLoader;
    private GitContentManager mockContentManager;
    private ArgumentCaptor<EmailCommunicationMessage> capturedArgument;
    private AbstractUserPreferenceManager userPreferenceManager;
    private ILogManager logManager;

    /**
     * Initial configuration of tests.
     * 
     * @throws Exception
     *             - test exception
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
        RegisteredUser userWithNulls = new RegisteredUser();
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
        emailCommunicator = mock(EmailCommunicator.class);

        // Create dummy email preferences
        userPreferenceManager = mock(PgUserPreferenceManager.class);

        mockPropertiesLoader = mock(AbstractConfigLoader.class);
        when(mockPropertiesLoader.getProperty("HOST_NAME")).thenReturn("dev.isaacphysics.org");
        when(mockPropertiesLoader.getProperty("REPLY_TO_ADDRESS")).thenReturn("test-reply@test.com");
        when(mockPropertiesLoader.getProperty("MAIL_FROM_ADDRESS")).thenReturn("no-reply@isaacphysics.org");
        when(mockPropertiesLoader.getProperty("MAIL_NAME")).thenReturn("Isaac Physics");

        // Create content manager
        mockContentManager = mock(GitContentManager.class);

        // Create log manager
        logManager = mock(ILogManager.class);

        capturedArgument = ArgumentCaptor.forClass(EmailCommunicationMessage.class);

        // Mock the emailCommunicator methods so we can see what is sent
        doNothing().when(emailCommunicator).sendMessage(capturedArgument.capture());

        SegueLocalAuthenticator mockAuthenticator = mock(SegueLocalAuthenticator.class);

        when(mockAuthenticator.createEmailVerificationTokenForUser(eq(user), eq(user.getEmail())))
                .thenAnswer(invocation -> {
                    user.setEmailVerificationToken("emailVerificationToken");
                    return user;
                });
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
    public final void sendTemplatedEmailToUser_checkForTemplateCompletion_emailShouldBeSentWithTemplateTagsFilledIn() throws CommunicationException {
        EmailTemplateDTO template = createDummyEmailTemplate("""
                Hi, {{givenName}}.\
                
                Thanks for registering!
                Your Isaac email address is: \
                </a href='mailto:{{email}}'>{{email}}<a>.
                address</a>
                {{sig}}""");

        ContentDTO htmlTemplate = createDummyContentTemplate("<!DOCTYPE html><html><head><meta charset='utf-8'>"
                + "<title>Isaac Physics project</title></head><body>" + "{{content}}" + "</body></html>");

        ContentDTO asciiTemplate = createDummyContentTemplate("{{content}}");
        try {
            when(mockContentManager.getContentById("email-template-registration-confirmation")).thenReturn(template);
            when(mockContentManager.getContentById("email-template-html")).thenReturn(htmlTemplate);
            when(mockContentManager.getContentById("email-template-ascii")).thenReturn(asciiTemplate);
            when(mockContentManager.getCurrentContentSHA()).thenReturn(CONTENT_VERSION);
        } catch (final ContentManagerException e) {
            e.printStackTrace();
            fail();
        }

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        try {
            ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL", "https://testUrl.com");
            manager.sendTemplatedEmailToUser(userDTO,
                    manager.getEmailTemplateDTO("email-template-registration-confirmation"),
                    emailTokens, EmailType.SYSTEM);
        } catch (ContentManagerException | SegueDatabaseException e) {
            e.printStackTrace();
            fail();
        }

        final String expectedMessagePlainText = """
                Hi, tester.\
                
                Thanks for registering!
                Your Isaac email address is: \
                </a href='mailto:test@test.com'>test@test.com<a>.
                address</a>
                Isaac Physics Project""";

        final String expectedMessageHTML = """
                <!DOCTYPE html><html><head><meta charset='utf-8'><title>Isaac \
                Physics project</title></head><body>\
                Hi, tester.
                Thanks for registering!
                Your Isaac email address is: \
                </a href='mailto:test@test.com'>test@test.com<a>.
                address</a>
                Isaac \
                Physics Project</body></html>""";

        // Wait for the emailQueue to spin up and send our message
        verify(emailCommunicator, timeout(500)).sendMessage(capturedArgument.capture());

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
    public final void sendFederatedPasswordReset_checkForTemplateCompletion_emailShouldBeSentWithTemplateTagsFilledIn() throws CommunicationException {

        EmailTemplateDTO template = createDummyEmailTemplate("""
                Hello, {{givenName}}.
                
                You requested a \
                password reset. However you use {{providerString}} to log in to our site. You need\
                 to go to your authentication {{providerWord}} to reset your password.
                
                Regards,
                
                {{sig}}""");

        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
        try {
            when(mockContentManager.getContentById("email-template-federated-password-reset")).thenReturn(template);
            when(mockContentManager.getContentById("email-template-html")).thenReturn(htmlTemplate);
            when(mockContentManager.getContentById("email-template-ascii")).thenReturn(htmlTemplate);
            when(mockContentManager.getCurrentContentSHA()).thenReturn(CONTENT_VERSION);
        } catch (ContentManagerException e) {
            e.printStackTrace();
            fail();
        }

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        try {
            Map<String, Object> emailTokens = ImmutableMap.of("providerString", "testString", "providerWord", "testWord");
            manager.sendTemplatedEmailToUser(userDTO,
                    manager.getEmailTemplateDTO("email-template-federated-password-reset"),
                    emailTokens, EmailType.SYSTEM);

        } catch (ContentManagerException | SegueDatabaseException e) {
            e.printStackTrace();
            fail();
            log.debug(e.getMessage());
        }

        final String expectedMessage = """
                Hello, tester.
                
                You requested a password reset. \
                However you use testString to log in to our site. You need to go to your \
                authentication testWord to reset your password.
                
                Regards,
                
                Isaac Physics Project""";

        // Wait for the emailQueue to spin up and send our message
        verify(emailCommunicator, timeout(500)).sendMessage(capturedArgument.capture());
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
    public final void sendPasswordReset_checkForTemplateCompletion_emailShouldBeSentWithTemplateTagsFilledIn() throws CommunicationException {

        EmailTemplateDTO template = createDummyEmailTemplate("""
                Hello, {{givenName}}.
                
                A request has been \
                made to reset the password for the account: </a href='mailto:{{email}}'>{{email}}<a>\
                .
                
                To reset your password <a href='{{resetURL}}'>Click Here</a>
                
                Regards,
                
                {{sig}}""");

        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");

        try {
            when(mockContentManager.getContentById("email-template-password-reset")).thenReturn(template);
            when(mockContentManager.getContentById("email-template-html")).thenReturn(htmlTemplate);
            when(mockContentManager.getContentById("email-template-ascii")).thenReturn(htmlTemplate);
            when(mockContentManager.getCurrentContentSHA()).thenReturn(CONTENT_VERSION);
        } catch (final ContentManagerException e) {
            e.printStackTrace();
            fail();
        }

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        try {
            Map<String, Object> emailValues = ImmutableMap.of("resetURL",
                    "https://dev.isaacphysics.org/resetpassword?token=resetToken");

            manager.sendTemplatedEmailToUser(userDTO,
                    manager.getEmailTemplateDTO("email-template-password-reset"),
                    emailValues, EmailType.SYSTEM);

        } catch (ContentManagerException | SegueDatabaseException e) {
            e.printStackTrace();
            fail();
        }

        final String expectedMessage = """
                Hello, tester.
                
                A request has been \
                made to reset the password for the account: </a href='mailto:test@test.com'>test@test.com<a>\
                .
                
                To reset your password <a href='https://dev.isaacphysics.org/resetpassword?token=resetToken'>\
                Click Here</a>
                
                Regards,
                
                Isaac Physics Project""";

        // Wait for the emailQueue to spin up and send our message
        verify(emailCommunicator, timeout(500)).sendMessage(capturedArgument.capture());
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
    @Test
    public final void sendRegistrationConfirmation_checkForInvalidTemplateTags_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            EmailTemplateDTO template = createDummyEmailTemplate("""
                    Hi, {{givenName}} {{surname}}.
                    Thanks for registering!
                    Your Isaac email address is: \
                    </a href='mailto:{{email}}'>{{email}}<a>.
                    address</a>
                    {{sig}}""");

            ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
            // Create content manager
            try {
                when(mockContentManager.getContentById("email-template-registration-confirmation")).thenReturn(template);
                when(mockContentManager.getContentById("email-template-html")).thenReturn(htmlTemplate);
                when(mockContentManager.getCurrentContentSHA()).thenReturn(CONTENT_VERSION);
            } catch (final ContentManagerException e) {
                e.printStackTrace();
                fail();
            }

            EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                    mockContentManager, logManager, generateGlobalTokenMap());
            try {
                ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL", "https://testUrl.com");

                manager.sendTemplatedEmailToUser(userDTO,
                        template,
                        emailTokens, EmailType.SYSTEM);

            } catch (ContentManagerException | SegueDatabaseException e) {
                e.printStackTrace();
                fail();
            }
        });
    }

    /**
     * Verify that the system responds correctly when there are fewer tags than expected in the email template.
     *
     * @throws CommunicationException
     */
    @Test
    public final void sendRegistrationConfirmation_checkTemplatesWithNoTagsWorks_emailIsGeneratedWithoutTemplateContent() throws CommunicationException {
        EmailTemplateDTO template = createDummyEmailTemplate("this is a template with no tags");

        // Create content manager
        GitContentManager mockContentManager = mock(GitContentManager.class);

        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
        try {
            when(mockContentManager.getContentById("email-template-registration-confirmation")).thenReturn(template);
            when(mockContentManager.getContentById("email-template-html")).thenReturn(htmlTemplate);
            when(mockContentManager.getContentById("email-template-ascii")).thenReturn(htmlTemplate);
            when(mockContentManager.getCurrentContentSHA()).thenReturn(CONTENT_VERSION);
        } catch (final ContentManagerException e) {
            e.printStackTrace();
            fail();
        }

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        try {
            ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL", "https://testUrl.com");

            manager.sendTemplatedEmailToUser(userDTO,
                    template,
                    emailTokens, EmailType.SYSTEM);
        } catch (ContentManagerException | SegueDatabaseException e) {
            e.printStackTrace();
            fail();
        }

        // Wait for the emailQueue to spin up and send our message
        verify(emailCommunicator, timeout(500)).sendMessage(capturedArgument.capture());
        email = capturedArgument.getValue();
        assertNotNull(email);
        assertEquals("this is a template with no tags", email.getPlainTextMessage());
        System.out.println(email.getPlainTextMessage());
    }

    /**
     * Make sure that when the templates are published:false, that the method reacts appropriately.
     *
     * @throws CommunicationException
     */
    @Test
    public void sendRegistrationConfirmation_checkNullContentDTO_exceptionThrownAndDealtWith() throws CommunicationException {
        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
        try {
            when(mockContentManager.getContentById("email-template-registration-confirmation")).thenReturn(null);
            when(mockContentManager.getContentById("email-template-html")).thenReturn(htmlTemplate);
            when(mockContentManager.getCurrentContentSHA()).thenReturn(CONTENT_VERSION);
        } catch (final ContentManagerException e) {
            e.printStackTrace();
            fail();
        }

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());
        try {

            ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL", "https://testUrl.com");
            manager.sendTemplatedEmailToUser(userDTO,
                    manager.getEmailTemplateDTO("email-template-registration-confirmation"),
                    emailTokens, EmailType.SYSTEM);

        } catch (final ContentManagerException e) {
            e.printStackTrace();
            fail();
        } catch (final SegueDatabaseException e) {
            e.printStackTrace();
            log.info(e.getMessage());
        }

        // Assert that the emailQueue never sends our message
        verify(emailCommunicator, after(500).never()).sendMessage(capturedArgument.capture());
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

        UserPreference userPreference = new UserPreference(userDTOWithNulls.getId(), SegueUserPreferences.EMAIL_PREFERENCE.name(),
                "ASSIGNMENTS", false);
        try {
            when(userPreferenceManager.getUserPreference(eq(SegueUserPreferences.EMAIL_PREFERENCE.name()),
                    eq("ASSIGNMENTS"), eq(userDTOWithNulls.getId()))).thenReturn(userPreference);
        } catch (final SegueDatabaseException e) {
            e.printStackTrace();
            fail();
        }

        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
        EmailTemplateDTO emailTemplate = createDummyEmailTemplate("Hello {{givenName}}, "
                + "how are you {{familyName}}? {{sig}}");
        String contentObjectId = "test-email-template";

        try {
            when(mockContentManager.getContentById(contentObjectId)).thenReturn(emailTemplate);
            when(mockContentManager.getContentById("email-template-html")).thenReturn(htmlTemplate);
            when(mockContentManager.getContentById("email-template-ascii")).thenReturn(htmlTemplate);
            when(mockContentManager.getCurrentContentSHA()).thenReturn(CONTENT_VERSION);

        } catch (ContentManagerException e) {
            e.printStackTrace();
            fail();
        }

        try {
            manager.sendCustomEmail(userDTOWithNulls, contentObjectId, allSelectedUsers, EmailType.ASSIGNMENTS);
        } catch (SegueDatabaseException | ContentManagerException e) {
            fail();
        }

    }

    /**
     * Check we don't send custom content emails to users with null / preference
     */
    @Test
    public void sendCustomContentEmail_checkNullProperties_replacedWithEmptyString() {

        EmailManager manager = new EmailManager(emailCommunicator, userPreferenceManager, mockPropertiesLoader,
                mockContentManager, logManager, generateGlobalTokenMap());

        List<RegisteredUserDTO> allSelectedUsers = Lists.newArrayList();
        allSelectedUsers.add(userDTOWithNulls);
        allSelectedUsers.add(userDTOWithNulls);

        UserPreference userPreference = new UserPreference(userDTOWithNulls.getId(), SegueUserPreferences.EMAIL_PREFERENCE.name(), "ASSIGNMENTS", false);
        try {
            when(userPreferenceManager.getUserPreference(eq(SegueUserPreferences.EMAIL_PREFERENCE.name()), eq("ASSIGNMENTS"), eq(userDTOWithNulls.getId())))
                    .thenReturn(userPreference);
        } catch (final SegueDatabaseException e) {
            e.printStackTrace();
            fail();
        }

        ContentDTO htmlTemplate = createDummyContentTemplate("{{content}}");
        String htmlContent = "hi {{givenName}}<br><br>This is a test";
        String plainTextContent = "hi {{givenName}}\n\nThis is a test";
        String subject = "Test email";

        EmailTemplateDTO emailTemplate = new EmailTemplateDTO();
        emailTemplate.setHtmlContent(htmlContent);
        emailTemplate.setPlainTextContent(plainTextContent);
        emailTemplate.setSubject(subject);

        try {
            when(mockContentManager.getContentById("email-template-html")).thenReturn(htmlTemplate);
            when(mockContentManager.getContentById("email-template-ascii")).thenReturn(htmlTemplate);
            when(mockContentManager.getCurrentContentSHA()).thenReturn(CONTENT_VERSION);
        } catch (final ContentManagerException e) {
            e.printStackTrace();
            fail();
        }

        try {
            manager.sendCustomContentEmail(userDTOWithNulls, emailTemplate, allSelectedUsers, EmailType.ASSIGNMENTS);
        } catch (SegueDatabaseException | ContentManagerException e) {
            fail();
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

        assert mapUnderTest.get("address.line1").equals("Computer Laboratory");
    }

    private Map<String, String> generateGlobalTokenMap() {
        Map<String, String> globalTokens = Maps.newHashMap();
        globalTokens.put("sig", "Isaac Physics Project");
        globalTokens.put("emailPreferencesURL", "https://test/assignments");

        return globalTokens;
    }

}
