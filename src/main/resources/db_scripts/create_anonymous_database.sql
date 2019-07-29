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



CREATE OR REPLACE FUNCTION create_anonymous_database(hash_salt TEXT) RETURNS boolean
AS
$$
BEGIN

DROP SCHEMA IF EXISTS anonymous CASCADE;
CREATE SCHEMA anonymous;

-- User related tables:

CREATE TABLE anonymous.users AS
    SELECT
        id AS old_id,
        anonymise(id, hash_salt) AS id,
        role,
        CASE
            WHEN date_part('month', date_of_birth) < 9 THEN make_date((date_part('year', date_of_birth)-1)::int, 9, 1)
            WHEN date_part('month', date_of_birth) >= 9 THEN make_date(date_part('year', date_of_birth)::int, 9, 1)
            ELSE NULL
        END AS date_of_birth,
        school_id,
        registration_date,
        last_seen
    FROM public.users;

CREATE TABLE anonymous.linked_accounts AS
    SELECT
        anonymise(user_id, hash_salt) AS user_id,
        provider
    FROM public.linked_accounts;

CREATE TABLE anonymous.user_preferences AS
    SELECT
        anonymise(user_id, hash_salt) AS user_id,
        preference_type,
        preference_name,
        preference_value
    FROM public.user_preferences;


-- Group related tables:

CREATE TABLE anonymous.user_associations AS
    SELECT
        anonymise(user_id_granting_permission, hash_salt) AS user_id_granting_permission ,
        anonymise(user_id_receiving_permission, hash_salt) AS user_id_receiving_permission,
        created
    FROM public.user_associations;

CREATE TABLE anonymous.groups AS
    SELECT
        anonymise(id, hash_salt) AS id,
        anonymise(owner_id, hash_salt) AS owner_id,
        created,
        archived
    FROM public.groups;

CREATE TABLE anonymous.group_memberships AS
    SELECT
        anonymise(group_id, hash_salt) AS group_id,
        anonymise(user_id, hash_salt) AS user_id,
        created
    FROM public.group_memberships;

CREATE TABLE anonymous.group_additional_managers AS
    SELECT
        anonymise(group_id, hash_salt) AS group_id,
        anonymise(user_id, hash_salt) AS user_id,
        created
    FROM public.group_additional_managers;


-- Assignment related tables:

CREATE TABLE anonymous.gameboards AS
    SELECT
        id,
        questions,
        wildcard,
        wildcard_position,
        game_filter,
        anonymise(owner_user_id, hash_salt) AS owner_user_id,
        creation_method,
        creation_date,
        tags
    FROM public.gameboards;

CREATE TABLE anonymous.user_gameboards AS
    SELECT
        anonymise(user_id, hash_salt) AS user_id,
        gameboard_id,
        created,
        last_visited
    FROM public.user_gameboards;

CREATE TABLE anonymous.assignments AS
    SELECT
        anonymise(id, hash_salt) AS id,
        gameboard_id,
        anonymise(group_id, hash_salt) AS group_id,
        anonymise(owner_user_id, hash_salt) AS owner_user_id,
        creation_date,
        due_date
    FROM public.assignments;


-- Event bookings:

CREATE TABLE anonymous.event_bookings AS
    SELECT
        id,
        event_id,
        created,
        anonymise(user_id, hash_salt) AS user_id,
        status,
        updated
    FROM public.event_bookings;


-- Question attempt related tables:

CREATE TABLE anonymous.question_attempts AS
    SELECT
        id,
        anonymise(user_id, hash_salt) AS user_id,
        question_id,
        question_attempt,
        correct,
        timestamp
    FROM public.question_attempts;

CREATE TABLE anonymous.user_streak_freezes AS
    SELECT
        anonymise(user_id, hash_salt) AS user_id,
        start_date,
        end_date
    FROM public.user_streak_freezes;

CREATE TABLE anonymous.user_streak_targets AS
    SELECT
        anonymise(user_id, hash_salt) AS user_id,
        target_count,
        start_date,
        end_date
    FROM public.user_streak_targets;


-- Logged events:

