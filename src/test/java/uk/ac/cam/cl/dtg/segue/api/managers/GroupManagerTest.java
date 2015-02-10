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

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import ma.glasnost.orika.MapperFacade;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.comm.ICommunicator;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserGroupDataManager;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Test class for the user manager class.
 * 
 */
@PowerMockIgnore({ "javax.ws.*" })
public class GroupManagerTest {

	private PropertiesLoader dummyPropertiesLoader;

	private MapperFacade dummyMapper;
	private ICommunicator dummyCommunicator;
	private SimpleDateFormat sdf;
	
	private IUserGroupDataManager groupDataManager;
	private UserManager userManager;
	
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
		
		this.groupDataManager = createMock(IUserGroupDataManager.class);
		this.userManager = createMock(UserManager.class);
		
		expect(this.dummyPropertiesLoader.getProperty(Constants.SESSION_EXPIRY_SECONDS)).andReturn("60")
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
		someGroupOwner.setDbId("533ee66842f639e95ce35e29");
		someGroupOwner.setEmail("test@test.com");
		
		Capture<UserGroup> capturedGroup = new Capture<UserGroup>();
		
		UserGroup resultFromDB = new UserGroup();
		
		GroupManager gm = new GroupManager(this.groupDataManager, this.userManager, dummyMapper);
		try {
			expect(this.groupDataManager.createGroup(and(capture(capturedGroup), isA(UserGroup.class))))
					.andReturn(resultFromDB);
			replay(this.groupDataManager);

			// check that the result of the method is whatever comes out of the database
			UserGroup createUserGroup = gm.createUserGroup(someGroupName, someGroupOwner);
			assertTrue(createUserGroup == resultFromDB);

			// check that what goes into the database is what we passed it.
			assertTrue(capturedGroup.getValue().getOwnerId().equals(someGroupOwner.getDbId()));
			assertTrue(capturedGroup.getValue().getGroupName().equals(someGroupName));
			assertTrue(capturedGroup.getValue().getCreated() instanceof Date);
			
		} catch (SegueDatabaseException e) {
			fail("No exception expected");
			e.printStackTrace();
		}
		verify(this.groupDataManager);
	}
}
