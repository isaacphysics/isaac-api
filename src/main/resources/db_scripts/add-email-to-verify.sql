-- Column: email_to_verify

-- ALTER TABLE public.users DROP COLUMN email_to_verify;

ALTER TABLE public.users ADD COLUMN email_to_verify text;
