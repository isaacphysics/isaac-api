INSERT INTO user_preferences(user_id, preference_type, preference_name, preference_value)
SELECT user_email_preferences.user_id,
    'EMAIL_PREFERENCE' as preference_type,
    CASE
        WHEN email_preference = 2 THEN 'ASSIGNMENTS'
        WHEN email_preference = 3 THEN 'NEWS_AND_UPDATES'
        WHEN email_preference = 4 THEN 'EVENTS'
    END as preference_name,
    email_preference_status as preference_value
FROM user_email_preferences WHERE email_preference_status=FALSE
ON CONFLICT DO NOTHING;