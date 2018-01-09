-- Column: archived

-- ALTER TABLE public.groups DROP COLUMN archived;

ALTER TABLE public.groups ADD COLUMN archived boolean DEFAULT false;
ALTER TABLE public.groups ALTER COLUMN archived SET NOT NULL;