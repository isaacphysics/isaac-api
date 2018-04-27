--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.2
-- Dumped by pg_dump version 9.6.2

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
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


SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: additional_group_managers; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE additional_group_managers (
    user_id integer NOT NULL,
    group_id integer NOT NULL,
    created timestamp with time zone DEFAULT now()
);


ALTER TABLE additional_group_managers OWNER TO rutherford;

--
-- Name: assignments; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE assignments (
    id integer NOT NULL,
    gameboard_id character varying(255) NOT NULL,
    group_id integer NOT NULL,
    owner_user_id integer,
    creation_date timestamp without time zone,
    due_date timestamp with time zone
);


ALTER TABLE assignments OWNER TO rutherford;

--
-- Name: assignments_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE assignments_id_seq OWNER TO rutherford;

--
-- Name: assignments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE assignments_id_seq OWNED BY assignments.id;


--
-- Name: event_bookings; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE event_bookings (
    id integer NOT NULL,
    event_id text NOT NULL,
    created timestamp without time zone NOT NULL,
    user_id integer NOT NULL,
    status text DEFAULT 'CONFIRMED'::text NOT NULL,
    updated timestamp without time zone,
    additional_booking_information jsonb
);


ALTER TABLE event_bookings OWNER TO rutherford;

--
-- Name: event_bookings_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE event_bookings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE event_bookings_id_seq OWNER TO rutherford;

--
-- Name: event_bookings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE event_bookings_id_seq OWNED BY event_bookings.id;


--
-- Name: gameboards; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE gameboards (
    id character varying NOT NULL,
    title text,
    questions character varying[],
    wildcard jsonb,
    wildcard_position integer,
    game_filter jsonb,
    owner_user_id integer,
    creation_method character varying,
    creation_date timestamp without time zone,
    tags jsonb DEFAULT '[]'::jsonb NOT NULL
);


ALTER TABLE gameboards OWNER TO rutherford;

--
-- Name: group_memberships; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE group_memberships (
    group_id integer NOT NULL,
    user_id integer NOT NULL,
    created timestamp without time zone
);


ALTER TABLE group_memberships OWNER TO rutherford;

--
-- Name: groups; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE groups (
    id integer NOT NULL,
    group_name text,
    owner_id integer,
    created timestamp without time zone,
    archived boolean DEFAULT false NOT NULL
);


ALTER TABLE groups OWNER TO rutherford;

--
-- Name: groups_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE groups_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE groups_id_seq OWNER TO rutherford;

--
-- Name: groups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE groups_id_seq OWNED BY groups.id;


--
-- Name: ip_location_history; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE ip_location_history (
    id integer NOT NULL,
    ip_address text NOT NULL,
    location_information jsonb,
    created timestamp without time zone,
    last_lookup timestamp without time zone,
    is_current boolean DEFAULT true
);


ALTER TABLE ip_location_history OWNER TO rutherford;

--
-- Name: ip_location_history_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE ip_location_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE ip_location_history_id_seq OWNER TO rutherford;

--
-- Name: ip_location_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE ip_location_history_id_seq OWNED BY ip_location_history.id;


--
-- Name: linked_accounts; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE linked_accounts (
    user_id bigint NOT NULL,
    provider character varying(100) NOT NULL,
    provider_user_id text
);


ALTER TABLE linked_accounts OWNER TO rutherford;

--
-- Name: COLUMN linked_accounts.user_id; Type: COMMENT; Schema: public; Owner: rutherford
--

COMMENT ON COLUMN linked_accounts.user_id IS 'This is the postgres foreign key for the users table.';


--
-- Name: COLUMN linked_accounts.provider_user_id; Type: COMMENT; Schema: public; Owner: rutherford
--

COMMENT ON COLUMN linked_accounts.provider_user_id IS 'user id from the remote service';


--
-- Name: logged_events; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE logged_events (
    id integer NOT NULL,
    user_id character varying(100) NOT NULL,
    anonymous_user boolean NOT NULL,
    event_type character varying(255),
    event_details_type text,
    event_details jsonb,
    ip_address inet,
    "timestamp" timestamp without time zone
);


ALTER TABLE logged_events OWNER TO rutherford;

--
-- Name: logged_events_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE logged_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE logged_events_id_seq OWNER TO rutherford;

--
-- Name: logged_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE logged_events_id_seq OWNED BY logged_events.id;


