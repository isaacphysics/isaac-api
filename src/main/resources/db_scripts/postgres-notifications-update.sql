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
  
CREATE TYPE user_email_preference AS ENUM ('NEWS_AND_UPDATES', 'EVENTS', 'ASSIGNMENTS');  

--
-- Name: user_email_preferences; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE user_email_preferences (
    user_id text NOT NULL,
    email_preference integer
    email_preference_status bool
);

ALTER TABLE ONLY user_email_preferences
    ADD CONSTRAINT "composite key" PRIMARY KEY (user_id, email_preference);