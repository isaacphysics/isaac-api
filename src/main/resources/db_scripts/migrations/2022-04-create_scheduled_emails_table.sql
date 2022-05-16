CREATE TABLE scheduled_emails (
    email_id TEXT NOT NULL,
    sent TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT scheduled_emails_pk PRIMARY KEY (email_id)
);
