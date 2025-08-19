/**
 * Copyright 2014 Stephen Cummins
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

import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_SERVER_LOG_TYPES;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.ac.cam.cl.dtg.segue.api.Constants.LogType;

/**
 * Utility class to provide common isaac-specific constants.
 */
public final class Constants {
  public static final String PROXY_PATH = "PROXY_PATH";

  /*
   * Subject specific constants.
   */
  public static final String CONCEPT_TYPE = "isaacConceptPage";
  public static final String QUESTION_TYPE = "isaacQuestionPage";
  public static final String WILDCARD_TYPE = "isaacWildcard";
  public static final String PAGE_FRAGMENT_TYPE = "isaacPageFragment";
  public static final String POD_FRAGMENT_TYPE = "isaacPod";
  public static final String PAGE_TYPE = "page";
  public static final String QUESTIONS_PAGE_TYPE = "questionsPage";
  public static final String TOPIC_SUMMARY_PAGE_TYPE = "isaacTopicSummaryPage";
  public static final String EVENT_TYPE = "isaacEventPage";
  public static final String QUIZ_TYPE = "isaacQuiz";

  public static final String SEARCHABLE_TAG = "search_result";
  public static final String HIDE_FROM_FILTER_TAG = "nofilter";
  public static final String RELATED_CONTENT_FIELDNAME = "relatedContent";

  public static final Set<String> SITE_WIDE_SEARCH_VALID_DOC_TYPES = Set.of(
      QUESTION_TYPE, CONCEPT_TYPE, TOPIC_SUMMARY_PAGE_TYPE, PAGE_TYPE, EVENT_TYPE);

  public static final int NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES = 2;

  /*
   * Game specific variables.
   */
  public static final int GAME_BOARD_TARGET_SIZE = 10;

  /**
   * GameboardItemState Represents the potential states of a gameboard item.
   */
  public enum GameboardItemState {
    PERFECT, PASSED, IN_PROGRESS, NOT_ATTEMPTED, FAILED
  }

  public enum QuestionPartState {
    CORRECT, INCORRECT, NOT_ATTEMPTED
  }

  /**
   * GameboardState Represents the potential states of a gameboard.
   */
  public enum GameboardState {
    COMPLETED, IN_PROGRESS, NOT_ATTEMPTED
  }

  // field names
  public static final String CREATED_DATE_FIELDNAME = "created";
  public static final String VISITED_DATE_FIELDNAME = "lastVisited";

  public static final String COMPLETION_FIELDNAME = "percentageCompleted";

  public static final String GAMEBOARD_ID_FKEY = "gameboardId";
  public static final String GAMEBOARD_ID_FKEYS = "gameboardIds";

  public static final String QUIZ_ID_FKEY = "quizId";
  public static final String QUIZ_SECTION = "quizSection";

  public static final String DATE_FIELDNAME = "date";
  public static final String ENDDATE_FIELDNAME = "endDate";
  public static final String PRIVATE_EVENT_FIELDNAME = "privateEvent";

  public static final String ALL_BOARDS = "ALL";
  public static final Integer DEFAULT_GAMEBOARDS_RESULTS_LIMIT = 6;
  public static final Integer MAX_PODS_TO_RETURN = 10;
  public static final Integer SEARCH_MAX_WINDOW_SIZE = 10000;
  public static final Integer GAMEBOARD_MAX_TITLE_LENGTH = 255;
  public static final Integer RANDOM_WILDCARD_SEARCH_LIMIT = 999;
  public static final Integer QUIZ_MAX_SEARCH_RESULTS = 9000;

  // Log events
  public static final String QUESTION_ID_LOG_FIELDNAME = "questionId";
  public static final String CONCEPT_ID_LOG_FIELDNAME = "conceptId";
  public static final String PAGE_ID_LOG_FIELDNAME = "pageId";
  public static final String FRAGMENT_ID_LOG_FIELDNAME = "pageFragmentId";
  public static final String DOCUMENT_PATH_LOG_FIELDNAME = "path";

  public static final Long DEFAULT_MISUSE_STATISTICS_LIMIT = 5L;

  /**
   * Class to represent Isaac log types.
   */
  public enum IsaacServerLogType implements LogType {
    ADD_BOARD_TO_PROFILE,
    CREATE_GAMEBOARD,
    DELETE_ASSIGNMENT,
    DELETE_QUIZ_ASSIGNMENT,
    DELETE_BOARD_FROM_PROFILE,
    DOWNLOAD_ASSIGNMENT_PROGRESS_CSV,
    DOWNLOAD_GROUP_PROGRESS_CSV,
    DOWNLOAD_FILE,
    GLOBAL_SITE_SEARCH,
    SET_NEW_ASSIGNMENT,
    SET_NEW_QUIZ_ASSIGNMENT,
    UPDATE_QUIZ_DEADLINE,
    UPDATE_QUIZ_FEEDBACK_MODE,
    VIEW_ASSIGNMENT_PROGRESS,
    VIEW_CONCEPT,
    VIEW_MY_BOARDS_PAGE,
    VIEW_PAGE,
    VIEW_PAGE_FRAGMENT,
    VIEW_QUESTION,
    VIEW_QUIZ_SECTION,
    VIEW_TOPIC_SUMMARY_PAGE,
    VIEW_USER_PROGRESS,
  }

  public static final Set<String> ISAAC_SERVER_LOG_TYPES =
      Arrays.stream(IsaacServerLogType.values()).map(IsaacServerLogType::name).collect(Collectors.toSet());

