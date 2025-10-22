ALTER TABLE question_attempts
ADD marks integer GENERATED ALWAYS AS (
    CASE
        WHEN
            (question_attempt -> 'answer' ->> 'type') = 'llmFreeTextChoice'
        THEN
            (question_attempt -> 'marksAwarded')::int
        ELSE
            correct::int
        END
    ) STORED;