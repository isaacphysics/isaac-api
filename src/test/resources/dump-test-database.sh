#!/usr/bin/env bash

echo "Cleaning up database..."
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE public.logged_events"
docker exec -it postgres psql -U rutherford -c "SELECT pg_catalog.setval('public.logged_events_id_seq', 1, true);"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE public.question_attempts"
docker exec -it postgres psql -U rutherford -c "SELECT pg_catalog.setval('public.question_attempts_id_seq', 1, true);"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE quartz_cluster.qrtz_blob_triggers CASCADE"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE quartz_cluster.qrtz_calendars CASCADE"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE quartz_cluster.qrtz_job_details CASCADE"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE quartz_cluster.qrtz_cron_triggers CASCADE"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE quartz_cluster.qrtz_fired_triggers CASCADE"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE quartz_cluster.qrtz_locks CASCADE"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE quartz_cluster.qrtz_paused_trigger_grps CASCADE"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE quartz_cluster.qrtz_scheduler_state CASCADE"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE quartz_cluster.qrtz_simple_triggers CASCADE"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE quartz_cluster.qrtz_simprop_triggers CASCADE"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE quartz_cluster.qrtz_triggers CASCADE"

echo "Dumping test data..."
docker exec -it postgres pg_dump -U rutherford --data-only > src/test/resources/test-postgres-rutherford-data-dump.sql

echo "... done. Check your test-postgres-rutherford-data-dump.sql for any issues, then commit and push."
