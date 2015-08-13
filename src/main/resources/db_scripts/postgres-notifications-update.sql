-- Table: user_notifications

-- DROP TABLE user_notifications;

CREATE TABLE user_notifications
(
  user_id text NOT NULL,
  notification_id text NOT NULL,
  status text,
  created timestamp without time zone NOT NULL,
  CONSTRAINT "composite key" PRIMARY KEY (user_id, notification_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE user_notifications
  OWNER TO rutherford;