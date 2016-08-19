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

--
-- Name: mergeuser(integer, integer); Type: FUNCTION; Schema: public; Owner: rutherford
--

CREATE FUNCTION mergeuser(targetuseridtokeep integer, targetuseridtodelete integer) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
BEGIN
	BEGIN
		UPDATE linked_accounts
		SET user_id = targetUserIdToKeep
		WHERE user_id = targetUserIdToDelete;
	EXCEPTION WHEN unique_violation THEN
	    -- Ignore duplicate inserts.
	END;

UPDATE question_attempts
SET user_id = targetUserIdToKeep
WHERE user_id = targetUserIdToDelete;

UPDATE logged_events
SET user_id = targetUserIdToKeep::varchar(255)
WHERE user_id = targetUserIdToDelete::varchar(255);

UPDATE groups
SET owner_id = targetUserIdToKeep
WHERE owner_id = targetUserIdToDelete;

	BEGIN
		UPDATE group_memberships
		SET user_id = targetUserIdToKeep
		WHERE user_id = targetUserIdToDelete;
	EXCEPTION WHEN unique_violation THEN
	    -- Ignore duplicate inserts.
	END;

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

-- Deal with user associations
 
UPDATE gameboards
SET owner_user_id = targetUserIdToKeep
WHERE owner_user_id = targetUserIdToDelete;

	BEGIN
		UPDATE user_gameboards
		SET user_id = targetUserIdToKeep
		WHERE user_id = targetUserIdToDelete;
	EXCEPTION WHEN unique_violation THEN
	    -- Ignore duplicate inserts.
	END;	

-- Deal with user associations

UPDATE user_associations_tokens
SET owner_user_id = targetUserIdToKeep
WHERE owner_user_id = targetUserIdToDelete;

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

 DELETE FROM users
 WHERE id = targetUserIdToDelete;
 
 RETURN true;
END
$$;


ALTER FUNCTION public.mergeuser(targetuseridtokeep integer, targetuseridtodelete integer) OWNER TO rutherford;

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
-- TOC entry 174 (class 1259 OID 16390)
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
-- TOC entry 2157 (class 0 OID 0)
-- Dependencies: 174
-- Name: assignments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE assignments_id_seq OWNED BY assignments.id;


--
-- TOC entry 175 (class 1259 OID 16392)
-- Name: event_bookings; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE event_bookings (
    id integer NOT NULL,
    event_id text NOT NULL,
    created timestamp without time zone NOT NULL,
    user_id integer,
    status text DEFAULT 'CONFIRMED'::text NOT NULL,
    updated timestamp without time zone,
    additional_booking_information jsonb
);


ALTER TABLE event_bookings OWNER TO rutherford;

--
-- TOC entry 176 (class 1259 OID 16398)
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
-- TOC entry 2158 (class 0 OID 0)
-- Dependencies: 176
-- Name: event_bookings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE event_bookings_id_seq OWNED BY event_bookings.id;


--
-- TOC entry 177 (class 1259 OID 16400)
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
    creation_date timestamp without time zone
);


ALTER TABLE gameboards OWNER TO rutherford;

--
-- TOC entry 178 (class 1259 OID 16406)
-- Name: group_memberships; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE group_memberships (
    group_id integer NOT NULL,
    user_id integer NOT NULL,
    created timestamp without time zone
);


ALTER TABLE group_memberships OWNER TO rutherford;

--
-- TOC entry 179 (class 1259 OID 16409)
-- Name: groups; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE groups (
    id integer NOT NULL,
    group_name text,
    owner_id integer,
    created timestamp without time zone
);


ALTER TABLE groups OWNER TO rutherford;

--
-- TOC entry 180 (class 1259 OID 16415)
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
-- TOC entry 2159 (class 0 OID 0)
-- Dependencies: 180
-- Name: groups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE groups_id_seq OWNED BY groups.id;


--
-- TOC entry 181 (class 1259 OID 16417)
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
-- TOC entry 182 (class 1259 OID 16424)
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
-- TOC entry 2160 (class 0 OID 0)
-- Dependencies: 182
-- Name: ip_location_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE ip_location_history_id_seq OWNED BY ip_location_history.id;


