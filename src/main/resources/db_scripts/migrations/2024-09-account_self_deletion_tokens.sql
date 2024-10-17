CREATE TABLE user_deletion_tokens (
    user_id INTEGER NOT NULL
        CONSTRAINT pk_user_deletion_tokens PRIMARY KEY
        CONSTRAINT fk_user_deletion_tokens_users_id REFERENCES users ON DELETE CASCADE,
    token TEXT NOT NULL,
    token_expiry TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_updated TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
