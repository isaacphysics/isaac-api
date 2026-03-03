-- Ada Computer Science + Isaac Science (Staging) Migration Functions
CREATE OR REPLACE FUNCTION update_marks_for_period(qa question_attempts)
    RETURNS int AS $$
BEGIN
    RETURN CASE
               WHEN (qa.question_attempt -> 'answer' ->> 'type') = 'llmFreeTextChoice'
                   THEN (qa.question_attempt ->> 'marksAwarded')::int
               ELSE qa.correct::int
        END;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION quiz_update_marks_for_period(qa quiz_question_attempts)
    RETURNS int AS $$
BEGIN
    RETURN CASE
               WHEN (qa.question_attempt -> 'answer' ->> 'type') = 'llmFreeTextChoice'
                   THEN (qa.question_attempt ->> 'marksAwarded')::int
               ELSE qa.correct::int
        END;
END;
$$ LANGUAGE plpgsql;

-- Isaac Science (Live) Migration Functions
/* CREATE OR REPLACE FUNCTION update_marks_for_period(qa question_attempts)
    RETURNS int AS $$
BEGIN
    RETURN qa.correct::int;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION quiz_update_marks_for_period(qa quiz_question_attempts)
    RETURNS int AS $$
BEGIN
    RETURN qa.correct::int;
END;
$$ LANGUAGE plpgsql; */

-- Date ranges taken from 2024-04-question_attempts_migrate_page_id_live.sql and extrapolated to 2026
-- See that script for the previous results of running on the phy live database

UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2014-01-01' AND timestamp <= '2015-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2015-01-01' AND timestamp <= '2016-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2016-01-01' AND timestamp <= '2016-06-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2016-06-01' AND timestamp <= '2017-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2017-01-01' AND timestamp <= '2017-06-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2017-06-01' AND timestamp <= '2018-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2018-01-01' AND timestamp <= '2018-06-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2018-06-01' AND timestamp <= '2018-10-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2018-10-01' AND timestamp <= '2019-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2019-01-01' AND timestamp <= '2019-06-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2019-06-01' AND timestamp <= '2019-10-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2019-10-01' AND timestamp <= '2020-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2020-01-01' AND timestamp <= '2020-04-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2020-04-01' AND timestamp <= '2020-09-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2020-09-01' AND timestamp <= '2020-10-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2020-10-01' AND timestamp <= '2021-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2021-01-01' AND timestamp <= '2021-04-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2021-04-01' AND timestamp <= '2021-09-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2021-09-01' AND timestamp <= '2021-10-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2021-10-01' AND timestamp <= '2022-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2022-01-01' AND timestamp <= '2022-04-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2022-04-01' AND timestamp <= '2022-09-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2022-09-01' AND timestamp <= '2022-10-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2022-10-01' AND timestamp <= '2023-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2023-01-01' AND timestamp <= '2023-03-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2023-03-01' AND timestamp <= '2023-06-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2023-06-01' AND timestamp <= '2023-09-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2023-09-01' AND timestamp <= '2023-10-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2023-10-01' AND timestamp <= '2023-11-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2023-11-01' AND timestamp <= '2024-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2024-01-01' AND timestamp <= '2024-02-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2024-02-01' AND timestamp <= '2024-03-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2024-03-01' AND timestamp <= '2024-06-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2024-06-01' AND timestamp <= '2024-09-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2024-09-01' AND timestamp <= '2024-10-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2024-10-01' AND timestamp <= '2024-11-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2024-11-01' AND timestamp <= '2025-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2025-01-01' AND timestamp <= '2025-02-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2025-02-01' AND timestamp <= '2025-03-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2025-03-01' AND timestamp <= '2025-06-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2025-06-01' AND timestamp <= '2025-09-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2025-09-01' AND timestamp <= '2025-10-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2025-10-01' AND timestamp <= '2025-11-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2025-11-01' AND timestamp <= '2026-01-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2026-01-01' AND timestamp <= '2026-02-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2026-02-01' AND timestamp <= '2026-03-02';
UPDATE question_attempts SET marks = update_marks_for_period(question_attempts) WHERE timestamp > '2026-03-01';


UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2014-01-01' AND timestamp <= '2015-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2015-01-01' AND timestamp <= '2016-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2016-01-01' AND timestamp <= '2016-06-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2016-06-01' AND timestamp <= '2017-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2017-01-01' AND timestamp <= '2017-06-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2017-06-01' AND timestamp <= '2018-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2018-01-01' AND timestamp <= '2018-06-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2018-06-01' AND timestamp <= '2018-10-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2018-10-01' AND timestamp <= '2019-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2019-01-01' AND timestamp <= '2019-06-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2019-06-01' AND timestamp <= '2019-10-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2019-10-01' AND timestamp <= '2020-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2020-01-01' AND timestamp <= '2020-04-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2020-04-01' AND timestamp <= '2020-09-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2020-09-01' AND timestamp <= '2020-10-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2020-10-01' AND timestamp <= '2021-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2021-01-01' AND timestamp <= '2021-04-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2021-04-01' AND timestamp <= '2021-09-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2021-09-01' AND timestamp <= '2021-10-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2021-10-01' AND timestamp <= '2022-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2022-01-01' AND timestamp <= '2022-04-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2022-04-01' AND timestamp <= '2022-09-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2022-09-01' AND timestamp <= '2022-10-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2022-10-01' AND timestamp <= '2023-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2023-01-01' AND timestamp <= '2023-03-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2023-03-01' AND timestamp <= '2023-06-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2023-06-01' AND timestamp <= '2023-09-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2023-09-01' AND timestamp <= '2023-10-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2023-10-01' AND timestamp <= '2023-11-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2023-11-01' AND timestamp <= '2024-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2024-01-01' AND timestamp <= '2024-02-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2024-02-01' AND timestamp <= '2024-03-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2024-03-01' AND timestamp <= '2026-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2024-03-01' AND timestamp <= '2024-06-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2024-06-01' AND timestamp <= '2024-09-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2024-09-01' AND timestamp <= '2024-10-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2024-10-01' AND timestamp <= '2024-11-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2024-11-01' AND timestamp <= '2025-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2025-01-01' AND timestamp <= '2025-02-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2025-02-01' AND timestamp <= '2025-03-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2025-03-01' AND timestamp <= '2025-06-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2025-06-01' AND timestamp <= '2025-09-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2025-09-01' AND timestamp <= '2025-10-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2025-10-01' AND timestamp <= '2025-11-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2025-11-01' AND timestamp <= '2026-01-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2026-01-01' AND timestamp <= '2026-02-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2026-02-01' AND timestamp <= '2026-03-02';
UPDATE quiz_question_attempts SET marks = quiz_update_marks_for_period(quiz_question_attempts) WHERE timestamp > '2026-03-01';

DROP FUNCTION update_marks_for_period;
DROP FUNCTION quiz_update_marks_for_period;