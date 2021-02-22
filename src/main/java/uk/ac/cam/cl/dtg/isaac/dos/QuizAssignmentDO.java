/*
 * Copyright 2021 Raspberry Pi Foundation
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

import javax.annotation.Nullable;
import java.util.Date;

/**
 * This class is the Domain Object used to store Quiz assignments in the isaac CMS.
 */
public class QuizAssignmentDO {
    private Long id;
    private String quizId;
    private Long groupId;
    private Long ownerUserId;
    private Date creationDate;
    private Date dueDate;
    private QuizFeedbackMode quizFeedbackMode;

    /**
     * Complete AssignmentDO constructor with all dependencies.
     *
     * @param id
     *            - unique id for the gameboard
     * @param quizId
     *            - The quiz to assign.
     * @param ownerUserId
     *            - User id of the teacher who assigned the quiz.
     * @param groupId
     *            - Group id who should be assigned the quiz.
     * @param creationDate
     *            - the date the assignment was created.
     * @param dueDate
     *            - optional date the assignment should be completed by.
     * @param quizFeedbackMode
     *            - what level of feedback to give to students.
     */
    public QuizAssignmentDO(final Long id, final String quizId, final Long ownerUserId, final Long groupId,
                            final Date creationDate, @Nullable final Date dueDate, final QuizFeedbackMode quizFeedbackMode) {
        this.id = id;
        this.quizId = quizId;
        this.ownerUserId = ownerUserId;
        this.groupId = groupId;
        this.creationDate = creationDate;
        this.dueDate = dueDate;
        this.quizFeedbackMode = quizFeedbackMode;
    }

    /**
     * Default constructor required for AutoMapping.
     */
    public QuizAssignmentDO() {

    }

    /**
     * Gets the id.
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     * @param id the id to set
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Gets the quizId.
     * @return the quizId
     */
    public String getQuizId() {
        return quizId;
    }

    /**
     * Sets the quizId.
     * @param quizId the quizId to set
     */
    public void setQuizId(final String quizId) {
        this.quizId = quizId;
    }

    /**
     * Gets the groupId.
     * @return the groupId
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     * Sets the groupId.
     * @param groupId the groupId to set
     */
    public void setGroupId(final Long groupId) {
        this.groupId = groupId;
    }

    /**
     * Gets the ownerUserId.
     * @return the ownerUserId
     */
    public Long getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * Sets the ownerUserId.
     * @param ownerUserId the ownerUserId to set
     */
    public void setOwnerUserId(final Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    /**
     * Gets the creationDate.
     * @return the creationDate
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Sets the creationDate.
     * @param creationDate the creationDate to set
     */
    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public int hashCode() {
        return 31 + ((id == null) ? 0 : id.hashCode());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof QuizAssignmentDO)) {
            return false;
        }
        QuizAssignmentDO other = (QuizAssignmentDO) obj;
        if (id == null) {
            return other.id == null;
        } else {
            return id.equals(other.id);
        }
    }

    /**
     * get the due date of the assignment.
     * @return dueDate
     */
    @Nullable
    public Date getDueDate() {
        return dueDate;
    }

    /**
     * set the due date of an assignment.
     * @param dueDate - date due
     */
    public void setDueDate(@Nullable Date dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Gets the QuizFeedbackMode.
     * @return the quizFeedbackMode
     */
    public QuizFeedbackMode getQuizFeedbackMode() {
        return quizFeedbackMode;
    }

    /**
     * Sets the quizFeedbackMode.
     * @param quizFeedbackMode the QuizFeedbackMode to set
     */
    public void setQuizFeedbackMode(final QuizFeedbackMode quizFeedbackMode) {
        this.quizFeedbackMode = quizFeedbackMode;
    }
}
