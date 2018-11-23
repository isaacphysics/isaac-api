/* delete PII from log events events */

/* Remove email address from simple log events - log retention policy*/
UPDATE logged_events
SET event_details = event_details - 'email' WHERE event_type IN ('PASSWORD_RESET_REQUEST_RECEIVED', 'PASSWORD_RESET_REQUEST_SUCCESSFUL', 'SENT_EMAIL')
AND timestamp < NOW() - INTERVAL '1 year';

/* Remove email address from message value in contact us log events - log retention policy*/
UPDATE logged_events
SET event_details = event_details - 'message' WHERE event_type IN ('CONTACT_US_FORM_USED')
AND timestamp < NOW() - INTERVAL '1 year';

/* Remove token from email verification request log events - log retention policy*/
UPDATE logged_events
SET event_details = event_details - 'emailVerificationToken' WHERE event_type IN ('EMAIL_VERIFICATION_REQUEST_RECEIVED')
AND timestamp < NOW() - INTERVAL '1 year';
