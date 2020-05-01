/* Clean up expired reservations */

DELETE FROM event_bookings
WHERE status = 'RESERVED'
  AND (additional_booking_information->>'reservationCloseDate')::timestamptz < NOW();