-- Column: archived

-- ALTER TABLE public.groups DROP COLUMN archived;

ALTER TABLE public.groups ADD COLUMN archived boolean;
ALTER TABLE public.groups ALTER COLUMN archived SET NOT NULL;
ALTER TABLE public.groups ALTER COLUMN archived SET DEFAULT false;