--
-- TOC entry 183 (class 1259 OID 16426)
-- Name: linked_accounts; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE linked_accounts (
    user_id bigint NOT NULL,
    provider character varying(100) NOT NULL,
    provider_user_id text
);


ALTER TABLE linked_accounts OWNER TO rutherford;

--
-- TOC entry 2161 (class 0 OID 0)
-- Dependencies: 183
-- Name: COLUMN linked_accounts.user_id; Type: COMMENT; Schema: public; Owner: rutherford
--

COMMENT ON COLUMN linked_accounts.user_id IS 'This is the postgres foreign key for the users table.';


--
-- TOC entry 2162 (class 0 OID 0)
-- Dependencies: 183
-- Name: COLUMN linked_accounts.provider_user_id; Type: COMMENT; Schema: public; Owner: rutherford
--

COMMENT ON COLUMN linked_accounts.provider_user_id IS 'user id from the remote service';


--
-- TOC entry 184 (class 1259 OID 16432)
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
-- TOC entry 185 (class 1259 OID 16438)
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
-- TOC entry 2163 (class 0 OID 0)
-- Dependencies: 185
-- Name: logged_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE logged_events_id_seq OWNED BY logged_events.id;


--
-- TOC entry 186 (class 1259 OID 16440)
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
-- TOC entry 187 (class 1259 OID 16446)
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
-- TOC entry 2164 (class 0 OID 0)
-- Dependencies: 187
-- Name: question_attempts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE question_attempts_id_seq OWNED BY question_attempts.id;


--
-- TOC entry 188 (class 1259 OID 16448)
-- Name: uk_post_codes; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE uk_post_codes (
    postcode character varying(255) NOT NULL,
    lat numeric NOT NULL,
    lon numeric NOT NULL
);


ALTER TABLE uk_post_codes OWNER TO rutherford;

--
-- TOC entry 189 (class 1259 OID 16454)
-- Name: user_associations; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_associations (
    user_id_granting_permission integer NOT NULL,
    user_id_receiving_permission integer NOT NULL,
    created timestamp without time zone
);


ALTER TABLE user_associations OWNER TO rutherford;

--
-- TOC entry 190 (class 1259 OID 16457)
-- Name: user_associations_tokens; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_associations_tokens (
    token character varying(100) NOT NULL,
    owner_user_id integer,
    group_id integer
);


ALTER TABLE user_associations_tokens OWNER TO rutherford;

--
-- TOC entry 191 (class 1259 OID 16460)
-- Name: user_email_preferences; Type: TABLE; Schema: public; Owner: rutherford
--

CREATE TABLE user_email_preferences (
    user_id integer NOT NULL,
    email_preference integer NOT NULL,
    email_preference_status boolean
);


ALTER TABLE user_email_preferences OWNER TO rutherford;

--
-- TOC entry 192 (class 1259 OID 16463)
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
-- TOC entry 193 (class 1259 OID 16469)
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
-- TOC entry 194 (class 1259 OID 16475)
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
    email_verification_token text
);


ALTER TABLE users OWNER TO rutherford;

--
-- TOC entry 195 (class 1259 OID 16482)
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
-- TOC entry 2165 (class 0 OID 0)
-- Dependencies: 195
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rutherford
--

ALTER SEQUENCE users_id_seq OWNED BY users.id;


--
-- TOC entry 1969 (class 2604 OID 16484)
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY assignments ALTER COLUMN id SET DEFAULT nextval('assignments_id_seq'::regclass);


--
-- TOC entry 1970 (class 2604 OID 16485)
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY event_bookings ALTER COLUMN id SET DEFAULT nextval('event_bookings_id_seq'::regclass);


--
-- TOC entry 1972 (class 2604 OID 16486)
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY groups ALTER COLUMN id SET DEFAULT nextval('groups_id_seq'::regclass);


--
-- TOC entry 1974 (class 2604 OID 16487)
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY ip_location_history ALTER COLUMN id SET DEFAULT nextval('ip_location_history_id_seq'::regclass);


--
-- TOC entry 1975 (class 2604 OID 16488)
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY logged_events ALTER COLUMN id SET DEFAULT nextval('logged_events_id_seq'::regclass);


