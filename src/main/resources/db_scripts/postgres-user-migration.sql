-- Table: users

-- DROP TABLE users CASCADE;

CREATE TABLE users
(
  id serial NOT NULL,
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
  email_verification_token_expiry timestamp without time zone,
  CONSTRAINT "User Id" PRIMARY KEY (id),
  CONSTRAINT "unique email" UNIQUE (email),
  CONSTRAINT "unique sha id" UNIQUE (_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE users
  OWNER TO rutherford;

-- Table: linked_accounts

-- DROP TABLE linked_accounts;

CREATE TABLE linked_accounts
(
  user_id bigint NOT NULL, -- This is the postgres foreign key for the users table.
  provider character varying(100) NOT NULL,
  provider_user_id text, -- user id from the remote service
  CONSTRAINT "compound key" PRIMARY KEY (user_id, provider),
  CONSTRAINT "local_user_id fkey" FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "provider and user id" UNIQUE (provider, provider_user_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE linked_accounts
  OWNER TO rutherford;
COMMENT ON COLUMN linked_accounts.user_id IS 'This is the postgres foreign key for the users table.';
COMMENT ON COLUMN linked_accounts.provider_user_id IS 'user id from the remote service';

-- Table: logged_events

-- DROP TABLE logged_events;

CREATE TABLE logged_events
(
  id serial NOT NULL,
  user_id character varying(100) NOT NULL,
  anonymous_user boolean NOT NULL,
  event_type character varying(255),
  event_details_type text,
  event_details jsonb,
  ip_address inet,
  "timestamp" timestamp without time zone,
  CONSTRAINT "id pkey" PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE logged_events
  OWNER TO rutherford;
  
-- Table: user_notifications

DROP TABLE user_notifications;

CREATE TABLE user_notifications
(
  user_id integer NOT NULL,
  notification_id text NOT NULL,
  status text,
  created timestamp without time zone NOT NULL,
  CONSTRAINT notification_pkey PRIMARY KEY (user_id, notification_id),
  CONSTRAINT "user_id fkey" FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE user_notifications
  OWNER TO rutherford;

-- Index: "fki_user_id fkey"

-- DROP INDEX "fki_user_id fkey";

CREATE INDEX "fki_user_id fkey"
  ON user_notifications
  USING btree
  (user_id);
 
