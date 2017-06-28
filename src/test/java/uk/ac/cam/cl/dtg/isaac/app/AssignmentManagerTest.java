/**
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.isaac.app;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dao.IAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Test class for the user manager class.
 * 
 */
public class AssignmentManagerTest {
	private IAssignmentPersistenceManager dummyAssignmentPersistenceManager;
	private GroupManager dummyGroupManager;
    private EmailManager dummyEmailManager;
    private UserAccountManager dummyUserManager;
    private GameManager dummyGameManager;
    private UserAssociationManager userAssociationManager;
	private PropertiesLoader dummyPropertiesLoader;

	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		this.dummyGroupManager = createMock(GroupManager.class);
		this.dummyAssignmentPersistenceManager = createMock(PgAssignmentPersistenceManager.class);
        this.dummyEmailManager = createMock(EmailManager.class);
        this.dummyUserManager = createMock(UserAccountManager.class);
        this.dummyGameManager = createMock(GameManager.class);
        this.userAssociationManager = createMock(UserAssociationManager.class);
		this.dummyPropertiesLoader = createMock(PropertiesLoader.class);
	}

	/**
	 * Verify that when an empty gameboard is noticed a 204 is returned.
	 * @throws SegueDatabaseException - if an error occurs
	 * 
	 * @throws NoUserLoggedInException
	 * @throws ContentManagerException
	 */
	@Test
	@PowerMockIgnore({ "javax.ws.*" })
	public final void getAssignments_checkNoGroups_emptyListReturned() throws SegueDatabaseException {
		
        AssignmentManager am = new AssignmentManager(dummyAssignmentPersistenceManager, dummyGroupManager,
                dummyEmailManager, dummyUserManager, dummyGameManager, null, dummyPropertiesLoader);
		RegisteredUserDTO dummyUser = createMock(RegisteredUserDTO.class);
		
        expect(dummyGroupManager.getGroupMembershipList(dummyUser)).andReturn(new ArrayList<UserGroupDTO>());

        replay(dummyGroupManager);
		
		Collection<AssignmentDTO> assignments = am.getAssignments(dummyUser);
		
		assertTrue(assignments != null);
		assertTrue(assignments.size() == 0);
	}
}
