package uk.ac.cam.cl.dtg.segue;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import uk.ac.cam.cl.dtg.segue.api.UserManagerTest;
import uk.ac.cam.cl.dtg.segue.database.GitDbTest;

@RunWith(Suite.class)
@SuiteClasses({UserManagerTest.class, GitDbTest.class})
public class SegueTests {

}
