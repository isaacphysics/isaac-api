-- This is NOT scheduled via Quartz, but intended to be run regularly using cron.

-- Repeatable read is required to ensure the same users are moved into archived as are deleted.
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;

CREATE TEMPORARY TABLE archive_in_progress(user_id INTEGER) ON COMMIT DROP;

-- We need to use coalesce(last_seen, last_updated, registration_date) since some accounts have NULL for last_seen,
-- if they were never used immediately after registration.
INSERT INTO archive_in_progress SELECT id FROM users
WHERE coalesce(last_seen, last_updated, registration_date) <= CURRENT_DATE - INTERVAL '4 YEARS' AND NOT deleted;

-- Archive the PII.
INSERT INTO archived_users SELECT id, family_name, given_name, email, date_of_birth, school_other, CURRENT_TIMESTAMP FROM users
WHERE id IN (SELECT user_id FROM archive_in_progress);

-- The following mirrors PgUsers::deleteUserAccount, particularly PgUsers::removePIIFromUserDO.

-- Remove PII from users table.
UPDATE users SET family_name=NULL, given_name=NULL,
                 email=gen_random_uuid(),
                 email_to_verify=NULL, email_verification_token=NULL,
                 date_of_birth=date_trunc('MONTH', date_of_birth),
                 school_other=NULL,
                 deleted=TRUE
WHERE id IN (SELECT user_id FROM archive_in_progress);

COMMIT;
