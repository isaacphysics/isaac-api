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
package uk.ac.cam.cl.dtg.util.locations;

import java.util.List;
import java.util.Map;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * Interface to allow postcode-related searches using external service.
 *
 * @author Alistair Stead
 *
 */
public interface PostCodeLocationResolver {

    /**
     * @param postCodeAndUserIds
     *            - a map of postcodes to userids
     * @param targetPostCode
     *            - the target post code
     * @param distanceInMiles
     *            - the distance away from the target postcode to filter by
     * @return - a list of userids within the specified distance from the target postcode
     * @throws LocationServerException
     *             - an exception when there's an issue with the location service
     * @throws SegueDatabaseException
     *             - something went wrong in the database
     */
    List<Long> filterPostcodesWithinProximityOfPostcode(final Map<String, List<Long>> postCodeAndUserIds,
            final String targetPostCode, final PostCodeRadius distanceInMiles)
            throws LocationServerException, SegueDatabaseException;

}
