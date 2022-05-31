ALTER TABLE public.gameboards
    ADD COLUMN contents jsonb[] DEFAULT array[]::jsonb[] NOT NULL;
