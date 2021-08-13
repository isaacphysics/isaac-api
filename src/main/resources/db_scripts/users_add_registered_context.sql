ALTER TABLE public.users
    ADD COLUMN registered_contexts jsonb[] DEFAULT array[]::jsonb[] NOT NULL,
    ADD COLUMN registered_contexts_last_confirmed timestamp without time zone;
