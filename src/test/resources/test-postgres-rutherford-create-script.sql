--
-- PostgreSQL database dump
--

-- Dumped from database version 12.6 (Debian 12.6-1.pgdg100+1)
-- Dumped by pg_dump version 12.6 (Debian 12.6-1.pgdg100+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: quartz_cluster; Type: SCHEMA; Schema: -; Owner: rutherford
--

CREATE SCHEMA quartz_cluster;


ALTER SCHEMA quartz_cluster OWNER TO rutherford;

--
-- Name: mergeuser(bigint, bigint); Type: FUNCTION; Schema: public; Owner: rutherford
--

CREATE FUNCTION public.mergeuser(targetuseridtokeep bigint, targetuseridtodelete bigint) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
BEGIN

  UPDATE assignments
  SET owner_user_id = targetUserIdToKeep
  WHERE owner_user_id = targetUserIdToDelete;

  BEGIN
    UPDATE event_bookings
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  UPDATE gameboards
  SET owner_user_id = targetUserIdToKeep
  WHERE owner_user_id = targetUserIdToDelete;

  BEGIN
    UPDATE group_additional_managers
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  BEGIN
    UPDATE group_memberships
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  UPDATE groups
  SET owner_id = targetUserIdToKeep
  WHERE owner_id = targetUserIdToDelete;

  BEGIN
    UPDATE linked_accounts
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  UPDATE logged_events
  SET user_id = targetUserIdToKeep::varchar(255)
  WHERE user_id = targetUserIdToDelete::varchar(255);

  UPDATE question_attempts
  SET user_id = targetUserIdToKeep
  WHERE user_id = targetUserIdToDelete;

  UPDATE user_alerts
  SET user_id = targetUserIdToKeep
  WHERE user_id = targetUserIdToDelete;

  BEGIN
    UPDATE user_associations
    SET user_id_granting_permission = targetUserIdToKeep
    WHERE user_id_granting_permission = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;
  BEGIN
    UPDATE user_associations
    SET user_id_receiving_permission = targetUserIdToKeep
    WHERE user_id_receiving_permission = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  UPDATE user_associations_tokens
  SET owner_user_id = targetUserIdToKeep
  WHERE owner_user_id = targetUserIdToDelete;

  BEGIN
    UPDATE user_gameboards
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  BEGIN
    UPDATE user_notifications
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  BEGIN
    UPDATE user_preferences
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  BEGIN
    UPDATE user_streak_freezes
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  BEGIN
    UPDATE user_streak_targets
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  DELETE FROM users
  WHERE id = targetUserIdToDelete;

  RETURN true;
END
$$;


ALTER FUNCTION public.mergeuser(targetuseridtokeep bigint, targetuseridtodelete bigint) OWNER TO rutherford;

--
-- Name: user_streaks(bigint, integer); Type: FUNCTION; Schema: public; Owner: rutherford
--

CREATE FUNCTION public.user_streaks(useridofinterest bigint, defaultquestionsperday integer DEFAULT 3) RETURNS TABLE(streaklength bigint, startdate date, enddate date, totaldays bigint)
    LANGUAGE plpgsql
    AS $$
BEGIN
  RETURN QUERY
  -----
  -----
  WITH

    -- Filter only users first correct attempts at questions:
      first_correct_attempts AS (
        SELECT
          question_id,
          MIN(timestamp) AS timestamp
        FROM question_attempts
        WHERE correct AND user_id=useridofinterest
        GROUP BY question_id
    ),

    -- Count how many of these first correct attempts per day:
      daily_counts AS (
        SELECT
          timestamp::DATE AS date,
          COUNT(DISTINCT question_id) AS count
        FROM first_correct_attempts
        GROUP BY date
    ),

    -- Create the list of targets and dates, allowing NULL end dates to mean "to present":
      daily_targets AS (
        SELECT
          generate_series(start_date, COALESCE(end_date, CURRENT_DATE), INTERVAL '1 DAY')::DATE AS date,
          MIN(target_count) AS target_count
        FROM user_streak_targets
        WHERE user_id=useridofinterest
        GROUP BY date
    ),

    -- Filter the list of dates by the minimum number of parts required.
    -- If no user-specific target, use global default:
      active_dates AS (
        SELECT
          daily_counts.date
        FROM daily_counts LEFT OUTER JOIN daily_targets
            ON daily_counts.date=daily_targets.date
        WHERE count >= COALESCE(target_count, defaultquestionsperday)
    ),

    -- Create a list of dates streaks were frozen on, allowing NULL end dates to mean "to present":
      frozen_dates AS (
        SELECT
          DISTINCT generate_series(start_date, COALESCE(end_date, CURRENT_DATE), INTERVAL '1 DAY')::DATE AS date
        FROM user_streak_freezes
        WHERE user_id=useridofinterest
    ),

    -- Merge in streak freeze dates if there was no activity on that date:
      date_list(date, activity) AS (
        SELECT date, 1 FROM active_dates
        UNION
        SELECT date, 0 FROM frozen_dates WHERE date NOT IN (SELECT date FROM active_dates)
        ORDER BY date ASC
    ),

    -- Group consecutive dates in this merged list, this is the magic part.
      groups AS (
        SELECT
          date - (ROW_NUMBER() OVER (ORDER BY date) * INTERVAL '1 day') AS grp,
          date,
          activity
        FROM date_list
    )

  -- Return the data in a human-readable format.
  -- The length of the streak is the sum of active days.
  SELECT
    SUM(activity) AS streak_length,
    MIN(date) AS start_date,
    MAX(date) AS end_date,
    COUNT(*) AS total_days
  FROM groups
  GROUP BY grp
  ORDER BY end_date DESC;
  -----
  -----
END
$$;


ALTER FUNCTION public.user_streaks(useridofinterest bigint, defaultquestionsperday integer) OWNER TO rutherford;

--
-- Name: user_streaks_current_progress(bigint, integer); Type: FUNCTION; Schema: public; Owner: rutherford
--

CREATE FUNCTION public.user_streaks_current_progress(useridofinterest bigint, defaultquestionsperday integer DEFAULT 3) RETURNS TABLE(currentdate date, currentprogress bigint, targetprogress bigint)
    LANGUAGE plpgsql
    AS $$
BEGIN
  RETURN QUERY
  -----
  -----
  WITH

    -- Filter only users first correct attempts at questions:
      first_correct_attempts AS (
        SELECT
          question_id,
          MIN(timestamp) AS timestamp
        FROM question_attempts
        WHERE correct AND user_id=useridofinterest
        GROUP BY question_id
    ),

    -- Count how many of these first correct attempts are today:
      daily_count AS (
        SELECT
          timestamp::DATE AS date,
          COUNT(DISTINCT question_id) AS count
        FROM first_correct_attempts
        WHERE timestamp >= CURRENT_DATE
        GROUP BY date
    ),

    -- Create the list of targets and dates, allowing NULL end dates to mean "to present":
      daily_targets AS (
        SELECT
          generate_series(start_date, COALESCE(end_date, CURRENT_DATE), INTERVAL '1 DAY')::DATE AS date,
          MIN(target_count) AS target_count
        FROM user_streak_targets
        WHERE user_id=useridofinterest
        GROUP BY date
    ),

    -- To ensure there is always a return value, make a row containing only today's date:
      date_of_interest AS (
        SELECT CURRENT_DATE AS date
    )

  -- Using LEFT OUTER JOINs to ensure always keep the date; if there is a daily count
  -- then join to it, else use zero; and if there is a custom target join to it, else
  -- use the global streak target default.
  SELECT
    date_of_interest.date AS currentdate,
    COALESCE(daily_count.count, 0)::BIGINT AS currentprogress,
    COALESCE(target_count, defaultquestionsperday)::BIGINT AS targetprogress
  FROM
    (date_of_interest LEFT OUTER JOIN daily_count ON date_of_interest.date=daily_count.date)
    LEFT OUTER JOIN
    daily_targets ON date_of_interest.date=daily_targets.date;

  -----
  -----
END
$$;


ALTER FUNCTION public.user_streaks_current_progress(useridofinterest bigint, defaultquestionsperday integer) OWNER TO rutherford;

--
-- Name: user_streaks_weekly(bigint, integer); Type: FUNCTION; Schema: public; Owner: rutherford
--

CREATE FUNCTION public.user_streaks_weekly(useridofinterest bigint, defaultquestionsperweek integer DEFAULT 10) RETURNS TABLE(streaklength bigint, startdate date, enddate date, totalweeks bigint)
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN QUERY
        -----
        -----
        WITH

            -- Filter only users first correct attempts at questions:
            first_correct_attempts AS (
                SELECT
                    question_id,
                    MIN(timestamp) AS timestamp
                FROM question_attempts
                WHERE correct AND user_id=useridofinterest
                GROUP BY question_id
            ),

            -- Count how many of these first correct attempts per week:
            weekly_counts AS (
                SELECT
                    date_trunc('WEEK', timestamp)::DATE AS date,
                    COUNT(DISTINCT question_id) AS count
                FROM first_correct_attempts
                GROUP BY date
            ),

            -- Create the list of targets and dates, allowing NULL end dates to mean "to present":
            weekly_targets AS (
                SELECT
                    generate_series(date_trunc('WEEK', start_date), date_trunc('WEEK', COALESCE(end_date, CURRENT_DATE)), INTERVAL '7 DAY')::DATE AS date,
                    MIN(target_count) AS target_count
                FROM user_streak_targets
                WHERE user_id=useridofinterest
                GROUP BY date
            ),

            -- Filter the list of dates by the minimum number of parts required.
            -- If no user-specific target, use global default:
            active_dates AS (
                SELECT
                    weekly_counts.date
                FROM weekly_counts LEFT OUTER JOIN weekly_targets
                                                   ON weekly_counts.date=weekly_targets.date
                WHERE count >= COALESCE(target_count, defaultquestionsperweek)
            ),

            -- Create a list of dates streaks were frozen on, allowing NULL end dates to mean "to present":
            frozen_dates AS (
                SELECT
                    DISTINCT generate_series(date_trunc('WEEK', start_date), date_trunc('WEEK', COALESCE(end_date, CURRENT_DATE)), INTERVAL '7 DAY')::DATE AS date
                FROM user_streak_freezes
                WHERE user_id=useridofinterest
            ),

            -- Merge in streak freeze dates if there was no activity on that date:
            date_list(date, activity) AS (
                SELECT date, 1 FROM active_dates
                UNION
                SELECT date, 0 FROM frozen_dates WHERE date NOT IN (SELECT date FROM active_dates)
                ORDER BY date ASC
            ),

            -- Group consecutive dates in this merged list, this is the magic part.
            groups AS (
                SELECT
                        date - (ROW_NUMBER() OVER (ORDER BY date) * INTERVAL '7 day') AS grp,
                        date,
                        activity
                FROM date_list
            )

            -- Return the data in a human-readable format.
            -- The length of the streak is the sum of active days.
        SELECT
            SUM(activity) AS streak_length,
            MIN(date) AS start_date,
            MAX(date) AS end_date,
            COUNT(*) AS total_weeks
        FROM groups
        GROUP BY grp
        ORDER BY end_date DESC;
    -----
    -----
END
$$;


ALTER FUNCTION public.user_streaks_weekly(useridofinterest bigint, defaultquestionsperweek integer) OWNER TO rutherford;

--
-- Name: user_streaks_weekly_current_progress(bigint, integer); Type: FUNCTION; Schema: public; Owner: rutherford
--

CREATE FUNCTION public.user_streaks_weekly_current_progress(useridofinterest bigint, defaultquestionsperweek integer DEFAULT 10) RETURNS TABLE(currentweek date, currentprogress bigint, targetprogress bigint)
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN QUERY
        -----
        -----
        WITH

            -- Filter only users first correct attempts at questions:
            first_correct_attempts AS (
                SELECT
                    question_id,
                    MIN(timestamp) AS timestamp
                FROM question_attempts
                WHERE correct AND user_id=useridofinterest
                GROUP BY question_id
            ),

            -- Count how many of these first correct attempts are this week:
            weekly_count AS (
                SELECT
                    date_trunc('WEEK', timestamp)::DATE AS date,
                    COUNT(DISTINCT question_id) AS count
                FROM first_correct_attempts
                WHERE timestamp >= date_trunc('WEEK', CURRENT_DATE)
                GROUP BY date
            ),

            -- Create the list of targets and dates, allowing NULL end dates to mean "to present":
            weekly_targets AS (
                SELECT
                    generate_series(date_trunc('WEEK', start_date), date_trunc('WEEK', COALESCE(end_date, CURRENT_DATE)), INTERVAL '7 DAY')::DATE AS date,
                    MIN(target_count) AS target_count
                FROM user_streak_targets
                WHERE user_id=useridofinterest
                GROUP BY date
            ),

            -- To ensure there is always a return value, make a row containing only this week's date:
            date_of_interest AS (
                SELECT date_trunc('WEEK', CURRENT_DATE)::DATE AS date
            )

            -- Using LEFT OUTER JOINs to ensure always keep the date; if there is a daily count
            -- then join to it, else use zero; and if there is a custom target join to it, else
            -- use the global streak target default.
        SELECT
            date_of_interest.date AS currentweek,
            COALESCE(weekly_count.count, 0)::BIGINT AS currentprogress,
            COALESCE(target_count, defaultquestionsperweek)::BIGINT AS targetprogress
        FROM
            (date_of_interest LEFT OUTER JOIN weekly_count ON date_of_interest.date=weekly_count.date)
                LEFT OUTER JOIN
            weekly_targets ON date_of_interest.date=weekly_targets.date;
    -----
    -----
END
$$;


ALTER FUNCTION public.user_streaks_weekly_current_progress(useridofinterest bigint, defaultquestionsperweek integer) OWNER TO rutherford;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: assignments; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.assignments (
    id integer NOT NULL,
    gameboard_id character varying(255) NOT NULL,
    group_id integer NOT NULL,
    owner_user_id integer,
    notes text,
    creation_date timestamp without time zone,
    due_date timestamp with time zone
);


ALTER TABLE public.assignments OWNER TO rutherford;

--
-- Name: assignments_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE public.assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.assignments_id_seq OWNER TO rutherford;

--
-- Name: assignments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE public.assignments_id_seq OWNED BY public.assignments.id;


--
-- Name: event_bookings; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.event_bookings (
    id integer NOT NULL,
    event_id text NOT NULL,
    created timestamp without time zone NOT NULL,
    user_id integer NOT NULL,
    reserved_by integer,
    status text DEFAULT 'CONFIRMED'::text NOT NULL,
    updated timestamp without time zone,
    additional_booking_information jsonb
);


ALTER TABLE public.event_bookings OWNER TO rutherford;

--
-- Name: event_bookings_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE public.event_bookings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.event_bookings_id_seq OWNER TO rutherford;

--
-- Name: event_bookings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE public.event_bookings_id_seq OWNED BY public.event_bookings.id;


--
-- Name: external_accounts; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.external_accounts (
    user_id integer NOT NULL,
    provider_name text NOT NULL,
    provider_user_identifier text,
    provider_last_updated timestamp without time zone
);


ALTER TABLE public.external_accounts OWNER TO rutherford;

--
-- Name: gameboards; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.gameboards (
    id character varying NOT NULL,
    title text,
    questions character varying[],
    contents jsonb[] DEFAULT ARRAY[]::jsonb[] NOT NULL,
    wildcard jsonb,
    wildcard_position integer,
    game_filter jsonb,
    owner_user_id integer,
    creation_method character varying,
    creation_date timestamp without time zone,
    tags jsonb DEFAULT '[]'::jsonb NOT NULL
);


ALTER TABLE public.gameboards OWNER TO rutherford;

--
-- Name: group_additional_managers; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.group_additional_managers (
    user_id integer NOT NULL,
    group_id integer NOT NULL,
    created timestamp with time zone DEFAULT now()
);


ALTER TABLE public.group_additional_managers OWNER TO rutherford;

--
-- Name: group_memberships; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.group_memberships (
    group_id integer NOT NULL,
    user_id integer NOT NULL,
    created timestamp without time zone,
    updated timestamp with time zone DEFAULT now(),
    status text DEFAULT 'ACTIVE'::text
);


ALTER TABLE public.group_memberships OWNER TO rutherford;

--
-- Name: groups; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.groups (
    id integer NOT NULL,
    group_name text,
    owner_id integer,
    created timestamp without time zone,
    archived boolean DEFAULT false NOT NULL,
    group_status text DEFAULT 'ACTIVE'::text,
    last_updated timestamp without time zone
);


ALTER TABLE public.groups OWNER TO rutherford;

--
-- Name: groups_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE public.groups_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.groups_id_seq OWNER TO rutherford;

--
-- Name: groups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE public.groups_id_seq OWNED BY public.groups.id;


--
-- Name: ip_location_history; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.ip_location_history (
    id integer NOT NULL,
    ip_address text NOT NULL,
    location_information jsonb,
    created timestamp without time zone,
    last_lookup timestamp without time zone,
    is_current boolean DEFAULT true
);


ALTER TABLE public.ip_location_history OWNER TO rutherford;

--
-- Name: ip_location_history_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE public.ip_location_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ip_location_history_id_seq OWNER TO rutherford;

--
-- Name: ip_location_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE public.ip_location_history_id_seq OWNED BY public.ip_location_history.id;


--
-- Name: linked_accounts; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.linked_accounts (
    user_id bigint NOT NULL,
    provider character varying(100) NOT NULL,
    provider_user_id text
);


ALTER TABLE public.linked_accounts OWNER TO rutherford;

--
-- Name: COLUMN linked_accounts.user_id; Type: COMMENT; Schema: public; Owner: rutherford
--

COMMENT ON COLUMN public.linked_accounts.user_id IS 'This is the postgres foreign key for the users table.';


--
-- Name: COLUMN linked_accounts.provider_user_id; Type: COMMENT; Schema: public; Owner: rutherford
--

COMMENT ON COLUMN public.linked_accounts.provider_user_id IS 'user id from the remote service';


--
-- Name: logged_events; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.logged_events (
    id integer NOT NULL,
    user_id character varying(100) NOT NULL,
    anonymous_user boolean NOT NULL,
    event_type character varying(255),
    event_details_type text,
    event_details jsonb,
    ip_address inet,
    "timestamp" timestamp without time zone
);


ALTER TABLE public.logged_events OWNER TO rutherford;

--
-- Name: logged_events_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE public.logged_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.logged_events_id_seq OWNER TO rutherford;

--
-- Name: logged_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE public.logged_events_id_seq OWNED BY public.logged_events.id;


--
-- Name: question_attempts; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.question_attempts (
    id integer NOT NULL,
    user_id integer NOT NULL,
    question_id text NOT NULL,
    question_attempt jsonb,
    correct boolean,
    "timestamp" timestamp without time zone
);


ALTER TABLE public.question_attempts OWNER TO rutherford;

--
-- Name: question_attempts_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE public.question_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.question_attempts_id_seq OWNER TO rutherford;

--
-- Name: question_attempts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE public.question_attempts_id_seq OWNED BY public.question_attempts.id;


--
-- Name: quiz_assignments; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.quiz_assignments (
    id integer NOT NULL,
    quiz_id character varying(255) NOT NULL,
    group_id integer NOT NULL,
    owner_user_id integer,
    creation_date timestamp without time zone,
    due_date timestamp with time zone,
    quiz_feedback_mode text NOT NULL,
    deleted boolean DEFAULT false NOT NULL
);


ALTER TABLE public.quiz_assignments OWNER TO rutherford;

--
-- Name: quiz_assignments_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE public.quiz_assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.quiz_assignments_id_seq OWNER TO rutherford;

--
-- Name: quiz_assignments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE public.quiz_assignments_id_seq OWNED BY public.quiz_assignments.id;


--
-- Name: quiz_attempts; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.quiz_attempts (
    id integer NOT NULL,
    user_id integer NOT NULL,
    quiz_id character varying(255) NOT NULL,
    quiz_assignment_id integer,
    start_date timestamp without time zone NOT NULL,
    completed_date timestamp with time zone
);


ALTER TABLE public.quiz_attempts OWNER TO rutherford;

--
-- Name: quiz_attempts_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE public.quiz_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.quiz_attempts_id_seq OWNER TO rutherford;

--
-- Name: quiz_attempts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE public.quiz_attempts_id_seq OWNED BY public.quiz_attempts.id;


--
-- Name: quiz_question_attempts; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.quiz_question_attempts (
    id integer NOT NULL,
    quiz_attempt_id integer NOT NULL,
    question_id text NOT NULL,
    question_attempt jsonb,
    correct boolean,
    "timestamp" timestamp without time zone
);


ALTER TABLE public.quiz_question_attempts OWNER TO rutherford;

--
-- Name: quiz_question_attempts_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE public.quiz_question_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.quiz_question_attempts_id_seq OWNER TO rutherford;

--
-- Name: quiz_question_attempts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE public.quiz_question_attempts_id_seq OWNED BY public.quiz_question_attempts.id;


--
-- Name: temporary_user_store; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.temporary_user_store (
    id character varying NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    last_updated timestamp with time zone DEFAULT now() NOT NULL,
    temporary_app_data jsonb
);


ALTER TABLE public.temporary_user_store OWNER TO rutherford;

--
-- Name: uk_post_codes; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.uk_post_codes (
    postcode character varying(255) NOT NULL,
    lat numeric NOT NULL,
    lon numeric NOT NULL
);


ALTER TABLE public.uk_post_codes OWNER TO rutherford;

--
-- Name: user_alerts_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE public.user_alerts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.user_alerts_id_seq OWNER TO rutherford;

--
-- Name: user_alerts; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_alerts (
    id integer DEFAULT nextval('public.user_alerts_id_seq'::regclass) NOT NULL,
    user_id integer NOT NULL,
    message text,
    link text,
    created timestamp without time zone DEFAULT now() NOT NULL,
    seen timestamp without time zone,
    clicked timestamp without time zone,
    dismissed timestamp without time zone
);


ALTER TABLE public.user_alerts OWNER TO rutherford;

--
-- Name: user_associations; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_associations (
    user_id_granting_permission integer NOT NULL,
    user_id_receiving_permission integer NOT NULL,
    created timestamp without time zone
);


ALTER TABLE public.user_associations OWNER TO rutherford;

--
-- Name: user_associations_tokens; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_associations_tokens (
    token character varying(100) NOT NULL,
    owner_user_id integer,
    group_id integer
);


ALTER TABLE public.user_associations_tokens OWNER TO rutherford;

--
-- Name: user_badges; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_badges (
    user_id integer,
    badge text,
    state jsonb
);


ALTER TABLE public.user_badges OWNER TO rutherford;

--
-- Name: user_credentials; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_credentials (
    user_id integer NOT NULL,
    password text NOT NULL,
    secure_salt text,
    security_scheme text DEFAULT 'SeguePBKDF2v1'::text NOT NULL,
    reset_token text,
    reset_expiry timestamp with time zone,
    created timestamp with time zone DEFAULT now(),
    last_updated timestamp with time zone DEFAULT now()
);


ALTER TABLE public.user_credentials OWNER TO rutherford;

--
-- Name: user_email_preferences; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_email_preferences (
    user_id integer NOT NULL,
    email_preference integer NOT NULL,
    email_preference_status boolean
);


ALTER TABLE public.user_email_preferences OWNER TO rutherford;

--
-- Name: user_gameboards; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_gameboards (
    user_id integer NOT NULL,
    gameboard_id character varying NOT NULL,
    created timestamp without time zone,
    last_visited timestamp without time zone
);


ALTER TABLE public.user_gameboards OWNER TO rutherford;

--
-- Name: user_notifications; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_notifications (
    user_id integer NOT NULL,
    notification_id text NOT NULL,
    status text,
    created timestamp without time zone NOT NULL
);


ALTER TABLE public.user_notifications OWNER TO rutherford;

--
-- Name: user_preferences; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_preferences (
    user_id integer NOT NULL,
    preference_type character varying(255) NOT NULL,
    preference_name character varying(255) NOT NULL,
    preference_value boolean NOT NULL,
    last_updated timestamp without time zone
);


ALTER TABLE public.user_preferences OWNER TO rutherford;

--
-- Name: user_streak_freezes; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_streak_freezes (
    user_id bigint NOT NULL,
    start_date date NOT NULL,
    end_date date,
    comment text
);


ALTER TABLE public.user_streak_freezes OWNER TO rutherford;

--
-- Name: user_streak_targets; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_streak_targets (
    user_id bigint NOT NULL,
    target_count integer NOT NULL,
    start_date date NOT NULL,
    end_date date,
    comment text
);


ALTER TABLE public.user_streak_targets OWNER TO rutherford;

--
-- Name: user_totp; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.user_totp (
    user_id integer NOT NULL,
    shared_secret text NOT NULL,
    created timestamp with time zone DEFAULT now(),
    last_updated timestamp with time zone DEFAULT now()
);


ALTER TABLE public.user_totp OWNER TO rutherford;

--
-- Name: users; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE public.users (
    id integer NOT NULL,
    _id character varying(255),
    family_name text,
    given_name text,
    email text NOT NULL,
    role character varying(255) DEFAULT 'STUDENT'::character varying NOT NULL,
    date_of_birth date,
    gender character varying(255),
    registration_date timestamp without time zone,
    school_id text,
    school_other text,
    exam_board text,
    registered_contexts jsonb[] DEFAULT ARRAY[]::jsonb[] NOT NULL,
    registered_contexts_last_confirmed timestamp without time zone,
    last_updated timestamp without time zone,
    email_verification_status character varying(255),
    last_seen timestamp without time zone,
    email_to_verify text,
    email_verification_token text,
    session_token integer DEFAULT 0 NOT NULL,
    deleted boolean DEFAULT false NOT NULL
);


ALTER TABLE public.users OWNER TO rutherford;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.users_id_seq OWNER TO rutherford;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: qrtz_blob_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_blob_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    blob_data bytea
);


