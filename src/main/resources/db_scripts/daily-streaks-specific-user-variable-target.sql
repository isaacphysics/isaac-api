CREATE OR REPLACE FUNCTION user_streaks(useridofinterest bigint, defaultquestionsperday INT DEFAULT 3)
RETURNS TABLE(streaklength BIGINT, startdate DATE, enddate DATE, totaldays BIGINT) AS 
$BODY$
BEGIN
RETURN QUERY
-----
-----
WITH

-- Create some fake streak freezes:
user_streak_freezes(user_id, start_date, end_date) AS (
    VALUES (0, '2018-02-01'::date, '2018-03-01'::date)
),

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
    WHERE correct AND user_id=useridofinterest
    GROUP BY question_id
),

-- Count how many of these first correct attempts per day:
daily_counts AS (
   SELECT
       timestamp::DATE AS date,
       count(DISTINCT question_id) AS count
   FROM first_correct_attempts
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

-- Filter the list of dates by the minimum number of parts required.
-- If no user-specific target, use gloabl default:
active_dates AS (
    SELECT
        daily_counts.date
    FROM daily_counts LEFT OUTER JOIN daily_targets 
    ON daily_counts.date=daily_targets.date
    WHERE count >= coalesce(target_count, defaultquestionsperday)
),

-- Create a list of dates streaks were frozen on:
frozen_dates AS (
    SELECT
        DISTINCT generate_series(start_date, end_date, INTERVAL '1 DAY')::date AS date
    FROM user_streak_freezes
    WHERE user_id=useridofinterest
),

-- Merge in streak freeze dates if there was no activity on that date:
date_list(date, activity) AS (
    SELECT date, 1 FROM active_dates
    UNION
    SELECT date, 0 FROM frozen_dates WHERE date NOT IN (SELECT date FROM active_dates)
    ORDER BY date ASC
),
   
-- Group consecutive dates in this merged list, this is the magic part.
-- 
  groups AS (
    SELECT
        date - (ROW_NUMBER() OVER (ORDER BY date) * INTERVAL '1 day') AS grp,
        date,
        activity
    FROM date_list
  )

-- Return the data in a human-readable format.
-- The length of the streak is the sum of active days.
SELECT
    sum(activity) AS streak_length,
    MIN(date) AS start_date,
    MAX(date) AS end_date,
    count(*) AS total_days
FROM groups
GROUP BY grp
ORDER BY end_date DESC;
-----
-----
END
$BODY$
LANGUAGE plpgsql;

-- Then an example of using the function:
SELECT * FROM user_streaks(0);