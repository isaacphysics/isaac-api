-- Table: public.additional_group_managers

-- DROP TABLE public.additional_group_managers;

CREATE TABLE public.additional_group_managers
(
  user_id integer NOT NULL,
  group_id integer NOT NULL,
  created timestamp with time zone DEFAULT now(),
  CONSTRAINT ck_user_group_manager PRIMARY KEY (user_id, group_id),
  CONSTRAINT fk_group_id FOREIGN KEY (group_id)
      REFERENCES public.groups (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT fk_user_manager_id FOREIGN KEY (user_id)
      REFERENCES public.users (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE public.additional_group_managers
  OWNER TO rutherford;
