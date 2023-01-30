-- Note that this may require the pg_crypto extension:
--CREATE EXTENSION pgcrypto;

-- The anonymous database produced may not yet be fully anonymous due to issues with gameboards!

CREATE OR REPLACE FUNCTION anonymise(id TEXT, salt TEXT) RETURNS TEXT AS
$$ BEGIN
    RETURN encode(hmac(id, salt, 'sha256'), 'hex');
END; $$
LANGUAGE plpgsql;



CREATE OR REPLACE FUNCTION anonymise(id BIGINT, salt TEXT) RETURNS TEXT AS
$$ BEGIN
    RETURN anonymise(id::TEXT, salt);
END; $$
LANGUAGE plpgsql;

-- Idea is to create a clone of as much the database as is needed to run. Switch the scheme over from public to anonymous
-- and run the regression tests (or test manually?) to check if it works. Maybe check for every database table that is
-- mentioned in the codebase.

CREATE OR REPLACE FUNCTION create_anonymous_database(hash_salt TEXT) RETURNS boolean
AS
$$
BEGIN

DROP SCHEMA IF EXISTS anonymous CASCADE;
CREATE SCHEMA anonymous;


-- User related tables:

CREATE TABLE anonymous.users AS SELECT * FROM public.users;

CREATE TABLE anonymous.linked_accounts AS SELECT * FROM public.linked_accounts;

CREATE TABLE anonymous.user_preferences AS SELECT * FROM public.user_preferences;

CREATE TABLE anonymous.user_associations_tokens AS SELECT * FROM public.user_associations_tokens;

CREATE TABLE anonymous.external_accounts AS SELECT * FROM public.external_accounts;

CREATE TABLE anonymous.user_badges AS SELECT * FROM public.user_badges;

CREATE TABLE anonymous.user_credentials AS SELECT * FROM public.user_credentials;

CREATE TABLE anonymous.user_totp AS SELECT * FROM public.user_totp;

CREATE TABLE anonymous.user_email_preferences AS SELECT * FROM public.user_email_preferences;

CREATE TABLE anonymous.user_notifications AS SELECT * FROM public.user_notifications;

CREATE TABLE anonymous.user_alerts AS SELECT * FROM public.user_alerts;

CREATE TABLE anonymous.temporary_user_store (
    id character varying NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    last_updated timestamp with time zone DEFAULT now() NOT NULL,
    temporary_app_data jsonb
);

CREATE TABLE anonymous.ip_location_history AS SELECT * FROM public.ip_location_history;

CREATE TABLE anonymous.uk_post_codes AS SELECT * FROM public.uk_post_codes;


-- Group related tables:

CREATE TABLE anonymous.user_associations AS SELECT * FROM public.user_associations;

CREATE TABLE anonymous.groups AS SELECT * FROM public.groups;

CREATE TABLE anonymous.group_memberships AS SELECT * FROM public.group_memberships;

CREATE TABLE anonymous.group_additional_managers AS SELECT * FROM public.group_additional_managers;


-- Assignment related tables:

CREATE TABLE anonymous.gameboards AS SELECT * FROM public.gameboards;

CREATE TABLE anonymous.user_gameboards AS SELECT * FROM public.user_gameboards;

CREATE TABLE anonymous.assignments AS SELECT * FROM public.assignments;


-- Event bookings:

CREATE TABLE anonymous.event_bookings AS SELECT * FROM public.event_bookings;


-- Question attempt related tables:

CREATE TABLE anonymous.question_attempts AS SELECT * FROM public.question_attempts;

CREATE TABLE anonymous.user_streaks AS SELECT * FROM public.user_streaks;

CREATE TABLE anonymous.user_streaks_current_progress AS SELECT * FROM public.user_streaks_current_progress;

CREATE TABLE anonymous.user_streaks_weekly AS SELECT * FROM public.user_streaks_weekly;

CREATE TABLE anonymous.user_streaks_weekly_current_progress AS SELECT * FROM public.user_streaks_weekly_current_progress;

CREATE TABLE anonymous.user_streak_freezes AS SELECT * FROM public.user_streak_freezes;

CREATE TABLE anonymous.user_streak_targets AS SELECT * FROM public.user_streak_targets;


-- Quiz tables:

CREATE TABLE anonymous.quiz_assignments AS SELECT * FROM public.quiz_assignments;

CREATE TABLE anonymous.quiz_attempts AS SELECT * FROM public.quiz_attempts;

CREATE TABLE anonymous.quiz_question_attempts AS SELECT * FROM public.quiz_question_attempts;


-- Logged events

CREATE TABLE anonymous.logged_events (
    id integer NOT NULL,
    user_id character varying(100) NOT NULL,
    anonymous_user boolean NOT NULL,
    event_type character varying(255),
    event_details_type text,
    event_details jsonb,
    ip_address inet,
    "timestamp" timestamp without time zone
);


