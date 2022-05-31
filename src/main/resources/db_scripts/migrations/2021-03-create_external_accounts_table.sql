CREATE TABLE external_accounts (
   user_id INTEGER NOT NULL,
   provider_name TEXT NOT NULL,
   provider_user_identifier TEXT,
   provider_last_updated TIMESTAMP WITHOUT TIME ZONE,
   CONSTRAINT external_accounts_pk PRIMARY KEY (user_id, provider_name),
   CONSTRAINT external_accounts_fk FOREIGN KEY (user_id) REFERENCES public.users (id)
       ON UPDATE CASCADE ON DELETE CASCADE
);

-- Add stub records to prevent synchronisation?
--     (With noise to prevent a single large spike should the time be reached).
-- INSERT INTO external_accounts SELECT id, 'MailJet', NULL, NOW() + INTERVAL '100 DAYS' + (random() * INTERVAL '100 days') FROM users;
