-- Column: updated

-- ALTER TABLE public.group_memberships DROP COLUMN updated;

ALTER TABLE public.group_memberships ADD COLUMN updated timestamp with time zone;
ALTER TABLE public.group_memberships ALTER COLUMN updated SET DEFAULT now();

-- Column: status

-- ALTER TABLE public.group_memberships DROP COLUMN status;

ALTER TABLE public.group_memberships ADD COLUMN status text;
ALTER TABLE public.group_memberships ALTER COLUMN status SET DEFAULT 'ACTIVE'::text;


-- Column: status

-- ALTER TABLE public.groups DROP COLUMN status;

ALTER TABLE public.groups ADD COLUMN status text;
ALTER TABLE public.groups ALTER COLUMN status SET DEFAULT 'ACTIVE'::text;