-- Logged events: (commented out)
--
-- CREATE TABLE anonymous.logged_events AS
--     SELECT
--         id,
--         anonymise(user_id, hash_salt) AS user_id,
--         anonymous_user,
--         event_type,
--         event_details,
--         timestamp
--     FROM public.logged_events
--     WHERE event_type IN (
--         -- Question usage:
--         'VIEW_QUESTION', 'ANSWER_QUESTION', 'QUESTION_ATTEMPT_RATE_LIMITED', 'QUICK_QUESTION_SHOW_ANSWER',
--         'QUESTION_PART_OPEN', 'VIEW_HINT',
--         'VIEW_RELATED_CONCEPT', 'VIEW_RELATED_QUESTION',
--         'VIDEO_ENDED', 'VIDEO_PAUSE', 'VIDEO_PLAY',
--         'VIEW_SUPERSEDED_BY_QUESTION',
--         -- Concept usage:
--         'VIEW_CONCEPT', 'CONCEPT_SECTION_OPEN', 'QUICK_QUESTION_TAB_VIEW', 'VIEW_GITHUB_CODE',
--         -- Page usage:
--         'VIEW_PAGE', 'VIEW_PAGE_FRAGMENT',
--         -- Assignment usage:
--         'VIEW_MY_ASSIGNMENTS', 'VIEW_MY_BOARDS_PAGE', 'VIEW_GAMEBOARD_BY_ID',
--         -- Group and assignment related usage:
--         'CREATE_USER_ASSOCIATION', 'RELEASE_USER_ASSOCIATION', 'REVOKE_USER_ASSOCIATION',
--         'CREATE_USER_GROUP', 'DELETE_USER_GROUP', 'ADD_ADDITIONAL_GROUP_MANAGER', 'DELETE_ADDITIONAL_GROUP_MANAGER',
--         'SET_NEW_ASSIGNMENT', 'DELETE_ASSIGNMENT', 'VIEW_BOARD_BUILDER', 'CLONE_GAMEBOARD',
--         'VIEW_ASSIGNMENT_PROGRESS', 'DOWNLOAD_ASSIGNMENT_PROGRESS_CSV', 'DOWNLOAD_GROUP_PROGRESS_CSV', 'VIEW_USER_PROGRESS',
--         -- Event related usage:
--         'ADMIN_EVENT_ATTENDANCE_RECORDED', 'ADMIN_EVENT_BOOKING_CANCELLED', 'ADMIN_EVENT_BOOKING_CONFIRMED',
--         'ADMIN_EVENT_BOOKING_CREATED', 'ADMIN_EVENT_BOOKING_DELETED', 'ADMIN_EVENT_WAITING_LIST_PROMOTION',
--         'EVENT_BOOKING', 'EVENT_BOOKING_CANCELLED', 'EVENT_WAITING_LIST_BOOKING',
--         -- Other useful events:
--         'EQN_EDITOR_LOG', 'USER_REGISTRATION', 'LOG_OUT', 'CHANGE_CONTENT_VERSION', 'DELETE_USER_ACCOUNT',
--         'USER_SCHOOL_CHANGE'
--     );
-- -- Anonymise specific details:
-- -- userIds
-- WITH
--     expanded_details AS (SELECT id, jsonb_array_elements_text(event_details->'userIds') AS elements FROM anonymous.logged_events WHERE event_details->'userIds' IS NOT NULL),
--     new_details AS (SELECT id, jsonb_strip_nulls(jsonb_agg(  anonymise(elements, hash_salt)  )) AS new_event_detail FROM expanded_details GROUP BY id)
-- UPDATE anonymous.logged_events
--     SET
--         event_details=jsonb_set(event_details, '{userIds}', new_event_detail)
--     FROM new_details
--     WHERE anonymous.logged_events.id=new_details.id;
-- -- userId
-- UPDATE anonymous.logged_events
--     SET event_details=jsonb_set(event_details, '{userId}', to_jsonb(anonymise(event_details->>'userId', hash_salt)))
--     WHERE event_details->>'userId' IS NOT NULL;
-- -- groupId
-- UPDATE anonymous.logged_events
--     SET event_details=jsonb_set(event_details, '{groupId}', to_jsonb(anonymise(event_details->>'groupId', hash_salt)))
--     WHERE event_details->>'groupId' IS NOT NULL;
-- -- assignmentId
-- UPDATE anonymous.logged_events
--     SET event_details=jsonb_set(event_details, '{assignmentId}', to_jsonb(anonymise(event_details->>'assignmentId', hash_salt)))
--     WHERE event_details->>'assignmentId' IS NOT NULL;
-- -- token
-- UPDATE anonymous.logged_events
--     SET event_details=event_details - 'token'
--     WHERE event_details->>'token' IS NOT NULL;
-- -- school_other
-- UPDATE anonymous.logged_events
-- SET event_details=jsonb_strip_nulls(event_details - 'oldSchoolOther')
-- WHERE event_details->>'oldSchoolOther' IS NOT NULL;
-- UPDATE anonymous.logged_events
-- SET event_details=jsonb_strip_nulls(event_details - 'newSchoolOther')
-- WHERE event_details->>'newSchoolOther' IS NOT NULL;

RETURN true;
END;
$$
LANGUAGE plpgsql;