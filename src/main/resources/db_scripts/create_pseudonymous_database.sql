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
        id,
        anonymise(_id, hash_salt) as _id,
        'FamilyName-' || id::varchar(255) as family_name,
        'GivenName-' || id::varchar(255) as given_name,
        id::varchar(255) || '@isaac-cs-anonymous-email.com' as email,
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

CREATE TABLE anonymous.user_preferences AS SELECT * FROM public.user_preferences;

CREATE TABLE anonymous.user_associations_tokens AS SELECT * FROM public.user_associations_tokens;

-- external_accounts ignored

CREATE TABLE anonymous.user_badges AS SELECT * FROM public.user_badges;

CREATE TABLE anonymous.user_credentials AS
    SELECT
        user_id,
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

CREATE TABLE anonymous.user_associations AS SELECT * FROM public.user_associations;

CREATE TABLE anonymous.groups AS
    SELECT
        id,
        'Group-' || id::varchar(255) as group_name,
        owner_id,
        created,
        archived,
        group_status,
        last_updated
    FROM public.groups;

CREATE TABLE anonymous.group_memberships AS SELECT * FROM public.group_memberships;

CREATE TABLE anonymous.group_additional_managers AS SELECT * FROM public.group_additional_managers;


-- Assignment related tables:

CREATE TABLE anonymous.gameboards AS
    SELECT
       id,
       'Gameboard-' || public.gameboards.id as title,
       contents,
       wildcard,
       wildcard_position,
       game_filter,
       owner_user_id,
       creation_method,
       creation_date,
       tags
    FROM public.gameboards;

CREATE TABLE anonymous.user_gameboards AS SELECT * FROM public.user_gameboards;

CREATE TABLE anonymous.assignments AS
    SELECT
        id,
        gameboard_id,
        group_id,
        owner_user_id,
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
        user_id,
        reserved_by,
        status,
        updated,
        NULL as additional_booking_information,
        TRUE as pii_removed
    FROM public.event_bookings;


-- Question attempt related tables:

CREATE TABLE anonymous.question_attempts AS
    SELECT
        id,
        user_id,
        question_id,
        '{}'::jsonb as question_attempt,
        correct,
        timestamp
    FROM public.question_attempts;

CREATE TABLE anonymous.user_streak_freezes AS
    SELECT
        user_id,
        start_date,
        end_date,
        'Streak freeze' as comment
    FROM public.user_streak_freezes;

CREATE TABLE anonymous.user_streak_targets AS
    SELECT
        user_id,
        target_count,
        start_date,
        end_date,
        'Streak target' as comment
    FROM public.user_streak_targets;


-- Quiz tables:

CREATE TABLE anonymous.quiz_assignments AS SELECT * FROM public.quiz_assignments;

CREATE TABLE anonymous.quiz_attempts AS SELECT * FROM public.quiz_attempts;

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
        '{"placeholder": "The quick, brown fox jumps over a lazy dog. DJs flock by when MTV ax quiz prog. Junk MTV quiz graced by fox whelps. Bawds jog, flick quartz, vex nymphs."}'::jsonb as event_details,
        '192.168.1.1'::inet as ip_address,
        timestamp
    FROM public.logged_events;

RETURN true;
END;
$$
LANGUAGE plpgsql;