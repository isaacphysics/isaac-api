/**
 * Copyright 2014 Ian Davies
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.database;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

public class GitDbTest {

  @Test
  public void gitDbOtherConstructor_checkForBadParameters_exceptionsShouldBeThrown() {
    // Test that if you provide an empty string or null, an IllegalArgumentException gets thrown and git.open never gets called.

    GitDb gitDb = null;

    try {
      gitDb = new GitDb("", null, null);
      fail("GitDb constructor was given an empty string, but didn't throw an exception");
    } catch (IllegalArgumentException e) {
      // Exception correctly thrown.
    } catch (Exception e) {
      fail("GitDb constructor threw wrong exception type: " + e);
    }

    try {
      gitDb = new GitDb(null, null, null);
      fail("GitDb constructor was given null, but didn't throw an exception");
    } catch (NullPointerException e) {
      // Exception correctly thrown.
    } catch (Exception e) {
      fail("GitDb constructor threw wrong exception type: " + e);
    }

    assertNull(gitDb);
  }

  @Test
  public void getTreeWalk_checkThatBlankPathsAreAllowed_noExceptionThrown() throws IOException {

    Git git = createMock(Git.class);

    GitDb db = new GitDb(git);

    try {
      db.getTreeWalk("", "");
      fail("Failed to throw required exception on blank sha.");
    } catch (IllegalArgumentException e) {
      // Exception correctly thrown.
    } catch (Exception e) {
      fail("Wrong type of exception thrown on blank sha");
    }

    try {
      db.getTreeWalk(null, "");
      fail("Failed to throw required exception on null sha.");
    } catch (NullPointerException e) {
      // Exception correctly thrown.
    } catch (Exception e) {
      fail("Wrong type of exception thrown on null sha");
    }

    try {
      db.getTreeWalk("sha", null);
      fail("Failed to throw required exception on null path.");
    } catch (NullPointerException e) {
      // Exception correctly thrown.
    } catch (Exception e) {
      fail("Wrong type of exception thrown on null path");
    }

    Repository repo = createMock(Repository.class);

    expect(git.getRepository()).andReturn(repo);
    expect(repo.resolve("sha")).andReturn(null);

    replay(git);
    replay(repo);

    assertNull(db.getTreeWalk("sha",
        "")); // Blank path is explicitly allowed. This should not throw an exception. But in this case we've passed an invalid sha, so we should get null back.
  }
}