  public enum IsaacClientLogType implements LogType {
    QUESTION_PART_OPEN,
    CONCEPT_SECTION_OPEN,
    ACCORDION_SECTION_OPEN,
    VIDEO_PLAY,
    VIDEO_PAUSE,
    VIDEO_ENDED,
    VIEW_SUPERSEDED_BY_QUESTION,
    CLONE_GAMEBOARD,
    VIEW_HINT,
    QUESTION_CONFIDENCE_BEFORE,
    QUESTION_CONFIDENCE_AFTER,
    QUESTION_CONFIDENCE_HINT,
    QUICK_QUESTION_SHOW_ANSWER,
    VIEW_RELATED_CONCEPT,
    VIEW_RELATED_QUESTION,
    VIEW_RELATED_PAGE,
    VIEW_MY_ASSIGNMENTS,
    VIEW_GAMEBOARD_BY_ID,
    VIEW_GITHUB_CODE,
    ACCEPT_COOKIES,
    LEAVE_GAMEBOARD_BUILDER,
    SAVE_GAMEBOARD,
    CLIENT_SIDE_ERROR,
    LOGIN_MODAL_SHOWN,
    USER_CONSISTENCY_WARNING_SHOWN,
    REVIEW_TEACHER_CONNECTIONS,
    REPORT_CONTENT_ACCORDION_SECTION,
    REPORT_CONTENT_PAGE
  }

  public static final Set<String> ISAAC_CLIENT_LOG_TYPES =
      Arrays.stream(IsaacClientLogType.values()).map(IsaacClientLogType::name).collect(Collectors.toSet());

  public static final Set<String> ALL_ACCEPTED_LOG_TYPES =
      Stream.of(SEGUE_SERVER_LOG_TYPES, ISAAC_SERVER_LOG_TYPES, ISAAC_CLIENT_LOG_TYPES)
          .flatMap(Collection::stream)
          .collect(Collectors.toCollection(HashSet::new));

  public enum IsaacUserPreferences {
    BETA_FEATURE, EXAM_BOARD, PROGRAMMING_LANGUAGE, BOOLEAN_NOTATION, DISPLAY_SETTING
  }

  public static final Integer EMAIL_EVENT_REMINDER_DAYS_AHEAD = 3;
  public static final Integer EMAIL_EVENT_FEEDBACK_DAYS_AGO = 60;

  // Response messages
  public static final String EMPTY_ASSIGNMENT_GAMEBOARD =
      "Assignment gameboard has no questions, or its questions no longer exist. Cannot fetch assignment progress.";

  public static final Integer DO_HASHCODE_PRIME = 31;

  // Email Template Tokens
  public static final String EMAIL_TEMPLATE_TOKEN_CONTACT_US_URL = "contactUsURL";
  public static final String EMAIL_TEMPLATE_TOKEN_AUTHORIZATION_LINK = "authorizationLink";
  public static final String EMAIL_TEMPLATE_TOKEN_EVENT_DETAILS = "event.emailEventDetails";
  public static final String EMAIL_TEMPLATE_TOKEN_EVENT = "event";
  public static final String EMAIL_TEMPLATE_TOKEN_EVENT_URL = "eventURL";
  public static final String EMAIL_TEMPLATE_TOKEN_PROVIDER = "provider";

  // Email Template Ids
  public static final String EMAIL_TEMPLATE_ID_EVENT_BOOKING_CONFIRMED = "email-event-booking-confirmed";
  public static final String EMAIL_TEMPLATE_ID_WAITING_LIST_ONLY_ADDITION =
      "email-event-waiting-list-only-addition-notification";
  public static final String EMAIL_TEMPLATE_ID_WAITING_LIST_ADDITION =
      "email-event-waiting-list-addition-notification";

  // Exception Message Strings
  public static final String EXCEPTION_MESSAGE_CONTENT_ERROR_RETRIEVING_BOOKING =
      "Content Database error occurred while trying to retrieve event booking information.";
  public static final String EXCEPTION_MESSAGE_DATABASE_ERROR_RETRIEVING_BOOKING =
      "Database error occurred while trying to retrieve all event booking information.";
  public static final String EXCEPTION_MESSAGE_EVENT_REQUEST_ERROR = "Error during event request";
  public static final String EXCEPTION_MESSAGE_ERROR_LOCATING_CONTENT = "Error locating the content you requested.";
  public static final String EXCEPTION_MESSAGE_CANNOT_BOOK_CANCELLED_EVENT =
      "The event is cancelled, so no bookings are being accepted.";
  public static final String EXCEPTION_MESSAGE_DATABASE_ERROR_CREATING_BOOKING =
      "Database error occurred while trying to book a user onto an event.";
  public static final String EXCEPTION_MESSAGE_DATABASE_ERROR_DELETING_BOOKING =
      "Database error occurred while trying to delete an event booking.";
  public static final String EXCEPTION_MESSAGE_CANNOT_LOCATE_USER = "Unable to locate user specified.";
  public static final String EXCEPTION_MESSAGE_TEMPLATE_DUPLICATE_BOOKING =
      "Unable to book onto event (%s) as user (%s) is already booked on to it.";
  public static final String EXCEPTION_MESSAGE_TEMPLATE_CANCELLED_EVENT =
      "Unable to book user (%s) onto event (%s); the event is cancelled.";
  public static final String EXCEPTION_MESSAGE_TEMPLATE_UNABLE_TO_SEND_EMAIL =
      "Unable to send event email ({}) to user ({})";
  public static final String EXCEPTION_MESSAGE_NOT_EVENT = "Content object is not an event page.";
  public static final String EXCEPTION_MESSAGE_CANNOT_CREATE_BOOKING_DTO =
      "Unable to create event booking DTO from DO.";
  public static final String EXCEPTION_MESSAGE_INVALID_EMAIL = "The email address provided is invalid.";

  /**
   * Private constructor to prevent this class being created.
   */
  private Constants() {
    // not allowed to create one of these as it wouldn't make sense.
  }
}
