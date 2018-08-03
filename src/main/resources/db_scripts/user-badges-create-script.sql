CREATE TABLE user_badges (
    user_id integer,
    badge text,
    state jsonb
);

CREATE UNIQUE INDEX user_badges_user_id_badge_unique ON user_badges USING btree (user_id, badge);