--
-- Merge and Delete Users
--
-- Authors: Stephen Cummins, James Sharkey
-- Last Modified: 2023-11-01
--

CREATE OR REPLACE FUNCTION mergeuser(targetuseridtokeep bigint, targetuseridtodelete bigint) RETURNS boolean
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
    -- Do not ignore duplicate inserts; merging should fail if booking data would be lost for duplicated bookings!
  END;

  UPDATE gameboards
  SET owner_user_id = targetUserIdToKeep
  WHERE owner_user_id = targetUserIdToDelete;

  BEGIN
    INSERT INTO group_additional_managers (user_id, group_id, created)
    SELECT targetUserIdToKeep, group_id, created
    FROM group_additional_managers WHERE user_id=targetUserIdToDelete
    ON CONFLICT DO NOTHING;
  END;

  BEGIN
    INSERT INTO group_memberships (group_id, user_id, created, updated, status)
    SELECT group_id, targetuseridtokeep, created, updated, status
    FROM group_memberships WHERE user_id = targetuseridtodelete
    ON CONFLICT (group_id, user_id) DO UPDATE
      -- merge memberships, ensuring "earliest" membership is used and ACTIVE status takes precedence.
      SET created=LEAST(group_memberships.created, EXCLUDED.created), updated=GREATEST(group_memberships.updated, EXCLUDED.updated),
          status=CASE WHEN group_memberships.status='ACTIVE' OR EXCLUDED.status='ACTIVE' THEN 'ACTIVE' ELSE group_memberships.status END;
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

  UPDATE quiz_assignments
  SET owner_user_id = targetUserIdToKeep
  WHERE owner_user_id = targetUserIdToDelete;

  UPDATE quiz_attempts
  SET user_id = targetUserIdToKeep
  WHERE user_id = targetUserIdToDelete;

  UPDATE user_alerts
  SET user_id = targetUserIdToKeep
  WHERE user_id = targetUserIdToDelete;

  BEGIN
    INSERT INTO user_associations (user_id_granting_permission, user_id_receiving_permission, created)
    SELECT targetuseridtokeep, user_id_receiving_permission, created
    FROM user_associations WHERE user_id_granting_permission=targetuseridtodelete
    -- Ignore duplicate inserts.
    ON CONFLICT DO NOTHING;
  END;

  BEGIN
    INSERT INTO user_associations (user_id_granting_permission, user_id_receiving_permission, created)
    SELECT user_id_granting_permission, targetuseridtokeep, created
    FROM user_associations WHERE user_id_receiving_permission=targetuseridtodelete
    -- Ignore duplicate inserts.
    ON CONFLICT DO NOTHING;
  END;

  UPDATE user_associations_tokens
  SET owner_user_id = targetUserIdToKeep
  WHERE owner_user_id = targetUserIdToDelete;

  BEGIN
      UPDATE user_credentials
      SET user_id = targetUserIdToKeep
      WHERE user_id = targetUserIdToDelete;
  EXCEPTION WHEN unique_violation THEN
  -- Ignore duplicate inserts. This may lose some info.
  END;

  BEGIN
    INSERT INTO user_gameboards (user_id, gameboard_id, created, last_visited)
    SELECT targetUserIdToKeep, gameboard_id, created, last_visited
    FROM user_gameboards WHERE user_id=targetUserIdToDelete
    ON CONFLICT (user_id, gameboard_id) DO UPDATE
      SET created=LEAST(EXCLUDED.created, user_gameboards.created),
          last_visited=GREATEST(EXCLUDED.last_visited, user_gameboards.last_visited);
  END;

  BEGIN
    INSERT INTO user_notifications (user_id, notification_id, status, created)
    SELECT targetUserIdToKeep, notification_id, status, created
    FROM user_notifications WHERE user_id=targetUserIdToDelete
    ON CONFLICT DO NOTHING;
  END;

  BEGIN
    INSERT INTO user_preferences (user_id, preference_type, preference_name, preference_value, last_updated)
    SELECT targetUserIdToKeep, preference_type, preference_name, preference_value, last_updated
    FROM user_preferences WHERE user_id=targetUserIdToDelete
    ON CONFLICT DO NOTHING;
    -- Do not let "to delete" preferences override "to keep" ones.
  END;

  BEGIN
    INSERT INTO user_streak_freezes (user_id, start_date, end_date, comment)
    SELECT targetUserIdToKeep, start_date, end_date, comment
    FROM user_streak_freezes WHERE user_id=targetUserIdToDelete
    ON CONFLICT DO NOTHING;
  END;

  BEGIN
    INSERT INTO user_streak_targets (user_id, target_count, start_date, end_date, comment)
    SELECT user_id, target_count, start_date, end_date, comment
    FROM user_streak_targets WHERE user_id=targetUserIdToDelete
    ON CONFLICT DO NOTHING;
  END;

  BEGIN
      UPDATE user_totp
      SET user_id = targetUserIdToKeep
      WHERE user_id = targetUserIdToDelete;
  EXCEPTION WHEN unique_violation THEN
  -- Ignore duplicate inserts. This will prefer the "to keep" account 2FA.
  END;

  DELETE FROM users
  WHERE id = targetUserIdToDelete;

  RETURN true;
