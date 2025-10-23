ALTER TABLE question_attempts
ADD marks integer;

ALTER TABLE quiz_question_attempts
ADD marks integer;

CREATE FUNCTION update_marks_for_period(start_date date, end_date date)
    RETURNS void AS $$
BEGIN
    WITH case_statement AS (
        SELECT
            id,
            CASE
                WHEN
                    (question_attempt -> 'answer' ->> 'type') = 'llmFreeTextChoice'
                THEN
                    (question_attempt ->> 'marksAwarded')::int
                ELSE
                    correct::int
            END AS marks
        FROM question_attempts
        WHERE timestamp > start_date
          AND timestamp <= end_date
    )

    UPDATE question_attempts
    SET marks = case_statement.marks
    FROM case_statement
    WHERE question_attempts.id = case_statement.id;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION quiz_update_marks_for_period(start_date date, end_date date)
    RETURNS void AS $$
BEGIN
    WITH case_statement AS (
        SELECT
            id,
            CASE
                WHEN
                    (question_attempt -> 'answer' ->> 'type') = 'llmFreeTextChoice'
                    THEN
                    (question_attempt ->> 'marksAwarded')::int
                ELSE
                    correct::int
                END AS marks
        FROM quiz_question_attempts
        WHERE timestamp > start_date
          AND timestamp <= end_date
    )

    UPDATE quiz_question_attempts
    SET marks = case_statement.marks
    FROM case_statement
    WHERE quiz_question_attempts.id = case_statement.id;
END;
$$ LANGUAGE plpgsql;

SELECT update_marks_for_period('2014-01-01', '2015-01-02');
SELECT update_marks_for_period('2015-01-01', '2016-01-02');
SELECT update_marks_for_period('2016-01-02', '2025-12-01');

SELECT quiz_update_marks_for_period('2014-01-01', '2015-01-02');
SELECT quiz_update_marks_for_period('2015-01-01', '2016-01-02');
SELECT quiz_update_marks_for_period('2016-01-02', '2025-12-01');

DROP FUNCTION update_marks_for_period;
DROP FUNCTION quiz_update_marks_for_period;