--
-- TOC entry 1976 (class 2604 OID 16489)
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY question_attempts ALTER COLUMN id SET DEFAULT nextval('question_attempts_id_seq'::regclass);


--
-- TOC entry 1978 (class 2604 OID 16490)
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY users ALTER COLUMN id SET DEFAULT nextval('users_id_seq'::regclass);


--
-- TOC entry 2018 (class 2606 OID 16492)
-- Name: User Id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY users
    ADD CONSTRAINT "User Id" PRIMARY KEY (id);


--
-- TOC entry 1980 (class 2606 OID 16494)
-- Name: composite pkey assignments; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT "composite pkey assignments" PRIMARY KEY (gameboard_id, group_id);


--
-- TOC entry 1993 (class 2606 OID 16496)
-- Name: compound key; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "compound key" PRIMARY KEY (user_id, provider);


--
-- TOC entry 1983 (class 2606 OID 16498)
-- Name: eventbooking id pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY event_bookings
    ADD CONSTRAINT "eventbooking id pkey" PRIMARY KEY (id);


--
-- TOC entry 1985 (class 2606 OID 16500)
-- Name: gameboard-id-pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY gameboards
    ADD CONSTRAINT "gameboard-id-pkey" PRIMARY KEY (id);


--
-- TOC entry 1987 (class 2606 OID 16502)
-- Name: group_membership_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY group_memberships
    ADD CONSTRAINT group_membership_pkey PRIMARY KEY (group_id, user_id);


--
-- TOC entry 1989 (class 2606 OID 16504)
-- Name: group_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY groups
    ADD CONSTRAINT group_pkey PRIMARY KEY (id);


--
-- TOC entry 1997 (class 2606 OID 16506)
-- Name: id pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY logged_events
    ADD CONSTRAINT "id pkey" PRIMARY KEY (id);


--
-- TOC entry 1991 (class 2606 OID 16508)
-- Name: id pky; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY ip_location_history
    ADD CONSTRAINT "id pky" PRIMARY KEY (id);


--
-- TOC entry 2016 (class 2606 OID 16510)
-- Name: notification_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_notifications
    ADD CONSTRAINT notification_pkey PRIMARY KEY (user_id, notification_id);


--
-- TOC entry 2007 (class 2606 OID 16512)
-- Name: only_one_token_per_user_per_group; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT only_one_token_per_user_per_group UNIQUE (owner_user_id, group_id);


--
-- TOC entry 1995 (class 2606 OID 16514)
-- Name: provider and user id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "provider and user id" UNIQUE (provider, provider_user_id);


--
-- TOC entry 2001 (class 2606 OID 16516)
-- Name: question_attempts_id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY question_attempts
    ADD CONSTRAINT question_attempts_id PRIMARY KEY (id);


--
-- TOC entry 2009 (class 2606 OID 16518)
-- Name: token_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT token_pkey PRIMARY KEY (token);


--
-- TOC entry 2003 (class 2606 OID 16520)
-- Name: uk_post_codes_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY uk_post_codes
    ADD CONSTRAINT uk_post_codes_pk PRIMARY KEY (postcode);


--
-- TOC entry 2021 (class 2606 OID 16522)
-- Name: unique sha id; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY users
    ADD CONSTRAINT "unique sha id" UNIQUE (_id);


--
-- TOC entry 2005 (class 2606 OID 16524)
-- Name: user_associations_composite_pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations
    ADD CONSTRAINT user_associations_composite_pkey PRIMARY KEY (user_id_granting_permission, user_id_receiving_permission);


--
-- TOC entry 2013 (class 2606 OID 16526)
-- Name: user_gameboard_composite_key; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_gameboards
    ADD CONSTRAINT user_gameboard_composite_key PRIMARY KEY (user_id, gameboard_id);


--
-- TOC entry 2011 (class 2606 OID 16528)
-- Name: user_id_email_preference_pk; Type: CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_email_preferences
    ADD CONSTRAINT user_id_email_preference_pk PRIMARY KEY (user_id, email_preference);


