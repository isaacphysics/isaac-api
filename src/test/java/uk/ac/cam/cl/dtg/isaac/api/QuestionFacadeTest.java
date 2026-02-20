/*
 * Copyright 2021 Raspberry Pi Foundation
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
package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.isaac.dos.UserPreference;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatorUnavailableException;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.QuestionFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentSubclassMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response.Status;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class QuestionFacadeTest extends AbstractFacadeTest {

    private AbstractConfigLoader properties;
    private AbstractUserPreferenceManager userPreferenceManager;
    private IMisuseMonitor misuseMonitor;
    private QuestionFacade questionFacade;

    private void setUpQuestionFacade() throws ContentManagerException {
        Request requestForCaching = createMock(Request.class);
        expect(requestForCaching.evaluatePreconditions((EntityTag) anyObject())).andStubReturn(null);

        GitContentManager contentManager = createMock(GitContentManager.class);
        ILogManager logManager = createNiceMock(ILogManager.class); // We don't care about logging.
        ContentSubclassMapper contentSubclassMapper = createMock(ContentSubclassMapper.class);
        QuestionManager questionManager = createMock(QuestionManager.class);
        IUserStreaksManager userStreaksManager = createMock(IUserStreaksManager.class);
        UserAssociationManager userAssociationManager = createMock(UserAssociationManager.class);

        questionFacade = new QuestionFacade(properties, contentSubclassMapper, contentManager, userManager, userPreferenceManager,
                questionManager, logManager, misuseMonitor, userStreaksManager, userAssociationManager);

        String contentIndex = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";
        expect(contentManager.getCurrentContentSHA()).andStubReturn(contentIndex);
        expect(contentManager.getContentDOById(questionDO.getId())).andStubReturn(questionDO);
        expect(contentManager.getContentDOById(studentQuizDO.getId())).andStubReturn(studentQuizDO);
        expect(contentManager.getContentDOById(questionPageQuestionDO.getId())).andStubReturn(questionPageQuestionDO);

        replayAll();
    }

    @Test
    public void answerQuestionNotAvailableForQuizQuestions() throws ContentManagerException {
        // Arrange
        properties = createMock(AbstractConfigLoader.class);
        expect(properties.getProperty(Constants.SEGUE_APP_ENVIRONMENT)).andStubReturn(Constants.EnvironmentType.DEV.name());
        setUpQuestionFacade();

        // Act & Assert
        String jsonAnswer = "jsonAnswer";
        forEndpoint((questionId) -> () -> questionFacade.answerQuestion(httpServletRequest, questionId, jsonAnswer),
            with(question.getId(),
                beforeUserCheck(
                    failsWith(Status.FORBIDDEN)
                )
            )
        );
    }

    /*
        Test that a user with invalid config settings is unable to answer LLMFreeTextQuestions
    */
    @Test
    public final void assertUserCanAnswerLLMQuestions_InvalidSettings_ShouldThrowException() throws Exception {
        // Arrange
        properties = createMock(AbstractConfigLoader.class);
        expect(properties.getProperty(LLM_MARKER_FEATURE)).andReturn(("off"));

        setUpQuestionFacade();

        // Act & Assert
        ValidatorUnavailableException exception = assertThrows(
                ValidatorUnavailableException.class,
                () -> questionFacade.assertUserCanAnswerLLMQuestions(adminUser)
        );

        assertEquals("LLM marked questions are currently unavailable. Please try again later!", exception.getMessage());
    }

    /*
    Test that a non-consenting user is unable to answer LLMFreeTextQuestions
*/
    @Test
    public final void assertUserCanAnswerLLMQuestions_NonConsentingUser_ShouldThrowException() throws Exception {
        // Arrange
        properties = createMock(AbstractConfigLoader.class);
        expect(properties.getProperty(LLM_MARKER_FEATURE)).andReturn(("on"));

        userPreferenceManager = createMock(AbstractUserPreferenceManager.class);
        UserPreference userPreference = new UserPreference(adminUser.getId(), "CONSENT", "OPENAI", false);
        expect(userPreferenceManager.getUserPreference("CONSENT", LLM_PROVIDER_NAME, adminUser.getId())).andReturn(userPreference);

        setUpQuestionFacade();

        // Act & Assert
        QuestionFacade.NoUserConsentGrantedException exception = assertThrows(
                QuestionFacade.NoUserConsentGrantedException.class,
                () -> questionFacade.assertUserCanAnswerLLMQuestions(adminUser)
        );

        assertEquals(String.format("You must consent to sending your attempts to %s.", LLM_PROVIDER_NAME), exception.getMessage());
    }

    /*
        Test that a user with no available question attempts is unable to answer LLMFreeTextQuestions
    */
    @Test
    public final void assertUserCanAnswerLLMQuestions_NoUses_ShouldThrowException() throws Exception {
        // Arrange
        properties = createMock(AbstractConfigLoader.class);
        expect(properties.getProperty(LLM_MARKER_FEATURE)).andReturn(("on"));

        userPreferenceManager = createMock(AbstractUserPreferenceManager.class);
        UserPreference userPreference = new UserPreference(adminUser.getId(), "CONSENT", "OPENAI", true);
        expect(userPreferenceManager.getUserPreference("CONSENT", LLM_PROVIDER_NAME, adminUser.getId())).andReturn(userPreference);

        misuseMonitor = createMock(IMisuseMonitor.class);
        expect(misuseMonitor.getRemainingUses(adminUser.getId().toString(), "LLMFreeTextQuestionAttemptMisuseHandler")).andReturn(0);

        setUpQuestionFacade();

        // Act & Assert
        SegueResourceMisuseException exception = assertThrows(
                SegueResourceMisuseException.class,
                () -> questionFacade.assertUserCanAnswerLLMQuestions(adminUser)
        );

        assertEquals("You have exceeded the number of attempts you can make on LLM marked free-text questions. Please try again later.", exception.getMessage());
    }

    /*
        Test that a consenting user with valid config settings is able to answer LLMFreeTextQuestions
    */
    @Test
    public final void assertUserCanAnswerLLMQuestions_ConsentingUser_ShouldReturnOkayResponse() throws Exception {
        // Arrange
        properties = createMock(AbstractConfigLoader.class);
        expect(properties.getProperty(LLM_MARKER_FEATURE)).andReturn(("on"));
        misuseMonitor = createMock(IMisuseMonitor.class);
        expect(misuseMonitor.getRemainingUses(adminUser.getId().toString(), "LLMFreeTextQuestionAttemptMisuseHandler")).andReturn(30);
        userPreferenceManager = createMock(AbstractUserPreferenceManager.class);
        UserPreference userPreference = new UserPreference(adminUser.getId(), "CONSENT", "OPENAI", true);
        expect(userPreferenceManager.getUserPreference("CONSENT", LLM_PROVIDER_NAME, adminUser.getId())).andReturn(userPreference);

        setUpQuestionFacade();

        // Act
        RegisteredUserDTO outUser = questionFacade.assertUserCanAnswerLLMQuestions(adminUser);

        // Assert
        assertThat(outUser, instanceOf(RegisteredUserDTO.class));
        assertEquals(adminUser.getId(), outUser.getId());
    }
}
