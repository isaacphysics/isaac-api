/**
 * Copyright 2022 Matthew Trew
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

package uk.ac.cam.cl.dtg.isaac.api;

/**
 * Constants for use with IsaacIntegrationTests.
 */
public final class ITConstants {

  // Users
  public static final String TEST_ADMIN_EMAIL = "test-admin@test.com";
  public static final String TEST_ADMIN_PASSWORD = "test1234";
  public static final long TEST_ADMIN_ID = 2L;


  public static final String TEST_EDITOR_EMAIL = "test-editor@test.com";
  public static final String TEST_EDITOR_PASSWORD = "test1234";

  public static final String TEST_EVENTMANAGER_EMAIL = "test-event@test.com";
  public static final String TEST_EVENTMANAGER_PASSWORD = "test1234";

  public static final String TEST_TEACHER_EMAIL = "test-teacher@test.com";
  public static final String TEST_TEACHER_PASSWORD = "test1234";
  public static final long TEST_TEACHER_ID = 5L;

  public static final String DAVE_TEACHER_EMAIL = "dave-teacher@test.com";
  public static final String DAVE_TEACHER_PASSWORD = "test1234";

  public static final String TEST_STUDENT_EMAIL = "test-student@test.com";
  public static final String TEST_STUDENT_PASSWORD = "test1234";
  public static final long TEST_STUDENT_ID = 6L;

  // QuizFacade Additional Test Students
  // (1) Test Student is not in the QuizFacade assignment group and will make no free attempts
  // (2) Alice is the first member of QuizFacade assignment group, has no unassigned quiz attempts
  // (3) Bob is the second member of QuizFacade assignment group, has an unassigned quiz attempt
  // (4) Charlie is the third member of QuizFacade assignment group, does not provide viewing permissions to group owner
  // (5) Erika is also not in the QuizFacade assignment group but will make free attempts
  public static final String QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_EMAIL = "alice-student@test.com";
  public static final String QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_PASSWORD = "test1234";
  public static final long QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID = 7L;

  public static final String QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_EMAIL = "bob-student@test.com";
  public static final String QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_PASSWORD = "test1234";
  public static final long QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_ID = 8L;

  public static final long QUIZ_FACADE_TEST_STUDENT_4_RESTRICTED_VIEWING_FOR_TEST_TEACHER_ID = 9L;

  public static final String QUIZ_FACADE_TEST_STUDENT_5_WITH_NO_EXISTING_ATTEMPTS_EMAIL = "erika-student@test.com";
  public static final String QUIZ_FACADE_TEST_STUDENT_5_WITH_NO_EXISTING_ATTEMPTS_PASSWORD = "test1234";
  public static final long QUIZ_FACADE_TEST_STUDENT_5_WITH_NO_EXISTING_ATTEMPTS_ID = 11L;

  public static final String TEST_TUTOR_EMAIL = "test-tutor@test.com";
  public static final String TEST_TUTOR_PASSWORD = "test1234";
  public static final long TEST_TUTOR_ID = 12L;

  public static final long TEST_NON_EXISTENT_USER_ID = 1000L;

  public static final String TEST_PENDING_TEACHER_EMAIL = "pending-teacher@test.com";
  public static final String TEST_PENDING_TEACHER_PASSWORD = "test1234";

  public static final String TEST_UNKNOWN_USER_ONE_EMAIL = "test-student1@test.com";
  public static final String TEST_UNKNOWN_USER_TWO_EMAIL = "test-student2@test.com";
  public static final String TEST_UNKNOWN_USER_THREE_EMAIL = "test-student3@test.com";

  public static final String TEST_WRONG_PASSWORD = "wrongPassword";

  public static final String TEST_IP_ADDRESS = "0.0.0.0";

  // Groups
  public static final long TEST_TEACHERS_AB_GROUP_ID = 1L;
  public static final long DAVE_TEACHERS_BC_GROUP_ID = 2L;
  public static final long TEST_TUTORS_AB_GROUP_ID = 4L;
  public static final long QUIZ_FACADE_IT_TEST_GROUP_ID = 6L;
  public static final long QUIZ_FACADE_IT_SECONDARY_TEST_GROUP_ID = 7L;
  public static final long UNKNOWN_GROUP_ID = 1000L;