ALTER TABLE quartz_cluster.qrtz_blob_triggers OWNER TO rutherford;

--
-- Name: qrtz_calendars; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_calendars (
    sched_name character varying(120) NOT NULL,
    calendar_name character varying(200) NOT NULL,
    calendar bytea NOT NULL
);


ALTER TABLE quartz_cluster.qrtz_calendars OWNER TO rutherford;

--
-- Name: qrtz_cron_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_cron_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    cron_expression character varying(250) NOT NULL,
    time_zone_id character varying(80)
);


ALTER TABLE quartz_cluster.qrtz_cron_triggers OWNER TO rutherford;

--
-- Name: qrtz_fired_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_fired_triggers (
    sched_name character varying(120) NOT NULL,
    entry_id character varying(140) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    instance_name character varying(200) NOT NULL,
    fired_time bigint NOT NULL,
    sched_time bigint NOT NULL,
    priority integer NOT NULL,
    state character varying(16) NOT NULL,
    job_name character varying(200),
    job_group character varying(200),
    is_nonconcurrent boolean NOT NULL,
    requests_recovery boolean
);


ALTER TABLE quartz_cluster.qrtz_fired_triggers OWNER TO rutherford;

--
-- Name: qrtz_job_details; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_job_details (
    sched_name character varying(120) NOT NULL,
    job_name character varying(200) NOT NULL,
    job_group character varying(200) NOT NULL,
    description character varying(250),
    job_class_name character varying(250) NOT NULL,
    is_durable boolean NOT NULL,
    is_nonconcurrent boolean NOT NULL,
    is_update_data boolean NOT NULL,
    requests_recovery boolean NOT NULL,
    job_data bytea
);


