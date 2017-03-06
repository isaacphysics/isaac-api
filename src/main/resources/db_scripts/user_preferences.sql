-- Table: public.user_preferences

-- DROP TABLE public.user_preferences;

CREATE TABLE public.user_preferences
(
  user_id integer NOT NULL,
  preference_type character varying(255) NOT NULL,
  preference_name character varying(255) NOT NULL,
  preference_value boolean NOT NULL,
  CONSTRAINT user_id_preference_type_name_pk PRIMARY KEY (user_id, preference_type, preference_name),
  CONSTRAINT user_preference_user_id_fk FOREIGN KEY (user_id)
      REFERENCES public.users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public.user_preferences
  OWNER TO rutherford;
