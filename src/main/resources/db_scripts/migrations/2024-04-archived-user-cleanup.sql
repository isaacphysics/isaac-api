DROP TABLE IF EXISTS temp_users_to_archive;

-- Initially archived users still have credentials:
UPDATE user_credentials SET password = concat('DELETED@', gen_random_uuid()), reset_token=NULL, reset_expiry=NULL
WHERE user_id IN (SELECT id FROM users WHERE deleted) AND password NOT LIKE 'DELETED@%';

-- Clear linked account IDs for users where not already cleared:
UPDATE linked_accounts SET provider_user_id = gen_random_uuid()
WHERE user_id IN (SELECT id FROM users WHERE deleted) AND length(provider_user_id)<>36;
