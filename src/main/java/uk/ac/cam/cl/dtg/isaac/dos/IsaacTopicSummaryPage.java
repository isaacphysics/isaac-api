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
package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacTopicSummaryPageDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.dos.content.SeguePage;

import java.util.List;

/**
 * Isaac Topic Summary Page DO.
 *
 * Provide a summary of a topic with a list of relevant questions and concepts.
 * It is a separate type to a standard page to ensure that only pages that are
 * meant to be topic indices can be loaded as such.
 */
@DTOMapping(IsaacTopicSummaryPageDTO.class)
@JsonContentType("isaacTopicSummaryPage")
public class IsaacTopicSummaryPage extends SeguePage {

    private List<String> linkedGameboards;

    /**
     * Gets the list of linked gameboard IDs.
     * @return the linked gameboard IDs
     */
    public List<String> getLinkedGameboards() {
        return linkedGameboards;
    }

    /**
     * Sets the list of linked gameboard IDs.
     * @param linkedGameboards the linked gameboard IDs to set
     */
    public void setLinkedGameboards(final List<String> linkedGameboards) {
        this.linkedGameboards = linkedGameboards;
    }

}