END
$$;

ALTER FUNCTION mergeuser(targetuseridtokeep bigint, targetuseridtodelete bigint) OWNER TO rutherford;


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


--
-- Calculate User Weekly Streaks
--
-- Authors: James Sharkey
-- Last Modified: 2019-12-06
--

CREATE OR REPLACE FUNCTION user_streaks_weekly(useridofinterest BIGINT, defaultquestionsperweek integer DEFAULT 10)
    RETURNS TABLE(streaklength BIGINT, startdate date, enddate date, totalweeks BIGINT)
    LANGUAGE plpgsql
AS
$$
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

            -- Count how many of these first correct attempts per week:
            weekly_counts AS (
                SELECT
                    date_trunc('WEEK', timestamp)::DATE AS date,
                    COUNT(DISTINCT question_id) AS count
                FROM first_correct_attempts
                GROUP BY date
            ),

            -- Create the list of targets and dates, allowing NULL end dates to mean "to present":
            weekly_targets AS (
                SELECT
                    generate_series(date_trunc('WEEK', start_date), date_trunc('WEEK', COALESCE(end_date, CURRENT_DATE)), INTERVAL '7 DAY')::DATE AS date,
                    MIN(target_count) AS target_count
                FROM user_streak_targets
                WHERE user_id=useridofinterest
                GROUP BY date
            ),

            -- Filter the list of dates by the minimum number of parts required.
            -- If no user-specific target, use global default:
            active_dates AS (
                SELECT
                    weekly_counts.date
                FROM weekly_counts LEFT OUTER JOIN weekly_targets
                                                   ON weekly_counts.date=weekly_targets.date
                WHERE count >= COALESCE(target_count, defaultquestionsperweek)
            ),

            -- Create a list of dates streaks were frozen on, allowing NULL end dates to mean "to present":
            frozen_dates AS (
                SELECT
                    DISTINCT generate_series(date_trunc('WEEK', start_date), date_trunc('WEEK', COALESCE(end_date, CURRENT_DATE)), INTERVAL '7 DAY')::DATE AS date
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
                        date - (ROW_NUMBER() OVER (ORDER BY date) * INTERVAL '7 day') AS grp,
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
            COUNT(*) AS total_weeks
        FROM groups
        GROUP BY grp
        ORDER BY end_date DESC;
    -----
    -----
END
$$;

ALTER FUNCTION user_streaks_weekly(BIGINT, INTEGER) OWNER TO rutherford;


--
-- Calculate Current Progress towards User Weekly Streak
--
-- Authors: James Sharkey
-- Last Modified: 2019-12-06
--

CREATE OR REPLACE FUNCTION user_streaks_weekly_current_progress(useridofinterest BIGINT, defaultquestionsperweek integer DEFAULT 10)
    RETURNS TABLE(currentweek date, currentprogress BIGINT, targetprogress BIGINT)
    LANGUAGE plpgsql
AS
$$
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

            -- Count how many of these first correct attempts are this week:
            weekly_count AS (
                SELECT
                    date_trunc('WEEK', timestamp)::DATE AS date,
                    COUNT(DISTINCT question_id) AS count
                FROM first_correct_attempts
                WHERE timestamp >= date_trunc('WEEK', CURRENT_DATE)
                GROUP BY date
            ),

            -- Create the list of targets and dates, allowing NULL end dates to mean "to present":
            weekly_targets AS (
                SELECT
                    generate_series(date_trunc('WEEK', start_date), date_trunc('WEEK', COALESCE(end_date, CURRENT_DATE)), INTERVAL '7 DAY')::DATE AS date,
                    MIN(target_count) AS target_count
                FROM user_streak_targets
                WHERE user_id=useridofinterest
                GROUP BY date
            ),

            -- To ensure there is always a return value, make a row containing only this week's date:
            date_of_interest AS (
                SELECT date_trunc('WEEK', CURRENT_DATE)::DATE AS date
            )

            -- Using LEFT OUTER JOINs to ensure always keep the date; if there is a daily count
            -- then join to it, else use zero; and if there is a custom target join to it, else
            -- use the global streak target default.
        SELECT
            date_of_interest.date AS currentweek,
            COALESCE(weekly_count.count, 0)::BIGINT AS currentprogress,
            COALESCE(target_count, defaultquestionsperweek)::BIGINT AS targetprogress
        FROM
            (date_of_interest LEFT OUTER JOIN weekly_count ON date_of_interest.date=weekly_count.date)
                LEFT OUTER JOIN
            weekly_targets ON date_of_interest.date=weekly_targets.date;
    -----
    -----
END
$$;

ALTER FUNCTION user_streaks_weekly_current_progress(BIGINT, INTEGER) OWNER TO rutherford;
