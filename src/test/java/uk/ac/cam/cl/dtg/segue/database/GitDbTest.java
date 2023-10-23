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
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

public class GitDbTest {

  @Test
  public void gitDbOtherConstructor_checkForBadParameters_exceptionsShouldBeThrown() {
    assertThrows(IllegalArgumentException.class, () -> new GitDb("", null, null));

    assertThrows(NullPointerException.class, () -> new GitDb(null, null, null));
  }

  @Test
  public void getTreeWalk_checkThatBlankPathsAreAllowed_noExceptionThrown() throws IOException {
    Git git = createMock(Git.class);

    GitDb db = new GitDb(git);

    assertThrows(IllegalArgumentException.class, () -> db.getTreeWalk("", ""));
    assertThrows(NullPointerException.class, () -> db.getTreeWalk(null, ""));
    assertThrows(NullPointerException.class, () -> db.getTreeWalk("sha", null));

    Repository repo = createMock(Repository.class);

    expect(git.getRepository()).andReturn(repo);
    expect(repo.resolve("sha")).andReturn(null);

    replay(git);
    replay(repo);

    assertNull(db.getTreeWalk("sha",
        "")); // Blank path is explicitly allowed. This should not throw an exception. But in this case we've passed an invalid sha, so we should get null back.
  }
}
