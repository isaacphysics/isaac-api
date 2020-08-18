-- DROP SCHEMA quartz_cluster;

ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_triggers DROP CONSTRAINT IF EXISTS qrtz_triggers_sched_name_fkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_simprop_triggers DROP CONSTRAINT IF EXISTS qrtz_simprop_triggers_sched_name_fkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_simple_triggers DROP CONSTRAINT IF EXISTS qrtz_simple_triggers_sched_name_fkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_cron_triggers DROP CONSTRAINT IF EXISTS qrtz_cron_triggers_sched_name_fkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_blob_triggers DROP CONSTRAINT IF EXISTS qrtz_blob_triggers_sched_name_fkey;
DROP INDEX IF EXISTS quartz_cluster.idx_qrtz_t_state;
DROP INDEX IF EXISTS quartz_cluster.idx_qrtz_t_nft_st;
DROP INDEX IF EXISTS quartz_cluster.idx_qrtz_t_next_fire_time;
DROP INDEX IF EXISTS quartz_cluster.idx_qrtz_j_req_recovery;
DROP INDEX IF EXISTS quartz_cluster.idx_qrtz_ft_trig_nm_gp;
DROP INDEX IF EXISTS quartz_cluster.idx_qrtz_ft_trig_name;
DROP INDEX IF EXISTS quartz_cluster.idx_qrtz_ft_trig_inst_name;
DROP INDEX IF EXISTS quartz_cluster.idx_qrtz_ft_trig_group;
DROP INDEX IF EXISTS quartz_cluster.idx_qrtz_ft_job_req_recovery;
DROP INDEX IF EXISTS quartz_cluster.idx_qrtz_ft_job_name;
DROP INDEX IF EXISTS quartz_cluster.idx_qrtz_ft_job_group;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_triggers DROP CONSTRAINT IF EXISTS qrtz_triggers_pkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_simprop_triggers DROP CONSTRAINT IF EXISTS qrtz_simprop_triggers_pkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_simple_triggers DROP CONSTRAINT IF EXISTS qrtz_simple_triggers_pkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_scheduler_state DROP CONSTRAINT IF EXISTS qrtz_scheduler_state_pkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_paused_trigger_grps DROP CONSTRAINT IF EXISTS qrtz_paused_trigger_grps_pkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_locks DROP CONSTRAINT IF EXISTS qrtz_locks_pkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_job_details DROP CONSTRAINT IF EXISTS qrtz_job_details_pkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_fired_triggers DROP CONSTRAINT IF EXISTS qrtz_fired_triggers_pkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_cron_triggers DROP CONSTRAINT IF EXISTS qrtz_cron_triggers_pkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_calendars DROP CONSTRAINT IF EXISTS qrtz_calendars_pkey;
ALTER TABLE IF EXISTS ONLY quartz_cluster.qrtz_blob_triggers DROP CONSTRAINT IF EXISTS qrtz_blob_triggers_pkey;
DROP TABLE IF EXISTS quartz_cluster.qrtz_triggers;
DROP TABLE IF EXISTS quartz_cluster.qrtz_simprop_triggers;
DROP TABLE IF EXISTS quartz_cluster.qrtz_simple_triggers;
DROP TABLE IF EXISTS quartz_cluster.qrtz_scheduler_state;
DROP TABLE IF EXISTS quartz_cluster.qrtz_paused_trigger_grps;
DROP TABLE IF EXISTS quartz_cluster.qrtz_locks;
DROP TABLE IF EXISTS quartz_cluster.qrtz_job_details;
DROP TABLE IF EXISTS quartz_cluster.qrtz_fired_triggers;
DROP TABLE IF EXISTS quartz_cluster.qrtz_cron_triggers;
DROP TABLE IF EXISTS quartz_cluster.qrtz_calendars;
DROP TABLE IF EXISTS quartz_cluster.qrtz_blob_triggers;
DROP SCHEMA IF EXISTS quartz_cluster;
--
-- Name: quartz_cluster; Type: SCHEMA; Schema: -; Owner: rutherford
--

