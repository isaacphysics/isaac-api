ALTER TABLE ONLY assignments ADD COLUMN title varchar(255)
    DEFAULT NULL;
ALTER TABLE ONLY quiz_assignments ADD COLUMN title varchar(255)
    DEFAULT NULL;
