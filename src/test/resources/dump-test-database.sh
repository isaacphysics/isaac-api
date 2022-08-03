#!/usr/bin/env bash

echo "Cleaning up database..."
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE public.logged_events"
docker exec -it postgres psql -U rutherford -c "SELECT pg_catalog.setval('public.logged_events_id_seq', 1, true);"
docker exec -it postgres psql -U rutherford -c "TRUNCATE TABLE public.question_attempts"
docker exec -it postgres psql -U rutherford -c "SELECT pg_catalog.setval('public.question_attempts_id_seq', 1, true);"
echo "Dumping test data..."
docker exec -it postgres pg_dump -U rutherford --data-only > test-postgres-rutherford-data-dump.sql
echo "... done. Check your test-postgres-rutherford-data-dump.sql for any issues, then commit and push."