ALTER TABLE quartz_cluster.qrtz_job_details OWNER TO rutherford;

--
-- Name: qrtz_locks; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_locks (
    sched_name character varying(120) NOT NULL,
    lock_name character varying(40) NOT NULL
);


ALTER TABLE quartz_cluster.qrtz_locks OWNER TO rutherford;

--
-- Name: qrtz_paused_trigger_grps; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_paused_trigger_grps (
    sched_name character varying(120) NOT NULL,
    trigger_group character varying(150) NOT NULL
);


ALTER TABLE quartz_cluster.qrtz_paused_trigger_grps OWNER TO rutherford;

--
-- Name: qrtz_scheduler_state; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_scheduler_state (
    sched_name character varying(120) NOT NULL,
    instance_name character varying(200) NOT NULL,
    last_checkin_time bigint NOT NULL,
    checkin_interval bigint NOT NULL
);


ALTER TABLE quartz_cluster.qrtz_scheduler_state OWNER TO rutherford;

--
-- Name: qrtz_simple_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_simple_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    repeat_count bigint NOT NULL,
    repeat_interval bigint NOT NULL,
    times_triggered bigint NOT NULL
);


ALTER TABLE quartz_cluster.qrtz_simple_triggers OWNER TO rutherford;

--
-- Name: qrtz_simprop_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_simprop_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    str_prop_1 character varying(512),
    str_prop_2 character varying(512),
    str_prop_3 character varying(512),
    int_prop_1 integer,
    int_prop_2 integer,
    long_prop_1 bigint,
    long_prop_2 bigint,
    dec_prop_1 numeric,
    dec_prop_2 numeric,
    bool_prop_1 boolean,
    bool_prop_2 boolean,
    time_zone_id character varying(80)
);


