ALTER TABLE event_bookings
   ADD COLUMN status text NOT NULL DEFAULT 'CONFIRMED';

ALTER TABLE event_bookings
   ADD COLUMN updated timestamp without time zone;

CREATE UNIQUE INDEX event_booking_user_event_id_index
  ON event_bookings
  USING btree
  (event_id COLLATE pg_catalog."default", user_id);
