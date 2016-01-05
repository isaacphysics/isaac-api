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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import junit.framework.Assert;
import org.junit.Test;
import uk.ac.cam.cl.dtg.util.locations.LocationServerException;
import uk.ac.cam.cl.dtg.util.locations.PostCodeIOLocationResolver;

/**
 * Test suite to check that the use of the 3rd party service postcodes.io
 *
 * @author Alistair Stead
 *
 */
public class PostCodeLocationResolverTest {

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingCorrectPostCodesOfKnownDistance_ListOfSizeOneReturned() {
        PostCodeIOLocationResolver resolver = new PostCodeIOLocationResolver();
        
        
        HashMap<String, ArrayList<Long>> map = new HashMap<String, ArrayList<Long>>();

        ArrayList<Long> list1 = new ArrayList<Long>();
        list1.add(5l);

        ArrayList<Long> list2 = new ArrayList<Long>();
        list2.add(6l);

        map.put("BD175TP", list1);

        map.put("CB237AN", list2);

        try {
            List<Long> ids = resolver.filterPostcodesWithinProximityOfPostcode(map, "BD175TT", 20);
            System.out.println(ids.toString());
            Assert.assertTrue(ids.contains(5l));
        } catch (LocationServerException e) {
            Assert.fail();
        }
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingGoodPostCodesOutOfProximity_ExpectEmptyListReturned() {
        PostCodeIOLocationResolver resolver = new PostCodeIOLocationResolver();

        HashMap<String, ArrayList<Long>> map = new HashMap<String, ArrayList<Long>>();

        ArrayList<Long> list1 = new ArrayList<Long>();
        list1.add(1l);

        ArrayList<Long> list2 = new ArrayList<Long>();
        list2.add(2l);

        map.put("BD175TP", list1);

        map.put("PA477S", list2);

        try {
            List<Long> ids = resolver.filterPostcodesWithinProximityOfPostcode(map, "CB237AN", 200);
            System.out.println(ids.toString());
            Assert.assertTrue(ids.size() == 1);
        } catch (LocationServerException e) {
            Assert.fail();
        }
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingBadPostCodes_ExpectEmptyListReturned() {
        PostCodeIOLocationResolver resolver = new PostCodeIOLocationResolver();

        HashMap<String, ArrayList<Long>> map = new HashMap<String, ArrayList<Long>>();

        ArrayList<Long> list1 = new ArrayList<Long>();
        list1.add(1l);

        ArrayList<Long> list2 = new ArrayList<Long>();
        list2.add(2l);

        map.put("5436643", list1);

        map.put("654653", list2);

        try {
            List<Long> ids = resolver.filterPostcodesWithinProximityOfPostcode(map, "BD175TT", 20);
            Assert.assertTrue(ids.isEmpty());
            System.out.println(ids.toString());
        } catch (LocationServerException e) {
            Assert.fail();
        }
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingEmptyMap_ExpectEmptyListReturned() {
        PostCodeIOLocationResolver resolver = new PostCodeIOLocationResolver();

        HashMap<String, ArrayList<Long>> map = new HashMap<String, ArrayList<Long>>();

        try {
            List<Long> ids = resolver.filterPostcodesWithinProximityOfPostcode(map, "BD175TT", 20);
            Assert.assertTrue(ids.isEmpty());
            System.out.println(ids.toString());
        } catch (LocationServerException e) {
            Assert.fail();
        }
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingBadTargetPostCode_ExpectEmptyListReturned() {
        PostCodeIOLocationResolver resolver = new PostCodeIOLocationResolver();

        HashMap<String, ArrayList<Long>> map = new HashMap<String, ArrayList<Long>>();

        ArrayList<Long> list1 = new ArrayList<Long>();
        list1.add(1l);

        ArrayList<Long> list2 = new ArrayList<Long>();
        list2.add(2l);

        map.put("BD175TP", list1);

        map.put("654653", list2);

        try {
            List<Long> ids = resolver.filterPostcodesWithinProximityOfPostcode(map, "46346364", 20);
            Assert.fail();
        } catch (LocationServerException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void filterPostcodesWithinProximityOfPostcode_passingBadArguments_ExpectEmptyListReturned() {
        PostCodeIOLocationResolver resolver = new PostCodeIOLocationResolver();

        try {
            List<Long> ids = resolver.filterPostcodesWithinProximityOfPostcode(null, "", 0);
            Assert.fail();
        } catch (LocationServerException e) {
            System.out.println(e.getMessage());
        }
    }

}
