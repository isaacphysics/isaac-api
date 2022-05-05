
ALTER TABLE users ADD COLUMN exam_board TEXT;

UPDATE users SET exam_board=preference_name FROM user_preferences
WHERE user_preferences.user_id=users.id AND preference_type='EXAM_BOARD' AND preference_value;
