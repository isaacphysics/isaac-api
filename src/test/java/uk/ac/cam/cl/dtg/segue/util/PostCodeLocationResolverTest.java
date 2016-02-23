/**
 * Copyright 2016 Alistair Stead
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
package uk.ac.cam.cl.dtg.segue.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.api.client.util.Maps;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.LocationHistory;
import uk.ac.cam.cl.dtg.segue.dos.PgLocationHistory;
import uk.ac.cam.cl.dtg.util.locations.LocationServerException;
import uk.ac.cam.cl.dtg.util.locations.PostCodeIOLocationResolver;
import uk.ac.cam.cl.dtg.util.locations.PostCodeRadius;

/**
 * Test suite to check that the use of the 3rd party service postcodes.io
 *
 * @author Alistair Stead
 *
 */
public class PostCodeLocationResolverTest {

    private LocationHistory locationHistory;
    private PostCodeIOLocationResolver resolver;
    private PostgresSqlDb mockDatabase;

    @Before
    public final void setUp() throws Exception {
        mockDatabase = EasyMock.createMock(PostgresSqlDb.class);
        locationHistory = new PgLocationHistory(mockDatabase);
        resolver = new PostCodeIOLocationResolver(locationHistory);

        ResultSet mockResultSet = EasyMock.createMock(ResultSet.class);
        EasyMock.expect(mockResultSet.next()).andReturn(false).anyTimes();
        EasyMock.replay(mockResultSet);

        PreparedStatement mockPst = EasyMock.createNiceMock(PreparedStatement.class);
        EasyMock.expect(mockPst.executeQuery()).andReturn(mockResultSet).anyTimes();
        mockPst.setString(EasyMock.anyInt(), EasyMock.anyString());
        EasyMock.expectLastCall().anyTimes();
        mockPst.setDouble(EasyMock.anyInt(), EasyMock.anyDouble());
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(mockPst.executeUpdate()).andReturn(1).anyTimes();
        EasyMock.replay(mockPst);

        Connection mockConnection = EasyMock.createNiceMock(Connection.class);
        EasyMock.expect(mockConnection.prepareStatement(EasyMock.anyString())).andReturn(mockPst).anyTimes();
        EasyMock.replay(mockConnection);
        EasyMock.expect(mockDatabase.getDatabaseConnection()).andReturn(mockConnection).anyTimes();
        EasyMock.replay(mockDatabase);
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingCorrectPostCodesOfKnownDistance_ListOfSizeOneReturned() {
        
        Map<String, List<Long>> map = Maps.newHashMap();

        ArrayList<Long> list1 = new ArrayList<Long>();
        list1.add(5l);

        ArrayList<Long> list2 = new ArrayList<Long>();
        list2.add(6l);

        map.put("BD175TP", list1);

        map.put("CB237AN", list2);

        try {
            List<Long> ids = resolver.filterPostcodesWithinProximityOfPostcode(map, "BD175TT",
                    PostCodeRadius.TWENTY_FIVE_MILES);
            System.out.println(ids.toString());
            Assert.assertTrue(ids.contains(5l));
        } catch (LocationServerException e) {
            Assert.fail();
        } catch (SegueDatabaseException e) {
            Assert.fail();
        }
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingGoodPostCodesOutOfProximity_ExpectEmptyListReturned() {

        HashMap<String, List<Long>> map = Maps.newHashMap();

        ArrayList<Long> list1 = new ArrayList<Long>();
        list1.add(1l);

        ArrayList<Long> list2 = new ArrayList<Long>();
        list2.add(2l);

        map.put("BD175TP", list1);

        map.put("IP327JY", list2);

        try {
            List<Long> ids = resolver.filterPostcodesWithinProximityOfPostcode(map, "CB237AN",
                    PostCodeRadius.FIFTY_MILES);
            System.out.println(ids.toString());
            Assert.assertTrue(ids.size() == 1);
        } catch (LocationServerException | SegueDatabaseException e) {
            Assert.fail();
        }
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingBadPostCodes_ExpectEmptyListReturned() {

        HashMap<String, List<Long>> map = Maps.newHashMap();

        ArrayList<Long> list1 = new ArrayList<Long>();
        list1.add(1l);

        ArrayList<Long> list2 = new ArrayList<Long>();
        list2.add(2l);

        map.put("5436643", list1);

        map.put("654653", list2);

        try {
            List<Long> ids = resolver.filterPostcodesWithinProximityOfPostcode(map, "BD175TT",
                    PostCodeRadius.TWENTY_FIVE_MILES);
            Assert.assertTrue(ids.isEmpty());
            System.out.println(ids.toString());
        } catch (LocationServerException | SegueDatabaseException e) {
            Assert.fail();
        }
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingEmptyMap_ExpectEmptyListReturned() {
        HashMap<String, List<Long>> map = Maps.newHashMap();

        try {
            List<Long> ids = resolver.filterPostcodesWithinProximityOfPostcode(map, "BD175TT",
                    PostCodeRadius.TWENTY_FIVE_MILES);
            Assert.assertTrue(ids.isEmpty());
            System.out.println(ids.toString());
        } catch (LocationServerException | SegueDatabaseException e) {
            Assert.fail();
        }
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingBadTargetPostCode_ExpectEmptyListReturned() {
        HashMap<String, List<Long>> map = Maps.newHashMap();

        ArrayList<Long> list1 = new ArrayList<Long>();
        list1.add(1l);

        ArrayList<Long> list2 = new ArrayList<Long>();
        list2.add(2l);

        map.put("BD175TP", list1);

        map.put("654653", list2);

        try {
            resolver.filterPostcodesWithinProximityOfPostcode(map, "46346364",
                    PostCodeRadius.TWENTY_FIVE_MILES);
            Assert.fail();
        } catch (LocationServerException | SegueDatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingBadArguments_ExpectEmptyListReturned() {

        try {
            resolver.filterPostcodesWithinProximityOfPostcode(null, "", null);
            Assert.fail();
        } catch (LocationServerException | SegueDatabaseException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingPostCodesWithRandomSpaces_ExpectNonEmptyListReturned() {
        HashMap<String, List<Long>> map = Maps.newHashMap();

        ArrayList<Long> list1 = new ArrayList<Long>();
        list1.add(1l);

        ArrayList<Long> list2 = new ArrayList<Long>();
        list2.add(2l);

        map.put("BD17  5TP", list1);

        map.put("  CB23   6FE  ", list2);

        try {
            List<Long> ids = resolver.filterPostcodesWithinProximityOfPostcode(map, "CB237AN",
                    PostCodeRadius.TWENTY_FIVE_MILES);
            Assert.assertTrue(ids.contains(2l));
        } catch (LocationServerException | SegueDatabaseException e) {
            System.out.println(e.getMessage());
            Assert.fail();
        }
    }

}
