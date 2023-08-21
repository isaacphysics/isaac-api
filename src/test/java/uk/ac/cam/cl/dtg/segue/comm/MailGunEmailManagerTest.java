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

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.mailgun.model.message.MessageResponse;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * Test class for the EmailManager class.
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(GitContentManager.class)
@PowerMockIgnore("javax.management.*")
public class MailGunEmailManagerTest {
    private AbstractConfigLoader mockPropertiesLoader;
    private AbstractUserPreferenceManager userPreferenceManager;
    private ILogManager logManager;

    /**
     * Initial configuration of tests.
     * 
     * @throws Exception
     *             - test exception
     */
    @Before
    public final void setUp() throws Exception {
        // Create dummy email preferences
        userPreferenceManager = EasyMock.createMock(PgUserPreferenceManager.class);

        mockPropertiesLoader = EasyMock.createMock(AbstractConfigLoader.class);
        EasyMock.expect(mockPropertiesLoader.getProperty("MAILGUN_SECRET_KEY")).andReturn("testKey").anyTimes();
        EasyMock.expect(mockPropertiesLoader.getProperty("MAILGUN_FROM_ADDRESS")).andReturn("no-reply@isaacphysics.org").anyTimes();
        EasyMock.expect(mockPropertiesLoader.getProperty("REPLY_TO_ADDRESS")).andReturn("test-reply@test.com").anyTimes();
        EasyMock.expect(mockPropertiesLoader.getProperty("MAIL_NAME")).andReturn("Isaac Physics").anyTimes();
        EasyMock.replay(mockPropertiesLoader);

        // Create log manager
        logManager = EasyMock.createMock(ILogManager.class);
        logManager.logInternalEvent(null, null, null);
        EasyMock.expectLastCall().anyTimes();
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

    @Test
    public final void sendBatchEmails_toNoUsers_shouldNotThrowAnException() {
        MailGunEmailManager mailGunEmailManager = new MailGunEmailManager(Maps.newHashMap(), mockPropertiesLoader, userPreferenceManager);
        Collection<RegisteredUserDTO> emptyListOfUsers = Lists.newArrayList();
        Future<Optional<MessageResponse>> response = mailGunEmailManager.sendBatchEmails(
                emptyListOfUsers, createDummyEmailTemplate("test"), EmailType.ASSIGNMENTS, null, null);
        assert (response.isDone());
        try {
            // The response should be empty as there were no users to send the email to.
            // TODO a truer test would mock the MailgunClient and check that the send method was never called,
            // but this would require altering the MailGunEmailManager class to allow the client to be injected.
            assert (response.get().isEmpty());
        } catch (Exception e) {
            assert (false);
        }
    }
}
