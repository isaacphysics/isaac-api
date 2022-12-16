/*
 * Copyright 2022 Chris Purdy
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
package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.Date;

public class MisuseStatisticDTO {
    private String eventType;
    private Boolean isMisused;
    private Date lastEventTimestamp;
    private Integer currentCounter;
    private Integer softEventCountThreshold;
    private Integer hardEventCountThreshold;


    public MisuseStatisticDTO(final String eventType, final Boolean isMisused, final Date lastEventTimestamp,
                              final Integer currentCounter, final Integer softEventCountThreshold,
                              final Integer hardEventCountThreshold) {
        this.eventType = eventType;
        this.isMisused = isMisused;
        this.lastEventTimestamp = lastEventTimestamp;
        this.currentCounter = currentCounter;
        this.softEventCountThreshold = softEventCountThreshold;
        this.hardEventCountThreshold = hardEventCountThreshold;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    public Boolean getIsMisused() {
        return isMisused;
    }

    public void setIsMisused(final Boolean isMisused) {
        this.isMisused = isMisused;
    }

    public Date getLastEventTimestamp() {
        return lastEventTimestamp;
    }

    public void setLastEventTimestamp(final Date lastEventTimestamp) {
        this.lastEventTimestamp = lastEventTimestamp;
    }

    public Integer getCurrentCounter() {
        return currentCounter;
    }

    public void setCurrentCounter(final Integer currentCounter) {
        this.currentCounter = currentCounter;
    }

    public Integer getSoftEventCountThreshold() {
        return softEventCountThreshold;
    }

    public void setSoftEventCountThreshold(final Integer softEventCountThreshold) {
        this.softEventCountThreshold = softEventCountThreshold;
    }

    public Integer getHardEventCountThreshold() {
        return hardEventCountThreshold;
    }

    public void setHardEventCountThreshold(final Integer hardEventCountThreshold) {
        this.hardEventCountThreshold = hardEventCountThreshold;
    }
}
