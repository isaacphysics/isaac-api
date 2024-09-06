INSERT INTO user_preferences
SELECT id, 'EMAIL_PREFERENCE', 'ASSIGNMENTS', true, CURRENT_TIMESTAMP
FROM users
WHERE users.id NOT IN (SELECT user_id FROM user_preferences WHERE preference_type = 'EMAIL_PREFERENCE');