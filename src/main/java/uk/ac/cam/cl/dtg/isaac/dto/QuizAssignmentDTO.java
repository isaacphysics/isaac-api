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
package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * This class is the Data Transfer Object used to store quiz assignments in the isaac CMS.
 */
public class QuizAssignmentDTO {
    private Long id;
    private String quizId;
    private IsaacQuizDTO quiz;
    private Long groupId;
    private Long ownerUserId;
    private UserSummaryDTO assignerSummary;
    private Date creationDate;
    private Date dueDate;
    private QuizFeedbackMode quizFeedbackMode;

    /**
     * Complete AssignmentDTO constructor with all dependencies.
     *
     * @param id
     *            - unique id for the quiz
     * @param quizId
     *            - The quiz to assign as homework.
     * @param ownerUserId
     *            - User id of the owner of the quiz.
     * @param groupId
     *            - Group id who should be assigned the game board.
     * @param creationDate
     *            - the date the assignment was created.
     * @param dueDate
     *            - the optional date the assignment should be completed by.
     * @param quizFeedbackMode
     *            - what level of feedback to give to students.
     */
    public QuizAssignmentDTO(final Long id, final String quizId, final Long ownerUserId, final Long groupId,
                             final Date creationDate, final Date dueDate, final QuizFeedbackMode quizFeedbackMode) {
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
    public QuizAssignmentDTO() {

    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id
     *            the id to set
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Gets the quizId.
     *
     * @return the quizId
     */
    public String getQuizId() {
        return quizId;
    }

    /**
     * Sets the quizId.
     *
     * @param quizId
     *            the quizId to set
     */
    public void setQuizId(final String quizId) {
        this.quizId = quizId;
    }

    /**
     * Gets the quizDTO.
     *
     * @return the quizDTO
     */
    public IsaacQuizDTO getQuiz() {
        return quiz;
    }

    /**
     * Sets the quizDTO.
     *
     * @param quizDTO
     *            the quizDTO to set
     */
    public void setQuiz(final IsaacQuizDTO quizDTO) {
        this.quiz = quizDTO;
    }

    /**
     * Gets the groupId.
     *
     * @return the groupId
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     * Sets the groupId.
     *
     * @param groupId
     *            the groupId to set
     */
    public void setGroupId(final Long groupId) {
        this.groupId = groupId;
    }

    /**
     * Gets the ownerUserId.
     *
     * @return the ownerUserId
     */
    public Long getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * Sets the ownerUserId.
     *
     * @param ownerUserId
     *            the ownerUserId to set
     */
    public void setOwnerUserId(final Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    /**
     * Gets the assignerSummary.
     * @return the assignerSummary
     */
    public UserSummaryDTO getAssignerSummary() {
        return assignerSummary;
    }

    /**
     * Sets the assignerSummary.
     * @param assignerSummary the assignerSummary to set
     */
    public void setAssignerSummary(final UserSummaryDTO assignerSummary) {
        this.assignerSummary = assignerSummary;
    }

    /**
     * Gets the creationDate.
     *
     * @return the creationDate
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Sets the creationDate.
     *
     * @param creationDate
     *            the creationDate to set
     */
    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * get the due date of the assignment.
     * @return dueDate
     */
    @Nullable public Date getDueDate() {
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
