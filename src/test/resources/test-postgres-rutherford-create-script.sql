-- SCHEMA

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

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';

SET default_tablespace = '';

SET default_table_access_method = heap;

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

CREATE SEQUENCE public.assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.assignments_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.assignments_id_seq OWNED BY public.assignments.id;

CREATE TABLE public.event_bookings (
    id integer NOT NULL,
    event_id text NOT NULL,
    created timestamp without time zone NOT NULL,
    user_id integer NOT NULL,
    reserved_by integer DEFAULT NULL,
    status text DEFAULT 'CONFIRMED'::text NOT NULL,
    updated timestamp without time zone,
    additional_booking_information jsonb,
    pii_removed timestamp without time zone
);

ALTER TABLE public.event_bookings OWNER TO rutherford;

CREATE SEQUENCE public.event_bookings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.event_bookings_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.event_bookings_id_seq OWNED BY public.event_bookings.id;

CREATE TABLE public.external_accounts (
    user_id integer NOT NULL,
    provider_name text NOT NULL,
    provider_user_identifier text,
    provider_last_updated timestamp without time zone
);

ALTER TABLE public.external_accounts OWNER TO rutherford;

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

CREATE TABLE public.group_additional_managers (
    user_id integer NOT NULL,
    group_id integer NOT NULL,
    created timestamp with time zone DEFAULT now()
);

ALTER TABLE public.group_additional_managers OWNER TO rutherford;

CREATE TABLE public.group_memberships (
    group_id integer NOT NULL,
    user_id integer NOT NULL,
    created timestamp without time zone,
    updated timestamp with time zone DEFAULT now(),
    status text DEFAULT 'ACTIVE'::text
);

ALTER TABLE public.group_memberships OWNER TO rutherford;

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

CREATE SEQUENCE public.groups_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.groups_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.groups_id_seq OWNED BY public.groups.id;

CREATE TABLE public.ip_location_history (
    id integer NOT NULL,
    ip_address text NOT NULL,
    location_information jsonb,
    created timestamp without time zone,
    last_lookup timestamp without time zone,
    is_current boolean DEFAULT true
);

ALTER TABLE public.ip_location_history OWNER TO rutherford;

CREATE SEQUENCE public.ip_location_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.ip_location_history_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.ip_location_history_id_seq OWNED BY public.ip_location_history.id;

CREATE TABLE public.linked_accounts (
    user_id bigint NOT NULL,
    provider character varying(100) NOT NULL,
    provider_user_id text
);

ALTER TABLE public.linked_accounts OWNER TO rutherford;

COMMENT ON COLUMN public.linked_accounts.user_id IS 'This is the postgres foreign key for the users table.';

COMMENT ON COLUMN public.linked_accounts.provider_user_id IS 'user id from the remote service';

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

CREATE SEQUENCE public.logged_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.logged_events_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.logged_events_id_seq OWNED BY public.logged_events.id;

CREATE TABLE public.question_attempts (
    id integer NOT NULL,
    user_id integer NOT NULL,
    question_id text NOT NULL,
    question_attempt jsonb,
    correct boolean,
    "timestamp" timestamp without time zone
);

ALTER TABLE public.question_attempts OWNER TO rutherford;

CREATE SEQUENCE public.question_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.question_attempts_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.question_attempts_id_seq OWNED BY public.question_attempts.id;

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

CREATE SEQUENCE public.quiz_assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.quiz_assignments_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.quiz_assignments_id_seq OWNED BY public.quiz_assignments.id;

CREATE TABLE public.quiz_attempts (
    id integer NOT NULL,
    user_id integer NOT NULL,
    quiz_id character varying(255) NOT NULL,
    quiz_assignment_id integer,
    start_date timestamp without time zone NOT NULL,
    completed_date timestamp with time zone
);

ALTER TABLE public.quiz_attempts OWNER TO rutherford;

CREATE SEQUENCE public.quiz_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.quiz_attempts_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.quiz_attempts_id_seq OWNED BY public.quiz_attempts.id;

CREATE TABLE public.quiz_question_attempts (
    id integer NOT NULL,
    quiz_attempt_id integer NOT NULL,
    question_id text NOT NULL,
    question_attempt jsonb,
    correct boolean,
    "timestamp" timestamp without time zone
);

