/*
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.isaac.app;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.api.GameboardsFacade;
import uk.ac.cam.cl.dtg.isaac.api.managers.FastTrackManger;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the user manager class.
 * 
 */
public class GameboardsFacadeTest {

	private AbstractConfigLoader dummyPropertiesLoader = null;
	private GameManager dummyGameManager = null;
	private ILogManager dummyLogManager = null;
	private UserAccountManager userManager;
    private QuestionManager questionManager;
    private FastTrackManger fastTrackManager;

	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		this.dummyPropertiesLoader = createMock(AbstractConfigLoader.class);
		this.dummyGameManager = createMock(GameManager.class);
		this.dummyLogManager = createMock(ILogManager.class);
		this.userManager = createMock(UserAccountManager.class);
	    this.questionManager = createMock(QuestionManager.class);
		this.fastTrackManager = createMock(FastTrackManger.class);
		expect(this.dummyPropertiesLoader.getProperty(Constants.FASTTRACK_GAMEBOARD_WHITELIST))
				.andReturn("ft_board_1,ft_board_2").anyTimes();
		replay(this.dummyPropertiesLoader);
	}

	/**
	 * Verify that when an empty gameboard is noticed a 204 is returned.
	 * 
	 * @throws ContentManagerException
	 */
	@Test
	@PowerMockIgnore({ "jakarta.ws.*" })
	public final void isaacEndPoint_checkEmptyGameboardCausesErrorNoUser_SegueErrorResponseShouldBeReturned()
			throws SegueDatabaseException, ContentManagerException {
		GameboardsFacade gameboardFacade = new GameboardsFacade(
				dummyPropertiesLoader, dummyLogManager, dummyGameManager, questionManager,
				userManager, fastTrackManager);

		HttpServletRequest dummyRequest = createMock(HttpServletRequest.class);
		String subjects = "physics";
		String fields = "mechanics";
		String topics = "dynamics";
		String levels = "2,3,4";
		String concepts = "newtoni";
		String title = "Newton";
		String questionCategory = "problem_solving";
		String stages = "a_level";
		String difficulties = "practice_1";
		String examBoards = "wjec";

		expect(
				dummyGameManager.generateRandomGameboard(
						EasyMock.<String> anyObject(), EasyMock.<List<String>> anyObject(),
						EasyMock.<List<String>> anyObject(), EasyMock.<List<String>> anyObject(),
						EasyMock.<List<Integer>> anyObject(), EasyMock.<List<String>> anyObject(),
						EasyMock.<List<String>> anyObject(), EasyMock.<List<String>> anyObject(),
						EasyMock.<List<String>> anyObject(), EasyMock.<List<String>> anyObject(),
						EasyMock.<AbstractSegueUserDTO> anyObject()))
					.andReturn(null).atLeastOnce();

		expect(userManager.getCurrentUser(dummyRequest)).andReturn(new AnonymousUserDTO("testID"))
				.atLeastOnce();

		replay(dummyGameManager);

		Response r = gameboardFacade.generateTemporaryGameboard(dummyRequest, title, subjects, fields, topics,
				stages, difficulties, examBoards,levels, concepts, questionCategory);

		assertTrue(r.getStatus() == Status.NO_CONTENT.getStatusCode());
		verify(dummyGameManager);
	}
}
