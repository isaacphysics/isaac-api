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
SELECT update_marks_for_period('2016-01-01', '2016-06-02');
SELECT update_marks_for_period('2016-06-01', '2017-01-02');
SELECT update_marks_for_period('2017-01-01', '2017-06-02');
SELECT update_marks_for_period('2017-06-01', '2018-01-02');
SELECT update_marks_for_period('2018-01-01', '2018-06-02');
SELECT update_marks_for_period('2018-06-01', '2018-10-02');
SELECT update_marks_for_period('2018-10-01', '2019-01-02');
SELECT update_marks_for_period('2019-01-01', '2019-06-02');
SELECT update_marks_for_period('2019-06-01', '2019-10-02');
SELECT update_marks_for_period('2019-10-01', '2020-01-02');
SELECT update_marks_for_period('2020-01-01', '2020-04-02');
SELECT update_marks_for_period('2020-04-01', '2020-09-02');
SELECT update_marks_for_period('2020-09-01', '2020-10-02');
SELECT update_marks_for_period('2020-10-01', '2021-01-02');
SELECT update_marks_for_period('2021-01-01', '2021-04-02');
SELECT update_marks_for_period('2021-04-01', '2021-09-02');
SELECT update_marks_for_period('2021-09-01', '2021-10-02');
SELECT update_marks_for_period('2021-10-01', '2022-01-02');
SELECT update_marks_for_period('2022-01-01', '2022-04-02');
SELECT update_marks_for_period('2022-04-01', '2022-09-02');
SELECT update_marks_for_period('2022-09-01', '2022-10-02');
SELECT update_marks_for_period('2022-10-01', '2023-01-02');
SELECT update_marks_for_period('2023-01-01', '2023-03-02');
SELECT update_marks_for_period('2023-03-01', '2023-06-02');
SELECT update_marks_for_period('2023-06-01', '2023-09-02');
SELECT update_marks_for_period('2023-09-01', '2023-10-02');
SELECT update_marks_for_period('2023-10-01', '2023-11-02');
SELECT update_marks_for_period('2023-11-01', '2024-01-02');
SELECT update_marks_for_period('2024-01-01', '2024-02-02');
SELECT update_marks_for_period('2024-02-01', '2024-03-02');
SELECT update_marks_for_period('2024-03-01', '2026-01-02');

SELECT quiz_update_marks_for_period('2014-01-01', '2015-01-02');
SELECT quiz_update_marks_for_period('2015-01-01', '2016-01-02');
SELECT quiz_update_marks_for_period('2016-01-01', '2016-06-02');
SELECT quiz_update_marks_for_period('2016-06-01', '2017-01-02');
SELECT quiz_update_marks_for_period('2017-01-01', '2017-06-02');
SELECT quiz_update_marks_for_period('2017-06-01', '2018-01-02');
SELECT quiz_update_marks_for_period('2018-01-01', '2018-06-02');
SELECT quiz_update_marks_for_period('2018-06-01', '2018-10-02');
SELECT quiz_update_marks_for_period('2018-10-01', '2019-01-02');
SELECT quiz_update_marks_for_period('2019-01-01', '2019-06-02');
SELECT quiz_update_marks_for_period('2019-06-01', '2019-10-02');
SELECT quiz_update_marks_for_period('2019-10-01', '2020-01-02');
SELECT quiz_update_marks_for_period('2020-01-01', '2020-04-02');
SELECT quiz_update_marks_for_period('2020-04-01', '2020-09-02');
SELECT quiz_update_marks_for_period('2020-09-01', '2020-10-02');
SELECT quiz_update_marks_for_period('2020-10-01', '2021-01-02');
SELECT quiz_update_marks_for_period('2021-01-01', '2021-04-02');
SELECT quiz_update_marks_for_period('2021-04-01', '2021-09-02');
SELECT quiz_update_marks_for_period('2021-09-01', '2021-10-02');
SELECT quiz_update_marks_for_period('2021-10-01', '2022-01-02');
SELECT quiz_update_marks_for_period('2022-01-01', '2022-04-02');
SELECT quiz_update_marks_for_period('2022-04-01', '2022-09-02');
SELECT quiz_update_marks_for_period('2022-09-01', '2022-10-02');
SELECT quiz_update_marks_for_period('2022-10-01', '2023-01-02');
SELECT quiz_update_marks_for_period('2023-01-01', '2023-03-02');
SELECT quiz_update_marks_for_period('2023-03-01', '2023-06-02');
SELECT quiz_update_marks_for_period('2023-06-01', '2023-09-02');
SELECT quiz_update_marks_for_period('2023-09-01', '2023-10-02');
SELECT quiz_update_marks_for_period('2023-10-01', '2023-11-02');
SELECT quiz_update_marks_for_period('2023-11-01', '2024-01-02');
SELECT quiz_update_marks_for_period('2024-01-01', '2024-02-02');
SELECT quiz_update_marks_for_period('2024-02-01', '2024-03-02');
SELECT quiz_update_marks_for_period('2024-03-01', '2026-01-02');

DROP FUNCTION update_marks_for_period;
DROP FUNCTION quiz_update_marks_for_period;