--
-- TOC entry 1981 (class 1259 OID 16628)
-- Name: event_booking_user_event_id_index; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX event_booking_user_event_id_index ON event_bookings USING btree (event_id, user_id);


--
-- TOC entry 2014 (class 1259 OID 16529)
-- Name: fki_user_id fkey; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX "fki_user_id fkey" ON user_notifications USING btree (user_id);


--
-- TOC entry 1998 (class 1259 OID 16530)
-- Name: log_events_user_id; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX log_events_user_id ON logged_events USING btree (user_id);


--
-- TOC entry 1999 (class 1259 OID 16531)
-- Name: question-attempts-by-user; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE INDEX "question-attempts-by-user" ON question_attempts USING btree (user_id);


--
-- TOC entry 2019 (class 1259 OID 16532)
-- Name: unique email case insensitive; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX "unique email case insensitive" ON users USING btree (lower(email));


--
-- TOC entry 2022 (class 1259 OID 16533)
-- Name: user_email; Type: INDEX; Schema: public; Owner: rutherford
--

CREATE UNIQUE INDEX user_email ON users USING btree (email);


--
-- TOC entry 2023 (class 2606 OID 16534)
-- Name: assignment_owner_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT assignment_owner_fkey FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2025 (class 2606 OID 16539)
-- Name: event_bookings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY event_bookings
    ADD CONSTRAINT event_bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2024 (class 2606 OID 16544)
-- Name: gameboard_assignment_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT gameboard_assignment_fkey FOREIGN KEY (gameboard_id) REFERENCES gameboards(id) ON DELETE CASCADE;


--
-- TOC entry 2037 (class 2606 OID 16549)
-- Name: gameboard_id_fkey_gameboard_link; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_gameboards
    ADD CONSTRAINT gameboard_id_fkey_gameboard_link FOREIGN KEY (gameboard_id) REFERENCES gameboards(id) ON DELETE CASCADE;


--
-- TOC entry 2026 (class 2606 OID 16554)
-- Name: gameboard_user_id_pkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY gameboards
    ADD CONSTRAINT gameboard_user_id_pkey FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE SET NULL;


--
-- TOC entry 2034 (class 2606 OID 16559)
-- Name: group_id_token_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT group_id_token_fkey FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE;


--
-- TOC entry 2027 (class 2606 OID 16564)
-- Name: group_membership_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY group_memberships
    ADD CONSTRAINT group_membership_group_id_fkey FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE;


--
-- TOC entry 2028 (class 2606 OID 16569)
-- Name: group_membership_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY group_memberships
    ADD CONSTRAINT group_membership_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2030 (class 2606 OID 16574)
-- Name: local_user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "local_user_id fkey" FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2029 (class 2606 OID 16579)
-- Name: owner_user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY groups
    ADD CONSTRAINT "owner_user_id fkey" FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2035 (class 2606 OID 16584)
-- Name: token_owner_user_id; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations_tokens
    ADD CONSTRAINT token_owner_user_id FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2032 (class 2606 OID 16589)
-- Name: user_granting_permission_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations
    ADD CONSTRAINT user_granting_permission_fkey FOREIGN KEY (user_id_granting_permission) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2039 (class 2606 OID 16594)
-- Name: user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_notifications
    ADD CONSTRAINT "user_id fkey" FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2036 (class 2606 OID 16599)
-- Name: user_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_email_preferences
    ADD CONSTRAINT user_id_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2038 (class 2606 OID 16604)
-- Name: user_id_fkey_gameboard_link; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_gameboards
    ADD CONSTRAINT user_id_fkey_gameboard_link FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2031 (class 2606 OID 16609)
-- Name: user_id_question_attempts_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY question_attempts
    ADD CONSTRAINT user_id_question_attempts_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2033 (class 2606 OID 16614)
-- Name: user_receiving_permissions_key; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY user_associations
    ADD CONSTRAINT user_receiving_permissions_key FOREIGN KEY (user_id_receiving_permission) REFERENCES users(id) ON DELETE CASCADE;


--
-- TOC entry 2155 (class 0 OID 0)
-- Dependencies: 7
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


-- Completed on 2016-08-19 17:14:16

--
-- PostgreSQL database dump complete
--

