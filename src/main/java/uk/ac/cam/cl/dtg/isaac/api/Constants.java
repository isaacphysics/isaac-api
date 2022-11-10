/**
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.isaac.api;

import uk.ac.cam.cl.dtg.segue.api.Constants.LogType;
import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_SERVER_LOG_TYPES;

/**
 * Utility class to provide common isaac-specific constants.
 *
 */
public final class Constants {
    public static final String PROXY_PATH = "PROXY_PATH";

    /*
     * Subject specific constants.
     */
    public static final String CONCEPT_TYPE = "isaacConceptPage";
    public static final String QUESTION_TYPE = "isaacQuestionPage";
    public static final String FAST_TRACK_QUESTION_TYPE = "isaacFastTrackQuestionPage";
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

    public static final Set<String> SITE_WIDE_SEARCH_VALID_DOC_TYPES = ImmutableSet.of(
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
        PERFECT, PASSED, IN_PROGRESS, NOT_ATTEMPTED, FAILED;
    }

    public enum QuestionPartState {
        CORRECT, INCORRECT, NOT_ATTEMPTED;
    }

    public enum FASTTRACK_LEVEL {
        ft_top_ten,
        ft_upper,
        ft_lower;
        public static FASTTRACK_LEVEL getStateFromTags(Set<String> tags) {
            FASTTRACK_LEVEL state = null;
            if (tags.contains("ft_top_ten")) {
                state = ft_top_ten;
            } else if (tags.contains("ft_upper")) {
                state = ft_upper;
            } else if (tags.contains("ft_lower")) {
                state = ft_lower;
            }
            return state;
        }
    }
    public static final String FASTTRACK_GAMEBOARD_WHITELIST = "FASTTRACK_GAMEBOARD_WHITELIST";

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

    public static final String ALL_BOARDS = "ALL";
    public static final Integer DEFAULT_GAMEBOARDS_RESULTS_LIMIT = 6;
    public static final Integer MAX_PODS_TO_RETURN = 10;
    public static final Integer SEARCH_MAX_WINDOW_SIZE = 10000;
    public static final Integer GAMEBOARD_MAX_TITLE_LENGTH = 255;

    // Log events
    public static final String QUESTION_ID_LOG_FIELDNAME = "questionId";
    public static final String CONCEPT_ID_LOG_FIELDNAME = "conceptId";
    public static final String PAGE_ID_LOG_FIELDNAME = "pageId";
    public static final String FRAGMENT_ID_LOG_FIELDNAME = "pageFragmentId";
    public static final String DOCUMENT_PATH_LOG_FIELDNAME = "path";

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
    public static final Set<String> ISAAC_SERVER_LOG_TYPES = Arrays.stream(IsaacServerLogType.values()).map(IsaacServerLogType::name).collect(Collectors.toSet());

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
        REVIEW_TEACHER_CONNECTIONS,
        REPORT_CONTENT_ACCORDION_SECTION,
        REPORT_CONTENT_PAGE
    }
    public static final Set<String> ISAAC_CLIENT_LOG_TYPES = Arrays.stream(IsaacClientLogType.values()).map(IsaacClientLogType::name).collect(Collectors.toSet());

    public static final Set<String> ALL_ACCEPTED_LOG_TYPES = new HashSet<String>() {{
        addAll(SEGUE_SERVER_LOG_TYPES);
        addAll(ISAAC_SERVER_LOG_TYPES);
        addAll(ISAAC_CLIENT_LOG_TYPES);
    }};

    public enum IsaacUserPreferences {
        SUBJECT_INTEREST, BETA_FEATURE, EXAM_BOARD, PROGRAMMING_LANGUAGE, BOOLEAN_NOTATION, DISPLAY_SETTING
    }

    /**
     * Private constructor to prevent this class being created.
     */
    private Constants() {
        // not allowed to create one of these as it wouldn't make sense.
    }
}
