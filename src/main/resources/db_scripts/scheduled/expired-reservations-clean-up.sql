/* Clean up expired reservations */

UPDATE event_bookings
SET status = 'CANCELLED', updated = NOW()
WHERE status = 'RESERVED'
  AND (additional_booking_information->>'reservationCloseDate')::timestamptz < NOW();