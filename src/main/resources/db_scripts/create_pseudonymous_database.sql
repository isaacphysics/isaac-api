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

-- Slightly adapted from this postgresql mailing list post
-- https://www.postgresql.org/message-id/AANLkTi%3D34m32htPtrxb%2BTUks9i2oxu0YbJ7XyPbhK6BJ%40mail.gmail.com
CREATE OR REPLACE FUNCTION hex_to_integer(hexval varchar) RETURNS integer AS $$
    DECLARE
        result  integer;
    BEGIN
    EXECUTE 'SELECT x''' || LEFT(hexval, 8) || '''::integer' INTO result;
    RETURN result;
END;
$$
LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION anonymise_int(id BIGINT, salt TEXT) RETURNS integer AS
$$ BEGIN
    RETURN hex_to_integer(anonymise(id, salt));
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

CREATE TABLE anonymous.users AS
    SELECT
        anonymise_int(id, hash_salt) as id,
        anonymise(_id, hash_salt),
        'FamilyName-' || LEFT(anonymise(public.users.id, hash_salt), 5) as family_name,
        'GivenName-' || LEFT(anonymise(public.users.id, hash_salt), 5) as given_name,
        anonymise(public.users.id, hash_salt) || '@isaac-cs-anonymous-email.com' as email,
        role,
        CASE
            WHEN date_part('month', date_of_birth) < 9 THEN make_date((date_part('year', date_of_birth)-1)::int, 9, 1)
            WHEN date_part('month', date_of_birth) >= 9 THEN make_date(date_part('year', date_of_birth)::int, 9, 1)
            ELSE NULL
        END AS date_of_birth,
        'UNKNOWN' as gender,
        school_id,
        registration_date,
        last_seen
    FROM public.users;

-- linked_accounts ignored

CREATE TABLE anonymous.user_preferences AS
    SELECT
        anonymise_int(user_id, hash_salt) AS user_id,
        preference_type,
        preference_name,
        preference_value,
        last_updated
    FROM public.user_preferences;

CREATE TABLE anonymous.user_associations_tokens AS
    SELECT
        token,
        anonymise_int(owner_user_id, hash_salt) AS owner_user_id,
        anonymise_int(group_id, hash_salt) AS group_id,
    FROM public.user_associations_tokens;

-- external_accounts ignored

CREATE TABLE anonymous.user_badges AS SELECT * FROM public.user_badges;

CREATE TABLE anonymous.user_credentials AS
    SELECT
        anonymise_int(user_id, hash_salt) AS user_id,
        'bflrBg4DlCjZRl/k2hn2cotSWZERu44OFBeaywz/6Gzy4bX0k6TVjh/l+uI3cF1SKyib7zsk4fOulb1Moud+4A==' as password,
        'hHdj7z1JEus5yTf6FmEznw==' as secure_salt,
        'SeguePBKDF2v2' as security_scheme,
        NULL as reset_token,
        NULL as reset_expiry,
        created,
        last_updated
    FROM public.user_credentials;

-- user_totp ignored

-- user_email_preferences ignored

-- user_notifications ignored

-- user_alerts ignored

-- temporary_user_store ignored

-- ip_location_history ignored

CREATE TABLE anonymous.uk_post_codes AS SELECT * FROM public.uk_post_codes;


-- Group related tables:

CREATE TABLE anonymous.user_associations AS
    SELECT
        anonymise_int(user_id_granting_permission, hash_salt) AS user_id_granting_permission ,
        anonymise_int(user_id_receiving_permission, hash_salt) AS user_id_receiving_permission,
        created
    FROM public.user_associations;

CREATE TABLE anonymous.groups AS
    SELECT
        anonymise_int(id, hash_salt) AS id,
        'Group-' || anonymise_int(id, hash_salt)::varchar(255) as group_name,
        anonymise_int(owner_id, hash_salt) AS owner_id,
        created,
        archived,
        group_status,
        last_updated
    FROM public.groups;

CREATE TABLE anonymous.group_memberships AS
    SELECT
        anonymise_int(group_id, hash_salt) AS group_id,
        anonymise_int(user_id, hash_salt) AS user_id,
        created,
        updated,
        status
    FROM public.group_memberships;

CREATE TABLE anonymous.group_additional_managers AS
    SELECT
        anonymise_int(user_id, hash_salt) AS user_id,
        anonymise_int(group_id, hash_salt) AS group_id,
        created
    FROM public.group_additional_managers;


-- Assignment related tables:

CREATE TABLE anonymous.gameboards AS
    SELECT
       id,
       'Gameboard-' || public.gameboards.id as title,
       contents,
       wildcard,
       wildcard_position,
       game_filter,
       anonymise_int(owner_user_id, hash_salt) AS owner_user_id,
       creation_method,
       creation_date,
       tags
    FROM public.gameboards;

CREATE TABLE anonymous.user_gameboards AS
    SELECT
        anonymise_int(user_id, hash_salt) as user_id,
        gameboard_id,
        created
        last_visited
    FROM public.user_gameboards;

CREATE TABLE anonymous.assignments AS
    SELECT
        id,
        gameboard_id,
        anonymise_int(group_id, hash_salt) AS group_id,
        anonymise_int(owner_user_id, hash_salt) as owner_user_id,
        '' as notes,
        creation_date,
        due_date,
        scheduled_start_date
    FROM public.assignments;


-- Event bookings:

CREATE TABLE anonymous.event_bookings AS
    SELECT
        id,
        'Event-' || anonymise(event_id, hash_salt) as event_id,
        created,
        anonymise_int(user_id, hash_salt) as user_id,
        anonymise_int(reserved_by, hash_salt) as reserved_by,
        status,
        updated,
        additional_booking_information, -- might need removing (replace with {}::jsonb perhaps?)
        pii_removed
    FROM public.event_bookings;


-- Question attempt related tables:

CREATE TABLE anonymous.question_attempts AS
    SELECT
        id,
        anonymise_int(user_id, hash_salt) as user_id,
        question_id,
        '{}'::jsonb as question_attempt,
        correct,
        timestamp
    FROM public.question_attempts;

CREATE TABLE anonymous.user_streak_freezes AS
    SELECT
        anonymise_int(user_id, hash_salt) as user_id,
        start_date,
        end_date,
        'Streak freeze' as comment
    FROM public.user_streak_freezes;

CREATE TABLE anonymous.user_streak_targets AS
    SELECT
        anonymise_int(user_id, hash_salt) as user_id,
        target_count,
        start_date,
        end_date,
        'Streak target' as comment
    FROM public.user_streak_targets;


-- Quiz tables:

CREATE TABLE anonymous.quiz_assignments AS
    SELECT
        id,
        quiz_id,
        anonymise_int(group_id, hash_salt) as group_id,
        anonymise_int(owner_user_id, hash_salt) as owner_user_id,
        creation_date,
        due_date,
        quiz_feedback_mode,
        deleted
    FROM public.quiz_assignments;

CREATE TABLE anonymous.quiz_attempts AS
    SELECT
        id,
        anonymise_int(user_id, hash_salt) as user_id,
        quiz_id,
        quiz_assignment_id,
        start_date,
        completed_date
    FROM public.quiz_attempts;

CREATE TABLE anonymous.quiz_question_attempts AS
    SELECT
        id,
        quiz_attempt_id,
        question_id,
        '{}'::jsonb as question_attempt,
        correct,
        timestamp
    FROM public.quiz_question_attempts;


-- Logged events

CREATE TABLE anonymous.logged_events AS
    SELECT
        id,
        anonymise(user_id, hash_salt) AS user_id,
        anonymous_user,
        event_type,
        event_details,
        '192.168.1.1'::inet as ip_address,
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
        'VIEW_CONCEPT', 'CONCEPT_SECTION_OPEN', 'QUICK_QUESTION_TAB_VIEW', 'VIEW_GITHUB_CODE',
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
    -- school_other
    UPDATE anonymous.logged_events
    SET event_details=jsonb_strip_nulls(event_details - 'oldSchoolOther')
    WHERE event_details->>'oldSchoolOther' IS NOT NULL;
    UPDATE anonymous.logged_events
    SET event_details=jsonb_strip_nulls(event_details - 'newSchoolOther')
    WHERE event_details->>'newSchoolOther' IS NOT NULL;

RETURN true;
END;
$$
LANGUAGE plpgsql;