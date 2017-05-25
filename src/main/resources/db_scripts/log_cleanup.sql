/*
  List all top-level keys used in logs:
*/
--SELECT DISTINCT jsonb_object_keys(event_details) as keys FROM logged_events --WHERE event_type='VIEW_PAGE';--event_details::TEXT LIKE '{%';

/*
  Clean up non-JSON objects in the logs:
*/
UPDATE logged_events SET event_details=concat('{"message": ', event_details::text, '}')::jsonb, event_details_type='java.util.LinkedHashMap' WHERE event_type='CONTACT_US_FORM_USED';
UPDATE logged_events SET event_details=concat('{"gameboardId": ', event_details, '}')::jsonb, event_details_type='java.util.LinkedHashMap' WHERE event_type='ADD_BOARD_TO_PROFILE';
UPDATE logged_events SET event_details='{}', event_details_type='java.util.HashMap' WHERE event_type='VIEW_MY_ASSIGNMENTS' AND event_details::text='null';

/*
  Fix two versions of key 'gameboardId':
*/
UPDATE logged_events SET event_details=replace(event_details::text, '"gameBoard', '"gameboard')::jsonb WHERE event_type='VIEW_GAMEBOARD_BY_ID';

/*
  Fix four versions of key 'contentVersion':
*/
UPDATE logged_events SET event_details=replace(event_details::text, '"CONTENT_SHA"', '"contentVersion"')::jsonb WHERE event_type IN ('GLOBAL_SITE_SEARCH','VIEW_PAGE');
UPDATE logged_events SET event_details=replace(event_details::text, '"CONTENT_VERSION"', '"contentVersion"')::jsonb WHERE event_type IN ('GENERATE_RANDOM_GAMEBOARD','GLOBAL_SITE_SEARCH','VIEW_CONCEPT','VIEW_PAGE');
UPDATE logged_events SET event_details=replace(event_details::text, '"contentVersionId"', '"contentVersion"')::jsonb WHERE event_type IN ('SENT_MASS_EMAIL');