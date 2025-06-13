/*
 * Copyright 2022 Matthew Trew
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
    public static final long TEST_EDITOR_ID = 4L;

    public static final String FREDDIE_EDITOR_EMAIL = "freddie-editor@test.com";
    public static final String FREDDIE_EDITOR_PASSWORD = "test1234";
    public static final long FREDDIE_EDITOR_ID = 13L;

    public static final String TEST_EVENTMANAGER_EMAIL = "test-event@test.com";
    public static final String TEST_EVENTMANAGER_PASSWORD = "test1234";
    public static final long TEST_EVENTMANAGER_ID = 3L;

    public static final String GARY_EVENTMANAGER_EMAIL = "gary-event@test.com";
    public static final String GARY_EVENTMANAGER_PASSWORD = "test1234";
    public static final long GARY_EVENTMANAGER_ID = 14L;

    public static final String TEST_EVENTLEADER_EMAIL = "test-event-leader@test.com";
    public static final String TEST_EVENTLEADER_PASSWORD = "test1234";
    public static final long TEST_EVENTLEADER_ID = 18L;

    public static final String TEST_TEACHER_EMAIL = "test-teacher@test.com";
    public static final String TEST_TEACHER_PASSWORD = "test1234";
    public static final long TEST_TEACHER_ID = 5L;

    public static final String DAVE_TEACHER_EMAIL = "dave-teacher@test.com";
    public static final String DAVE_TEACHER_PASSWORD = "test1234";
    public static final long DAVE_TEACHER_ID = 10L;

    public static final String HARRY_TEACHER_EMAIL = "harry-teacher@test.com";
    public static final String HARRY_TEACHER_PASSWORD = "test1234";
    public static final long HARRY_TEACHER_ID = 17L;

    public static final String TEST_STUDENT_EMAIL = "test-student@test.com";
    public static final String TEST_STUDENT_PASSWORD = "test1234";
    public static final long TEST_STUDENT_ID = 6L;

    public static final String ALICE_STUDENT_EMAIL = "alice-student@test.com";
    public static final String ALICE_STUDENT_PASSWORD = "test1234";
    public static final long ALICE_STUDENT_ID = 7L;
    public static final String ALICE_STUDENT_VALID_DELETION_TOKEN = "someFakeDeletionToken";

    public static final String BOB_STUDENT_EMAIL = "bob-student@test.com";
    public static final String BOB_STUDENT_PASSWORD = "test1234";
    public static final long BOB_STUDENT_ID = 8L;
    public static final String BOB_STUDENT_EXPIRED_DELETION_TOKEN = "someExpiredDeletionToken";

    public static final String CHARLIE_STUDENT_EMAIL = "charlie-student@test.com";
    public static final String CHARLIE_STUDENT_PASSWORD = "test1234";
    public static final long CHARLIE_STUDENT_ID = 9L;

    public static final String ERIKA_STUDENT_EMAIL = "erika-student@test.com";
    public static final String ERIKA_STUDENT_PASSWORD = "test1234";
    public static final long ERIKA_STUDENT_ID = 11L;
    public static final String ERIKA_PROVIDER_USER_ID = "08cf8f7b-5c48-423c-a66c-d5fc1be634db";

    public static final String TEST_SIGNUP_EMAIL = "test-signup@test.com";
    public static final String TEST_SIGNUP_PASSWORD = "test1234";

    public static final String TEST_TUTOR_EMAIL = "test-tutor@test.com";
    public static final String TEST_TUTOR_PASSWORD = "test1234";
    public static final long TEST_TUTOR_ID = 12L;

    public static final String TEST_UNVERIFIED_CAVEAT_EMAIL = "test-unverified-caveat@test.com";
    public static final String TEST_UNVERIFIED_CAVEAT_PASSWORD = "test1234";
    public static final long TEST_UNVERIFIED_CAVEAT_ID = 15L;

    // Groups
    public static final long TEST_TEACHERS_AB_GROUP_ID = 1L;
    public static final long DAVE_TEACHERS_BC_GROUP_ID = 2L;
    public static final long TEST_TUTORS_AB_GROUP_ID = 4L;
    public static final long HARRY_TEACHERS_AB_GROUP_ID = 5L;
    public static final long TEST_EVENT_LEADERS_OPEN_GROUP_ID = 6L;
    public static final long TEST_EVENT_LEADERS_WAITING_LIST_GROUP_ID = 7L;

    // Questions
    public static final String REGRESSION_TEST_PAGE_ID = "_regression_test_";
    public static final String ASSIGNMENT_TEST_PAGE_ID = "_assignment_test";
    public static final String EXACT_MATCH_TEST_PAGE_ID = "_exact_match_test_";
    public static final String FUZZY_MATCH_TEST_PAGE_ID = "_fuzzy_match_test_";
    public static final String SEARCH_TEST_SUPERSEDED_BY_ID = "superseded_by";
    public static final String SEARCH_TEST_SUPERSEDES_ID = "supersedes";

    // Gameboards
    public static final String ASSIGNMENTS_TEST_EXISTING_TEACHER_AB_GAMEBOARD_ID = "9313cadd-d4ce-420f-9331-300e3067b45f";
    public static final String ASSIGNMENTS_TEST_GAMEBOARD_ID = "865072ab-9223-495f-a809-5ee2b98252e4";
    public static final String ASSIGNMENTS_DATE_TEST_GAMEBOARD_ID = "5acb113a-4d8b-4a6d-9714-6992e7e3dc35";
    public static final String ADDITIONAL_MANAGER_TEST_GAMEBOARD_ID = "43c3ca6d-4d1b-49df-3ec4-4ce2b306eb45";

    // Assignments
    public static final Long ASSIGNMENTS_TEST_EXISTING_TEACHER_AB_ASSIGNMENT_ID = 2L;
    public static final Long ASSIGNMENTS_TEST_EXISTING_DAVE_BC_ASSIGNMENT_ID = 3L;
    public static final Long ASSIGNMENTS_TEST_EXISTING_HARRY_AB_ASSIGNMENT_ID = 4L;


    // Search
    public static final String SEARCH_TEST_CONCEPT_ID = "33935571-5a6c-4d42-a243-b5c01d4293e6";
    public static final String SEARCH_TEST_TOPIC_SUMMARY_ID = "f9714b47-dd81-48db-b509-cc25e1884474";
    public static final String SEARCH_TEST_EVENT_ID = "dc8686cf-be3b-4c0d-8761-1e5504146867";

    // Quizzes/tests
    public static final String QUIZ_TEST_QUIZ_ID = "_quiz_test";
    public static final String QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID = "_hidden_from_roles_student_quiz_test";
    public static final String QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID = "_hidden_from_roles_tutor_quiz_test";
}