--
-- Name: question_attempts; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE question_attempts (
    id integer NOT NULL,
    user_id integer NOT NULL,
    question_id text NOT NULL,
    question_attempt jsonb,
    correct boolean,
    "timestamp" timestamp without time zone
);


ALTER TABLE question_attempts OWNER TO rutherford;

--
-- Name: question_attempts_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE question_attempts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE question_attempts_id_seq OWNER TO rutherford;

--
-- Name: question_attempts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE question_attempts_id_seq OWNED BY question_attempts.id;


--
-- Name: uk_post_codes; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE uk_post_codes (
    postcode character varying(255) NOT NULL,
    lat numeric NOT NULL,
    lon numeric NOT NULL
);


ALTER TABLE uk_post_codes OWNER TO rutherford;

--
-- Name: user_associations; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_associations (
    user_id_granting_permission integer NOT NULL,
    user_id_receiving_permission integer NOT NULL,
    created timestamp without time zone
);


ALTER TABLE user_associations OWNER TO rutherford;

--
-- Name: user_associations_tokens; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_associations_tokens (
    token character varying(100) NOT NULL,
    owner_user_id integer,
    group_id integer
);


ALTER TABLE user_associations_tokens OWNER TO rutherford;


--
-- Name: user_credentials; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_credentials (
    user_id integer NOT NULL,
    password text NOT NULL,
    secure_salt text,
    security_scheme text DEFAULT 'SeguePBKDF2v1'::text NOT NULL,
    reset_token text,
    reset_expiry timestamp with time zone,
    created timestamp with time zone DEFAULT now(),
    last_updated timestamp with time zone DEFAULT now()
);


ALTER TABLE user_credentials OWNER TO rutherford;

--
-- Name: user_email_preferences; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_email_preferences (
    user_id integer NOT NULL,
    email_preference integer NOT NULL,
    email_preference_status boolean
);


ALTER TABLE user_email_preferences OWNER TO rutherford;

--
-- Name: user_gameboards; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_gameboards (
    user_id integer NOT NULL,
    gameboard_id character varying NOT NULL,
    created timestamp without time zone,
    last_visited timestamp without time zone
);


ALTER TABLE user_gameboards OWNER TO rutherford;

--
-- Name: user_notifications; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_notifications (
    user_id integer NOT NULL,
    notification_id text NOT NULL,
    status text,
    created timestamp without time zone NOT NULL
);


ALTER TABLE user_notifications OWNER TO rutherford;

--
-- Name: user_preferences; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_preferences (
    user_id integer NOT NULL,
    preference_type character varying(255) NOT NULL,
    preference_name character varying(255) NOT NULL,
    preference_value boolean NOT NULL
);


ALTER TABLE user_preferences OWNER TO rutherford;

--
-- Name: user_streak_freezes; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_streak_freezes (
    user_id bigint NOT NULL,
    start_date date NOT NULL,
    end_date date,
    comment text
);


ALTER TABLE user_streak_freezes OWNER TO rutherford;

--
-- Name: user_streak_targets; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_streak_targets (
    user_id bigint NOT NULL,
    target_count integer NOT NULL,
    start_date date NOT NULL,
    end_date date,
    comment text
);


ALTER TABLE user_streak_targets OWNER TO rutherford;

--
-- Name: users; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE users (
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
    last_updated timestamp without time zone,
    email_verification_status character varying(255),
    last_seen timestamp without time zone,
    default_level integer,
    email_verification_token text
);


ALTER TABLE users OWNER TO rutherford;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: rutherford
--

CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE users_id_seq OWNER TO rutherford;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE users_id_seq OWNED BY users.id;


CREATE SEQUENCE user_alerts_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1;


CREATE TABLE user_alerts
(
    id INTEGER DEFAULT nextval('user_alerts_id_seq'::regclass) PRIMARY KEY NOT NULL,
    user_id INTEGER NOT NULL,
    message TEXT,
    link TEXT,
    created TIMESTAMP DEFAULT now() NOT NULL,
    seen TIMESTAMP,
    clicked TIMESTAMP,
    dismissed TIMESTAMP
);
CREATE UNIQUE INDEX user_alerts_id_uindex ON user_alerts (id);



--
-- Name: assignments id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY assignments ALTER COLUMN id SET DEFAULT nextval('assignments_id_seq'::regclass);


--
-- Name: event_bookings id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY event_bookings ALTER COLUMN id SET DEFAULT nextval('event_bookings_id_seq'::regclass);


