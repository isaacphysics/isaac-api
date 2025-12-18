CREATE INDEX CONCURRENTLY quiz_attempts_index_by_user_id_and_quiz_id
    ON public.quiz_attempts USING btree (user_id, quiz_id);

VACUUM public.quiz_attempts;

DROP INDEX IF EXISTS quiz_attempts_index_by_quiz_id_and_user_id;
