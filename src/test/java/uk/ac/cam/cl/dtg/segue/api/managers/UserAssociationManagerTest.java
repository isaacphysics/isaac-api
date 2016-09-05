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
import uk.ac.cam.cl.dtg.segue.dao.associations.InvalidUserAssociationTokenException;
import uk.ac.cam.cl.dtg.segue.dao.associations.UserGroupNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.associations.IAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dos.AssociationToken;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

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

		Long someUserId = 89745531132231213L;

		RegisteredUserDTO someRegisteredUser = createMock(RegisteredUserDTO.class);
		Long someAssociatedGroupId = 565481L;

		expect(someRegisteredUser.getId()).andReturn(someUserId).anyTimes();
		replay(someRegisteredUser);

		AssociationToken someToken = new AssociationToken("someToken", someUserId, someAssociatedGroupId);
		
		expect(dummyGroupDataManager.isValidGroup(someAssociatedGroupId)).andReturn(true).once();
		
		expect(dummyAssociationDataManager.saveAssociationToken((AssociationToken) anyObject())).andReturn(
				someToken).once();
		
		expect(dummyAssociationDataManager.getAssociationTokenByGroupId(anyLong())).andReturn(null);
		
		replay(dummyAssociationDataManager, dummyGroupDataManager);

		AssociationToken someGeneratedToken = managerUnderTest.generateAssociationToken(someRegisteredUser,
				someAssociatedGroupId);

		assertTrue(someGeneratedToken != null);

		verify(someRegisteredUser, dummyAssociationDataManager);
	}
	
	@Test
	public final void userAssociationManager_createAssociationWithTokenAndAddToGroup_associationShouldBeCreatedAndUserAddedToGroup()
			throws SegueDatabaseException, UserGroupNotFoundException {
		UserAssociationManager managerUnderTest = new UserAssociationManager(dummyAssociationDataManager,
				dummyGroupDataManager);

		Long someUserIdGrantingAccess = 89745531132231213L;
		Long someGroupOwnerUserId = 17659214141L;

		RegisteredUserDTO someRegisteredUserGrantingAccess = createMock(RegisteredUserDTO.class);
		RegisteredUserDTO someRegisteredUserReceivingAccess = createMock(RegisteredUserDTO.class);
		Long someAssociatedGroupId = 56548L;

		expect(someRegisteredUserGrantingAccess.getId()).andReturn(someUserIdGrantingAccess).anyTimes();
		
		expect(someRegisteredUserReceivingAccess.getId()).andReturn(someGroupOwnerUserId).anyTimes();
		replay(someRegisteredUserGrantingAccess);

		AssociationToken someToken = new AssociationToken("someToken", someGroupOwnerUserId, someAssociatedGroupId);
		
		expect(dummyAssociationDataManager.lookupAssociationToken(someToken.getToken())).andReturn(someToken);
		
		expect(
				dummyAssociationDataManager.hasValidAssociation(someGroupOwnerUserId,
						someUserIdGrantingAccess)).andReturn(false);
		
		dummyAssociationDataManager.createAssociation(someToken, someUserIdGrantingAccess);
		expectLastCall().once();
		
		UserGroupDTO groupToAddUserTo = createMock(UserGroupDTO.class);
		expect(dummyGroupDataManager.getGroupById(someAssociatedGroupId)).andReturn(groupToAddUserTo).once();
		
		dummyGroupDataManager.addUserToGroup(groupToAddUserTo, someRegisteredUserGrantingAccess);
		expectLastCall().once();
		
		replay(dummyAssociationDataManager, dummyGroupDataManager);

		try {
			managerUnderTest.createAssociationWithToken(someToken.getToken(), someRegisteredUserGrantingAccess);
		} catch (InvalidUserAssociationTokenException e) {
			e.printStackTrace();
			fail("InvalidUserAssociationTokenException is unexpected");
		}

		verify(someRegisteredUserGrantingAccess, dummyAssociationDataManager);
	}
	
	@Test
	public final void userAssociationManager_DuplicateAssociationButAddToGroupAnyway_associationShouldNotBeCreatedButUserShouldBeAddedToGroup()
			throws SegueDatabaseException, UserGroupNotFoundException {
		UserAssociationManager managerUnderTest = new UserAssociationManager(dummyAssociationDataManager,
				dummyGroupDataManager);

		Long someUserIdGrantingAccess = 89745531132231213L;
		Long someGroupOwnerUserId = 17659214141L;

		RegisteredUserDTO someRegisteredUserGrantingAccess = createMock(RegisteredUserDTO.class);
		RegisteredUserDTO someRegisteredUserReceivingAccess = createMock(RegisteredUserDTO.class);
		Long someAssociatedGroupId = 5654811L;

		expect(someRegisteredUserGrantingAccess.getId()).andReturn(someUserIdGrantingAccess).anyTimes();
		
		expect(someRegisteredUserReceivingAccess.getId()).andReturn(someGroupOwnerUserId).anyTimes();
		replay(someRegisteredUserGrantingAccess);

		AssociationToken someToken = new AssociationToken("someToken", someGroupOwnerUserId, someAssociatedGroupId);
		
		expect(dummyAssociationDataManager.lookupAssociationToken(someToken.getToken())).andReturn(someToken);
		
		expect(
				dummyAssociationDataManager.hasValidAssociation(someGroupOwnerUserId,
						someUserIdGrantingAccess)).andReturn(true);
		
		UserGroupDTO groupToAddUserTo = createMock(UserGroupDTO.class);
		expect(dummyGroupDataManager.getGroupById(someAssociatedGroupId)).andReturn(groupToAddUserTo).once();
		
		dummyGroupDataManager.addUserToGroup(groupToAddUserTo, someRegisteredUserGrantingAccess);
		expectLastCall().once();
		
		replay(dummyAssociationDataManager, dummyGroupDataManager);

		try {
			managerUnderTest.createAssociationWithToken(someToken.getToken(), someRegisteredUserGrantingAccess);
		} catch (InvalidUserAssociationTokenException e) {
			e.printStackTrace();
			fail("InvalidUserAssociationTokenException is unexpected");
		}

		verify(someRegisteredUserGrantingAccess, dummyAssociationDataManager);
	}	
	
	@Test
	public final void userAssociationManager_createAssociationWithTokenNoGroup_associationShouldBeCreated()
			throws SegueDatabaseException, UserGroupNotFoundException {
		UserAssociationManager managerUnderTest = new UserAssociationManager(dummyAssociationDataManager,
				dummyGroupDataManager);

		Long someUserIdGrantingAccess = 89745531132231213L;
		Long someGroupOwnerUserId = 17659214141L;

		RegisteredUserDTO someRegisteredUserGrantingAccess = createMock(RegisteredUserDTO.class);
		RegisteredUserDTO someRegisteredUserReceivingAccess = createMock(RegisteredUserDTO.class);
		Long someAssociatedGroupId = null; // no group

		expect(someRegisteredUserGrantingAccess.getId()).andReturn(someUserIdGrantingAccess).anyTimes();
		
		expect(someRegisteredUserReceivingAccess.getId()).andReturn(someGroupOwnerUserId).anyTimes();
		replay(someRegisteredUserGrantingAccess);

		AssociationToken someToken = new AssociationToken("someToken", someGroupOwnerUserId, someAssociatedGroupId);
		
		expect(dummyAssociationDataManager.lookupAssociationToken(someToken.getToken())).andReturn(someToken);
		
		expect(
				dummyAssociationDataManager.hasValidAssociation(someGroupOwnerUserId,
						someUserIdGrantingAccess)).andReturn(false);
		
		dummyAssociationDataManager.createAssociation(someToken, someUserIdGrantingAccess);
		expectLastCall().once();
		
		replay(dummyAssociationDataManager);

		try {
			managerUnderTest.createAssociationWithToken(someToken.getToken(), someRegisteredUserGrantingAccess);
		} catch (InvalidUserAssociationTokenException e) {
			e.printStackTrace();
			fail("InvalidUserAssociationTokenException is unexpected");
		}

		verify(someRegisteredUserGrantingAccess, dummyAssociationDataManager);
	}	
	
	@Test
	public final void userAssociationManager_createAssociationWithBadToken_exceptionShouldBeThrown()
			throws SegueDatabaseException, UserGroupNotFoundException {
		UserAssociationManager managerUnderTest = new UserAssociationManager(dummyAssociationDataManager,
				dummyGroupDataManager);

		Long someUserIdGrantingAccess = 89745531132231213L;
		Long someGroupOwnerUserId = 17659214141L;

		RegisteredUserDTO someRegisteredUserGrantingAccess = createMock(RegisteredUserDTO.class);
		RegisteredUserDTO someRegisteredUserReceivingAccess = createMock(RegisteredUserDTO.class);

		expect(someRegisteredUserGrantingAccess.getId()).andReturn(someUserIdGrantingAccess).anyTimes();
		
		expect(someRegisteredUserReceivingAccess.getId()).andReturn(someGroupOwnerUserId).anyTimes();
		replay(someRegisteredUserGrantingAccess);
		
		String someBadToken = "bad token";
		
		expect(dummyAssociationDataManager.lookupAssociationToken(someBadToken)).andReturn(null);
		
		replay(dummyAssociationDataManager, dummyGroupDataManager);

		try {
			managerUnderTest.createAssociationWithToken(someBadToken, someRegisteredUserGrantingAccess);
			fail("An exception was expected");
		} catch (InvalidUserAssociationTokenException e) {
			// this is a success as the exception was expected.
		}

		verify(someRegisteredUserGrantingAccess, dummyAssociationDataManager);
	}
	
	@Test
	public final void userAssociationManager_hasPermissionUserIsTheOwner_trueShouldBeRetured()
			throws SegueDatabaseException, UserGroupNotFoundException {
		UserAssociationManager managerUnderTest = new UserAssociationManager(dummyAssociationDataManager,
				dummyGroupDataManager);

		Long someGroupOwnerUserId = 17659214141L;

		RegisteredUserDTO someRegisteredUserGrantingAccess = createMock(RegisteredUserDTO.class);
		UserSummaryDTO someRegisteredUserGrantingAccessSummary = createMock(UserSummaryDTO.class);

		expect(someRegisteredUserGrantingAccess.getId()).andReturn(someGroupOwnerUserId).anyTimes();
		expect(someRegisteredUserGrantingAccessSummary.getId()).andReturn(someGroupOwnerUserId).anyTimes();
		
		replay(someRegisteredUserGrantingAccess, someRegisteredUserGrantingAccessSummary);
		
		if (!managerUnderTest.hasPermission(someRegisteredUserGrantingAccess, someRegisteredUserGrantingAccessSummary)) {
			fail("If the user id of the owner and the requester are the same always return true.");	
		}

		verify(someRegisteredUserGrantingAccess, someRegisteredUserGrantingAccessSummary);
	}
	
	@Test
	public final void userAssociationManager_hasPermissionUserIsAdmin_trueShouldBeRetured()
			throws SegueDatabaseException, UserGroupNotFoundException {
		UserAssociationManager managerUnderTest = new UserAssociationManager(dummyAssociationDataManager,
				dummyGroupDataManager);

		Long someUserIdGrantingAccess = 89745531132231213L;
		Long someGroupOwnerUserId = 17659214141L;

		RegisteredUserDTO someUserRequestingAccess = createMock(RegisteredUserDTO.class);
		UserSummaryDTO someRegisteredUserGrantingAccessSummary = createMock(UserSummaryDTO.class);

		expect(someUserRequestingAccess.getId()).andReturn(someGroupOwnerUserId).anyTimes();
		
		expect(someUserRequestingAccess.getRole()).andReturn(Role.ADMIN).anyTimes();
		
		expect(someRegisteredUserGrantingAccessSummary.getId()).andReturn(someUserIdGrantingAccess).anyTimes();
		
		replay(someUserRequestingAccess, someRegisteredUserGrantingAccessSummary);
		
		if (!managerUnderTest.hasPermission(someUserRequestingAccess, someRegisteredUserGrantingAccessSummary)) {
			fail("If the user requesting access is an admin they should always have access.");	
		}

		verify(someUserRequestingAccess, someRegisteredUserGrantingAccessSummary);
	}
	
	@Test
	public final void userAssociationManager_hasPermissionUserHasValidAssociation_trueShouldBeRetured()
			throws SegueDatabaseException, UserGroupNotFoundException {
		UserAssociationManager managerUnderTest = new UserAssociationManager(dummyAssociationDataManager,
				dummyGroupDataManager);

		Long someUserIdGrantingAccess = 89745531132231213L;
		Long someGroupOwnerUserId = 17659214141L;

		RegisteredUserDTO someUserRequestingAccess = createMock(RegisteredUserDTO.class);
		UserSummaryDTO someRegisteredUserGrantingAccessSummary = createMock(UserSummaryDTO.class);

		expect(someUserRequestingAccess.getId()).andReturn(someGroupOwnerUserId).anyTimes();
		
		expect(someUserRequestingAccess.getRole()).andReturn(Role.TEACHER).anyTimes();
		
		expect(someRegisteredUserGrantingAccessSummary.getId()).andReturn(someUserIdGrantingAccess).anyTimes();
		
		expect(
				dummyAssociationDataManager.hasValidAssociation(someGroupOwnerUserId,
						someUserIdGrantingAccess)).andReturn(true).once();
		
		replay(someUserRequestingAccess, someRegisteredUserGrantingAccessSummary, dummyAssociationDataManager);
		
		if (!managerUnderTest.hasPermission(someUserRequestingAccess, someRegisteredUserGrantingAccessSummary)) {
			fail("If the user requesting access has a valid association they should have permission");	
		}

		verify(someUserRequestingAccess, someRegisteredUserGrantingAccessSummary, dummyAssociationDataManager);
	}
	
	@Test
	public final void userAssociationManager_NoPermissionUserHasNoValidAssociation_falseShouldBeRetured()
			throws SegueDatabaseException, UserGroupNotFoundException {
		UserAssociationManager managerUnderTest = new UserAssociationManager(dummyAssociationDataManager,
				dummyGroupDataManager);

		Long someUserIdNotGrantingAccess = 89745531132231213L;
		Long someGroupOwnerUserId = 17659214141L;

		RegisteredUserDTO someUserRequestingAccess = createMock(RegisteredUserDTO.class);
		UserSummaryDTO someRegisteredUserGrantingAccessSummary = createMock(UserSummaryDTO.class);

		
		expect(someUserRequestingAccess.getId()).andReturn(someGroupOwnerUserId).anyTimes();
		
		expect(someUserRequestingAccess.getRole()).andReturn(Role.TEACHER).anyTimes();
		
		expect(someRegisteredUserGrantingAccessSummary.getId()).andReturn(someUserIdNotGrantingAccess).anyTimes();
		
		expect(
				dummyAssociationDataManager.hasValidAssociation(someGroupOwnerUserId,
						someUserIdNotGrantingAccess)).andReturn(false).once();
		
		replay(someUserRequestingAccess, someRegisteredUserGrantingAccessSummary, dummyAssociationDataManager);
		
		if (managerUnderTest.hasPermission(someUserRequestingAccess, someRegisteredUserGrantingAccessSummary)) {
			fail("This user should not have access as they are not an admin and do not have a valid association.");	
		}

		verify(someUserRequestingAccess, someRegisteredUserGrantingAccessSummary, dummyAssociationDataManager);
	}
}