--
-- Name: groups id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY groups ALTER COLUMN id SET DEFAULT nextval('groups_id_seq'::regclass);


--
-- Name: ip_location_history id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY ip_location_history ALTER COLUMN id SET DEFAULT nextval('ip_location_history_id_seq'::regclass);


--
-- Name: logged_events id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY logged_events ALTER COLUMN id SET DEFAULT nextval('logged_events_id_seq'::regclass);


--
-- Name: question_attempts id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY question_attempts ALTER COLUMN id SET DEFAULT nextval('question_attempts_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY users ALTER COLUMN id SET DEFAULT nextval('users_id_seq'::regclass);


--
-- Name: users User Id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY users
    ADD CONSTRAINT "User Id" PRIMARY KEY (id);

--
-- Name: additional_group_managers ck_user_group_manager; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY additional_group_managers
    ADD CONSTRAINT ck_user_group_manager PRIMARY KEY (user_id, group_id);

--
-- Name: assignments composite pkey assignments; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT "composite pkey assignments" PRIMARY KEY (gameboard_id, group_id);


--
-- Name: linked_accounts compound key; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "compound key" PRIMARY KEY (user_id, provider);


--
-- Name: event_bookings eventbooking id pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY event_bookings
    ADD CONSTRAINT "eventbooking id pkey" PRIMARY KEY (id);


--
-- Name: gameboards gameboard-id-pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY gameboards
    ADD CONSTRAINT "gameboard-id-pkey" PRIMARY KEY (id);


--
-- Name: group_memberships group_membership_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY group_memberships
    ADD CONSTRAINT group_membership_pkey PRIMARY KEY (group_id, user_id);


--
-- Name: groups group_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY groups
    ADD CONSTRAINT group_pkey PRIMARY KEY (id);


--
-- Name: logged_events id pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY logged_events
    ADD CONSTRAINT "id pkey" PRIMARY KEY (id);


--
-- Name: ip_location_history id pky; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY ip_location_history
    ADD CONSTRAINT "id pky" PRIMARY KEY (id);


--
-- Name: user_notifications notification_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_notifications
    ADD CONSTRAINT notification_pkey PRIMARY KEY (user_id, notification_id);


--
-- Name: user_associations_tokens only_one_token_per_user_per_group; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT only_one_token_per_user_per_group UNIQUE (owner_user_id, group_id);


--
-- Name: linked_accounts provider and user id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "provider and user id" UNIQUE (provider, provider_user_id);


--
-- Name: question_attempts question_attempts_id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY question_attempts
    ADD CONSTRAINT question_attempts_id PRIMARY KEY (id);


--
-- Name: user_associations_tokens token_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT token_pkey PRIMARY KEY (token);


--
-- Name: uk_post_codes uk_post_codes_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY uk_post_codes
    ADD CONSTRAINT uk_post_codes_pk PRIMARY KEY (postcode);


--
-- Name: users unique sha id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY users
    ADD CONSTRAINT "unique sha id" UNIQUE (_id);


--
-- Name: user_associations user_associations_composite_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations
    ADD CONSTRAINT user_associations_composite_pkey PRIMARY KEY (user_id_granting_permission, user_id_receiving_permission);


--
-- Name: user_gameboards user_gameboard_composite_key; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_gameboards
    ADD CONSTRAINT user_gameboard_composite_key PRIMARY KEY (user_id, gameboard_id);


--
-- Name: user_credentials user_id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_credentials
    ADD CONSTRAINT user_id PRIMARY KEY (user_id);


--
-- Name: user_email_preferences user_id_email_preference_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_email_preferences
    ADD CONSTRAINT user_id_email_preference_pk PRIMARY KEY (user_id, email_preference);


--
-- Name: user_preferences user_id_preference_type_name_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_preferences
    ADD CONSTRAINT user_id_preference_type_name_pk PRIMARY KEY (user_id, preference_type, preference_name);


--
-- Name: user_streak_freezes user_streak_freeze_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_streak_freezes
    ADD CONSTRAINT user_streak_freeze_pkey PRIMARY KEY (user_id, start_date);


--
-- Name: user_streak_targets user_streak_targets_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_streak_targets
    ADD CONSTRAINT user_streak_targets_pkey PRIMARY KEY (user_id, start_date);


--
-- Name: event_booking_user_event_id_index; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX event_booking_user_event_id_index ON event_bookings USING btree (event_id, user_id);


--
-- Name: fki_user_id fkey; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX "fki_user_id fkey" ON user_notifications USING btree (user_id);


--
-- Name: gameboards_tags_gin_index; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX gameboards_tags_gin_index ON gameboards USING gin (tags);


--
-- Name: log_events_timestamp; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX log_events_timestamp ON logged_events USING btree ("timestamp");


--
-- Name: log_events_type; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX log_events_type ON logged_events USING btree (event_type);


--
-- Name: log_events_user_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX log_events_user_id ON logged_events USING btree (user_id);


--
-- Name: logged_events_type_timestamp; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX logged_events_type_timestamp ON logged_events USING btree (event_type, "timestamp");


--
-- Name: question-attempts-by-user; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX "question-attempts-by-user" ON question_attempts USING btree (user_id);


--
-- Name: question_attempts_by_question; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX question_attempts_by_question ON question_attempts USING btree (question_id);


--
-- Name: question_attempts_by_timestamp; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX question_attempts_by_timestamp ON question_attempts USING btree ("timestamp");


--
-- Name: unique email case insensitive; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX "unique email case insensitive" ON users USING btree (lower(email));


--
-- Name: user_email; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX user_email ON users USING btree (email);


--
-- Name: user_streak_freezes_by_user_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX user_streak_freezes_by_user_id ON user_streak_freezes USING btree (user_id);


--
-- Name: user_streak_targets_by_user_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX user_streak_targets_by_user_id ON user_streak_targets USING btree (user_id);


--
-- Name: users_id_role; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX users_id_role ON users USING btree (id, role);


--
-- Name: assignments assignment_owner_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT assignment_owner_fkey FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: event_bookings event_bookings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY event_bookings
    ADD CONSTRAINT event_bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: additional_group_managers fk_group_id; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY additional_group_managers
    ADD CONSTRAINT fk_group_id FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE


--
-- Name: user_credentials fk_user_id_pswd; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_credentials
    ADD CONSTRAINT fk_user_id_pswd FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: additional_group_managers fk_user_manager_id; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY additional_group_managers
    ADD CONSTRAINT fk_user_manager_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: assignments gameboard_assignment_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT gameboard_assignment_fkey FOREIGN KEY (gameboard_id) REFERENCES gameboards(id) ON DELETE CASCADE;


--
-- Name: user_gameboards gameboard_id_fkey_gameboard_link; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_gameboards
    ADD CONSTRAINT gameboard_id_fkey_gameboard_link FOREIGN KEY (gameboard_id) REFERENCES gameboards(id) ON DELETE CASCADE;


--
-- Name: gameboards gameboard_user_id_pkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY gameboards
    ADD CONSTRAINT gameboard_user_id_pkey FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE SET NULL;


--
-- Name: user_associations_tokens group_id_token_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT group_id_token_fkey FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE;


--
-- Name: group_memberships group_membership_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY group_memberships
    ADD CONSTRAINT group_membership_group_id_fkey FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE;


--
-- Name: group_memberships group_membership_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY group_memberships
    ADD CONSTRAINT group_membership_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: linked_accounts local_user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "local_user_id fkey" FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: groups owner_user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY groups
    ADD CONSTRAINT "owner_user_id fkey" FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_associations_tokens token_owner_user_id; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT token_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_associations user_granting_permission_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations
    ADD CONSTRAINT user_granting_permission_fkey FOREIGN KEY (user_id_granting_permission) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_notifications user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_notifications
    ADD CONSTRAINT "user_id fkey" FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_email_preferences user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_email_preferences
    ADD CONSTRAINT user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_gameboards user_id_fkey_gameboard_link; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_gameboards
    ADD CONSTRAINT user_id_fkey_gameboard_link FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: question_attempts user_id_question_attempts_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY question_attempts
    ADD CONSTRAINT user_id_question_attempts_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_preferences user_preference_user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_preferences
    ADD CONSTRAINT user_preference_user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_associations user_receiving_permissions_key; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations
    ADD CONSTRAINT user_receiving_permissions_key FOREIGN KEY (user_id_receiving_permission) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_streak_freezes user_streak_freezes_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_streak_freezes
    ADD CONSTRAINT user_streak_freezes_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_streak_targets user_streak_targets_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_streak_targets
    ADD CONSTRAINT user_streak_targets_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--
