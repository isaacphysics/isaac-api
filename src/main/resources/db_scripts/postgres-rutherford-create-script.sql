--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

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
-- Name: assignments; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE assignments (
    id integer NOT NULL,
    gameboard_id character varying(255) NOT NULL,
    group_id integer NOT NULL,
    owner_user_id integer,
    creation_date timestamp without time zone
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
-- Name: event_bookings; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE event_bookings (
    id integer NOT NULL,
    event_id text NOT NULL,
    created timestamp without time zone NOT NULL,
    user_id integer
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
-- Name: gameboards; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
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
    creation_date timestamp without time zone
);


ALTER TABLE gameboards OWNER TO rutherford;

--
-- Name: group_memberships; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE group_memberships (
    group_id integer NOT NULL,
    user_id integer NOT NULL,
    created timestamp without time zone
);


ALTER TABLE group_memberships OWNER TO rutherford;

--
-- Name: groups; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE groups (
    id integer NOT NULL,
    group_name text,
    owner_id integer,
    created timestamp without time zone
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
-- Name: ip_location_history; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
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
-- Name: linked_accounts; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
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
-- Name: logged_events; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
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
-- Name: question_attempts; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
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
-- Name: user_associations; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE user_associations (
    user_id_granting_permission integer NOT NULL,
    user_id_receiving_permission integer NOT NULL,
    created timestamp without time zone
);


ALTER TABLE user_associations OWNER TO rutherford;

--
-- Name: user_associations_tokens; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE user_associations_tokens (
    token character varying(100) NOT NULL,
    owner_user_id integer,
    group_id integer
);


ALTER TABLE user_associations_tokens OWNER TO rutherford;

--
-- Name: user_email_preferences; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE user_email_preferences (
    user_id integer NOT NULL,
    email_preference integer NOT NULL,
    email_preference_status boolean
);


ALTER TABLE user_email_preferences OWNER TO rutherford;

--
-- Name: user_gameboards; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE user_gameboards (
    user_id integer NOT NULL,
    gameboard_id character varying NOT NULL,
    created timestamp without time zone,
    last_visited timestamp without time zone
);


ALTER TABLE user_gameboards OWNER TO rutherford;

--
-- Name: user_notifications; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE user_notifications (
    user_id integer NOT NULL,
    notification_id text NOT NULL,
    status text,
    created timestamp without time zone NOT NULL
);


ALTER TABLE user_notifications OWNER TO rutherford;

--
-- Name: users; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
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
    school_id bigint,
    school_other text,
    last_updated timestamp without time zone,
    email_verification_status character varying(255),
    last_seen timestamp without time zone,
    default_level integer,
    password text,
    secure_salt text,
    reset_token text,
    reset_expiry timestamp without time zone,
    email_verification_token text,
    email_verification_token_expiry timestamp without time zone
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


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY assignments ALTER COLUMN id SET DEFAULT nextval('assignments_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY event_bookings ALTER COLUMN id SET DEFAULT nextval('event_bookings_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY groups ALTER COLUMN id SET DEFAULT nextval('groups_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY ip_location_history ALTER COLUMN id SET DEFAULT nextval('ip_location_history_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY logged_events ALTER COLUMN id SET DEFAULT nextval('logged_events_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY question_attempts ALTER COLUMN id SET DEFAULT nextval('question_attempts_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY users ALTER COLUMN id SET DEFAULT nextval('users_id_seq'::regclass);


--
-- Name: User Id; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY users
    ADD CONSTRAINT "User Id" PRIMARY KEY (id);


--
-- Name: composite pkey assignments; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT "composite pkey assignments" PRIMARY KEY (gameboard_id, group_id);


--
-- Name: compound key; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "compound key" PRIMARY KEY (user_id, provider);


--
-- Name: eventbooking id pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY event_bookings
    ADD CONSTRAINT "eventbooking id pkey" PRIMARY KEY (id);


--
-- Name: gameboard-id-pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY gameboards
    ADD CONSTRAINT "gameboard-id-pkey" PRIMARY KEY (id);


--
-- Name: group_membership_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY group_memberships
    ADD CONSTRAINT group_membership_pkey PRIMARY KEY (group_id, user_id);


--
-- Name: group_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY groups
    ADD CONSTRAINT group_pkey PRIMARY KEY (id);


--
-- Name: id pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY logged_events
    ADD CONSTRAINT "id pkey" PRIMARY KEY (id);


--
-- Name: id pky; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY ip_location_history
    ADD CONSTRAINT "id pky" PRIMARY KEY (id);


--
-- Name: notification_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY user_notifications
    ADD CONSTRAINT notification_pkey PRIMARY KEY (user_id, notification_id);


--
-- Name: only_one_token_per_user_per_group; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT only_one_token_per_user_per_group UNIQUE (owner_user_id, group_id);


--
-- Name: provider and user id; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "provider and user id" UNIQUE (provider, provider_user_id);


--
-- Name: question_attempts_id; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY question_attempts
    ADD CONSTRAINT question_attempts_id PRIMARY KEY (id);


--
-- Name: token_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT token_pkey PRIMARY KEY (token);


--
-- Name: unique email; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY users
    ADD CONSTRAINT "unique email" UNIQUE (email);


--
-- Name: unique sha id; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY users
    ADD CONSTRAINT "unique sha id" UNIQUE (_id);


--
-- Name: user_associations_composite_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY user_associations
    ADD CONSTRAINT user_associations_composite_pkey PRIMARY KEY (user_id_granting_permission, user_id_receiving_permission);


--
-- Name: user_gameboard_composite_key; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY user_gameboards
    ADD CONSTRAINT user_gameboard_composite_key PRIMARY KEY (user_id, gameboard_id);


--
-- Name: user_id_email_preference_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY user_email_preferences
    ADD CONSTRAINT user_id_email_preference_pk PRIMARY KEY (user_id, email_preference);


--
-- Name: fki_user_id fkey; Type: INDEX; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE INDEX "fki_user_id fkey" ON user_notifications USING btree (user_id);


--
-- Name: log_events_user_id; Type: INDEX; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE INDEX log_events_user_id ON logged_events USING btree (user_id);


--
-- Name: question-attempts-by-user; Type: INDEX; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE INDEX "question-attempts-by-user" ON question_attempts USING btree (user_id);


--
-- Name: assignment_owner_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT assignment_owner_fkey FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: event_bookings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY event_bookings
    ADD CONSTRAINT event_bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: gameboard_assignment_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT gameboard_assignment_fkey FOREIGN KEY (gameboard_id) REFERENCES gameboards(id) ON DELETE CASCADE;


--
-- Name: gameboard_id_fkey_gameboard_link; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_gameboards
    ADD CONSTRAINT gameboard_id_fkey_gameboard_link FOREIGN KEY (gameboard_id) REFERENCES gameboards(id) ON DELETE CASCADE;


--
-- Name: gameboard_user_id_pkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY gameboards
    ADD CONSTRAINT gameboard_user_id_pkey FOREIGN KEY (owner_user_id) REFERENCES users(id);


--
-- Name: group_id_token_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT group_id_token_fkey FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE;


--
-- Name: group_membership_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY group_memberships
    ADD CONSTRAINT group_membership_group_id_fkey FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE;


--
-- Name: group_membership_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY group_memberships
    ADD CONSTRAINT group_membership_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: local_user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "local_user_id fkey" FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: owner_user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY groups
    ADD CONSTRAINT "owner_user_id fkey" FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: token_owner_user_id; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT token_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_granting_permission_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations
    ADD CONSTRAINT user_granting_permission_fkey FOREIGN KEY (user_id_granting_permission) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_notifications
    ADD CONSTRAINT "user_id fkey" FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_email_preferences
    ADD CONSTRAINT user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_id_fkey_gameboard_link; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_gameboards
    ADD CONSTRAINT user_id_fkey_gameboard_link FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_id_question_attempts_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY question_attempts
    ADD CONSTRAINT user_id_question_attempts_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: user_receiving_permissions_key; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations
    ADD CONSTRAINT user_receiving_permissions_key FOREIGN KEY (user_id_receiving_permission) REFERENCES users(id) ON DELETE CASCADE;


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

