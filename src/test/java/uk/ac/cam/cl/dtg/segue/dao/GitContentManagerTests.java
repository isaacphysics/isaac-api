package uk.ac.cam.cl.dtg.segue.dao;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.Validate;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;

/**
 * Test class for the GitContentManager class.
 * 
 */
public class GitContentManagerTests {
	private static final Logger log = LoggerFactory.getLogger(GitDb.class);

	private GitDb database;
	private ISearchProvider searchProvider;
	private ContentMapper contentMapper;

	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		this.database = createMock(GitDb.class);
		this.searchProvider = createMock(ISearchProvider.class);
		this.contentMapper = createMock(ContentMapper.class);
	}

	/**
	 * Test that the compareTo method returns the correct result when V1 is
	 * newer than V2.
	 */
	@Test
	public void compareTo_checkV1NewerThanV2_checkPositiveNumberReceived() {
		assertTrue(compareTo_getResult(2010, 2000) > 0);
	}

	/**
	 * Test that the compareTo method returns the correct result when V2 is
	 * newer than V1.
	 */
	@Test
	public void compareTo_checkV2NewerThanV1_checkNegativeNumberReceived() {
		assertTrue(compareTo_getResult(2000, 2010) < 0);
	}

	/**
	 * Test that the compareTo method returns the correct result when V1 is the
	 * same age as V2.
	 */
	@Test
	public void compareTo_checkV2SameAgeAsV1_checkZeroReceived() {
		assertTrue(compareTo_getResult(2000, 2000) == 0);
	}

	/**
	 * This method will evaluate the result of the compareTo method using the
	 * years provided as the arguments for datestamps. The rest of the timestamp
	 * will read 1st January 00:00:00
	 * 
	 * @param v1Year
	 *            - The year for v1
	 * @param v2Year
	 *            - The year for v2
	 * @return the result of comparing the two dates using the
	 *         GitContentManager.compareTo method
	 */
	private int compareTo_getResult(final int v1Year, final int v2Year) {
		GitContentManager gitContentManager = new GitContentManager(database,
				searchProvider, contentMapper);

		String v1Hash = "V1";
		String v2Hash = "V2";

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(0);
		cal.set(v1Year, 0, 1, 0, 0, 0);
		int v1Date = (int) (cal.getTimeInMillis() / 1000L);
		cal.set(v2Year, 0, 1, 0, 0, 0);
		int v2Date = (int) (cal.getTimeInMillis() / 1000L);

		expect(database.getCommitTime(v1Hash)).andReturn(v1Date).once();
		expect(database.getCommitTime(v2Hash)).andReturn(v2Date).once();

		replay(database);

		int result = gitContentManager.compareTo(v1Hash, v2Hash);

		verify(database);

		return result;
	}
}
