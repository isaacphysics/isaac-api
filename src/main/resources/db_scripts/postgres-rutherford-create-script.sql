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
-- Name: event_bookings; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE event_bookings (
    id integer NOT NULL,
    user_id text NOT NULL,
    event_id text NOT NULL,
    created timestamp without time zone NOT NULL
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
-- Name: user_notifications; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE user_notifications (
    user_id text NOT NULL,
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
    role character varying(255),
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

ALTER TABLE ONLY event_bookings ALTER COLUMN id SET DEFAULT nextval('event_bookings_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY ip_location_history ALTER COLUMN id SET DEFAULT nextval('ip_location_history_id_seq'::regclass);


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
-- Name: composite key; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY user_notifications
    ADD CONSTRAINT "composite key" PRIMARY KEY (user_id, notification_id);


--
-- Name: compound key; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "compound key" PRIMARY KEY (user_id, provider);


--
-- Name: event userid; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY event_bookings
    ADD CONSTRAINT "event userid" UNIQUE (event_id, user_id);


--
-- Name: eventbooking id pkey; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY event_bookings
    ADD CONSTRAINT "eventbooking id pkey" PRIMARY KEY (id);


--
-- Name: id pky; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY ip_location_history
    ADD CONSTRAINT "id pky" PRIMARY KEY (id);


--
-- Name: provider and user id; Type: CONSTRAINT; Schema: public; Owner: rutherford; Tablespace: 
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "provider and user id" UNIQUE (provider, provider_user_id);


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
-- Name: local_user_id fkey; Type: FK CONSTRAINT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY linked_accounts
    ADD CONSTRAINT "local_user_id fkey" FOREIGN KEY (user_id) REFERENCES users(id);


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