CREATE TABLE anonymous.logged_events AS
    SELECT
        id,
        anonymise(user_id, hash_salt) AS user_id,
        anonymous_user,
        event_type,
        event_details_type,
        event_details,
        timestamp
    FROM public.logged_events
    WHERE event_type IN (
        -- Question usage:
        'VIEW_QUESTION', 'ANSWER_QUESTION', 'QUESTION_ATTEMPT_RATE_LIMITED', 'QUICK_QUESTION_SHOW_ANSWER',
        'QUESTION_PART_OPEN', 'VIEW_HINT',
        'VIEW_RELATED_CONCEPT', 'VIEW_RELATED_QUESTION',
        'VIDEO_ENDED', 'VIDEO_PAUSE', 'VIDEO_PLAY',
        'VIEW_SUPERSEDED_BY_QUESTION',
        -- Concept usage:
        'VIEW_CONCEPT', 'CONCEPT_SECTION_OPEN', 'QUICK_QUESTION_TAB_VIEW',
        -- Page usage:
        'VIEW_PAGE', 'VIEW_PAGE_FRAGMENT',
        -- Assignment usage:
        'VIEW_MY_ASSIGNMENTS', 'VIEW_MY_BOARDS_PAGE', 'VIEW_GAMEBOARD_BY_ID',
        -- Group and assignment related usage:
        'CREATE_USER_ASSOCIATION', 'RELEASE_USER_ASSOCIATION', 'REVOKE_USER_ASSOCIATION',
        'CREATE_USER_GROUP', 'DELETE_USER_GROUP', 'ADD_ADDITIONAL_GROUP_MANAGER', 'DELETE_ADDITIONAL_GROUP_MANAGER',
        'SET_NEW_ASSIGNMENT', 'DELETE_ASSIGNMENT', 'VIEW_BOARD_BUILDER', 'CLONE_GAMEBOARD',
        'VIEW_ASSIGNMENT_PROGRESS', 'DOWNLOAD_ASSIGNMENT_PROGRESS_CSV', 'DOWNLOAD_GROUP_PROGRESS_CSV', 'VIEW_USER_PROGRESS',
        -- Event related usage:
        'ADMIN_EVENT_ATTENDANCE_RECORDED', 'ADMIN_EVENT_BOOKING_CANCELLED', 'ADMIN_EVENT_BOOKING_CONFIRMED',
        'ADMIN_EVENT_BOOKING_CREATED', 'ADMIN_EVENT_BOOKING_DELETED', 'ADMIN_EVENT_WAITING_LIST_PROMOTION',
        'EVENT_BOOKING', 'EVENT_BOOKING_CANCELLED', 'EVENT_WAITING_LIST_BOOKING',
        -- Other useful events:
        'EQN_EDITOR_LOG', 'USER_REGISTRATION', 'LOG_OUT', 'CHANGE_CONTENT_VERSION', 'DELETE_USER_ACCOUNT',
        'USER_SCHOOL_CHANGE'
    );
-- Anonymise specific details:
-- userIds
WITH 
    expanded_details AS (SELECT id, jsonb_array_elements_text(event_details->'userIds') AS elements FROM anonymous.logged_events WHERE event_details->'userIds' IS NOT NULL),
    new_details AS (SELECT id, jsonb_strip_nulls(jsonb_agg(  anonymise(elements, hash_salt)  )) AS new_event_detail FROM expanded_details GROUP BY id)
UPDATE anonymous.logged_events
    SET
        event_details=jsonb_set(event_details, '{userIds}', new_event_detail)
    FROM new_details
    WHERE anonymous.logged_events.id=new_details.id;
-- userId
UPDATE anonymous.logged_events
    SET event_details=jsonb_set(event_details, '{userId}', to_jsonb(anonymise(event_details->>'userId', hash_salt)))
    WHERE event_details->>'userId' IS NOT NULL;
-- groupId
UPDATE anonymous.logged_events
    SET event_details=jsonb_set(event_details, '{groupId}', to_jsonb(anonymise(event_details->>'groupId', hash_salt)))
    WHERE event_details->>'groupId' IS NOT NULL;
-- assignmentId
UPDATE anonymous.logged_events
    SET event_details=jsonb_set(event_details, '{assignmentId}', to_jsonb(anonymise(event_details->>'assignmentId', hash_salt)))
    WHERE event_details->>'assignmentId' IS NOT NULL;
-- token
UPDATE anonymous.logged_events
    SET event_details=event_details - 'token'
    WHERE event_details->>'token' IS NOT NULL;


RETURN true;
END;
$$
LANGUAGE plpgsql;