CREATE SCHEMA quartz_cluster;


ALTER SCHEMA quartz_cluster OWNER TO rutherford;

--
-- Name: qrtz_blob_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_blob_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    blob_data bytea
);


ALTER TABLE quartz_cluster.qrtz_blob_triggers OWNER TO rutherford;

--
-- Name: qrtz_calendars; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_calendars (
    sched_name character varying(120) NOT NULL,
    calendar_name character varying(200) NOT NULL,
    calendar bytea NOT NULL
);


ALTER TABLE quartz_cluster.qrtz_calendars OWNER TO rutherford;

--
-- Name: qrtz_cron_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_cron_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    cron_expression character varying(250) NOT NULL,
    time_zone_id character varying(80)
);


ALTER TABLE quartz_cluster.qrtz_cron_triggers OWNER TO rutherford;

--
-- Name: qrtz_fired_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_fired_triggers (
    sched_name character varying(120) NOT NULL,
    entry_id character varying(140) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    instance_name character varying(200) NOT NULL,
    fired_time bigint NOT NULL,
    sched_time bigint NOT NULL,
    priority integer NOT NULL,
    state character varying(16) NOT NULL,
    job_name character varying(200),
    job_group character varying(200),
    is_nonconcurrent boolean NOT NULL,
    requests_recovery boolean
);


ALTER TABLE quartz_cluster.qrtz_fired_triggers OWNER TO rutherford;

--
-- Name: qrtz_job_details; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_job_details (
    sched_name character varying(120) NOT NULL,
    job_name character varying(200) NOT NULL,
    job_group character varying(200) NOT NULL,
    description character varying(250),
    job_class_name character varying(250) NOT NULL,
    is_durable boolean NOT NULL,
    is_nonconcurrent boolean NOT NULL,
    is_update_data boolean NOT NULL,
    requests_recovery boolean NOT NULL,
    job_data bytea
);


ALTER TABLE quartz_cluster.qrtz_job_details OWNER TO rutherford;

--
-- Name: qrtz_locks; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_locks (
    sched_name character varying(120) NOT NULL,
    lock_name character varying(40) NOT NULL
);


ALTER TABLE quartz_cluster.qrtz_locks OWNER TO rutherford;

--
-- Name: qrtz_paused_trigger_grps; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_paused_trigger_grps (
    sched_name character varying(120) NOT NULL,
    trigger_group character varying(150) NOT NULL
);


ALTER TABLE quartz_cluster.qrtz_paused_trigger_grps OWNER TO rutherford;

--
-- Name: qrtz_scheduler_state; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_scheduler_state (
    sched_name character varying(120) NOT NULL,
    instance_name character varying(200) NOT NULL,
    last_checkin_time bigint NOT NULL,
    checkin_interval bigint NOT NULL
);


ALTER TABLE quartz_cluster.qrtz_scheduler_state OWNER TO rutherford;

--
-- Name: qrtz_simple_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_simple_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    repeat_count bigint NOT NULL,
    repeat_interval bigint NOT NULL,
    times_triggered bigint NOT NULL
);


ALTER TABLE quartz_cluster.qrtz_simple_triggers OWNER TO rutherford;

--
-- Name: qrtz_simprop_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_simprop_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    str_prop_1 character varying(512),
    str_prop_2 character varying(512),
    str_prop_3 character varying(512),
    int_prop_1 integer,
    int_prop_2 integer,
    long_prop_1 bigint,
    long_prop_2 bigint,
    dec_prop_1 numeric,
    dec_prop_2 numeric,
    bool_prop_1 boolean,
    bool_prop_2 boolean,
    time_zone_id character varying(80)
);


ALTER TABLE quartz_cluster.qrtz_simprop_triggers OWNER TO rutherford;

--
-- Name: qrtz_triggers; Type: TABLE; Schema: quartz_cluster; Owner: rutherford
--