ALTER TABLE public.quiz_question_attempts OWNER TO rutherford;

CREATE SEQUENCE public.quiz_question_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.quiz_question_attempts_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.quiz_question_attempts_id_seq OWNED BY public.quiz_question_attempts.id;

CREATE TABLE public.temporary_user_store (
    id character varying NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    last_updated timestamp with time zone DEFAULT now() NOT NULL,
    temporary_app_data jsonb
);

ALTER TABLE public.temporary_user_store OWNER TO rutherford;

CREATE TABLE public.uk_post_codes (
    postcode character varying(255) NOT NULL,
    lat numeric NOT NULL,
    lon numeric NOT NULL
);

ALTER TABLE public.uk_post_codes OWNER TO rutherford;

CREATE SEQUENCE public.user_alerts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.user_alerts_id_seq OWNER TO rutherford;

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

CREATE TABLE public.user_associations (
    user_id_granting_permission integer NOT NULL,
    user_id_receiving_permission integer NOT NULL,
    created timestamp without time zone
);

ALTER TABLE public.user_associations OWNER TO rutherford;

CREATE TABLE public.user_associations_tokens (
    token character varying(100) NOT NULL,
    owner_user_id integer,
    group_id integer
);

ALTER TABLE public.user_associations_tokens OWNER TO rutherford;

CREATE TABLE public.user_badges (
    user_id integer,
    badge text,
    state jsonb
);

ALTER TABLE public.user_badges OWNER TO rutherford;

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

CREATE TABLE public.user_email_preferences (
    user_id integer NOT NULL,
    email_preference integer NOT NULL,
    email_preference_status boolean
);

ALTER TABLE public.user_email_preferences OWNER TO rutherford;

CREATE TABLE public.user_gameboards (
    user_id integer NOT NULL,
    gameboard_id character varying NOT NULL,
    created timestamp without time zone,
    last_visited timestamp without time zone
);

ALTER TABLE public.user_gameboards OWNER TO rutherford;

CREATE TABLE public.user_notifications (
    user_id integer NOT NULL,
    notification_id text NOT NULL,
    status text,
    created timestamp without time zone NOT NULL
);

ALTER TABLE public.user_notifications OWNER TO rutherford;

CREATE TABLE public.user_preferences (
    user_id integer NOT NULL,
    preference_type character varying(255) NOT NULL,
    preference_name character varying(255) NOT NULL,
    preference_value boolean NOT NULL,
    last_updated timestamp without time zone
);

ALTER TABLE public.user_preferences OWNER TO rutherford;

CREATE TABLE public.user_streak_freezes (
    user_id bigint NOT NULL,
    start_date date NOT NULL,
    end_date date,
    comment text
);

ALTER TABLE public.user_streak_freezes OWNER TO rutherford;

CREATE TABLE public.user_streak_targets (
    user_id bigint NOT NULL,
    target_count integer NOT NULL,
    start_date date NOT NULL,
    end_date date,
    comment text
);

ALTER TABLE public.user_streak_targets OWNER TO rutherford;

CREATE TABLE public.user_totp (
    user_id integer NOT NULL,
    shared_secret text NOT NULL,
    created timestamp with time zone DEFAULT now(),
    last_updated timestamp with time zone DEFAULT now()
);

ALTER TABLE public.user_totp OWNER TO rutherford;

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

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER TABLE public.users_id_seq OWNER TO rutherford;

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;

ALTER TABLE ONLY public.assignments ALTER COLUMN id SET DEFAULT nextval('public.assignments_id_seq'::regclass);

ALTER TABLE ONLY public.event_bookings ALTER COLUMN id SET DEFAULT nextval('public.event_bookings_id_seq'::regclass);

ALTER TABLE ONLY public.groups ALTER COLUMN id SET DEFAULT nextval('public.groups_id_seq'::regclass);

ALTER TABLE ONLY public.ip_location_history ALTER COLUMN id SET DEFAULT nextval('public.ip_location_history_id_seq'::regclass);

ALTER TABLE ONLY public.logged_events ALTER COLUMN id SET DEFAULT nextval('public.logged_events_id_seq'::regclass);

ALTER TABLE ONLY public.question_attempts ALTER COLUMN id SET DEFAULT nextval('public.question_attempts_id_seq'::regclass);

