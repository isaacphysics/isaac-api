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
package uk.ac.cam.cl.dtg.segue;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import uk.ac.cam.cl.dtg.isaac.app.IsaacControllerTest;
import uk.ac.cam.cl.dtg.segue.api.UserManagerTest;
import uk.ac.cam.cl.dtg.segue.auth.FacebookAuthenticatorTest;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticatorTest;
import uk.ac.cam.cl.dtg.segue.auth.TwitterAuthenticatorTest;
import uk.ac.cam.cl.dtg.segue.dao.GitContentManagerTest;
import uk.ac.cam.cl.dtg.segue.database.GitDbTest;

/**
 * JUnit Test Suite.
 * This class should include references to all Segue test classes.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
	UserManagerTest.class, 
	FacebookAuthenticatorTest.class, 
	GoogleAuthenticatorTest.class, 
	TwitterAuthenticatorTest.class, 
	GitDbTest.class, 
	IsaacControllerTest.class,
	GitContentManagerTest.class
	})
public class SegueTests {

}
