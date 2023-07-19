UPDATE user_credentials SET reset_token=NULL, reset_expiry=NULL, password=concat('DELETED@', gen_random_uuid())
WHERE user_id IN (SELECT id FROM users WHERE deleted);
