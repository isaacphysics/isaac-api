-- Table: question_attempts

-- DROP TABLE question_attempts;

CREATE TABLE question_attempts
(
  id serial NOT NULL,
  user_id integer NOT NULL,
  question_id text NOT NULL,
  question_attempt jsonb,
  correct boolean,
  "timestamp" timestamp without time zone,
  CONSTRAINT question_attempts_id PRIMARY KEY (id),
  CONSTRAINT user_id_question_attempts_fkey FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE question_attempts
  OWNER TO rutherford;
