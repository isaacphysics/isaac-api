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

import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import ma.glasnost.orika.MapperFacade;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicationMessage;
import uk.ac.cam.cl.dtg.segue.comm.ICommunicator;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for the user manager class.
 * 
 */
@PowerMockIgnore({ "javax.ws.*" })
public class GroupManagerTest {

	private PropertiesLoader dummyPropertiesLoader;

	private MapperFacade dummyMapper;
	private ICommunicator<EmailCommunicationMessage> dummyCommunicator;
	private SimpleDateFormat sdf;
	
	private IUserGroupPersistenceManager groupDataManager;
	private UserAccountManager userManager;
	private GameManager gameManager;
	
	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		this.dummyMapper = createMock(MapperFacade.class);
		this.dummyCommunicator = createMock(ICommunicator.class);
		this.dummyPropertiesLoader = createMock(PropertiesLoader.class);
		this.sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
		
		this.groupDataManager = createMock(IUserGroupPersistenceManager.class);
		this.userManager = createMock(UserAccountManager.class);
		this.gameManager = createMock(GameManager.class);
		
		expect(this.dummyPropertiesLoader.getProperty(Constants.SESSION_EXPIRY_SECONDS_DEFAULT)).andReturn("60")
				.anyTimes();
		expect(this.dummyPropertiesLoader.getProperty(Constants.SESSION_EXPIRY_SECONDS_REMEMBERED)).andReturn("360")
				.anyTimes();
		replay(this.dummyPropertiesLoader);
	}

	/**
	 * Verify that the constructor responds correctly to bad input.
	 */
	@Test
	public final void groupManager_createValidGroup_aGroupShouldBeCreated() {
		String someGroupName = "Group Name";
		RegisteredUserDTO someGroupOwner = new RegisteredUserDTO();
		someGroupOwner.setId(5339L);
		someGroupOwner.setEmail("test@test.com");
		Set<Long> someSetOfManagers = Sets.newHashSet();
		Capture<UserGroup> capturedGroup = new Capture<UserGroup>();

		List<RegisteredUserDTO> someListOfUsers = Lists.newArrayList();
		List<UserSummaryWithEmailAddressDTO> someListOfUsersDTOs = Lists.newArrayList();

		UserGroup resultFromDB = new UserGroup();
		resultFromDB.setId(2L);
		UserGroupDTO mappedGroup = new UserGroupDTO();
		resultFromDB.setId(2L);
		
		GroupManager gm = new GroupManager(this.groupDataManager, this.userManager, this.gameManager, this.dummyMapper);
		try {
			expect(this.groupDataManager.createGroup(and(capture(capturedGroup), isA(UserGroup.class))))
					.andReturn(resultFromDB);
			expect(this.groupDataManager.getAdditionalManagerSetByGroupId(anyObject()))
					.andReturn(someSetOfManagers).atLeastOnce();
			expect(this.userManager.findUsers(someSetOfManagers)).andReturn(someListOfUsers);
			expect(this.userManager.convertToDetailedUserSummaryObjectList(someListOfUsers, UserSummaryWithEmailAddressDTO.class)).andReturn(someListOfUsersDTOs);
			expect(this.dummyMapper.map(resultFromDB, UserGroupDTO.class)).andReturn(mappedGroup).atLeastOnce();

			replay(this.userManager, this.groupDataManager, this.dummyMapper);

			// check that the result of the method is whatever comes out of the database
			UserGroupDTO createUserGroup = gm.createUserGroup(someGroupName, someGroupOwner);

			// check that what goes into the database is what we passed it.
			assertTrue(capturedGroup.getValue().getOwnerId().equals(someGroupOwner.getId()));
			assertTrue(capturedGroup.getValue().getGroupName().equals(someGroupName));
			assertTrue(capturedGroup.getValue().getCreated() instanceof Date);
			
		} catch (SegueDatabaseException e) {
			fail("No exception expected");
			e.printStackTrace();
		}
		verify(this.groupDataManager);
	}
}
