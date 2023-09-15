/*
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.Date;

import uk.ac.cam.cl.dtg.util.locations.Location;

/**
 * LocationHistoryEvent.
 */
public interface LocationHistoryEvent {
    /**
     * Gets the id.
     * 
     * @return the id
     */
    Long getId();

    /**
     * Gets the ipAddress.
     * 
     * @return the ipAddress
     */
    String getIpAddress();

    /**
     * Gets the locationInformation.
     * 
     * @return the locationInformation
     */
    Location getLocationInformation();

    /**
     * Gets the created.
     * 
     * @return the created
     */
    Date getCreated();

    /**
     * Gets the lastUpdated.
     * 
     * @return the lastUpdated
     */
    Date getLastUpdated();
}
