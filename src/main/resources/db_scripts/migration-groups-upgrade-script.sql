-- Step 1: Create new tables for data to be migrated into 
-- Table: groups

-- DROP TABLE groups;

CREATE TABLE groups
(
  id serial NOT NULL,
  group_name text,
  owner_id integer,
  created timestamp without time zone,
  CONSTRAINT group_pkey PRIMARY KEY (id),
  CONSTRAINT "owner_user_id fkey" FOREIGN KEY (owner_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE groups
  OWNER TO rutherford;

-- Table: assignments

-- DROP TABLE assignments;
  
CREATE TABLE assignments
(
  id serial NOT NULL,
  gameboard_id character varying(255) NOT NULL,
  group_id integer NOT NULL,
  owner_user_id integer,
  creation_date timestamp without time zone,
  CONSTRAINT "composite pkey assignments" PRIMARY KEY (gameboard_id, group_id),
  CONSTRAINT assignment_owner_fkey FOREIGN KEY (owner_user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE assignments
  OWNER TO rutherford;

-- Table: group_memberships

-- DROP TABLE group_memberships;

CREATE TABLE group_memberships
(
  group_id integer NOT NULL,
  user_id integer NOT NULL,
  created timestamp without time zone,
  CONSTRAINT group_membership_pkey PRIMARY KEY (group_id, user_id),
  CONSTRAINT group_membership_group_id_fkey FOREIGN KEY (group_id)
      REFERENCES groups (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT group_membership_user_id_fkey FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE group_memberships
  OWNER TO rutherford;

  
-- Table: user_associations

-- DROP TABLE user_associations;

CREATE TABLE user_associations
(
  user_id_granting_permission integer NOT NULL,
  user_id_receiving_permission integer NOT NULL,
  created timestamp without time zone,
  CONSTRAINT user_associations_composite_pkey PRIMARY KEY (user_id_granting_permission, user_id_receiving_permission),
  CONSTRAINT user_granting_permission_fkey FOREIGN KEY (user_id_granting_permission)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT user_receiving_permissions_key FOREIGN KEY (user_id_receiving_permission)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE user_associations
  OWNER TO rutherford;
  
-- Table: user_associations_tokens

-- DROP TABLE user_associations_tokens;

CREATE TABLE user_associations_tokens
(
  token character varying(100) NOT NULL,
  owner_user_id integer,
  group_id integer,
  CONSTRAINT token_pkey PRIMARY KEY (token),
  CONSTRAINT group_id_token_fkey FOREIGN KEY (group_id)
      REFERENCES groups (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT token_owner_user_id FOREIGN KEY (owner_user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT only_one_token_per_user_per_group UNIQUE (owner_user_id, group_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE user_associations_tokens
  OWNER TO rutherford;



-- Step 2: Modify user notification table to use new user id

ALTER TABLE event_bookings RENAME COLUMN user_id TO legacy_user_id;
ALTER TABLE event_bookings ADD COLUMN user_id integer;

UPDATE event_bookings
   SET user_id = users.id FROM users WHERE users._id = event_bookings.legacy_user_id;

ALTER TABLE event_bookings DROP COLUMN legacy_user_id;

ALTER TABLE event_bookings
  ADD CONSTRAINT event_bookings_user_id_fkey FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;


-- Step 3: Modify existing tables to make sure cascade constraints apply
 
ALTER TABLE linked_accounts DROP CONSTRAINT "local_user_id fkey";

ALTER TABLE linked_accounts
  ADD CONSTRAINT "local_user_id fkey" FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;
      
ALTER TABLE question_attempts DROP CONSTRAINT user_id_question_attempts_fkey;

ALTER TABLE question_attempts
  ADD CONSTRAINT user_id_question_attempts_fkey FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;
      
ALTER TABLE user_email_preferences DROP CONSTRAINT user_id_fk;

ALTER TABLE user_email_preferences
  ADD CONSTRAINT user_id_fk FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;
      
ALTER TABLE user_notifications DROP CONSTRAINT "user_id fkey";

ALTER TABLE user_notifications
  ADD CONSTRAINT "user_id fkey" FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE;