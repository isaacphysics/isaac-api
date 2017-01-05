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

/**
 * Enum to store allowable post code radius search distance.
 *
 * @author Alistair Stead
 *
 */
public enum PostCodeRadius {
    TEN_MILES, FIFTEEN_MILES, TWENTY_MILES, TWENTY_FIVE_MILES, FIFTY_MILES;

    /**
     * @return distance in miles of radius search
     */
    public double getDistance() {
        switch (this) {
            case TEN_MILES:
                return 10.0;
            case FIFTEEN_MILES:
                return 15.0;
            case TWENTY_MILES:
                return 20.0;
            case TWENTY_FIVE_MILES:
                return 25.0;
            case FIFTY_MILES:
                return 50.0;
            default:
                return 50.0;
        }
    }

}