ALTER TABLE ONLY public.quiz_assignments ALTER COLUMN id SET DEFAULT nextval('public.quiz_assignments_id_seq'::regclass);

ALTER TABLE ONLY public.quiz_attempts ALTER COLUMN id SET DEFAULT nextval('public.quiz_attempts_id_seq'::regclass);

ALTER TABLE ONLY public.quiz_question_attempts ALTER COLUMN id SET DEFAULT nextval('public.quiz_question_attempts_id_seq'::regclass);

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT "User Id" PRIMARY KEY (id);

ALTER TABLE ONLY public.group_additional_managers
    ADD CONSTRAINT ck_user_group_manager PRIMARY KEY (user_id, group_id);

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT "composite pkey assignments" PRIMARY KEY (gameboard_id, group_id);

ALTER TABLE ONLY public.linked_accounts
    ADD CONSTRAINT "compound key" PRIMARY KEY (user_id, provider);

ALTER TABLE ONLY public.event_bookings
    ADD CONSTRAINT "eventbooking id pkey" PRIMARY KEY (id);

ALTER TABLE ONLY public.external_accounts
    ADD CONSTRAINT external_accounts_pk PRIMARY KEY (user_id, provider_name);

ALTER TABLE ONLY public.gameboards
    ADD CONSTRAINT "gameboard-id-pkey" PRIMARY KEY (id);

ALTER TABLE ONLY public.group_memberships
    ADD CONSTRAINT group_membership_pkey PRIMARY KEY (group_id, user_id);

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT group_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.logged_events
    ADD CONSTRAINT "id pkey" PRIMARY KEY (id);

ALTER TABLE ONLY public.ip_location_history
    ADD CONSTRAINT "id pky" PRIMARY KEY (id);

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT notification_pkey PRIMARY KEY (user_id, notification_id);

ALTER TABLE ONLY public.user_associations_tokens
    ADD CONSTRAINT only_one_token_per_user_per_group UNIQUE (owner_user_id, group_id);

ALTER TABLE ONLY public.linked_accounts
    ADD CONSTRAINT "provider and user id" UNIQUE (provider, provider_user_id);

ALTER TABLE ONLY public.question_attempts
    ADD CONSTRAINT question_attempts_id PRIMARY KEY (id);

ALTER TABLE ONLY public.quiz_assignments
    ADD CONSTRAINT quiz_assignments_id PRIMARY KEY (id);

ALTER TABLE ONLY public.quiz_attempts
    ADD CONSTRAINT quiz_attempts_id PRIMARY KEY (id);

ALTER TABLE ONLY public.quiz_question_attempts
    ADD CONSTRAINT quiz_question_attempts_id PRIMARY KEY (id);

ALTER TABLE ONLY public.temporary_user_store
    ADD CONSTRAINT temporary_user_store_pk PRIMARY KEY (id);

ALTER TABLE ONLY public.user_associations_tokens
    ADD CONSTRAINT token_pkey PRIMARY KEY (token);

ALTER TABLE ONLY public.uk_post_codes
    ADD CONSTRAINT uk_post_codes_pk PRIMARY KEY (postcode);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT "unique sha id" UNIQUE (_id);

ALTER TABLE ONLY public.user_alerts
    ADD CONSTRAINT user_alerts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.user_associations
    ADD CONSTRAINT user_associations_composite_pkey PRIMARY KEY (user_id_granting_permission, user_id_receiving_permission);

ALTER TABLE ONLY public.user_gameboards
    ADD CONSTRAINT user_gameboard_composite_key PRIMARY KEY (user_id, gameboard_id);

ALTER TABLE ONLY public.user_credentials
    ADD CONSTRAINT user_id PRIMARY KEY (user_id);

ALTER TABLE ONLY public.user_email_preferences
    ADD CONSTRAINT user_id_email_preference_pk PRIMARY KEY (user_id, email_preference);

ALTER TABLE ONLY public.user_totp
    ADD CONSTRAINT user_id_mfa_pk PRIMARY KEY (user_id);

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT user_id_preference_type_name_pk PRIMARY KEY (user_id, preference_type, preference_name);

ALTER TABLE ONLY public.user_streak_freezes
    ADD CONSTRAINT user_streak_freeze_pkey PRIMARY KEY (user_id, start_date);

