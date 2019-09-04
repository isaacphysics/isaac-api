-- Table: public.temporary_user_store

-- DROP TABLE public.temporary_user_store;

CREATE TABLE public.temporary_user_store
(
  id character varying NOT NULL,
  temporary_app_data jsonb,
  created timestamp with time zone NOT NULL DEFAULT now(),
  last_updated timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT "temporary_user_store_pk" PRIMARY KEY (id)
)
  WITH (
    OIDS=FALSE
  );
ALTER TABLE public.temporary_user_store
  OWNER TO rutherford;
