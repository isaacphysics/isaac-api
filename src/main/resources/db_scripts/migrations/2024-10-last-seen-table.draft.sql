-- THIS IS A DRAFT!

CREATE TABLE user_last_seen (
    user_id INTEGER NOT NULL
        CONSTRAINT pk_user_last_seen PRIMARY KEY
        CONSTRAINT fk_user_last_seen_users_id REFERENCES users ON DELETE CASCADE,
    last_seen TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

INSERT INTO user_last_seen SELECT id, last_seen FROM users WHERE last_seen IS NOT NULL;

ALTER TABLE users RENAME COLUMN last_seen TO last_seen_old;

ANALYSE user_last_seen;
