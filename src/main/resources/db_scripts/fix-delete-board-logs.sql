UPDATE logged_events SET
    event_type='DELETE_BOARD_FROM_PROFILE',
    event_details_type='java.util.LinkedHashMap',
    event_details=concat('{"gameboardId": ', event_details, '}')::JSONB 
WHERE event_type='ADD_BOARD_TO_PROFILE' AND event_details::TEXT NOT LIKE '{%';
