/*
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

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dao.IAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dao.PgAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.Date;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
     */
	@Before
	public final void setUp() {
		this.dummyGroupManager = createMock(GroupManager.class);
		this.dummyAssignmentPersistenceManager = createMock(PgAssignmentPersistenceManager.class);
        this.dummyEmailManager = createMock(EmailManager.class);
        this.dummyUserManager = createMock(UserAccountManager.class);
        this.dummyGameManager = createMock(GameManager.class);
        this.userAssociationManager = createMock(UserAssociationManager.class);
		this.dummyPropertiesLoader = createMock(PropertiesLoader.class);
	}

    /**
     * Test teacher user name extraction.
     */
    @Test
    public final void testGetTeacherNameFromUser() throws Exception {

        AssignmentManager am = new AssignmentManager(dummyAssignmentPersistenceManager, dummyGroupManager,
                dummyEmailManager, dummyUserManager, dummyGameManager, userAssociationManager, dummyPropertiesLoader);

        // Check case with both first and last name:
        RegisteredUserDTO dummyUserFirstLast = new RegisteredUserDTO();
        dummyUserFirstLast.setGivenName("FirstName");
        dummyUserFirstLast.setFamilyName("LastName");
        String shortNameFirstLast = Whitebox.invokeMethod(am, "getTeacherNameFromUser", dummyUserFirstLast);
        assertEquals("F. LastName", shortNameFirstLast);

        // Check case with no first name:
        RegisteredUserDTO dummyUserNoFirst = new RegisteredUserDTO();
        dummyUserNoFirst.setGivenName("");
        dummyUserNoFirst.setFamilyName("LastName");
        String shortNameNoFirst = Whitebox.invokeMethod(am, "getTeacherNameFromUser", dummyUserNoFirst);
        assertEquals("LastName", shortNameNoFirst);

        UserSummaryDTO dummyUserSummary = new UserSummaryDTO();
        dummyUserSummary.setGivenName("FirstName");
        dummyUserSummary.setFamilyName("LastName");
        String shortNameUserSummary = Whitebox.invokeMethod(am, "getTeacherNameFromUser", dummyUserSummary);
        assertEquals("F. LastName", shortNameUserSummary);
    }

    /**
     * Test teacher user name extraction.
     */
    @Test
    public final void testGetFilteredGroupNameFromGroup() throws Exception {

        AssignmentManager am = new AssignmentManager(dummyAssignmentPersistenceManager, dummyGroupManager,
                dummyEmailManager, dummyUserManager, dummyGameManager, userAssociationManager, dummyPropertiesLoader);

        String groupName = "Group Name";

        // Check case with shared group name:
        UserGroupDTO dummyGroup = new UserGroupDTO();
        dummyGroup.setLastUpdated(new Date());
        dummyGroup.setGroupName(groupName);
        String filteredGroupName = Whitebox.invokeMethod(am, "getFilteredGroupNameFromGroup", dummyGroup);
        assertEquals(filteredGroupName, groupName);

        // Check case without shared group name:
        UserGroupDTO dummyGroupNotSharedName = new UserGroupDTO();
        dummyGroupNotSharedName.setGroupName(groupName);
        String filteredGroupNameNotShared = Whitebox.invokeMethod(am, "getFilteredGroupNameFromGroup", dummyGroupNotSharedName);
        assertFalse("Should not shared group name!", groupName.equals(filteredGroupNameNotShared));
    }
}