ALTER TABLE ONLY public.user_streak_targets
    ADD CONSTRAINT user_streak_targets_pkey PRIMARY KEY (user_id, start_date);

CREATE INDEX assignments_group_id ON public.assignments USING btree (group_id DESC);

CREATE UNIQUE INDEX event_booking_user_event_id_index ON public.event_bookings USING btree (event_id, user_id);

CREATE INDEX "fki_user_id fkey" ON public.user_notifications USING btree (user_id);

CREATE INDEX gameboards_tags_gin_index ON public.gameboards USING gin (tags);

CREATE INDEX group_additional_managers_group_id ON public.group_additional_managers USING btree (group_id);

CREATE INDEX groups_owner_id ON public.groups USING btree (owner_id);

CREATE INDEX ip_location_history_ips ON public.ip_location_history USING btree (ip_address DESC);

CREATE INDEX log_events_timestamp ON public.logged_events USING btree ("timestamp");

CREATE INDEX log_events_type ON public.logged_events USING btree (event_type);

CREATE INDEX log_events_user_id ON public.logged_events USING btree (user_id);

CREATE INDEX logged_events_type_timestamp ON public.logged_events USING btree (event_type, "timestamp");

CREATE UNIQUE INDEX only_one_attempt_per_assignment_per_user ON public.quiz_attempts USING btree (quiz_assignment_id, user_id) WHERE (quiz_assignment_id IS NOT NULL);

CREATE INDEX "question-attempts-by-user" ON public.question_attempts USING btree (user_id);

CREATE INDEX question_attempts_by_question ON public.question_attempts USING btree (question_id);

CREATE INDEX question_attempts_by_timestamp ON public.question_attempts USING btree ("timestamp");

CREATE INDEX question_attempts_by_user_question ON public.question_attempts USING btree (user_id, question_id text_pattern_ops);

CREATE INDEX quiz_attempts_index_by_quiz_id_and_user_id ON public.quiz_attempts USING btree (quiz_id, user_id);

CREATE INDEX quiz_question_attempts_by_quiz_attempt_id ON public.quiz_question_attempts USING btree (quiz_attempt_id);

CREATE UNIQUE INDEX "unique email case insensitive" ON public.users USING btree (lower(email));

CREATE UNIQUE INDEX user_alerts_id_uindex ON public.user_alerts USING btree (id);

CREATE UNIQUE INDEX user_badges_user_id_badge_unique ON public.user_badges USING btree (user_id, badge);

CREATE UNIQUE INDEX user_email ON public.users USING btree (email);

CREATE INDEX user_streak_freezes_by_user_id ON public.user_streak_freezes USING btree (user_id);

CREATE INDEX user_streak_targets_by_user_id ON public.user_streak_targets USING btree (user_id);

