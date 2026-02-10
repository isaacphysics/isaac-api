UPDATE question_attempts
SET question_attempt =
    jsonb_set(
        question_attempt - 'marksAwarded',
        '{marks}',
        question_attempt->'marksAwarded'
    )
WHERE question_attempt ? 'marksAwarded';