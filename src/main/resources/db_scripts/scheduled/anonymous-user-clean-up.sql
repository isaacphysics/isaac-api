/* clean up temporary user store */

DELETE FROM temporary_user_store
WHERE last_updated < NOW() - INTERVAL '1 hour';
