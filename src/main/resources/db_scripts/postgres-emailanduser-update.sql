-- Table: user_email_preferences

-- DROP TABLE user_email_preferences;

CREATE TABLE user_email_preferences
(
  user_id integer NOT NULL,
  email_preference integer NOT NULL,
  email_preference_status boolean,
  CONSTRAINT user_id_email_preference_pk PRIMARY KEY (user_id, email_preference),
  CONSTRAINT user_id_fk FOREIGN KEY (user_id)
      REFERENCES users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE user_email_preferences
  OWNER TO rutherford;

    
UPDATE users SET role = 'STUDENT' WHERE role = '';

ALTER TABLE ONLY users ALTER COLUMN role SET NOT NULL;  
ALTER TABLE ONLY users ALTER COLUMN role SET DEFAULT 'STUDENT';