CREATE TABLE quartz_cluster.qrtz_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    job_name character varying(200) NOT NULL,
    job_group character varying(200) NOT NULL,
    description character varying(250),
    next_fire_time bigint,
    prev_fire_time bigint,
    priority integer,
    trigger_state character varying(16) NOT NULL,
    trigger_type character varying(8) NOT NULL,
    start_time bigint NOT NULL,
    end_time bigint,
    calendar_name character varying(200),
    misfire_instr smallint,
    job_data bytea
);


ALTER TABLE quartz_cluster.qrtz_triggers OWNER TO rutherford;

--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_calendars qrtz_calendars_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_calendars
    ADD CONSTRAINT qrtz_calendars_pkey PRIMARY KEY (sched_name, calendar_name);


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_fired_triggers qrtz_fired_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_fired_triggers
    ADD CONSTRAINT qrtz_fired_triggers_pkey PRIMARY KEY (sched_name, entry_id);


--
-- Name: qrtz_job_details qrtz_job_details_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_job_details
    ADD CONSTRAINT qrtz_job_details_pkey PRIMARY KEY (sched_name, job_name, job_group);


--
-- Name: qrtz_locks qrtz_locks_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_locks
    ADD CONSTRAINT qrtz_locks_pkey PRIMARY KEY (sched_name, lock_name);


--
-- Name: qrtz_paused_trigger_grps qrtz_paused_trigger_grps_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_paused_trigger_grps
    ADD CONSTRAINT qrtz_paused_trigger_grps_pkey PRIMARY KEY (sched_name, trigger_group);


--
-- Name: qrtz_scheduler_state qrtz_scheduler_state_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_scheduler_state
    ADD CONSTRAINT qrtz_scheduler_state_pkey PRIMARY KEY (sched_name, instance_name);


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_triggers qrtz_triggers_pkey; Type: CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: idx_qrtz_ft_job_group; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_job_group ON quartz_cluster.qrtz_fired_triggers USING btree (job_group);


--
-- Name: idx_qrtz_ft_job_name; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_job_name ON quartz_cluster.qrtz_fired_triggers USING btree (job_name);


--
-- Name: idx_qrtz_ft_job_req_recovery; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_job_req_recovery ON quartz_cluster.qrtz_fired_triggers USING btree (requests_recovery);


--
-- Name: idx_qrtz_ft_trig_group; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_trig_group ON quartz_cluster.qrtz_fired_triggers USING btree (trigger_group);


--
-- Name: idx_qrtz_ft_trig_inst_name; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_trig_inst_name ON quartz_cluster.qrtz_fired_triggers USING btree (instance_name);


--
-- Name: idx_qrtz_ft_trig_name; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_trig_name ON quartz_cluster.qrtz_fired_triggers USING btree (trigger_name);


--
-- Name: idx_qrtz_ft_trig_nm_gp; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_ft_trig_nm_gp ON quartz_cluster.qrtz_fired_triggers USING btree (sched_name, trigger_name, trigger_group);


--
-- Name: idx_qrtz_j_req_recovery; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_j_req_recovery ON quartz_cluster.qrtz_job_details USING btree (requests_recovery);


--
-- Name: idx_qrtz_t_next_fire_time; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_t_next_fire_time ON quartz_cluster.qrtz_triggers USING btree (next_fire_time);


--
-- Name: idx_qrtz_t_nft_st; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_t_nft_st ON quartz_cluster.qrtz_triggers USING btree (next_fire_time, trigger_state);


--
-- Name: idx_qrtz_t_state; Type: INDEX; Schema: quartz_cluster; Owner: rutherford
--

CREATE INDEX idx_qrtz_t_state ON quartz_cluster.qrtz_triggers USING btree (trigger_state);


--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES quartz_cluster.qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES quartz_cluster.qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES quartz_cluster.qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES quartz_cluster.qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_triggers qrtz_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: quartz_cluster; Owner: rutherford
--

ALTER TABLE ONLY quartz_cluster.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_sched_name_fkey FOREIGN KEY (sched_name, job_name, job_group) REFERENCES quartz_cluster.qrtz_job_details(sched_name, job_name, job_group);