ALTER TABLE quartz_cluster.qrtz_simprop_triggers OWNER TO rutherford;

--
-- Name: qrtz_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    job_name character varying(200) NOT NULL,
    job_group character varying(200) NOT NULL,
    description character varying(250),
    next_fire_time bigint,
    prev_fire_time bigint,
    priority integer,
    trigger_state character varying(16) NOT NULL,
    trigger_type character varying(8) NOT NULL,
    start_time bigint NOT NULL,
    end_time bigint,
    calendar_name character varying(200),
    misfire_instr smallint,
    job_data bytea
);


ALTER TABLE quartz_cluster.qrtz_triggers OWNER TO rutherford;

--
-- Name: assignments id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.assignments ALTER COLUMN id SET DEFAULT nextval('public.assignments_id_seq'::regclass);


--
-- Name: event_bookings id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.event_bookings ALTER COLUMN id SET DEFAULT nextval('public.event_bookings_id_seq'::regclass);


--
-- Name: groups id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.groups ALTER COLUMN id SET DEFAULT nextval('public.groups_id_seq'::regclass);


--
-- Name: ip_location_history id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.ip_location_history ALTER COLUMN id SET DEFAULT nextval('public.ip_location_history_id_seq'::regclass);


--
-- Name: logged_events id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.logged_events ALTER COLUMN id SET DEFAULT nextval('public.logged_events_id_seq'::regclass);


--
-- Name: question_attempts id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.question_attempts ALTER COLUMN id SET DEFAULT nextval('public.question_attempts_id_seq'::regclass);


--
-- Name: quiz_assignments id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.quiz_assignments ALTER COLUMN id SET DEFAULT nextval('public.quiz_assignments_id_seq'::regclass);


--
-- Name: quiz_attempts id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.quiz_attempts ALTER COLUMN id SET DEFAULT nextval('public.quiz_attempts_id_seq'::regclass);


