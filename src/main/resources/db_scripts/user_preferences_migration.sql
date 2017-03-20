UPDATE user_preferences SET preference_name='PHYSICS_ALEVEL' WHERE preference_type='SUBJECT_INTEREST' AND preference_name='PHYSICS';
UPDATE user_preferences SET preference_name='MATHS_ALEVEL' WHERE preference_type='SUBJECT_INTEREST' AND preference_name='MATHEMATICS';
UPDATE user_preferences SET preference_name='CHEMISTRY_ALEVEL' WHERE preference_type='SUBJECT_INTEREST' AND preference_name='CHEMISTRY';
DELETE FROM user_preferences WHERE preference_name='BIOLOGY' AND preference_type='SUBJECT_INTEREST';