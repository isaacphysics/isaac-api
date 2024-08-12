CREATE INDEX CONCURRENTLY IF NOT EXISTS quiz_assignments_group_quiz ON quiz_assignments USING btree (group_id, quiz_id);
