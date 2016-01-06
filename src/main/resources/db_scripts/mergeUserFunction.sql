CREATE OR REPLACE FUNCTION mergeUser(targetUserIdToKeep INTEGER, targetUserIdToDelete INTEGER) RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
	BEGIN
		UPDATE linked_accounts
		SET user_id = targetUserIdToKeep
		WHERE user_id = targetUserIdToDelete;
	EXCEPTION WHEN unique_violation THEN
	    -- Ignore duplicate inserts.
	END;

UPDATE question_attempts
SET user_id = targetUserIdToKeep
WHERE user_id = targetUserIdToDelete;

UPDATE logged_events
SET user_id = targetUserIdToKeep::varchar(255)
WHERE user_id = targetUserIdToDelete::varchar(255);

UPDATE groups
SET owner_id = targetUserIdToKeep
WHERE owner_id = targetUserIdToDelete;

	BEGIN
		UPDATE group_memberships
		SET user_id = targetUserIdToKeep
		WHERE user_id = targetUserIdToDelete;
	EXCEPTION WHEN unique_violation THEN
	    -- Ignore duplicate inserts.
	END;

UPDATE assignments
SET owner_user_id = targetUserIdToKeep
WHERE owner_user_id = targetUserIdToDelete;

	BEGIN
		UPDATE event_bookings
		SET user_id = targetUserIdToKeep
		WHERE user_id = targetUserIdToDelete;	
	EXCEPTION WHEN unique_violation THEN
	    -- Ignore duplicate inserts.
	END;

-- Deal with user associations
 
UPDATE gameboards
SET owner_user_id = targetUserIdToKeep
WHERE owner_user_id = targetUserIdToDelete;

	BEGIN
		UPDATE user_gameboards
		SET user_id = targetUserIdToKeep
		WHERE user_id = targetUserIdToDelete;
	EXCEPTION WHEN unique_violation THEN
	    -- Ignore duplicate inserts.
	END;	

-- Deal with user associations

UPDATE user_associations_tokens
SET owner_user_id = targetUserIdToKeep
WHERE owner_user_id = targetUserIdToDelete;

	BEGIN
		UPDATE user_associations
		SET user_id_granting_permission = targetUserIdToKeep
		WHERE user_id_granting_permission = targetUserIdToDelete;
	EXCEPTION WHEN unique_violation THEN
	    -- Ignore duplicate inserts.
	END;	

	BEGIN
		UPDATE user_associations
		SET user_id_receiving_permission = targetUserIdToKeep
		WHERE user_id_receiving_permission = targetUserIdToDelete;
	EXCEPTION WHEN unique_violation THEN
	    -- Ignore duplicate inserts.
	END;		

 DELETE FROM users
 WHERE id = targetUserIdToDelete;
 
 RETURN true;
END
$$;

-- remember params are userToKEEP userToDelete
--select mergeUser(?,?);