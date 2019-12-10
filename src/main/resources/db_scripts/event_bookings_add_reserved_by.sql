ALTER TABLE ONLY event_bookings ADD COLUMN reserved_by integer
        CONSTRAINT event_bookings_users_id_fk
        REFERENCES users
        ON DELETE CASCADE;

