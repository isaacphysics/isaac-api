-- Step 1 -- Create new tables.

-- Table: gameboards

-- DROP TABLE gameboards;

CREATE TABLE gameboards
(
  id character varying NOT NULL,
  title text,
  questions character varying[],
  wildcard jsonb,
  wildcard_position integer,
  game_filter jsonb,
  owner_user_id integer,
  creation_method character varying,
  creation_date timestamp without time zone,
  CONSTRAINT "gameboard-id-pkey" PRIMARY KEY (id),
  CONSTRAINT gameboard_user_id_pkey FOREIGN KEY (owner_user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE gameboards
  OWNER TO rutherford;

  
-- Table: user_gameboards

-- DROP TABLE user_gameboards;

CREATE TABLE user_gameboards
(
  user_id integer NOT NULL,
  gameboard_id character varying NOT NULL,
  created timestamp without time zone,
  last_visited timestamp without time zone,
  CONSTRAINT user_gameboard_composite_key PRIMARY KEY (user_id, gameboard_id),
  CONSTRAINT gameboard_id_fkey_gameboard_link FOREIGN KEY (gameboard_id)
      REFERENCES gameboards (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT user_id_fkey_gameboard_link FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE user_gameboards
  OWNER TO rutherford;


-- Step 2 - Modify existing tables - create indexes etc.

     
-- Index: "question-attempts-by-user"

-- DROP INDEX "question-attempts-by-user";

CREATE INDEX "question-attempts-by-user"
  ON question_attempts
  USING btree
  (user_id);

  
-- Index: log_events_user_id

-- DROP INDEX log_events_user_id;

CREATE INDEX log_events_user_id
  ON logged_events
  USING btree
  (user_id COLLATE pg_catalog."default");  


-- run this after migration  
-- Foreign Key: gameboard_assignment_fkey

-- ALTER TABLE assignments DROP CONSTRAINT gameboard_assignment_fkey;

--ALTER TABLE assignments
--  ADD CONSTRAINT gameboard_assignment_fkey FOREIGN KEY (gameboard_id)
--      REFERENCES gameboards (id) MATCH SIMPLE
--      ON UPDATE NO ACTION ON DELETE CASCADE;  