  // Gameboards
  public static final String ASSIGNMENTS_TEST_GAMEBOARD_ID = "865072ab-9223-495f-a809-5ee2b98252e4";
  public static final String ASSIGNMENTS_DATE_TEST_GAMEBOARD_ID = "5acb113a-4d8b-4a6d-9714-6992e7e3dc35";
  public static final String ADDITIONAL_MANAGER_TEST_GAMEBOARD_ID = "43c3ca6d-4d1b-49df-3ec4-4ce2b306eb45";

  // Quizzes/tests
  public static final String QUIZ_TEST_QUIZ_ID = "_quiz_test";
  public static final String QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID = "_hidden_from_roles_student_quiz_test";
  public static final String QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID = "_hidden_from_roles_tutor_quiz_test";
  public static final String UNKNOWN_QUIZ_ID = "_not_a_quiz";

  // Assignments
  public static final Long ASSIGNMENT_FACADE_TEST_GROUP_ID = 5L;

  public static final Long QUIZ_ASSIGNMENT_ID = 1L;
  public static final Long QUIZ_ASSIGNMENT_FOR_CANCELLATION_TEST_FIRST_ID = 2L;
  public static final Long QUIZ_ASSIGNMENT_FOR_CANCELLATION_TEST_SECOND_ID = 3L;
  public static final Long QUIZ_ASSIGNMENT_SECOND_ID = 4L;
  public static final Long QUIZ_ASSIGNMENT_CANCELLED_ID = 5L;
  public static final Long QUIZ_ASSIGNMENT_EXPIRED_ID = 6L;
  public static final Long QUIZ_ASSIGNMENT_FEEDBACK_MODE_ID = 7L;
  public static final Long QUIZ_ASSIGNMENT_SET_INCOMPLETE_TEST_ID = 8L;
  public static final Long QUIZ_ASSIGNMENT_NON_EXISTENT_ID = 1000L;

  public static final Long QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID = 1L;
  public static final Long QUIZ_ASSIGNMENT_ATTEMPT_BOB_FREE_ID = 2L;
  public static final Long QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID = 3L;
  public static final Long QUIZ_ASSIGNMENT_ATTEMPT_ALICE_CANCELLED_ID = 4L;
  public static final Long QUIZ_ASSIGNMENT_ATTEMPT_ALICE_EXPIRED_ID = 5L;
  public static final Long QUIZ_ASSIGNMENT_ATTEMPT_BOB_COMPLETE_ID = 6L;
  public static final Long QUIZ_ASSIGNMENT_ATTEMPT_ALICE_FEEDBACK_MODE_ID = 7L;
  public static final Long QUIZ_ASSIGNMENT_ATTEMPT_BOB_FOR_SET_COMPLETE_TEST_ID = 8L;
  public static final Long QUIZ_ASSIGNMENT_ATTEMPT_ALICE_FREE_ID = 12L;

  public static final String QUIZ_TEST_QUESTION_FIRST_ID = "_quiz_test|0ec982f6-e2bf-4974-b777-c50b9471beb1|84c48a78-2a27-4843-866a-c8895aa60e70";
  public static final String QUIZ_TEST_HIDDEN_FROM_TUTORS_QUESTION_FIRST_ID = "_hidden_from_roles_tutor_quiz_test|0ec982f6-e2bf-4974-b777-c50b9471beb1|84c48a78-2a27-4843-866a-c8895aa60e70";
  public static final String QUIZ_TEST_UNKNOWN_QUESTION_ID = "not_a_question";

  public static final String QUIZ_TEST_FIRST_QUESTION_ANSWER = "{\"type\":\"quantity\",\"value\":\"3.0\",\"units\":\"V\"}";

  public static final String NOT_LOGGED_IN_ERROR_MESSAGE = "You must be logged in to access this resource.";
  public static final String INCORRECT_ROLE_ERROR_MESSAGE = "You do not have the permissions to complete this action";

  private ITConstants() {
  }
}
