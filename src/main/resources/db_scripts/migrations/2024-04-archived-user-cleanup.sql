DROP TABLE IF EXISTS temp_users_to_archive;

-- Some historically deleted users still have credentials:
DELETE FROM user_credentials WHERE user_id IN (SELECT id FROM users WHERE deleted);

-- Clear linked account IDs for users where not already cleared:
UPDATE linked_accounts SET provider_user_id = gen_random_uuid()
WHERE user_id IN (SELECT id FROM users WHERE deleted) AND length(provider_user_id)<>36;
