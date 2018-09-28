-- Add deleted field

-- Column: deleted
-- ALTER TABLE public.users DROP COLUMN deleted;

ALTER TABLE public.users ADD COLUMN deleted boolean;
ALTER TABLE public.users ALTER COLUMN deleted SET NOT NULL;
ALTER TABLE public.users ALTER COLUMN deleted SET DEFAULT false;
