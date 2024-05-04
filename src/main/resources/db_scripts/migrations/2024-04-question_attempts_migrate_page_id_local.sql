-- DO NOT RUN THIS ON A PRODUCTION DATABASE.
-- It takes exclusive locks which will block all question attempts for a long time!

-- Do the update of existing rows:
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1);

-- Add the new index and update the stats for the table:
CREATE INDEX question_attempts_by_user_question_page ON public.question_attempts USING btree (user_id, page_id);

-- Clean up the old rows and update the table stats:
VACUUM (VERBOSE, ANALYSE) question_attempts;

-- Add the correct not-null constraint, which should succeed if we have done the right thing above.
ALTER TABLE question_attempts ALTER COLUMN page_id SET NOT NULL;

-- Remove the old index, to prevent it slowing down writes:
DROP INDEX question_attempts_by_user_question;