--
-- Name: quiz_question_attempts id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.quiz_question_attempts ALTER COLUMN id SET DEFAULT nextval('public.quiz_question_attempts_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: assignments; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: event_bookings; Type: TABLE DATA; Schema: public; Owner: rutherford
--

INSERT INTO public.event_bookings (id, event_id, created, user_id, reserved_by, status, updated, additional_booking_information) VALUES (2, '_regular_test_event', '2022-07-06 10:54:41.525', 7, NULL, 'CONFIRMED', '2022-07-06 10:54:41.525', '{"yearGroup": "13", "emergencyName": "Alice''s mom", "emergencyNumber": "+44020123456", "medicalRequirements": "Alice''s dietary requirements", "accessibilityRequirements": "Alice''s accessibility requirements"}');
INSERT INTO public.event_bookings (id, event_id, created, user_id, reserved_by, status, updated, additional_booking_information) VALUES (3, '_regular_test_event', '2022-07-06 10:56:36.676', 8, NULL, 'CONFIRMED', '2022-07-06 10:56:36.676', '{"yearGroup": "9", "emergencyName": "Bob''s dad", "emergencyNumber": "+44020654321", "medicalRequirements": "Bob''s dietary requirements", "accessibilityRequirements": "Bob''s accessibility requirements"}');
INSERT INTO public.event_bookings (id, event_id, created, user_id, reserved_by, status, updated, additional_booking_information) VALUES (4, '_regular_test_event', '2022-07-14 14:41:58', 11, NULL, 'CONFIRMED', '2022-07-14 14:42:07', '{"yearGroup": "8", "emergencyName": "Charlie''s uncle", "emergencyNumber": "+44020918273", "medicalRequirements": "Charlie''s dietary requirements", "accessibilityRequirements": "Charlie''s accessibility requirements"}');


--
-- Data for Name: external_accounts; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: gameboards; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: group_additional_managers; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: group_memberships; Type: TABLE DATA; Schema: public; Owner: rutherford
--

INSERT INTO public.group_memberships (group_id, user_id, created, updated, status) VALUES (1, 7, NULL, '2022-07-06 14:38:03.339711+00', 'ACTIVE');
INSERT INTO public.group_memberships (group_id, user_id, created, updated, status) VALUES (1, 8, NULL, '2022-07-06 14:38:17.286686+00', 'ACTIVE');
INSERT INTO public.group_memberships (group_id, user_id, created, updated, status) VALUES (2, 8, NULL, '2022-07-06 14:38:36.064903+00', 'ACTIVE');
INSERT INTO public.group_memberships (group_id, user_id, created, updated, status) VALUES (2, 9, NULL, '2022-07-06 14:38:36.064903+00', 'ACTIVE');


--
-- Data for Name: groups; Type: TABLE DATA; Schema: public; Owner: rutherford
--

INSERT INTO public.groups (id, group_name, owner_id, created, archived, group_status, last_updated) VALUES (1, 'AB Group (Test)', 5, '2022-07-06 15:36:58', false, 'ACTIVE', NULL);
INSERT INTO public.groups (id, group_name, owner_id, created, archived, group_status, last_updated) VALUES (2, 'BC Group (Dave)', 10, '2022-07-06 15:37:32', false, 'ACTIVE', NULL);


--
-- Data for Name: ip_location_history; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: linked_accounts; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: logged_events; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: question_attempts; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: quiz_assignments; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: quiz_attempts; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: quiz_question_attempts; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: temporary_user_store; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: uk_post_codes; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: user_alerts; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: user_associations; Type: TABLE DATA; Schema: public; Owner: rutherford
--

INSERT INTO public.user_associations (user_id_granting_permission, user_id_receiving_permission, created) VALUES (7, 5, '2022-07-06 16:05:29');
INSERT INTO public.user_associations (user_id_granting_permission, user_id_receiving_permission, created) VALUES (8, 5, '2022-07-06 16:05:46');
INSERT INTO public.user_associations (user_id_granting_permission, user_id_receiving_permission, created) VALUES (8, 10, '2022-07-06 16:06:03');
INSERT INTO public.user_associations (user_id_granting_permission, user_id_receiving_permission, created) VALUES (9, 10, '2022-07-06 16:06:11');


--
-- Data for Name: user_associations_tokens; Type: TABLE DATA; Schema: public; Owner: rutherford
--

INSERT INTO public.user_associations_tokens (token, owner_user_id, group_id) VALUES ('ABTOK7', 7, 1);
INSERT INTO public.user_associations_tokens (token, owner_user_id, group_id) VALUES ('ABTOK8', 8, 1);
INSERT INTO public.user_associations_tokens (token, owner_user_id, group_id) VALUES ('BCTOK8', 8, 2);
INSERT INTO public.user_associations_tokens (token, owner_user_id, group_id) VALUES ('BCTOK9', 9, 2);


--
-- Data for Name: user_badges; Type: TABLE DATA; Schema: public; Owner: rutherford
--

INSERT INTO public.user_badges (user_id, badge, state) VALUES (2, 'TEACHER_GROUPS_CREATED', '{"groups": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (2, 'TEACHER_ASSIGNMENTS_SET', '{"assignments": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (2, 'TEACHER_BOOK_PAGES_SET', '{"assignments": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (2, 'TEACHER_GAMEBOARDS_CREATED', '{"gameboards": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (2, 'TEACHER_CPD_EVENTS_ATTENDED', '{"cpdEvents": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (7, 'TEACHER_GROUPS_CREATED', '{"groups": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (7, 'TEACHER_ASSIGNMENTS_SET', '{"assignments": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (7, 'TEACHER_BOOK_PAGES_SET', '{"assignments": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (7, 'TEACHER_GAMEBOARDS_CREATED', '{"gameboards": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (7, 'TEACHER_CPD_EVENTS_ATTENDED', '{"cpdEvents": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (8, 'TEACHER_GROUPS_CREATED', '{"groups": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (8, 'TEACHER_ASSIGNMENTS_SET', '{"assignments": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (8, 'TEACHER_BOOK_PAGES_SET', '{"assignments": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (8, 'TEACHER_GAMEBOARDS_CREATED', '{"gameboards": []}');
INSERT INTO public.user_badges (user_id, badge, state) VALUES (8, 'TEACHER_CPD_EVENTS_ATTENDED', '{"cpdEvents": []}');


--
-- Data for Name: user_credentials; Type: TABLE DATA; Schema: public; Owner: rutherford
--

INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (1, 'Qkq8HUWI3BiMTtIXOLQHrimVHDHibm/Sv+b7l9R+MQTB4QZQZsELGz1sugaUUYEGTz/+s1yOHJA4+3/vtvcRqg==', 'quqt4W6AXeWYnarqPFPJFg==', 'SeguePBKDF2v3', NULL, NULL, '2019-08-01 12:28:23.463026+00', '2021-06-17 15:50:44.66+00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (2, '6iUsE3Dm/W83KE+fKWex9kDS4nCsV1sxWVXXAo7toS4WcKf2h6V5RVgQgvAgsBkxSXuQc6CaV2pyOAQq+MtuWg==', 'HaP5yiXzyfxjKotGKPDVQQ==', 'SeguePBKDF2v3', NULL, NULL, '2019-08-01 12:40:17.294925+00', '2021-01-25 15:12:43.876+00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (3, 'oyjyh9eOb9g/7oDphjUjZxhIYNVplFTcVbkR6IrUfstgUu3Uai0H+5523IWJy6q0ZEg03TJ9D5yov2EtQ/b+vg==', 'wrf9iczzodG4X9+2buuqiw==', 'SeguePBKDF2v3', NULL, NULL, '2019-08-01 12:43:15.133957+00', '2021-01-25 15:11:19.331+00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (4, 'C58FZjuY6HMDjmIbsCcS4cLQTU5Raee+qMraexPoDZ434RLmP59EJ+Tn0c4QVkMjZMqvZPwLWM4VyumEgJW7kg==', 'NFBFqQ+DwCwUNp6YFq8x6g==', 'SeguePBKDF2v3', NULL, NULL, '2019-08-01 12:50:33.329901+00', '2021-01-25 15:11:09.825+00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (5, 'ak86hEtKZzGppIDDaPBOIftJ5rrI/lSKz30q3hkX0utfxsv+f5rzb1RfEEi5rbfIEaseGs18Aj6X3zYV/ZRNwQ==', 'EXNmh521xIMQBh11ayfawg==', 'SeguePBKDF2v3', NULL, NULL, '2019-08-01 12:51:05.940811+00', '2021-01-25 15:11:02.617+00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (6, 'ak86hEtKZzGppIDDaPBOIftJ5rrI/lSKz30q3hkX0utfxsv+f5rzb1RfEEi5rbfIEaseGs18Aj6X3zYV/ZRNwQ==', 'EXNmh521xIMQBh11ayfawg==', 'SeguePBKDF2v3', NULL, NULL, '2019-08-01 12:51:05.940811+00', '2021-01-25 15:11:02.617+00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (7, 'ak86hEtKZzGppIDDaPBOIftJ5rrI/lSKz30q3hkX0utfxsv+f5rzb1RfEEi5rbfIEaseGs18Aj6X3zYV/ZRNwQ==', 'EXNmh521xIMQBh11ayfawg==', 'SeguePBKDF2v3', NULL, NULL, '2019-08-01 12:51:05.940811+00', '2021-01-25 15:11:02.617+00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (8, 'ak86hEtKZzGppIDDaPBOIftJ5rrI/lSKz30q3hkX0utfxsv+f5rzb1RfEEi5rbfIEaseGs18Aj6X3zYV/ZRNwQ==', 'EXNmh521xIMQBh11ayfawg==', 'SeguePBKDF2v3', NULL, NULL, '2019-08-01 12:51:05.940811+00', '2021-01-25 15:11:02.617+00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (9, 'ak86hEtKZzGppIDDaPBOIftJ5rrI/lSKz30q3hkX0utfxsv+f5rzb1RfEEi5rbfIEaseGs18Aj6X3zYV/ZRNwQ==', 'EXNmh521xIMQBh11ayfawg==', 'SeguePBKDF2v3', NULL, NULL, '2019-08-01 12:51:05.940811+00', '2021-01-25 15:11:02.617+00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (10, 'ak86hEtKZzGppIDDaPBOIftJ5rrI/lSKz30q3hkX0utfxsv+f5rzb1RfEEi5rbfIEaseGs18Aj6X3zYV/ZRNwQ==', 'EXNmh521xIMQBh11ayfawg==', 'SeguePBKDF2v3', NULL, NULL, '2019-08-01 12:51:05.940811+00', '2021-01-25 15:11:02.617+00');


--
-- Data for Name: user_email_preferences; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: user_gameboards; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: user_notifications; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: user_preferences; Type: TABLE DATA; Schema: public; Owner: rutherford
--

INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (6, 'EMAIL_PREFERENCE', 'ASSIGNMENTS', true, '2021-03-09 16:00:34.563979');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (2, 'EMAIL_PREFERENCE', 'ASSIGNMENTS', true, '2022-07-06 10:48:59.821444');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (2, 'EMAIL_PREFERENCE', 'NEWS_AND_UPDATES', false, '2022-07-06 10:48:59.821444');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (2, 'EMAIL_PREFERENCE', 'EVENTS', false, '2022-07-06 10:48:59.821444');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (2, 'BOOLEAN_NOTATION', 'ENG', false, '2022-07-06 10:48:59.821444');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (2, 'BOOLEAN_NOTATION', 'MATH', true, '2022-07-06 10:48:59.821444');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (2, 'DISPLAY_SETTING', 'HIDE_NON_AUDIENCE_CONTENT', true, '2022-07-06 10:48:59.821444');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (7, 'BOOLEAN_NOTATION', 'ENG', false, '2022-07-06 10:52:36.096292');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (7, 'BOOLEAN_NOTATION', 'MATH', true, '2022-07-06 10:52:36.096292');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (7, 'DISPLAY_SETTING', 'HIDE_NON_AUDIENCE_CONTENT', true, '2022-07-06 10:52:36.096292');


--
-- Data for Name: user_streak_freezes; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: user_streak_targets; Type: TABLE DATA; Schema: public; Owner: rutherford
--



--
-- Data for Name: user_totp; Type: TABLE DATA; Schema: public; Owner: rutherford
--

INSERT INTO public.user_totp (user_id, shared_secret, created, last_updated) VALUES (2, 'OQXZE3PEGIGKAAP6', '2022-07-06 10:48:02.111+00', '2022-07-06 10:48:05.498+00');


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: rutherford
--

INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (11, NULL, 'Student', 'Erika', 'erika-student@test.com', 'STUDENT', NULL, 'FEMALE', '2022-07-03 17:34:07', '', 'A Manually Entered School', 'OTHER', '{}', NULL, '2022-07-05 17:34:31', 'VERIFIED', NULL, 'erika-student@test.com', NULL, 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (9, NULL, 'Student', 'Charlie', 'charlie-student@test.com', 'STUDENT', NULL, 'MALE', '2022-07-05 17:34:07', '130615', NULL, 'OCR', '{}', NULL, '2022-07-05 17:34:31', 'VERIFIED', NULL, 'charlie-student@test.com', NULL, 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (4, NULL, 'Editor', 'Test Editor', 'test-editor@test.com', 'CONTENT_EDITOR', NULL, 'PREFER_NOT_TO_SAY', '2019-08-01 12:50:32.631', '133801', NULL, 'OTHER', '{}', NULL, '2021-03-09 16:46:26.28', 'VERIFIED', '2021-03-09 17:09:32.472', 'test-editor@test.com', 'nAAK4xSBuAPRejM4YPNfTKRDGK4Oa1VuL3EMmJburjE', 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (3, NULL, 'Event Manager', 'Test Event', 'test-event@test.com', 'EVENT_MANAGER', NULL, 'OTHER', '2019-08-01 12:43:14.583', '133801', NULL, 'AQA', '{}', NULL, '2021-03-09 16:47:03.77', 'VERIFIED', '2021-03-09 17:10:04.552', 'test-event@test.com', 'QlIS3PVS33I8jmMo3JPQgIn2xaKe4gFgwXfH4qiI8', 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (5, NULL, 'Teacher', 'Test Teacher', 'test-teacher@test.com', 'TEACHER', NULL, 'FEMALE', '2019-08-01 12:51:05.416', NULL, 'A Manually Entered School', 'AQA', '{}', NULL, '2021-03-31 10:19:04.939', 'VERIFIED', '2021-06-17 16:51:29.977', 'test-teacher@test.com', 'm9A8P0VbpFQnzOdXOywx75lpaWSpssLmQ779ij2b5LQ', 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (1, NULL, 'Progress', 'Test Progress', 'test-progress@test.com', 'STUDENT', NULL, 'FEMALE', '2019-08-01 12:28:22.869', '130615', NULL, 'AQA', '{"{\"stage\": \"all\", \"examBoard\": \"ocr\"}"}', '2021-10-04 14:10:37.441', '2021-11-05 10:52:13.018', 'VERIFIED', '2021-11-05 10:52:13.14', 'test-progress@test.com', 'scIF1UJeYyGRGwGrwGNUyIWuZxKBrQHd8evcAeZk', 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (6, NULL, 'Student', 'Test Student', 'test-student@test.com', 'STUDENT', NULL, 'MALE', '2019-08-01 12:51:39.981', '110158', NULL, 'OCR', '{"{\"stage\": \"all\", \"examBoard\": \"ocr\"}"}', '2021-10-04 14:12:13.351', '2021-10-04 14:12:13.384', 'VERIFIED', '2022-03-22 15:37:44.585', 'test-student@test.com', 'ZMUU7NbjhUSawOClEzb1KPEMcUA93QCkxuGejMwmE', 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (7, NULL, 'Student', 'Alice', 'alice-student@test.com', 'STUDENT', '1991-01-01', 'FEMALE', '2022-07-05 17:31:12', NULL, 'A Manually Entered School', 'OTHER', '{"{\"stage\": \"all\", \"examBoard\": \"all\"}"}', '2022-07-06 10:52:35.922', '2022-07-06 10:52:36.056', 'VERIFIED', '2022-07-06 10:52:36.163', 'alice-student@test.com', NULL, 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (8, NULL, 'Student', 'Bob', 'bob-student@test.com', 'STUDENT', NULL, 'MALE', '2022-07-05 17:32:41', '110158', NULL, 'AQA', '{}', NULL, '2022-07-05 17:32:57', 'VERIFIED', '2022-07-06 10:55:53.905', 'bob-student@test.com', NULL, 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (10, NULL, 'Teacher', 'Dave', 'dave-teacher@test.com', 'TEACHER', NULL, 'MALE', '2022-07-06 15:15:00', '110158', NULL, 'OTHER', '{}', NULL, NULL, 'VERIFIED', NULL, 'dave-teacher@test.com', NULL, 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (2, NULL, 'Test', 'Test Admin', 'test-admin@test.com', 'ADMIN', NULL, 'OTHER', '2019-08-01 12:40:16.738', NULL, 'A Manually Entered School', 'OTHER', '{"{\"stage\": \"all\", \"examBoard\": \"all\"}"}', '2022-07-06 10:48:59.527', '2022-07-06 10:48:59.673', 'VERIFIED', '2022-07-06 15:38:56.023', 'test-admin@test.com', 'AwrblcwVoRFMWxJtV2TXAalOeA7a84TpD3rO2RmE', 0, false);


--
-- Data for Name: qrtz_blob_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--



--
-- Data for Name: qrtz_calendars; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--



--
-- Data for Name: qrtz_cron_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

INSERT INTO quartz_cluster.qrtz_cron_triggers (sched_name, trigger_name, trigger_group, cron_expression, time_zone_id) VALUES ('SegueScheduler', 'PIIDeleteScheduledJob_trigger', 'SQLMaintenance', '0 0 2 * * ?', 'Europe/London');
INSERT INTO quartz_cluster.qrtz_cron_triggers (sched_name, trigger_name, trigger_group, cron_expression, time_zone_id) VALUES ('SegueScheduler', 'cleanAnonymousUsers_trigger', 'SQLMaintenance', '0 30 2 * * ?', 'Europe/London');
INSERT INTO quartz_cluster.qrtz_cron_triggers (sched_name, trigger_name, trigger_group, cron_expression, time_zone_id) VALUES ('SegueScheduler', 'cleanUpExpiredReservations_trigger', 'SQLMaintenence', '0 0 7 * * ?', 'Europe/London');
INSERT INTO quartz_cluster.qrtz_cron_triggers (sched_name, trigger_name, trigger_group, cron_expression, time_zone_id) VALUES ('SegueScheduler', 'deleteEventAdditionalBookingInformation_trigger', 'JavaJob', '0 0 7 * * ?', 'Europe/London');
INSERT INTO quartz_cluster.qrtz_cron_triggers (sched_name, trigger_name, trigger_group, cron_expression, time_zone_id) VALUES ('SegueScheduler', 'deleteEventAdditionalBookingInformationOneYear_trigger', 'JavaJob', '0 0 7 * * ?', 'Europe/London');


--
-- Data for Name: qrtz_fired_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--



--
-- Data for Name: qrtz_job_details; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

INSERT INTO quartz_cluster.qrtz_job_details (sched_name, job_name, job_group, description, job_class_name, is_durable, is_nonconcurrent, is_update_data, requests_recovery, job_data) VALUES ('SegueScheduler', 'PIIDeleteScheduledJob', 'SQLMaintenance', 'SQL scheduled job that deletes PII', 'uk.ac.cam.cl.dtg.segue.scheduler.DatabaseScriptExecutionJob', false, false, false, false, '\xaced0005737200156f72672e71756172747a2e4a6f62446174614d61709fb083e8bfa9b0cb020000787200266f72672e71756172747a2e7574696c732e537472696e674b65794469727479466c61674d61708208e8c3fbc55d280200015a0013616c6c6f77735472616e7369656e74446174617872001d6f72672e71756172747a2e7574696c732e4469727479466c61674d617013e62ead28760ace0200025a000564697274794c00036d617074000f4c6a6176612f7574696c2f4d61703b787000737200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c7708000000100000000174000753514c46696c6574002864625f736372697074732f7363686564756c65642f7069692d64656c6574652d7461736b2e73716c7800');
INSERT INTO quartz_cluster.qrtz_job_details (sched_name, job_name, job_group, description, job_class_name, is_durable, is_nonconcurrent, is_update_data, requests_recovery, job_data) VALUES ('SegueScheduler', 'cleanAnonymousUsers', 'SQLMaintenance', 'SQL scheduled job that deletes old AnonymousUsers', 'uk.ac.cam.cl.dtg.segue.scheduler.DatabaseScriptExecutionJob', false, false, false, false, '\xaced0005737200156f72672e71756172747a2e4a6f62446174614d61709fb083e8bfa9b0cb020000787200266f72672e71756172747a2e7574696c732e537472696e674b65794469727479466c61674d61708208e8c3fbc55d280200015a0013616c6c6f77735472616e7369656e74446174617872001d6f72672e71756172747a2e7574696c732e4469727479466c61674d617013e62ead28760ace0200025a000564697274794c00036d617074000f4c6a6176612f7574696c2f4d61703b787000737200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c7708000000100000000174000753514c46696c6574003064625f736372697074732f7363686564756c65642f616e6f6e796d6f75732d757365722d636c65616e2d75702e73716c7800');
INSERT INTO quartz_cluster.qrtz_job_details (sched_name, job_name, job_group, description, job_class_name, is_durable, is_nonconcurrent, is_update_data, requests_recovery, job_data) VALUES ('SegueScheduler', 'cleanUpExpiredReservations', 'SQLMaintenence', 'SQL scheduled job that deletes expired reservations for the event booking system', 'uk.ac.cam.cl.dtg.segue.scheduler.DatabaseScriptExecutionJob', false, false, false, false, '\xaced0005737200156f72672e71756172747a2e4a6f62446174614d61709fb083e8bfa9b0cb020000787200266f72672e71756172747a2e7574696c732e537472696e674b65794469727479466c61674d61708208e8c3fbc55d280200015a0013616c6c6f77735472616e7369656e74446174617872001d6f72672e71756172747a2e7574696c732e4469727479466c61674d617013e62ead28760ace0200025a000564697274794c00036d617074000f4c6a6176612f7574696c2f4d61703b787000737200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c7708000000100000000174000753514c46696c6574003664625f736372697074732f7363686564756c65642f657870697265642d7265736572766174696f6e732d636c65616e2d75702e73716c7800');
INSERT INTO quartz_cluster.qrtz_job_details (sched_name, job_name, job_group, description, job_class_name, is_durable, is_nonconcurrent, is_update_data, requests_recovery, job_data) VALUES ('SegueScheduler', 'deleteEventAdditionalBookingInformation', 'JavaJob', 'Delete event additional booking information a given period after an event has taken place', 'uk.ac.cam.cl.dtg.segue.scheduler.jobs.DeleteEventAdditionalBookingInformationJob', false, false, false, false, '\xaced0005737200156f72672e71756172747a2e4a6f62446174614d61709fb083e8bfa9b0cb020000787200266f72672e71756172747a2e7574696c732e537472696e674b65794469727479466c61674d61708208e8c3fbc55d280200015a0013616c6c6f77735472616e7369656e74446174617872001d6f72672e71756172747a2e7574696c732e4469727479466c61674d617013e62ead28760ace0200025a000564697274794c00036d617074000f4c6a6176612f7574696c2f4d61703b787000737200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f40000000000010770800000010000000007800');
INSERT INTO quartz_cluster.qrtz_job_details (sched_name, job_name, job_group, description, job_class_name, is_durable, is_nonconcurrent, is_update_data, requests_recovery, job_data) VALUES ('SegueScheduler', 'deleteEventAdditionalBookingInformationOneYear', 'JavaJob', 'Delete event additional booking information a year after an event has taken place if not already removed', 'uk.ac.cam.cl.dtg.segue.scheduler.jobs.DeleteEventAdditionalBookingInformationOneYearJob', false, false, false, false, '\xaced0005737200156f72672e71756172747a2e4a6f62446174614d61709fb083e8bfa9b0cb020000787200266f72672e71756172747a2e7574696c732e537472696e674b65794469727479466c61674d61708208e8c3fbc55d280200015a0013616c6c6f77735472616e7369656e74446174617872001d6f72672e71756172747a2e7574696c732e4469727479466c61674d617013e62ead28760ace0200025a000564697274794c00036d617074000f4c6a6176612f7574696c2f4d61703b787000737200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f40000000000010770800000010000000007800');


--
-- Data for Name: qrtz_locks; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

INSERT INTO quartz_cluster.qrtz_locks (sched_name, lock_name) VALUES ('SegueScheduler', 'TRIGGER_ACCESS');
INSERT INTO quartz_cluster.qrtz_locks (sched_name, lock_name) VALUES ('SegueScheduler', 'STATE_ACCESS');


--
-- Data for Name: qrtz_paused_trigger_grps; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--



--
-- Data for Name: qrtz_scheduler_state; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

INSERT INTO quartz_cluster.qrtz_scheduler_state (sched_name, instance_name, last_checkin_time, checkin_interval) VALUES ('SegueScheduler', 'user-109-9.vpn.cl.cam.ac.uk1657718942083', 1657719042704, 20000);


--
-- Data for Name: qrtz_simple_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--



--
-- Data for Name: qrtz_simprop_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--



--
-- Data for Name: qrtz_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

INSERT INTO quartz_cluster.qrtz_triggers (sched_name, trigger_name, trigger_group, job_name, job_group, description, next_fire_time, prev_fire_time, priority, trigger_state, trigger_type, start_time, end_time, calendar_name, misfire_instr, job_data) VALUES ('SegueScheduler', 'PIIDeleteScheduledJob_trigger', 'SQLMaintenance', 'PIIDeleteScheduledJob', 'SQLMaintenance', NULL, 1657760400000, -1, 5, 'WAITING', 'CRON', 1657718942000, 0, NULL, 0, '\x');
INSERT INTO quartz_cluster.qrtz_triggers (sched_name, trigger_name, trigger_group, job_name, job_group, description, next_fire_time, prev_fire_time, priority, trigger_state, trigger_type, start_time, end_time, calendar_name, misfire_instr, job_data) VALUES ('SegueScheduler', 'cleanAnonymousUsers_trigger', 'SQLMaintenance', 'cleanAnonymousUsers', 'SQLMaintenance', NULL, 1657762200000, -1, 5, 'WAITING', 'CRON', 1657718942000, 0, NULL, 0, '\x');
INSERT INTO quartz_cluster.qrtz_triggers (sched_name, trigger_name, trigger_group, job_name, job_group, description, next_fire_time, prev_fire_time, priority, trigger_state, trigger_type, start_time, end_time, calendar_name, misfire_instr, job_data) VALUES ('SegueScheduler', 'cleanUpExpiredReservations_trigger', 'SQLMaintenence', 'cleanUpExpiredReservations', 'SQLMaintenence', NULL, 1657778400000, -1, 5, 'WAITING', 'CRON', 1657718942000, 0, NULL, 0, '\x');
INSERT INTO quartz_cluster.qrtz_triggers (sched_name, trigger_name, trigger_group, job_name, job_group, description, next_fire_time, prev_fire_time, priority, trigger_state, trigger_type, start_time, end_time, calendar_name, misfire_instr, job_data) VALUES ('SegueScheduler', 'deleteEventAdditionalBookingInformation_trigger', 'JavaJob', 'deleteEventAdditionalBookingInformation', 'JavaJob', NULL, 1657778400000, -1, 5, 'WAITING', 'CRON', 1657718942000, 0, NULL, 0, '\x');
INSERT INTO quartz_cluster.qrtz_triggers (sched_name, trigger_name, trigger_group, job_name, job_group, description, next_fire_time, prev_fire_time, priority, trigger_state, trigger_type, start_time, end_time, calendar_name, misfire_instr, job_data) VALUES ('SegueScheduler', 'deleteEventAdditionalBookingInformationOneYear_trigger', 'JavaJob', 'deleteEventAdditionalBookingInformationOneYear', 'JavaJob', NULL, 1657778400000, -1, 5, 'WAITING', 'CRON', 1657718942000, 0, NULL, 0, '\x');


--
-- Name: assignments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.assignments_id_seq', 1, true);


--
-- Name: event_bookings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.event_bookings_id_seq', 4, true);


--
-- Name: groups_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.groups_id_seq', 1, true);


--
-- Name: ip_location_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.ip_location_history_id_seq', 1, false);


--
-- Name: logged_events_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.logged_events_id_seq', 412, true);


--
-- Name: question_attempts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.question_attempts_id_seq', 25, true);


--
-- Name: quiz_assignments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.quiz_assignments_id_seq', 2, true);


--
-- Name: quiz_attempts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.quiz_attempts_id_seq', 1, true);


--
-- Name: quiz_question_attempts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.quiz_question_attempts_id_seq', 3, true);


--
-- Name: user_alerts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.user_alerts_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.users_id_seq', 1, true);


--
-- Name: users User Id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT "User Id" PRIMARY KEY (id);


--
-- Name: group_additional_managers ck_user_group_manager; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.group_additional_managers
    ADD CONSTRAINT ck_user_group_manager PRIMARY KEY (user_id, group_id);


--
-- Name: assignments composite pkey assignments; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT "composite pkey assignments" PRIMARY KEY (gameboard_id, group_id);


--
-- Name: linked_accounts compound key; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.linked_accounts
    ADD CONSTRAINT "compound key" PRIMARY KEY (user_id, provider);


--
-- Name: event_bookings eventbooking id pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.event_bookings
    ADD CONSTRAINT "eventbooking id pkey" PRIMARY KEY (id);


--
-- Name: external_accounts external_accounts_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.external_accounts
    ADD CONSTRAINT external_accounts_pk PRIMARY KEY (user_id, provider_name);


--
-- Name: gameboards gameboard-id-pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.gameboards
    ADD CONSTRAINT "gameboard-id-pkey" PRIMARY KEY (id);


--
-- Name: group_memberships group_membership_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.group_memberships
    ADD CONSTRAINT group_membership_pkey PRIMARY KEY (group_id, user_id);


--
-- Name: groups group_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT group_pkey PRIMARY KEY (id);


--
-- Name: logged_events id pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.logged_events
    ADD CONSTRAINT "id pkey" PRIMARY KEY (id);


--
-- Name: ip_location_history id pky; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.ip_location_history
    ADD CONSTRAINT "id pky" PRIMARY KEY (id);


--
-- Name: user_notifications notification_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT notification_pkey PRIMARY KEY (user_id, notification_id);


--
-- Name: user_associations_tokens only_one_token_per_user_per_group; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_associations_tokens
    ADD CONSTRAINT only_one_token_per_user_per_group UNIQUE (owner_user_id, group_id);


--
-- Name: linked_accounts provider and user id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.linked_accounts
    ADD CONSTRAINT "provider and user id" UNIQUE (provider, provider_user_id);


--
-- Name: question_attempts question_attempts_id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.question_attempts
    ADD CONSTRAINT question_attempts_id PRIMARY KEY (id);


--
-- Name: quiz_assignments quiz_assignments_id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.quiz_assignments
    ADD CONSTRAINT quiz_assignments_id PRIMARY KEY (id);


--
-- Name: quiz_attempts quiz_attempts_id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.quiz_attempts
    ADD CONSTRAINT quiz_attempts_id PRIMARY KEY (id);


--
-- Name: quiz_question_attempts quiz_question_attempts_id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.quiz_question_attempts
    ADD CONSTRAINT quiz_question_attempts_id PRIMARY KEY (id);


--
-- Name: temporary_user_store temporary_user_store_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.temporary_user_store
    ADD CONSTRAINT temporary_user_store_pk PRIMARY KEY (id);


--
-- Name: user_associations_tokens token_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_associations_tokens
    ADD CONSTRAINT token_pkey PRIMARY KEY (token);


--
-- Name: uk_post_codes uk_post_codes_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.uk_post_codes
    ADD CONSTRAINT uk_post_codes_pk PRIMARY KEY (postcode);


--
-- Name: users unique sha id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT "unique sha id" UNIQUE (_id);


--
-- Name: user_alerts user_alerts_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_alerts
    ADD CONSTRAINT user_alerts_pkey PRIMARY KEY (id);


--
-- Name: user_associations user_associations_composite_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_associations
    ADD CONSTRAINT user_associations_composite_pkey PRIMARY KEY (user_id_granting_permission, user_id_receiving_permission);


--
-- Name: user_gameboards user_gameboard_composite_key; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_gameboards
    ADD CONSTRAINT user_gameboard_composite_key PRIMARY KEY (user_id, gameboard_id);


--
-- Name: user_credentials user_id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_credentials
    ADD CONSTRAINT user_id PRIMARY KEY (user_id);


--
-- Name: user_email_preferences user_id_email_preference_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_email_preferences
    ADD CONSTRAINT user_id_email_preference_pk PRIMARY KEY (user_id, email_preference);


--
-- Name: user_totp user_id_mfa_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_totp
    ADD CONSTRAINT user_id_mfa_pk PRIMARY KEY (user_id);


--
-- Name: user_preferences user_id_preference_type_name_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT user_id_preference_type_name_pk PRIMARY KEY (user_id, preference_type, preference_name);


--
-- Name: user_streak_freezes user_streak_freeze_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_streak_freezes
    ADD CONSTRAINT user_streak_freeze_pkey PRIMARY KEY (user_id, start_date);


--
-- Name: user_streak_targets user_streak_targets_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_streak_targets
    ADD CONSTRAINT user_streak_targets_pkey PRIMARY KEY (user_id, start_date);


--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_calendars qrtz_calendars_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_calendars
    ADD CONSTRAINT qrtz_calendars_pkey PRIMARY KEY (sched_name, calendar_name);


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_fired_triggers qrtz_fired_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_fired_triggers
    ADD CONSTRAINT qrtz_fired_triggers_pkey PRIMARY KEY (sched_name, entry_id);


--
-- Name: qrtz_job_details qrtz_job_details_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_job_details
    ADD CONSTRAINT qrtz_job_details_pkey PRIMARY KEY (sched_name, job_name, job_group);


--
-- Name: qrtz_locks qrtz_locks_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_locks
    ADD CONSTRAINT qrtz_locks_pkey PRIMARY KEY (sched_name, lock_name);


--
-- Name: qrtz_paused_trigger_grps qrtz_paused_trigger_grps_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_paused_trigger_grps
    ADD CONSTRAINT qrtz_paused_trigger_grps_pkey PRIMARY KEY (sched_name, trigger_group);


--
-- Name: qrtz_scheduler_state qrtz_scheduler_state_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_scheduler_state
    ADD CONSTRAINT qrtz_scheduler_state_pkey PRIMARY KEY (sched_name, instance_name);


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_triggers qrtz_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: assignments_group_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX assignments_group_id ON public.assignments USING btree (group_id DESC);


--
-- Name: event_booking_user_event_id_index; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX event_booking_user_event_id_index ON public.event_bookings USING btree (event_id, user_id);


--
-- Name: fki_user_id fkey; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX "fki_user_id fkey" ON public.user_notifications USING btree (user_id);


--
-- Name: gameboards_tags_gin_index; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX gameboards_tags_gin_index ON public.gameboards USING gin (tags);


--
-- Name: group_additional_managers_group_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX group_additional_managers_group_id ON public.group_additional_managers USING btree (group_id);


--
-- Name: groups_owner_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX groups_owner_id ON public.groups USING btree (owner_id);


--
-- Name: ip_location_history_ips; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX ip_location_history_ips ON public.ip_location_history USING btree (ip_address DESC);


--
-- Name: log_events_timestamp; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX log_events_timestamp ON public.logged_events USING btree ("timestamp");


--
-- Name: log_events_type; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX log_events_type ON public.logged_events USING btree (event_type);


--
-- Name: log_events_user_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX log_events_user_id ON public.logged_events USING btree (user_id);


--
-- Name: logged_events_type_timestamp; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX logged_events_type_timestamp ON public.logged_events USING btree (event_type, "timestamp");


--
-- Name: only_one_attempt_per_assignment_per_user; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX only_one_attempt_per_assignment_per_user ON public.quiz_attempts USING btree (quiz_assignment_id, user_id) WHERE (quiz_assignment_id IS NOT NULL);


--
-- Name: question-attempts-by-user; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX "question-attempts-by-user" ON public.question_attempts USING btree (user_id);


--
-- Name: question_attempts_by_question; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX question_attempts_by_question ON public.question_attempts USING btree (question_id);


--
-- Name: question_attempts_by_timestamp; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX question_attempts_by_timestamp ON public.question_attempts USING btree ("timestamp");


--
-- Name: question_attempts_by_user_question; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX question_attempts_by_user_question ON public.question_attempts USING btree (user_id, question_id text_pattern_ops);


--
-- Name: quiz_attempts_index_by_quiz_id_and_user_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX quiz_attempts_index_by_quiz_id_and_user_id ON public.quiz_attempts USING btree (quiz_id, user_id);


--
-- Name: quiz_question_attempts_by_quiz_attempt_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX quiz_question_attempts_by_quiz_attempt_id ON public.quiz_question_attempts USING btree (quiz_attempt_id);


--
-- Name: unique email case insensitive; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX "unique email case insensitive" ON public.users USING btree (lower(email));


--
-- Name: user_alerts_id_uindex; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX user_alerts_id_uindex ON public.user_alerts USING btree (id);


--
-- Name: user_badges_user_id_badge_unique; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX user_badges_user_id_badge_unique ON public.user_badges USING btree (user_id, badge);


--
-- Name: user_email; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX user_email ON public.users USING btree (email);


--
-- Name: user_streak_freezes_by_user_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX user_streak_freezes_by_user_id ON public.user_streak_freezes USING btree (user_id);


--
-- Name: user_streak_targets_by_user_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX user_streak_targets_by_user_id ON public.user_streak_targets USING btree (user_id);


--
-- Name: users_id_role; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX users_id_role ON public.users USING btree (id, role);


--
-- Name: idx_qrtz_ft_job_group; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_job_group ON quartz_cluster.qrtz_fired_triggers USING btree (job_group);


--
-- Name: idx_qrtz_ft_job_name; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_job_name ON quartz_cluster.qrtz_fired_triggers USING btree (job_name);


--
-- Name: idx_qrtz_ft_job_req_recovery; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_job_req_recovery ON quartz_cluster.qrtz_fired_triggers USING btree (requests_recovery);


--
-- Name: idx_qrtz_ft_trig_group; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_trig_group ON quartz_cluster.qrtz_fired_triggers USING btree (trigger_group);


--
-- Name: idx_qrtz_ft_trig_inst_name; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_trig_inst_name ON quartz_cluster.qrtz_fired_triggers USING btree (instance_name);


--
-- Name: idx_qrtz_ft_trig_name; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_trig_name ON quartz_cluster.qrtz_fired_triggers USING btree (trigger_name);


--
-- Name: idx_qrtz_ft_trig_nm_gp; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_trig_nm_gp ON quartz_cluster.qrtz_fired_triggers USING btree (sched_name, trigger_name, trigger_group);


--
-- Name: idx_qrtz_j_req_recovery; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_j_req_recovery ON quartz_cluster.qrtz_job_details USING btree (requests_recovery);


--
-- Name: idx_qrtz_t_next_fire_time; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_t_next_fire_time ON quartz_cluster.qrtz_triggers USING btree (next_fire_time);


--
-- Name: idx_qrtz_t_nft_st; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_t_nft_st ON quartz_cluster.qrtz_triggers USING btree (next_fire_time, trigger_state);


--
-- Name: idx_qrtz_t_state; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_t_state ON quartz_cluster.qrtz_triggers USING btree (trigger_state);


--
-- Name: assignments assignment_group_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT assignment_group_fkey FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;


--
-- Name: assignments assignment_owner_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT assignment_owner_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: event_bookings event_bookings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.event_bookings
    ADD CONSTRAINT event_bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: event_bookings event_bookings_users_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.event_bookings
    ADD CONSTRAINT event_bookings_users_id_fk FOREIGN KEY (reserved_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: external_accounts external_accounts_fk; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.external_accounts
    ADD CONSTRAINT external_accounts_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: group_additional_managers fk_group_id; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.group_additional_managers
    ADD CONSTRAINT fk_group_id FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;


--
-- Name: user_credentials fk_user_id_pswd; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_credentials
    ADD CONSTRAINT fk_user_id_pswd FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: group_additional_managers fk_user_manager_id; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.group_additional_managers
    ADD CONSTRAINT fk_user_manager_id FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: assignments gameboard_assignment_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT gameboard_assignment_fkey FOREIGN KEY (gameboard_id) REFERENCES public.gameboards(id) ON DELETE CASCADE;


--
-- Name: user_gameboards gameboard_id_fkey_gameboard_link; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_gameboards
    ADD CONSTRAINT gameboard_id_fkey_gameboard_link FOREIGN KEY (gameboard_id) REFERENCES public.gameboards(id) ON DELETE CASCADE;


--
-- Name: gameboards gameboard_user_id_pkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.gameboards
    ADD CONSTRAINT gameboard_user_id_pkey FOREIGN KEY (owner_user_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: user_associations_tokens group_id_token_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_associations_tokens
    ADD CONSTRAINT group_id_token_fkey FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;


--
-- Name: group_memberships group_membership_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.group_memberships
    ADD CONSTRAINT group_membership_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;


--
-- Name: group_memberships group_membership_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.group_memberships
    ADD CONSTRAINT group_membership_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: linked_accounts local_user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.linked_accounts
    ADD CONSTRAINT "local_user_id fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: groups owner_user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT "owner_user_id fkey" FOREIGN KEY (owner_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: quiz_question_attempts quiz_attempt_id_quiz_question_attempts_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.quiz_question_attempts
    ADD CONSTRAINT quiz_attempt_id_quiz_question_attempts_fkey FOREIGN KEY (quiz_attempt_id) REFERENCES public.quiz_attempts(id) ON DELETE CASCADE;


--
-- Name: user_associations_tokens token_owner_user_id; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_associations_tokens
    ADD CONSTRAINT token_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_associations user_granting_permission_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_associations
    ADD CONSTRAINT user_granting_permission_fkey FOREIGN KEY (user_id_granting_permission) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_notifications user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT "user_id fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_email_preferences user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_email_preferences
    ADD CONSTRAINT user_id_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_gameboards user_id_fkey_gameboard_link; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_gameboards
    ADD CONSTRAINT user_id_fkey_gameboard_link FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_totp user_id_mfa_fk; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_totp
    ADD CONSTRAINT user_id_mfa_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: question_attempts user_id_question_attempts_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.question_attempts
    ADD CONSTRAINT user_id_question_attempts_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: quiz_attempts user_id_quiz_attempts_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.quiz_attempts
    ADD CONSTRAINT user_id_quiz_attempts_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_preferences user_preference_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT user_preference_user_id_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_associations user_receiving_permissions_key; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_associations
    ADD CONSTRAINT user_receiving_permissions_key FOREIGN KEY (user_id_receiving_permission) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_streak_freezes user_streak_freezes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_streak_freezes
    ADD CONSTRAINT user_streak_freezes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_streak_targets user_streak_targets_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY public.user_streak_targets
    ADD CONSTRAINT user_streak_targets_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES quartz_cluster.qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES quartz_cluster.qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES quartz_cluster.qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES quartz_cluster.qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_triggers qrtz_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_sched_name_fkey FOREIGN KEY (sched_name, job_name, job_group) REFERENCES quartz_cluster.qrtz_job_details(sched_name, job_name, job_group);


--
-- PostgreSQL database dump complete
--

