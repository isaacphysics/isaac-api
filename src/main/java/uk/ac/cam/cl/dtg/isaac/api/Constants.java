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

import com.google.common.collect.ImmutableSet;

import java.util.Set;

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
    public static final String EVENT_TYPE = "isaacEventPage";

    public static final String RELATED_CONTENT_FIELDNAME = "relatedContent";

    public static final int NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES = 2;

    /*
     * Game specific variables.
     */
    public static final int GAME_BOARD_TARGET_SIZE = 10;
    public static final Set<String> FASTTRACK_GAMEBOARD_WHITELIST
            = ImmutableSet.of("ft_core_2017"); // Keep in sync with APP's fastTrackProgressEnabledBoards constant

    /**
     * GameboardItemState Represents the potential states of a gameboard item.
     */
    public enum GameboardItemState {
        PERFECT, PASSED, IN_PROGRESS, NOT_ATTEMPTED, FAILED
    }

    public enum FastTrackConceptState {
        ft_upper,
        ft_lower;
        public static FastTrackConceptState getStateFromTags(Set<String> tags) {
            if (tags.contains("ft_upper")) {
                return ft_upper;
            } else if (tags.contains("ft_lower")) {
                return ft_lower;
            } else {
                return null;
            }
        }
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

    public static final String GAMEBOARD_ID_FKEY = "gameboardId";

    public static final String EVENT_DATE_FIELDNAME = "date";
    public static final String EVENT_ENDDATE_FIELDNAME = "endDate";


    public static final Integer DEFAULT_GAMEBOARDS_RESULTS_LIMIT = 6;
    public static final Integer MAX_PODS_TO_RETURN = 10;
    public static final Integer SEARCH_MAX_WINDOW_SIZE = 10000;

    // Log events
    public static final String VIEW_QUESTION = "VIEW_QUESTION";
    public static final String QUESTION_ID_LOG_FIELDNAME = "questionId";
    public static final String ADD_BOARD_TO_PROFILE = "ADD_BOARD_TO_PROFILE";
    public static final String DELETE_BOARD_FROM_PROFILE = "ADD_BOARD_TO_PROFILE";
    public static final String GLOBAL_SITE_SEARCH = "GLOBAL_SITE_SEARCH";
    public static final String VIEW_CONCEPT = "VIEW_CONCEPT";
    public static final String CONCEPT_ID_LOG_FIELDNAME = "conceptId";
    public static final String VIEW_PAGE = "VIEW_PAGE";
    public static final String PAGE_ID_LOG_FIELDNAME = "pageId";

    public static final String VIEW_MY_ASSIGNMENTS = "VIEW_MY_ASSIGNMENTS";
    public static final String VIEW_GROUPS_ASSIGNMENTS = "VIEW_GROUPS_ASSIGNMENTS";
    public static final String VIEW_ASSIGNMENT_PROGRESS = "VIEW_ASSIGNMENT_PROGRESS";
    public static final String DOWNLOAD_ASSIGNMENT_PROGRESS_CSV = "DOWNLOAD_ASSIGNMENT_PROGRESS_CSV";
    public static final String DOWNLOAD_GROUP_PROGRESS_CSV = "DOWNLOAD_GROUP_PROGRESS_CSV";
    public static final String SET_NEW_ASSIGNMENT = "SET_NEW_ASSIGNMENT";
    public static final String DELETE_ASSIGNMENT = "DELETE_ASSIGNMENT";

    public static final String VIEW_MY_BOARDS_PAGE = "VIEW_MY_BOARDS_PAGE";

    public static final String VIEW_USER_PROGRESS = "VIEW_USER_PROGRESS";

    public static final String CREATE_GAMEBOARD = "CREATE_GAMEBOARD";

    public static final String SUBJECT_INTEREST = "SUBJECT_INTEREST";

    /**
     * Private constructor to prevent this class being created.
     */
    private Constants() {
        // not allowed to create one of these as it wouldn't make sense.
    }
}
