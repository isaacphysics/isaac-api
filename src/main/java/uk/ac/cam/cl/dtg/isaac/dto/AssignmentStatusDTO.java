/*
 * Copyright 2022 Chris Purdy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dto;

/**
 * This DTO records an error during attempting to set an assignment to a group
 */
public class AssignmentStatusDTO {
    private Long groupId;
    private Long assignmentId;
    private String errorMessage;

    public AssignmentStatusDTO(final Long groupId, final String errorMessage) {
        this.groupId = groupId;
        this.errorMessage = errorMessage;
    }

    public AssignmentStatusDTO(final Long groupId, final Long assignmentId) {
        this.groupId = groupId;
        this.assignmentId = assignmentId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(final Long groupId) {
        this.groupId = groupId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(final Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    @Override
    public String toString() {
        return "AssignmentStatusDTO ["
                + "groupId=" + groupId
                + ", assignmentId=" + assignmentId
                + ", errorMessage=" + errorMessage
                + ']';
    }

    @Override
    public int hashCode() {
        final int prime = 37;
        int result = 1;
        result = prime * result + (null == groupId ? 0 : groupId.hashCode());
        result = prime * result + (null == assignmentId ? 0 : assignmentId.hashCode());
        result = prime * result + (null == errorMessage ? 0 : errorMessage.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AssignmentStatusDTO)) {
            return false;
        }

        AssignmentStatusDTO other = (AssignmentStatusDTO) obj;

        if (null == groupId) {
            if (null != other.groupId) {
                return false;
            }
        } else if (!groupId.equals(other.groupId)) {
            return false;
        }

        if (null == assignmentId) {
            if (null != other.assignmentId) {
                return false;
            }
        } else if (!assignmentId.equals(other.assignmentId)) {
            return false;
        }

        if (null == errorMessage) {
            if (null != other.errorMessage) {
                return false;
            }
        } else if (!errorMessage.equals(other.errorMessage)) {
            return false;
        }

        return true;
    }
}
