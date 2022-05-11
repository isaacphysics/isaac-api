-- I would recommend running this in a transaction BEGIN; and ROLLBACK or COMMIT
DELETE FROM public.user_preferences WHERE preference_type = 'BOOLEAN_NOTATION';

INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated)
SELECT
    id,
    'BOOLEAN_NOTATION',
    CASE
        WHEN exam_board = 'AQA' THEN 'ENG'
        WHEN exam_board = 'OCR' THEN 'MATH'
        WHEN exam_board = 'OTHER' THEN 'ENG'
        WHEN exam_board IS NULL THEN 'ENG'
    END,
    true,
    NOW()
FROM users;
