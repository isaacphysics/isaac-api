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

import java.util.List;

/**
 * This is the Data Transfer Object used to respond to the front-end with which assignments were (un)successfully set to which groups
 */
public class AssignmentSettingResponseDTO {
    private List<Long> assignedGroupIds;
    private List<AssignmentErrorDTO> assignmentErrors;

    /**
     * Complete AssignmentDTO constructor with all dependencies.
     * @param assignedGroupIds
     *            - list of group ids that were successfully assigned to
     * @param assignmentErrors
     *            - list of assignment error objects, containing group ids along with a reason why they errored
     */
    public AssignmentSettingResponseDTO(final List<Long> assignedGroupIds, final List<AssignmentErrorDTO> assignmentErrors) {
        this.assignedGroupIds = assignedGroupIds;
        this.assignmentErrors = assignmentErrors;
    }

    public List<Long> getAssignedGroupIds() {
        return assignedGroupIds;
    }

    public void setAssignedGroupIds(final List<Long> assignedGroupIds) {
        this.assignedGroupIds = assignedGroupIds;
    }

    public List<AssignmentErrorDTO> getAssignmentErrors() {
        return assignmentErrors;
    }

    public void setAssignmentErrors(final List<AssignmentErrorDTO> assignmentErrors) {
        this.assignmentErrors = assignmentErrors;
    }
}
