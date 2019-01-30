-- Add deleted field

-- Column: deleted
-- ALTER TABLE public.users DROP COLUMN deleted;

ALTER TABLE public.users ADD COLUMN deleted boolean NOT NULL DEFAULT false;
