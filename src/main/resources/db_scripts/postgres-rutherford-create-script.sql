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
-- Name: id; Type: DEFAULT; Schema: public; Owner: rutherford
--

ALTER TABLE ONLY event_bookings ALTER COLUMN id SET DEFAULT nextval('event_bookings_id_seq'::regclass);


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
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

