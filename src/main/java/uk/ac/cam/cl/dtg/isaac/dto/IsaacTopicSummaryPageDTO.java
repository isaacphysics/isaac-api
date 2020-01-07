/*
 * Copyright 2019 James Sharkey
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

import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;

import java.util.List;

/**
 * Isaac Topic Summary Page DTO.
 *
 * Provide a summary of a topic with a list of relevant questions and concepts.
 * It is a separate type to a standard page to ensure that only pages that are
 * meant to be topic indices can be loaded as such.
 */
@JsonContentType("isaacTopicSummaryPage")
public class IsaacTopicSummaryPageDTO extends SeguePageDTO {

    private List<GameboardDTO> linkedGameboards;

    /**
     * Gets the list of linked gameboard DTOs.
     * @return the linked gameboard DTOs
     */
    public List<GameboardDTO> getLinkedGameboards() {
        return linkedGameboards;
    }

    /**
     * Sets the list of linked gameboard DTOs.
     * @param linkedGameboards the linked gameboard DTOs to set
     */
    public void setLinkedGameboards(final List<GameboardDTO> linkedGameboards) {
        this.linkedGameboards = linkedGameboards;
    }

}
