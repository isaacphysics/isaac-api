-- Step 1: Create Table: public.user_credentials
--DROP TABLE public.user_credentials;

CREATE TABLE public.user_credentials
(
  user_id integer NOT NULL,
  password text NOT NULL,
  secure_salt text,
  security_scheme text NOT NULL DEFAULT 'SeguePBKDF2v1'::text,
  reset_token text,
  reset_expiry timestamp with time zone,
  created timestamp with time zone DEFAULT now(),
  last_updated timestamp with time zone DEFAULT now(),
  CONSTRAINT user_id PRIMARY KEY (user_id),
  CONSTRAINT fk_user_id_pswd FOREIGN KEY (user_id)
      REFERENCES public.users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public.user_credentials
  OWNER TO rutherford;

-- Step 2: Move data
TRUNCATE TABLE user_credentials;

INSERT INTO user_credentials (user_id, password, secure_salt, reset_token, reset_expiry)
SELECT users.id, users.password, users.secure_salt, users.reset_token, users.reset_expiry FROM users WHERE users.password IS NOT NULL;

--- Step 3: confirm data move
-- check that the new table looks sensible.

--- Step 4: Remove old fields from user table.
--- Manually remove these later...
