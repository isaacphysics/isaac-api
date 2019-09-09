/*
 * Copyright 2014 Ian Davies
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
package uk.ac.cam.cl.dtg.segue.database;

import static org.junit.Assert.*;

import java.io.IOException;

import org.easymock.EasyMock;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.Test;

import org.powermock.api.easymock.PowerMock;

public class GitDbTest {

	@Test
	public void gitDb_checkForBadParameters_exceptionsShouldBeThrown() throws IOException {
		PowerMock.mockStatic(Git.class);

		// Test that if you provide an empty string or null, an IllegalArgumentException gets thrown and git.open never gets called.

		PowerMock.replay(Git.class);

		try {
			new GitDb("");
			fail("GitDb constructor was given an empty string, but didn't throw an exception");
		} catch (IllegalArgumentException e) {
			// Exception correctly thrown.
		} catch (Exception e) {
			fail("GitDb constructor threw wrong exception type: " + e);
		}

		try {
			new GitDb((String)null);
			fail("GitDb constructor was given null, but didn't throw an exception");
		} catch (NullPointerException e) {
			// Exception correctly thrown.
		} catch (Exception e) {
			fail("GitDb constructor threw wrong exception type: " + e);
		}
	}

	@Test
	public void gitDbOtherConstructor_checkForBadParameters_exceptionsShouldBeThrown() {
		// Test that if you provide an empty string or null, an IllegalArgumentException gets thrown and git.open never gets called.

		PowerMock.replay(Git.class);

		try {
			new GitDb("", null, null);
			fail("GitDb constructor was given an empty string, but didn't throw an exception");
		} catch (IllegalArgumentException e) {
			// Exception correctly thrown.
		} catch (Exception e) {
			fail("GitDb constructor threw wrong exception type: " + e);
		}

		try {
			new GitDb(null, null, null);
			fail("GitDb constructor was given null, but didn't throw an exception");
		} catch (NullPointerException e) {
			// Exception correctly thrown.
		} catch (Exception e) {
			fail("GitDb constructor threw wrong exception type: " + e);
		}

	}

	@Test
	public void getTreeWalk_checkThatBlankPathsAreAllowed_noExceptionThrown() throws IOException {

		Git git = EasyMock.createMock(Git.class);

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

		Repository repo = EasyMock.createMock(Repository.class);

		EasyMock.expect(git.getRepository()).andReturn(repo);
		EasyMock.expect(repo.resolve("sha")).andReturn(null);

		EasyMock.replay(git);
		EasyMock.replay(repo);

		assertNull(db.getTreeWalk("sha", "")); // Blank path is explicitly allowed. This should not throw an exception. But in this case we've passed an invalid sha, so we should get null back.
	}
}
