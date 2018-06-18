--
-- Merge and Delete Users
--
-- Authors: Stephen Cummins, James Sharkey
-- Last Modified: 2018-06-13
--

CREATE OR REPLACE FUNCTION mergeuser(targetuseridtokeep integer, targetuseridtodelete integer) RETURNS boolean
LANGUAGE plpgsql
AS $$
BEGIN

  UPDATE assignments
  SET owner_user_id = targetUserIdToKeep
  WHERE owner_user_id = targetUserIdToDelete;

  BEGIN
    UPDATE event_bookings
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  UPDATE gameboards
  SET owner_user_id = targetUserIdToKeep
  WHERE owner_user_id = targetUserIdToDelete;

  BEGIN
    UPDATE group_additional_managers
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  BEGIN
    UPDATE group_memberships
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  UPDATE groups
  SET owner_id = targetUserIdToKeep
  WHERE owner_id = targetUserIdToDelete;

  BEGIN
    UPDATE linked_accounts
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  UPDATE logged_events
  SET user_id = targetUserIdToKeep::varchar(255)
  WHERE user_id = targetUserIdToDelete::varchar(255);

  UPDATE question_attempts
  SET user_id = targetUserIdToKeep
  WHERE user_id = targetUserIdToDelete;

  UPDATE user_alerts
  SET user_id = targetUserIdToKeep
  WHERE user_id = targetUserIdToDelete;

  BEGIN
    UPDATE user_associations
    SET user_id_granting_permission = targetUserIdToKeep
    WHERE user_id_granting_permission = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;
  BEGIN
    UPDATE user_associations
    SET user_id_receiving_permission = targetUserIdToKeep
    WHERE user_id_receiving_permission = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  UPDATE user_associations_tokens
  SET owner_user_id = targetUserIdToKeep
  WHERE owner_user_id = targetUserIdToDelete;

  BEGIN
    UPDATE user_gameboards
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  BEGIN
    UPDATE user_notifications
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  BEGIN
    UPDATE user_preferences
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  BEGIN
    UPDATE user_streak_freezes
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  BEGIN
    UPDATE user_streak_targets
    SET user_id = targetUserIdToKeep
    WHERE user_id = targetUserIdToDelete;
    EXCEPTION WHEN unique_violation THEN
    -- Ignore duplicate inserts.
  END;

  DELETE FROM users
  WHERE id = targetUserIdToDelete;

  RETURN true;
END
$$;

ALTER FUNCTION mergeuser(targetuseridtokeep integer, targetuseridtodelete integer) OWNER TO rutherford;


--
-- Calculate User Streaks
--
-- Authors: James Sharkey
-- Last Modified: 2018-04-20
--

CREATE OR REPLACE FUNCTION user_streaks(useridofinterest BIGINT, defaultquestionsperday INTEGER DEFAULT 3)
  RETURNS TABLE(streaklength BIGINT, startdate DATE, enddate DATE, totaldays BIGINT) AS
$BODY$
BEGIN
  RETURN QUERY
  -----
  -----
  WITH

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
          COUNT(DISTINCT question_id) AS count
        FROM first_correct_attempts
        GROUP BY date
    ),

    -- Create the list of targets and dates, allowing NULL end dates to mean "to present":
      daily_targets AS (
        SELECT
          generate_series(start_date, COALESCE(end_date, CURRENT_DATE), INTERVAL '1 DAY')::DATE AS date,
          MIN(target_count) AS target_count
        FROM user_streak_targets
        WHERE user_id=useridofinterest
        GROUP BY date
    ),

    -- Filter the list of dates by the minimum number of parts required.
    -- If no user-specific target, use global default:
      active_dates AS (
        SELECT
          daily_counts.date
        FROM daily_counts LEFT OUTER JOIN daily_targets
            ON daily_counts.date=daily_targets.date
        WHERE count >= COALESCE(target_count, defaultquestionsperday)
    ),

    -- Create a list of dates streaks were frozen on, allowing NULL end dates to mean "to present":
      frozen_dates AS (
        SELECT
          DISTINCT generate_series(start_date, COALESCE(end_date, CURRENT_DATE), INTERVAL '1 DAY')::DATE AS date
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
    SUM(activity) AS streak_length,
    MIN(date) AS start_date,
    MAX(date) AS end_date,
    COUNT(*) AS total_days
  FROM groups
  GROUP BY grp
  ORDER BY end_date DESC;
  -----
  -----
END
$BODY$
LANGUAGE plpgsql;

ALTER FUNCTION user_streaks(useridofinterest BIGINT, defaultquestionsperday INTEGER) OWNER TO rutherford;


--
-- Calculate Current Progress towards User Streak
--
-- Authors: James Sharkey
-- Last Modified: 2018-04-20
--

CREATE OR REPLACE FUNCTION user_streaks_current_progress(useridofinterest BIGINT, defaultquestionsperday INTEGER DEFAULT 3)
  RETURNS TABLE(currentdate DATE, currentprogress BIGINT, targetprogress BIGINT) AS
$BODY$
BEGIN
  RETURN QUERY
  -----
  -----
  WITH

    -- Filter only users first correct attempts at questions:
      first_correct_attempts AS (
        SELECT
          question_id,
          MIN(timestamp) AS timestamp
        FROM question_attempts
        WHERE correct AND user_id=useridofinterest
        GROUP BY question_id
    ),

    -- Count how many of these first correct attempts are today:
      daily_count AS (
        SELECT
          timestamp::DATE AS date,
          COUNT(DISTINCT question_id) AS count
        FROM first_correct_attempts
        WHERE timestamp >= CURRENT_DATE
        GROUP BY date
    ),

    -- Create the list of targets and dates, allowing NULL end dates to mean "to present":
      daily_targets AS (
        SELECT
          generate_series(start_date, COALESCE(end_date, CURRENT_DATE), INTERVAL '1 DAY')::DATE AS date,
          MIN(target_count) AS target_count
        FROM user_streak_targets
        WHERE user_id=useridofinterest
        GROUP BY date
    ),

    -- To ensure there is always a return value, make a row containing only today's date:
      date_of_interest AS (
        SELECT CURRENT_DATE AS date
    )

  -- Using LEFT OUTER JOINs to ensure always keep the date; if there is a daily count
  -- then join to it, else use zero; and if there is a custom target join to it, else
  -- use the global streak target default.
  SELECT
    date_of_interest.date AS currentdate,
    COALESCE(daily_count.count, 0)::BIGINT AS currentprogress,
    COALESCE(target_count, defaultquestionsperday)::BIGINT AS targetprogress
  FROM
    (date_of_interest LEFT OUTER JOIN daily_count ON date_of_interest.date=daily_count.date)
    LEFT OUTER JOIN
    daily_targets ON date_of_interest.date=daily_targets.date;

  -----
  -----
END
$BODY$
LANGUAGE plpgsql;

ALTER FUNCTION user_streaks_current_progress(useridofinterest BIGINT, defaultquestionsperday INTEGER) OWNER TO rutherford;
