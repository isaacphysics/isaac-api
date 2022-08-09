--
-- PostgreSQL database dump
--

-- Dumped from database version 12.6 (Debian 12.6-1.pgdg100+1)
-- Dumped by pg_dump version 12.6 (Debian 12.6-1.pgdg100+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.users (id, _id, family_name, given_name, email, role, date_of_birth, gender, registration_date, school_id, school_other, registered_contexts, registered_contexts_last_confirmed, last_updated, email_verification_status, last_seen, email_to_verify, email_verification_token, session_token, deleted) FROM stdin;
2	\N	Test	Test Admin	test-admin@test.com	ADMIN	\N	OTHER	2019-08-01 12:40:16.738	\N	A Manually Entered School	{"{\\"stage\\": \\"all\\", \\"examBoard\\": \\"all\\"}"}	2022-07-06 10:48:59.527	2022-07-06 10:48:59.673	VERIFIED	2022-08-03 12:07:08.761	test-admin@test.com	AwrblcwVoRFMWxJtV2TXAalOeA7a84TpD3rO2RmE	0	f
5	\N	Teacher	Test Teacher	test-teacher@test.com	TEACHER	\N	FEMALE	2019-08-01 12:51:05.416	\N	A Manually Entered School	{"{\\"stage\\": \\"all\\", \\"examBoard\\": \\"all\\"}"}	2022-08-03 12:08:57.662	2022-08-03 12:08:57.741	VERIFIED	2022-08-09 10:53:23.573	test-teacher@test.com	m9A8P0VbpFQnzOdXOywx75lpaWSpssLmQ779ij2b5LQ	0	f
11	\N	Student	Erika	erika-student@test.com	STUDENT	\N	FEMALE	2022-07-03 17:34:07		A Manually Entered School	{}	\N	2022-07-05 17:34:31	VERIFIED	2022-08-09 10:53:44.547	erika-student@test.com	\N	0	f
9	\N	Student	Charlie	charlie-student@test.com	STUDENT	\N	MALE	2022-07-05 17:34:07	130615	\N	{}	\N	2022-07-05 17:34:31	VERIFIED	2022-08-09 10:54:15.741	charlie-student@test.com	\N	0	f
4	\N	Editor	Test Editor	test-editor@test.com	CONTENT_EDITOR	\N	PREFER_NOT_TO_SAY	2019-08-01 12:50:32.631	133801	\N	{}	\N	2021-03-09 16:46:26.28	VERIFIED	2022-08-09 10:54:30.095	test-editor@test.com	nAAK4xSBuAPRejM4YPNfTKRDGK4Oa1VuL3EMmJburjE	0	f
3	\N	Event Manager	Test Event	test-event@test.com	EVENT_MANAGER	\N	OTHER	2019-08-01 12:43:14.583	133801	\N	{}	\N	2021-03-09 16:47:03.77	VERIFIED	2022-08-09 10:54:42.013	test-event@test.com	QlIS3PVS33I8jmMo3JPQgIn2xaKe4gFgwXfH4qiI8	0	f
1	\N	Progress	Test Progress	test-progress@test.com	STUDENT	\N	FEMALE	2019-08-01 12:28:22.869	130615	\N	{"{\\"stage\\": \\"all\\", \\"examBoard\\": \\"ocr\\"}"}	2021-10-04 14:10:37.441	2021-11-05 10:52:13.018	VERIFIED	2022-08-09 10:54:55.362	test-progress@test.com	scIF1UJeYyGRGwGrwGNUyIWuZxKBrQHd8evcAeZk	0	f
6	\N	Student	Test Student	test-student@test.com	STUDENT	\N	MALE	2019-08-01 12:51:39.981	110158	\N	{"{\\"stage\\": \\"all\\", \\"examBoard\\": \\"ocr\\"}"}	2021-10-04 14:12:13.351	2021-10-04 14:12:13.384	VERIFIED	2022-08-09 10:55:06.592	test-student@test.com	ZMUU7NbjhUSawOClEzb1KPEMcUA93QCkxuGejMwmE	0	f
10	\N	Teacher	Dave	dave-teacher@test.com	TEACHER	\N	MALE	2022-07-06 15:15:00	110158	\N	{}	\N	\N	VERIFIED	2022-08-09 10:55:35.591	dave-teacher@test.com	\N	0	f
7	\N	Student	Alice	alice-student@test.com	STUDENT	1991-01-01	FEMALE	2022-07-05 17:31:12	\N	A Manually Entered School	{"{\\"stage\\": \\"all\\", \\"examBoard\\": \\"all\\"}"}	2022-07-06 10:52:35.922	2022-07-06 10:52:36.056	VERIFIED	2022-08-09 10:56:00.055	alice-student@test.com	\N	0	f
8	\N	Student	Bob	bob-student@test.com	STUDENT	\N	MALE	2022-07-05 17:32:41	110158	\N	{}	\N	2022-07-05 17:32:57	VERIFIED	2022-08-09 10:56:12.965	bob-student@test.com	\N	0	f
\.


--
-- Data for Name: gameboards; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.gameboards (id, title, contents, wildcard, wildcard_position, game_filter, owner_user_id, creation_method, creation_date, tags) FROM stdin;
9313cadd-d4ce-420f-9331-300e3067b45f	Test Teacher's gameboard 1	{"{\\"id\\": \\"_regression_test_\\", \\"context\\": {\\"role\\": null, \\"stage\\": null, \\"examBoard\\": null, \\"difficulty\\": null}, \\"contentType\\": \\"isaacQuestionPage\\"}","{\\"id\\": \\"f2d24c70-9b6b-4a46-ad2a-002886dff84e\\", \\"context\\": {\\"role\\": null, \\"stage\\": null, \\"examBoard\\": null, \\"difficulty\\": null}, \\"contentType\\": \\"isaacQuestionPage\\"}"}	{"id": "0d1c84b9-d77d-4dd0-9d05-ea91e49d7adb", "url": "https://isaacphysics.org", "tags": ["computer_science"], "type": "isaacWildcard", "level": null, "title": "Isaac Physics", "value": null, "author": "jsharkey13", "layout": null, "display": null, "version": null, "audience": null, "children": [], "encoding": null, "subtitle": null, "published": false, "deprecated": null, "expandable": null, "attribution": null, "description": "Check out our sister site!", "relatedContent": null, "searchableContent": null, "canonicalSourceFile": "content/isaac-physics-demo-page/example_wildcard.json"}	0	{"fields": [], "levels": [], "stages": [], "topics": [], "concepts": [], "subjects": ["computer_science"], "examBoards": [], "difficulties": [], "questionCategories": []}	5	BUILDER	2022-08-03 12:09:47.537	[]
\.


--
-- Data for Name: groups; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.groups (id, group_name, owner_id, created, archived, group_status, last_updated) FROM stdin;
1	AB Group (Test)	5	2022-07-06 15:36:58	f	ACTIVE	\N
2	BC Group (Dave)	10	2022-07-06 15:37:32	f	ACTIVE	\N
\.


--
-- Data for Name: assignments; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.assignments (id, gameboard_id, group_id, owner_user_id, notes, creation_date, due_date, scheduled_start_date) FROM stdin;
2	9313cadd-d4ce-420f-9331-300e3067b45f	1	5	\N	2022-08-03 12:14:50.134	\N	\N
\.


--
-- Data for Name: event_bookings; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.event_bookings (id, event_id, created, user_id, reserved_by, status, updated, additional_booking_information, pii_removed) FROM stdin;
2	_regular_test_event	2022-07-06 10:54:41.525	7	\N	CONFIRMED	2022-07-06 10:54:41.525	{"yearGroup": "13", "emergencyName": "Alice's mom", "emergencyNumber": "+44020123456", "medicalRequirements": "Alice's dietary requirements", "accessibilityRequirements": "Alice's accessibility requirements"}	\N
3	_regular_test_event	2022-07-06 10:56:36.676	8	\N	CONFIRMED	2022-07-06 10:56:36.676	{"yearGroup": "9", "emergencyName": "Bob's dad", "emergencyNumber": "+44020654321", "medicalRequirements": "Bob's dietary requirements", "accessibilityRequirements": "Bob's accessibility requirements"}	\N
4	_regular_test_event	2022-07-14 14:41:58	11	\N	CONFIRMED	2022-07-14 14:42:07	{"yearGroup": "8", "emergencyName": "Charlie's uncle", "emergencyNumber": "+44020918273", "medicalRequirements": "Charlie's dietary requirements", "accessibilityRequirements": "Charlie's accessibility requirements"}	\N
\.


--
-- Data for Name: external_accounts; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.external_accounts (user_id, provider_name, provider_user_identifier, provider_last_updated) FROM stdin;
\.


--
-- Data for Name: group_additional_managers; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.group_additional_managers (user_id, group_id, created) FROM stdin;
\.


--
-- Data for Name: group_memberships; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.group_memberships (group_id, user_id, created, updated, status) FROM stdin;
1	7	\N	2022-07-06 14:38:03.339711+00	ACTIVE
1	8	\N	2022-07-06 14:38:17.286686+00	ACTIVE
2	8	\N	2022-07-06 14:38:36.064903+00	ACTIVE
2	9	\N	2022-07-06 14:38:36.064903+00	ACTIVE
\.


--
-- Data for Name: ip_location_history; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.ip_location_history (id, ip_address, location_information, created, last_lookup, is_current) FROM stdin;
\.


--
-- Data for Name: linked_accounts; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.linked_accounts (user_id, provider, provider_user_id) FROM stdin;
\.


--
-- Data for Name: logged_events; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.logged_events (id, user_id, anonymous_user, event_type, event_details_type, event_details, ip_address, "timestamp") FROM stdin;
\.


--
-- Data for Name: question_attempts; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.question_attempts (id, user_id, question_id, question_attempt, correct, "timestamp") FROM stdin;
\.


--
-- Data for Name: quiz_assignments; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.quiz_assignments (id, quiz_id, group_id, owner_user_id, creation_date, due_date, quiz_feedback_mode, deleted) FROM stdin;
\.


--
-- Data for Name: quiz_attempts; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.quiz_attempts (id, user_id, quiz_id, quiz_assignment_id, start_date, completed_date) FROM stdin;
\.


--
-- Data for Name: quiz_question_attempts; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.quiz_question_attempts (id, quiz_attempt_id, question_id, question_attempt, correct, "timestamp") FROM stdin;
\.


--
-- Data for Name: scheduled_emails; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.scheduled_emails (email_id, sent) FROM stdin;
\.


--
-- Data for Name: temporary_user_store; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.temporary_user_store (id, created, last_updated, temporary_app_data) FROM stdin;
\.


--
-- Data for Name: uk_post_codes; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.uk_post_codes (postcode, lat, lon) FROM stdin;
\.


--
-- Data for Name: user_alerts; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_alerts (id, user_id, message, link, created, seen, clicked, dismissed) FROM stdin;
\.


--
-- Data for Name: user_associations; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_associations (user_id_granting_permission, user_id_receiving_permission, created) FROM stdin;
7	5	2022-07-06 16:05:29
8	5	2022-07-06 16:05:46
8	10	2022-07-06 16:06:03
9	10	2022-07-06 16:06:11
\.


--
-- Data for Name: user_associations_tokens; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_associations_tokens (token, owner_user_id, group_id) FROM stdin;
ABTOK7	7	1
ABTOK8	8	1
BCTOK8	8	2
BCTOK9	9	2
\.


--
-- Data for Name: user_badges; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_badges (user_id, badge, state) FROM stdin;
\.


--
-- Data for Name: user_credentials; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_credentials (user_id, password, secure_salt, security_scheme, reset_token, reset_expiry, created, last_updated) FROM stdin;
2	r5qdcuA2J1vVh46iBkAndMO4yKAaaUFHKGCrxfe5wGru29+jEoe0laMGAwEqOzg0Nqk3zDbcWPVeIXudxsA/iA==	W3IupoCouzIf0fDjYSB2Tg==	SegueSCryptv1	\N	\N	2019-08-01 12:40:17.294925+00	2022-08-03 10:06:36.86+00
5	S+VthIPxGTotnrVn2de1ju8njdPCle+XSBgRMeuBQ/NO3yg2RCWIankd0CqfJMo8EJ4E1O3knzWGQMWIRg7aTQ==	JfZKaJzTUH9VzWwwU4w8XA==	SegueSCryptv1	\N	\N	2019-08-01 12:51:05.940811+00	2022-08-03 10:08:26.604+00
11	g79YHOLqV3TbZOT2kX8bBUzIkh2R8mHWytTKO1l/Q2DT1iWp/jyz/FO7aBpbwrJ+qmuFExTPzdaoJksZ8qEEng==	lfohwZStRsQLd3BaGdquYg==	SegueSCryptv1	\N	\N	2019-08-01 12:51:05.940811+00	2022-08-09 08:53:44.484+00
9	cYyLCYnI/DaKYsTdB/c8Cnk9wCsETxPD8qxXUiC7QbUfA5Vo364mMX6JuKjWoSYkaDJVRwG6hsjLetjC7olNJg==	1/gsnWDv2HLfo3sLoScANw==	SegueSCryptv1	\N	\N	2019-08-01 12:51:05.940811+00	2022-08-09 08:54:15.689+00
4	5WRjQt8oLsyNaABI43lmWPI05hEWzvY33/Cc8oVPy1Q7KZRuzdG8PTXcU56mLhWRHTXg3Egw44Ib5P0khkbRYQ==	XkRZgiCp6weXY0mglXMnWg==	SegueSCryptv1	\N	\N	2019-08-01 12:50:33.329901+00	2022-08-09 08:54:30.032+00
3	wxv7J2AVfbqXPrglrhouNoRARF3J0ReLd5y+kPr1mjOSU7H44sSnHraPS0x8KVUC29LOxyKdd7LT78Dd2YRiDg==	PoKVXSX/bfpEFB/VZTpP9w==	SegueSCryptv1	\N	\N	2019-08-01 12:43:15.133957+00	2022-08-09 08:54:41.962+00
1	6DE79eGP9gLMMJYAJZ7+1kfcKOBrmmJz1Me2BiILqbn3uJJ37VB+jD7bQ+J30OEErs4KHUoSCDpFQf1fQgJQAQ==	nTKemc+aGyTQnayPdXdl+A==	SegueSCryptv1	\N	\N	2019-08-01 12:28:23.463026+00	2022-08-09 08:54:55.312+00
6	QVRxU3m/pX/Z8PiJIhmHm+iXafkprmIFhNjGGO8mwIooEBP5m13952LyTRgDofPqSmhrjxEBsgtRJPHi7lZitw==	m4Lp3d0v1zfAtiqnJSN5bA==	SegueSCryptv1	\N	\N	2019-08-01 12:51:05.940811+00	2022-08-09 08:55:06.523+00
10	Xzw8SAqDSTScpgVxzzzock2DbPpBd9XwquaTtNnI622i9Nf89m6l3cCy5YM4z//9ALorAg7geuqGOrTLg/GWyA==	ULVBTRSdyYvJLZH6dyOTog==	SegueSCryptv1	\N	\N	2019-08-01 12:51:05.940811+00	2022-08-09 08:55:35.544+00
7	gOOQ4j/Ioy2BSS8AUOb1DWUkKnEGL9p8/k9je1FrKkoje48xpE762L7C/VoEjsWo0s4nrk+xE/Uo2+/EgYaygA==	nR+oCwVdXVBykk04RfzTJA==	SegueSCryptv1	\N	\N	2019-08-01 12:51:05.940811+00	2022-08-09 08:56:00+00
8	U7s5oE17P649t7pDrB9biUsV8vdHLiAIHP/74PiiVY/Ma+RL0lnEKdCcnQtuhlxhvZWRaLr8kz0JubC/qNa0NA==	qHoZcgNTNdilJSp5k3WLrg==	SegueSCryptv1	\N	\N	2019-08-01 12:51:05.940811+00	2022-08-09 08:56:12.918+00
\.


--
-- Data for Name: user_email_preferences; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_email_preferences (user_id, email_preference, email_preference_status) FROM stdin;
\.


--
-- Data for Name: user_gameboards; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_gameboards (user_id, gameboard_id, created, last_visited) FROM stdin;
5	9313cadd-d4ce-420f-9331-300e3067b45f	2022-08-03 12:09:47.916	2022-08-03 12:09:49.959
\.


--
-- Data for Name: user_notifications; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_notifications (user_id, notification_id, status, created) FROM stdin;
\.


--
-- Data for Name: user_preferences; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_preferences (user_id, preference_type, preference_name, preference_value, last_updated) FROM stdin;
6	EMAIL_PREFERENCE	ASSIGNMENTS	t	2021-03-09 16:00:34.563979
2	EMAIL_PREFERENCE	ASSIGNMENTS	t	2022-07-06 10:48:59.821444
2	EMAIL_PREFERENCE	NEWS_AND_UPDATES	f	2022-07-06 10:48:59.821444
2	EMAIL_PREFERENCE	EVENTS	f	2022-07-06 10:48:59.821444
2	DISPLAY_SETTING	HIDE_NON_AUDIENCE_CONTENT	t	2022-07-06 10:48:59.821444
7	DISPLAY_SETTING	HIDE_NON_AUDIENCE_CONTENT	t	2022-07-06 10:52:36.096292
5	EMAIL_PREFERENCE	ASSIGNMENTS	t	2022-08-03 12:08:57.777349
5	EMAIL_PREFERENCE	NEWS_AND_UPDATES	f	2022-08-03 12:08:57.777349
5	EMAIL_PREFERENCE	EVENTS	f	2022-08-03 12:08:57.777349
5	BOOLEAN_NOTATION	ENG	f	2022-08-03 12:08:57.777349
5	BOOLEAN_NOTATION	MATH	t	2022-08-03 12:08:57.777349
5	DISPLAY_SETTING	HIDE_NON_AUDIENCE_CONTENT	t	2022-08-03 12:08:57.777349
\.


--
-- Data for Name: user_streak_freezes; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_streak_freezes (user_id, start_date, end_date, comment) FROM stdin;
\.


--
-- Data for Name: user_streak_targets; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_streak_targets (user_id, target_count, start_date, end_date, comment) FROM stdin;
\.


--
-- Data for Name: user_totp; Type: TABLE DATA; Schema: public; Owner: rutherford
--

COPY public.user_totp (user_id, shared_secret, created, last_updated) FROM stdin;
2	OQXZE3PEGIGKAAP6	2022-07-06 10:48:02.111+00	2022-07-06 10:48:05.498+00
\.


--
-- Data for Name: qrtz_job_details; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

COPY quartz_cluster.qrtz_job_details (sched_name, job_name, job_group, description, job_class_name, is_durable, is_nonconcurrent, is_update_data, requests_recovery, job_data) FROM stdin;
\.


--
-- Data for Name: qrtz_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

COPY quartz_cluster.qrtz_triggers (sched_name, trigger_name, trigger_group, job_name, job_group, description, next_fire_time, prev_fire_time, priority, trigger_state, trigger_type, start_time, end_time, calendar_name, misfire_instr, job_data) FROM stdin;
\.


--
-- Data for Name: qrtz_blob_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

COPY quartz_cluster.qrtz_blob_triggers (sched_name, trigger_name, trigger_group, blob_data) FROM stdin;
\.


--
-- Data for Name: qrtz_calendars; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

COPY quartz_cluster.qrtz_calendars (sched_name, calendar_name, calendar) FROM stdin;
\.


--
-- Data for Name: qrtz_cron_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

COPY quartz_cluster.qrtz_cron_triggers (sched_name, trigger_name, trigger_group, cron_expression, time_zone_id) FROM stdin;
\.


--
-- Data for Name: qrtz_fired_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

COPY quartz_cluster.qrtz_fired_triggers (sched_name, entry_id, trigger_name, trigger_group, instance_name, fired_time, sched_time, priority, state, job_name, job_group, is_nonconcurrent, requests_recovery) FROM stdin;
\.


--
-- Data for Name: qrtz_locks; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

COPY quartz_cluster.qrtz_locks (sched_name, lock_name) FROM stdin;
\.


--
-- Data for Name: qrtz_paused_trigger_grps; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

COPY quartz_cluster.qrtz_paused_trigger_grps (sched_name, trigger_group) FROM stdin;
\.


--
-- Data for Name: qrtz_scheduler_state; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

COPY quartz_cluster.qrtz_scheduler_state (sched_name, instance_name, last_checkin_time, checkin_interval) FROM stdin;
\.


--
-- Data for Name: qrtz_simple_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

COPY quartz_cluster.qrtz_simple_triggers (sched_name, trigger_name, trigger_group, repeat_count, repeat_interval, times_triggered) FROM stdin;
\.


--
-- Data for Name: qrtz_simprop_triggers; Type: TABLE DATA; Schema: quartz_cluster; Owner: rutherford
--

COPY quartz_cluster.qrtz_simprop_triggers (sched_name, trigger_name, trigger_group, str_prop_1, str_prop_2, str_prop_3, int_prop_1, int_prop_2, long_prop_1, long_prop_2, dec_prop_1, dec_prop_2, bool_prop_1, bool_prop_2, time_zone_id) FROM stdin;
\.


--
-- Name: assignments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.assignments_id_seq', 2, true);


--
-- Name: event_bookings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.event_bookings_id_seq', 4, true);


--
-- Name: groups_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.groups_id_seq', 1, true);


--
-- Name: ip_location_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.ip_location_history_id_seq', 1, false);


--
-- Name: logged_events_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.logged_events_id_seq', 1, true);


--
-- Name: question_attempts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.question_attempts_id_seq', 1, true);


--
-- Name: quiz_assignments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.quiz_assignments_id_seq', 2, true);


--
-- Name: quiz_attempts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.quiz_attempts_id_seq', 1, true);


--
-- Name: quiz_question_attempts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.quiz_question_attempts_id_seq', 3, true);


--
-- Name: user_alerts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.user_alerts_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rutherford
--

SELECT pg_catalog.setval('public.users_id_seq', 1, true);


--
-- PostgreSQL database dump complete
--

