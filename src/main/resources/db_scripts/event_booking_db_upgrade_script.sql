ALTER TABLE event_bookings
   ADD COLUMN status text NOT NULL DEFAULT 'CONFIRMED';

ALTER TABLE event_bookings
   ADD COLUMN updated timestamp without time zone;
