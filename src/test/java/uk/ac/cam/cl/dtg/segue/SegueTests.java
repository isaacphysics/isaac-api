package uk.ac.cam.cl.dtg.segue;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import uk.ac.cam.cl.dtg.isaac.app.IsaacControllerTest;
import uk.ac.cam.cl.dtg.segue.api.UserManagerTest;
import uk.ac.cam.cl.dtg.segue.auth.FacebookAuthenticatorTest;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticatorTest;
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
	GitDbTest.class, 
	IsaacControllerTest.class,
	GitContentManagerTest.class
	})
public class SegueTests {

}
