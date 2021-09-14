--
-- PostgreSQL database dump
--

-- Dumped from database version 13rc1 (Debian 13~rc1-1.pgdg100+1)
-- Dumped by pg_dump version 13rc1 (Debian 13~rc1-1.pgdg100+1)

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
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';

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
    reserved_by integer DEFAULT NULL,
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
    contents jsonb[] DEFAULT array[]::jsonb[] NOT NULL,
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
    registered_contexts jsonb[] DEFAULT array[]::jsonb[] NOT NULL,
    registered_contexts_last_confirmed timestamp without time zone;
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
-- PostgreSQL database dump complete
--

