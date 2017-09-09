-- Column: due_date

-- ALTER TABLE public.assignments DROP COLUMN due_date;

ALTER TABLE public.assignments ADD COLUMN due_date timestamp with time zone;

