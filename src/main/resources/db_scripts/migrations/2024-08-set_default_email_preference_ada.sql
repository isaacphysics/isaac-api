INSERT INTO public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated)
SELECT DISTINCT user_id, 'EMAIL_PREFERENCE' AS preference_type, 'ASSIGNMENTS' AS preference_name, true AS preference_value, now() as last_updated
FROM public.user_preferences
WHERE user_id NOT IN (
    SELECT user_id FROM public.user_preferences
    WHERE preference_type = 'EMAIL_PREFERENCE'
);
