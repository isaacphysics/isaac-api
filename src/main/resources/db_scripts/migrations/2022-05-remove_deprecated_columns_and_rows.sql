-- I would recommend running this in a transaction starting with BEGIN; and then either ROLLBACK; or COMMIT;

-- Remove unused columns
ALTER TABLE users DROP COLUMN exam_board;
ALTER TABLE gameboards DROP COLUMN questions;

-- Delete deprecated 'EXAM_BOARD' user_preferences (should only affect CS)
DELETE FROM user_preferences WHERE preference_type='EXAM_BOARD';
