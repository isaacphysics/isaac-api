-- Migration script to add streak tables:

CREATE TABLE user_streak_freezes (
    user_id bigint NOT NULL,
    start_date date NOT NULL,
    end_date date,
    comment text
);
ALTER TABLE user_streak_freezes OWNER TO rutherford;


CREATE TABLE user_streak_targets (
    user_id bigint NOT NULL,
    target_count integer NOT NULL,
    start_date date NOT NULL,
    end_date date,
    comment text
);
ALTER TABLE user_streak_targets OWNER TO rutherford;

ALTER TABLE ONLY user_streak_freezes
    ADD CONSTRAINT user_streak_freeze_pkey PRIMARY KEY (user_id, start_date);

ALTER TABLE ONLY user_streak_targets
    ADD CONSTRAINT user_streak_targets_pkey PRIMARY KEY (user_id, start_date);


ALTER TABLE ONLY user_streak_freezes
    ADD CONSTRAINT user_streak_freezes_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE ONLY user_streak_targets
    ADD CONSTRAINT user_streak_targets_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


CREATE INDEX user_streak_freezes_by_user_id ON user_streak_freezes USING btree (user_id);

CREATE INDEX user_streak_targets_by_user_id ON user_streak_targets USING btree (user_id);


-- Then run postgres-rutherford-functions.sql to get the required functions!
