/**
 * Copyright 2022 Chris Purdy
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

package uk.ac.cam.cl.dtg.isaac.dto;

import java.time.Instant;

public class MisuseStatisticDTO {
  private String agentIdentifier;
  private String eventType;
  private Boolean isMisused;
  private Boolean isOverSoftThreshold;
  private Instant lastEventTimestamp;
  private Integer currentCounter;

  public MisuseStatisticDTO(final String agentIdentifier, final String eventType, final Boolean isMisused,
                            final Boolean isOverSoftThreshold, final Instant lastEventTimestamp,
                            final Integer currentCounter) {
    this.agentIdentifier = agentIdentifier;
    this.eventType = eventType;
    this.isMisused = isMisused;
    this.isOverSoftThreshold = isOverSoftThreshold;
    this.lastEventTimestamp = lastEventTimestamp;
    this.currentCounter = currentCounter;
  }

  public String getAgentIdentifier() {
    return agentIdentifier;
  }

  public void setAgentIdentifier(final String agentIdentifier) {
    this.agentIdentifier = agentIdentifier;
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

  public Boolean getIsOverSoftThreshold() {
    return isOverSoftThreshold;
  }

  public void setIsOverSoftThreshold(final Boolean isOverSoftThreshold) {
    this.isOverSoftThreshold = isOverSoftThreshold;
  }

  public Instant getLastEventTimestamp() {
    return lastEventTimestamp;
  }

  public void setLastEventTimestamp(final Instant lastEventTimestamp) {
    this.lastEventTimestamp = lastEventTimestamp;
  }

  public Integer getCurrentCounter() {
    return currentCounter;
  }

  public void setCurrentCounter(final Integer currentCounter) {
    this.currentCounter = currentCounter;
  }
}
