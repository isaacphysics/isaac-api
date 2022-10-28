/*
 * Copyright 2021 Raspberry Pi Foundation
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
package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.QuestionFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response.Status;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GitContentManager.class)
@PowerMockIgnore("javax.management.*")
public class QuestionFacadeTest extends AbstractFacadeTest {

    private QuestionFacade questionFacade;

    private Request requestForCaching;

    private QuestionManager questionManager;

    @Before
    public void setUp() throws ContentManagerException {
        requestForCaching = createMock(Request.class);
        expect(requestForCaching.evaluatePreconditions((EntityTag) anyObject())).andStubReturn(null);

        PropertiesLoader properties = createMock(PropertiesLoader.class);
        expect(properties.getProperty(Constants.SEGUE_APP_ENVIRONMENT)).andStubReturn(Constants.EnvironmentType.DEV.name());

        ILogManager logManager = createNiceMock(ILogManager.class); // We don't care about logging.
        GitContentManager contentManager = createMock(GitContentManager.class);
        ContentMapper contentMapper = createMock(ContentMapper.class);

        String contentIndex = "4b825dc642cb6eb9a060e54bf8d69288fbee4904";
        IMisuseMonitor misuseMonitor = createMock(IMisuseMonitor.class);
        UserBadgeManager userBadgeManager = createMock(UserBadgeManager.class);
        IUserStreaksManager userStreaksManager = createMock(IUserStreaksManager.class);
        UserAssociationManager userAssociationManager = createMock(UserAssociationManager.class);

        questionManager = createMock(QuestionManager.class);

        questionFacade = new QuestionFacade(properties, contentMapper, contentManager, contentIndex,
            userManager, questionManager, logManager, misuseMonitor, userBadgeManager, userStreaksManager, userAssociationManager);

        expect(contentManager.getCurrentContentSHA()).andStubReturn(contentIndex);
        expect(contentManager.getContentDOById(questionDO.getId())).andStubReturn(questionDO);
        expect(contentManager.getContentDOById(studentQuizDO.getId())).andStubReturn(studentQuizDO);
        expect(contentManager.getContentDOById(questionPageQuestionDO.getId())).andStubReturn(questionPageQuestionDO);

        replayAll();
    }

    @Test
    public void answerQuestionNotAvailableForQuizQuestions() {
        String jsonAnswer = "jsonAnswer";
        forEndpoint((questionId) -> () -> questionFacade.answerQuestion(httpServletRequest, questionId, jsonAnswer),
            with(question.getId(),
                beforeUserCheck(
                    failsWith(Status.FORBIDDEN)
                )
            )
        );
    }
}
