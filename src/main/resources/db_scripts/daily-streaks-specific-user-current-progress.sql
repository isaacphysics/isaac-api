CREATE OR REPLACE FUNCTION user_streaks_current_progress(useridofinterest bigint, defaultquestionsperday INT DEFAULT 3)
RETURNS TABLE(currentdate DATE, currentprogress BIGINT, targetprogress BIGINT) AS 
$BODY$
BEGIN
RETURN QUERY
-----
-----
WITH

-- Create some fake streak targets:
user_streak_targets(user_id, target_count, start_date, end_date) AS (
    VALUES (0, 3, '2018-01-01'::date, '2018-03-01'::date),
           (0, 2, '2017-01-01'::date, '2018-01-01'::date),
           (0, 1, '2016-01-01'::date, '2017-01-01'::date),
           (0, 5, '2018-03-01'::date, NULL::date)
),

-- Filter only users first correct attempts at questions:
first_correct_attempts AS (
    SELECT
        question_id,
        MIN(timestamp) AS timestamp
    FROM question_attempts
    WHERE correct
    AND user_id=useridofinterest
    GROUP BY question_id
),

-- Count how many of these first correct attempts are today:
daily_count AS (
   SELECT
       timestamp::DATE AS date,
       count(DISTINCT question_id) AS count
   FROM first_correct_attempts_today
   WHERE timestamp >= CURRENT_DATE
   GROUP BY date
),

-- Create the list of targets and dates, allowing NULL end dates to mean "to present":
daily_targets AS (
    SELECT
        generate_series(start_date, coalesce(end_date, CURRENT_DATE), INTERVAL '1 DAY')::date AS date,
        min(target_count) AS target_count
    FROM user_streak_targets
    WHERE user_id=useridofinterest
    GROUP BY date
),

-- To ensure there is always a return value, make a row containing only todays date:
date_of_interest AS (
    SELECT CURRENT_DATE AS date
)

-- Using LEFT OUTER JOINs to ensure always keep the date; if there is a daily count
-- then join to it, else use zero; and if there is a custom target join to it, else
-- use the global streak target default.
SELECT
    date_of_interest.date AS currentdate,
    coalesce(daily_count.count, 0) AS currentprogress,
    coalesce(target_count, defaultquestionsperday)::BIGINT AS targetprogress
FROM
    (date_of_interest LEFT OUTER JOIN daily_count ON date_of_interest.date=daily_count.date)
LEFT OUTER JOIN
    daily_targets ON date_of_interest.date=daily_targets.date;

-----
-----
END
$BODY$
LANGUAGE plpgsql;

-- Then an example of using the function:
SELECT * FROM user_streaks_current_progress(0);