--
-- Name: user_email_preferences; Type: TABLE; Schema: public; Owner: rutherford; Tablespace: 
--

CREATE TABLE user_email_preferences (
    user_id serial NOT NULL,
    email_preference integer,
    email_preference_status bool
);

ALTER TABLE ONLY user_email_preferences
    ADD CONSTRAINT "composite key" PRIMARY KEY (user_id, email_preference);
    

UPDATE users SET role = 'STUDENT' WHERE role = ''

ALTER TABLE ONLY users ALTER COLUMN role SET NOT NULL    
ALTER TABLE ONLY users ALTER COLUMN role SET DEFAULT 'STUDENT'