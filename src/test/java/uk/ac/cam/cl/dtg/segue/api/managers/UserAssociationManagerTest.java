/**
 * Copyright 2014 Stephen Cummins and Nick Rogers
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
package uk.ac.cam.cl.dtg.segue.api.managers;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.UserGroupNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.associations.IAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dos.AssociationToken;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * Test class for the user Association class.
 * 
 */
@PowerMockIgnore({ "javax.ws.*" })
public class UserAssociationManagerTest {
	private IAssociationDataManager dummyAssociationDataManager;
	private GroupManager dummyGroupDataManager;

	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		dummyAssociationDataManager = createMock(IAssociationDataManager.class);
		dummyGroupDataManager = createMock(GroupManager.class);
	}

	/**
	 * Verify that the constructor responds correctly to bad input.
	 * 
	 * @throws SegueDatabaseException
	 * @throws UserGroupNotFoundException
	 */
	@Test
	public final void userAssociationManager_generateToken_tokenShouldBeCreatedAndPersisted()
			throws SegueDatabaseException, UserGroupNotFoundException {
		UserAssociationManager managerUnderTest = new UserAssociationManager(dummyAssociationDataManager,
				dummyGroupDataManager);

		String someUserId = "89745531132231213";

		RegisteredUserDTO someRegisteredUser = createMock(RegisteredUserDTO.class);
		String someAssociatedGroupId = "5654811fd6g51gd8r";

		expect(someRegisteredUser.getDbId()).andReturn(someUserId).anyTimes();
		replay(someRegisteredUser);

		AssociationToken someToken = new AssociationToken("someToken", someUserId, someAssociatedGroupId);
		
		expect(dummyGroupDataManager.isValidGroup(someAssociatedGroupId)).andReturn(true).once();
		
		expect(dummyAssociationDataManager.saveAssociationToken((AssociationToken) anyObject())).andReturn(
				someToken).once();
		replay(dummyAssociationDataManager, dummyGroupDataManager);

		AssociationToken someGeneratedToken = managerUnderTest.generateToken(someRegisteredUser,
				someAssociatedGroupId);

		assertTrue(someGeneratedToken != null);

		verify(someRegisteredUser, dummyAssociationDataManager);
	}

}