CREATE INDEX users_id_role ON public.users USING btree (id, role);

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT assignment_group_fkey FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT assignment_owner_fkey FOREIGN KEY (owner_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.event_bookings
    ADD CONSTRAINT event_bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.event_bookings
    ADD CONSTRAINT event_bookings_users_id_fk FOREIGN KEY (reserved_by) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.external_accounts
    ADD CONSTRAINT external_accounts_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY public.group_additional_managers
    ADD CONSTRAINT fk_group_id FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_credentials
    ADD CONSTRAINT fk_user_id_pswd FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.group_additional_managers
    ADD CONSTRAINT fk_user_manager_id FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.assignments
    ADD CONSTRAINT gameboard_assignment_fkey FOREIGN KEY (gameboard_id) REFERENCES public.gameboards(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_gameboards
    ADD CONSTRAINT gameboard_id_fkey_gameboard_link FOREIGN KEY (gameboard_id) REFERENCES public.gameboards(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.gameboards
    ADD CONSTRAINT gameboard_user_id_pkey FOREIGN KEY (owner_user_id) REFERENCES public.users(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.user_associations_tokens
    ADD CONSTRAINT group_id_token_fkey FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.group_memberships
    ADD CONSTRAINT group_membership_group_id_fkey FOREIGN KEY (group_id) REFERENCES public.groups(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.group_memberships
    ADD CONSTRAINT group_membership_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.linked_accounts
    ADD CONSTRAINT "local_user_id fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT "owner_user_id fkey" FOREIGN KEY (owner_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.quiz_question_attempts
    ADD CONSTRAINT quiz_attempt_id_quiz_question_attempts_fkey FOREIGN KEY (quiz_attempt_id) REFERENCES public.quiz_attempts(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_associations_tokens
    ADD CONSTRAINT token_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_associations
    ADD CONSTRAINT user_granting_permission_fkey FOREIGN KEY (user_id_granting_permission) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_notifications
    ADD CONSTRAINT "user_id fkey" FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_email_preferences
    ADD CONSTRAINT user_id_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_gameboards
    ADD CONSTRAINT user_id_fkey_gameboard_link FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_totp
    ADD CONSTRAINT user_id_mfa_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE ONLY public.question_attempts
    ADD CONSTRAINT user_id_question_attempts_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.quiz_attempts
    ADD CONSTRAINT user_id_quiz_attempts_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT user_preference_user_id_fk FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_associations
    ADD CONSTRAINT user_receiving_permissions_key FOREIGN KEY (user_id_receiving_permission) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_streak_freezes
    ADD CONSTRAINT user_streak_freezes_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.user_streak_targets
    ADD CONSTRAINT user_streak_targets_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


-- PERMISSIONS

ALTER USER rutherford SET search_path TO public;
grant usage on schema public to rutherford;
grant create on schema public to rutherford;


-- DATA

INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (2, null, 'Test', 'Test Admin', 'test-admin@test.com', 'ADMIN', null, 'OTHER', '2019-08-01 12:40:16.738000', null, 'A Manually Entered School', 'OTHER', '{}', null, '2021-03-09 16:47:33.901000', 'VERIFIED', '2021-03-09 17:10:39.609000', 'test-admin@test.com', 'AwrblcwVoRFMWxJtV2TXAalOeA7a84TpD3rO2RmE', 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (4, null, 'Editor', 'Test Editor', 'test-editor@test.com', 'CONTENT_EDITOR', null, 'PREFER_NOT_TO_SAY', '2019-08-01 12:50:32.631000', '133801', null, 'OTHER', '{}', null, '2021-03-09 16:46:26.280000', 'VERIFIED', '2021-03-09 17:09:32.472000', 'test-editor@test.com', 'nAAK4xSBuAPRejM4YPNfTKRDGK4Oa1VuL3EMmJburjE', 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (3, null, 'Event Manager', 'Test Event', 'test-event@test.com', 'EVENT_MANAGER', null, 'OTHER', '2019-08-01 12:43:14.583000', '133801', null, 'AQA', '{}', null, '2021-03-09 16:47:03.770000', 'VERIFIED', '2021-03-09 17:10:04.552000', 'test-event@test.com', 'QlIS3PVS33I8jmMo3JPQgIn2xaKe4gFgwXfH4qiI8', 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (5, null, 'Teacher', 'Test Teacher', 'test-teacher@test.com', 'TEACHER', null, 'FEMALE', '2019-08-01 12:51:05.416000', null, 'A Manually Entered School', 'AQA', '{}', null, '2021-03-31 10:19:04.939000', 'VERIFIED', '2021-06-17 16:51:29.977000', 'test-teacher@test.com', 'm9A8P0VbpFQnzOdXOywx75lpaWSpssLmQ779ij2b5LQ', 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (1, null, 'Progress', 'Test Progress', 'test-progress@test.com', 'STUDENT', null, 'FEMALE', '2019-08-01 12:28:22.869000', '130615', null, 'AQA', '{"{\"stage\": \"all\", \"examBoard\": \"ocr\"}"}', '2021-10-04 14:10:37.441000', '2021-11-05 10:52:13.018000', 'VERIFIED', '2021-11-05 10:52:13.140000', 'test-progress@test.com', 'scIF1UJeYyGRGwGrwGNUyIWuZxKBrQHd8evcAeZk', 0, false);
INSERT INTO public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, exam_board, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) VALUES (6, null, 'Student', 'Test Student', 'test-student@test.com', 'STUDENT', null, 'MALE', '2019-08-01 12:51:39.981000', '110158', null, 'OCR', '{"{\"stage\": \"all\", \"examBoard\": \"ocr\"}"}', '2021-10-04 14:12:13.351000', '2021-10-04 14:12:13.384000', 'VERIFIED', '2022-03-22 15:37:44.585000', 'test-student@test.com', 'ZMUU7NbjhUSawOClEzb1KPEMcUA93QCkxuGejMwmE', 0, false);
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (5, 'ak86hEtKZzGppIDDaPBOIftJ5rrI/lSKz30q3hkX0utfxsv+f5rzb1RfEEi5rbfIEaseGs18Aj6X3zYV/ZRNwQ==', 'EXNmh521xIMQBh11ayfawg==', 'SeguePBKDF2v3', null, null, '2019-08-01 12:51:05.940811 +00:00', '2021-01-25 15:11:02.617000 +00:00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (4, 'C58FZjuY6HMDjmIbsCcS4cLQTU5Raee+qMraexPoDZ434RLmP59EJ+Tn0c4QVkMjZMqvZPwLWM4VyumEgJW7kg==', 'NFBFqQ+DwCwUNp6YFq8x6g==', 'SeguePBKDF2v3', null, null, '2019-08-01 12:50:33.329901 +00:00', '2021-01-25 15:11:09.825000 +00:00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (3, 'oyjyh9eOb9g/7oDphjUjZxhIYNVplFTcVbkR6IrUfstgUu3Uai0H+5523IWJy6q0ZEg03TJ9D5yov2EtQ/b+vg==', 'wrf9iczzodG4X9+2buuqiw==', 'SeguePBKDF2v3', null, null, '2019-08-01 12:43:15.133957 +00:00', '2021-01-25 15:11:19.331000 +00:00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (2, '6iUsE3Dm/W83KE+fKWex9kDS4nCsV1sxWVXXAo7toS4WcKf2h6V5RVgQgvAgsBkxSXuQc6CaV2pyOAQq+MtuWg==', 'HaP5yiXzyfxjKotGKPDVQQ==', 'SeguePBKDF2v3', null, null, '2019-08-01 12:40:17.294925 +00:00', '2021-01-25 15:12:43.876000 +00:00');
INSERT INTO public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) VALUES (1, 'Qkq8HUWI3BiMTtIXOLQHrimVHDHibm/Sv+b7l9R+MQTB4QZQZsELGz1sugaUUYEGTz/+s1yOHJA4+3/vtvcRqg==', 'quqt4W6AXeWYnarqPFPJFg==', 'SeguePBKDF2v3', null, null, '2019-08-01 12:28:23.463026 +00:00', '2021-06-17 15:50:44.660000 +00:00');INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (6, 'EMAIL_PREFERENCE', 'ASSIGNMENTS', true, '2021-03-09 16:00:34.563979');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (6, 'EMAIL_PREFERENCE', 'NEWS_AND_UPDATES', true, '2021-03-09 16:00:34.563979');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (6, 'EMAIL_PREFERENCE', 'EVENTS', true, '2021-03-09 16:00:34.563979');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (5, 'EMAIL_PREFERENCE', 'ASSIGNMENTS', true, '2021-03-09 16:44:02.788993');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (5, 'EMAIL_PREFERENCE', 'NEWS_AND_UPDATES', false, '2021-03-09 16:44:02.788993');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (5, 'EMAIL_PREFERENCE', 'EVENTS', false, '2021-03-09 16:44:02.788993');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (4, 'EMAIL_PREFERENCE', 'ASSIGNMENTS', true, '2021-03-09 16:46:26.317778');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (4, 'EMAIL_PREFERENCE', 'NEWS_AND_UPDATES', true, '2021-03-09 16:46:26.317778');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (4, 'EMAIL_PREFERENCE', 'EVENTS', true, '2021-03-09 16:46:26.317778');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (3, 'EMAIL_PREFERENCE', 'ASSIGNMENTS', true, '2021-03-09 16:47:03.822152');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (3, 'EMAIL_PREFERENCE', 'NEWS_AND_UPDATES', true, '2021-03-09 16:47:03.822152');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (3, 'EMAIL_PREFERENCE', 'EVENTS', true, '2021-03-09 16:47:03.822152');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (2, 'EMAIL_PREFERENCE', 'ASSIGNMENTS', true, '2021-03-09 16:47:33.937235');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (2, 'EMAIL_PREFERENCE', 'NEWS_AND_UPDATES', true, '2021-03-09 16:47:33.937235');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (2, 'EMAIL_PREFERENCE', 'EVENTS', true, '2021-03-09 16:47:33.937235');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (5, 'SUBJECT_INTEREST', 'CS_ALEVEL', false, '2021-03-31 10:19:04.978480');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (6, 'SUBJECT_INTEREST', 'CS_ALEVEL', false, '2021-06-17 16:50:13.161176');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (1, 'EMAIL_PREFERENCE', 'ASSIGNMENTS', true, '2021-06-17 16:50:55.404972');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (1, 'EMAIL_PREFERENCE', 'NEWS_AND_UPDATES', false, '2021-06-17 16:50:55.404972');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (1, 'EMAIL_PREFERENCE', 'EVENTS', false, '2021-06-17 16:50:55.404972');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (1, 'SUBJECT_INTEREST', 'CS_ALEVEL', false, '2021-06-17 16:50:55.404972');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (2, 'BOOLEAN_NOTATION', 'ENG', true, '2021-09-03 09:25:06.743172');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (4, 'BOOLEAN_NOTATION', 'ENG', true, '2021-09-03 09:25:06.743172');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (3, 'BOOLEAN_NOTATION', 'ENG', true, '2021-09-03 09:25:06.743172');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (5, 'BOOLEAN_NOTATION', 'ENG', true, '2021-09-03 09:25:06.743172');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (6, 'BOOLEAN_NOTATION', 'MATH', true, '2021-09-03 09:25:06.743172');
INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) VALUES (1, 'BOOLEAN_NOTATION', 'ENG', true, '2021-09-03 09:25:06.743172');


-- FUNCTIONS

CREATE OR REPLACE FUNCTION public.mergeuser(targetuseridtokeep bigint, targetuseridtodelete bigint) RETURNS boolean
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
-- Calculate User Streaks
--
-- Authors: James Sharkey
-- Last Modified: 2018-04-20
--

CREATE OR REPLACE FUNCTION public.user_streaks(useridofinterest BIGINT, defaultquestionsperday INTEGER DEFAULT 3)
  RETURNS TABLE(streaklength BIGINT, startdate DATE, enddate DATE, totaldays BIGINT) AS
$BODY$
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
$BODY$
LANGUAGE plpgsql;

ALTER FUNCTION public.user_streaks(useridofinterest BIGINT, defaultquestionsperday INTEGER) OWNER TO rutherford;


--
-- Calculate Current Progress towards User Streak
--
-- Authors: James Sharkey
-- Last Modified: 2018-04-20
--

CREATE OR REPLACE FUNCTION public.user_streaks_current_progress(useridofinterest BIGINT, defaultquestionsperday INTEGER DEFAULT 3)
  RETURNS TABLE(currentdate DATE, currentprogress BIGINT, targetprogress BIGINT) AS
$BODY$
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
$BODY$
LANGUAGE plpgsql;

ALTER FUNCTION public.user_streaks_current_progress(useridofinterest BIGINT, defaultquestionsperday INTEGER) OWNER TO rutherford;


--
-- Calculate User Weekly Streaks
--
-- Authors: James Sharkey
-- Last Modified: 2019-12-06
--

CREATE OR REPLACE FUNCTION public.user_streaks_weekly(useridofinterest BIGINT, defaultquestionsperweek integer DEFAULT 10)
    RETURNS TABLE(streaklength BIGINT, startdate date, enddate date, totalweeks BIGINT)
    LANGUAGE plpgsql
AS
$$
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

ALTER FUNCTION public.user_streaks_weekly(BIGINT, INTEGER) OWNER TO rutherford;


--
-- Calculate Current Progress towards User Weekly Streak
--
-- Authors: James Sharkey
-- Last Modified: 2019-12-06
--

CREATE OR REPLACE FUNCTION public.user_streaks_weekly_current_progress(useridofinterest BIGINT, defaultquestionsperweek integer DEFAULT 10)
    RETURNS TABLE(currentweek date, currentprogress BIGINT, targetprogress BIGINT)
    LANGUAGE plpgsql
AS
$$
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

ALTER FUNCTION public.user_streaks_weekly_current_progress(BIGINT, INTEGER) OWNER TO rutherford;
