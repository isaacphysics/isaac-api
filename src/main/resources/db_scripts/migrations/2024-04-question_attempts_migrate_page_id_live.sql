-- Do the update of existing rows bit-by-bit to save performance and disk space, the ranges deliberately overlap:
-- (The results of running on the phy live database are included for future reference).

UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2014-01-01' AND timestamp <= '2015-01-02';
--      UPDATE 29641
--      Time: xxxxxxxxxx ms (00:05.00)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2015-01-01' AND timestamp <= '2016-01-02';
--      UPDATE 1,549,679
--      Time: 206216.568 ms (03:26.217)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2016-01-01' AND timestamp <= '2016-06-02';
--      UPDATE 1,140,789
--      Time: 101411.663 ms (01:41.412)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2016-06-01' AND timestamp <= '2017-01-02' AND page_id IS NULL;
--      UPDATE 2,928,387
--      Time: 210761.003 ms (03:30.761)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2017-01-01' AND timestamp <= '2017-06-02' AND page_id IS NULL;
--      UPDATE 1,735,199
--      Time:  99382.400 ms (01:39.382)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2017-06-01' AND timestamp <= '2018-01-02' AND page_id IS NULL;
--      UPDATE 6,487,103
--      Time: 486460.357 ms (08:06.460)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2018-01-01' AND timestamp <= '2018-06-02' AND page_id IS NULL;
--      UPDATE 4,317,835
--      Time: 386938.277 ms (06:26.938)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2018-06-01' AND timestamp <= '2018-10-02' AND page_id IS NULL;
--      UPDATE 3,923,653
--      Time: xxxxxxxxxx ms (08:00.00)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2018-10-01' AND timestamp <= '2019-01-02' AND page_id IS NULL;
--      UPDATE 4,817,517
--      Time: 332676.106 ms (05:32.676)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2019-01-01' AND timestamp <= '2019-06-02' AND page_id IS NULL;
--      UPDATE 4,630,508
--      Time: 247190.928 ms (04:07.191)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2019-06-01' AND timestamp <= '2019-10-02' AND page_id IS NULL;
--      UPDATE 5,057,210
--      Time: 534596.733 ms (08:54.597)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2019-10-01' AND timestamp <= '2020-01-02' AND page_id IS NULL;
--      UPDATE 5,325,292
--      Time: 479627.921 ms (07:59.628)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2020-01-01' AND timestamp <= '2020-04-02' AND page_id IS NULL;
--      UPDATE 4,486,212
--      Time: 386435.368 ms (06:26.435)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2020-04-01' AND timestamp <= '2020-09-02' AND page_id IS NULL;
--      UPDATE 6,128,364
--      Time: 989344.371 ms (16:29.344)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2020-09-01' AND timestamp <= '2020-10-02' AND page_id IS NULL;
--      UPDATE 3,653,930
--      Time: 811732.506 ms (13:31.733)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2020-10-01' AND timestamp <= '2021-01-02' AND page_id IS NULL;
--      UPDATE 6,123,920
--      Time: 827560.299 ms (13:47.560)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2021-01-01' AND timestamp <= '2021-04-02' AND page_id IS NULL;
--      UPDATE 6,246,463
--      Time: 665478.008 ms (11:05.478)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2021-04-01' AND timestamp <= '2021-09-02' AND page_id IS NULL;
--      UPDATE 4,093,688
--      Time: 436689.546 ms (07:16.690)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2021-09-01' AND timestamp <= '2021-10-02' AND page_id IS NULL;
--      UPDATE 3,968,582
--      Time: 328272.842 ms (05:28.273)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2021-10-01' AND timestamp <= '2022-01-02' AND page_id IS NULL;
--      UPDATE 6,051,894
--      Time: 466796.601 ms (07:46.797)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2022-01-01' AND timestamp <= '2022-04-02' AND page_id IS NULL;
--      UPDATE 5,809,853
--      Time: 392837.152 ms (06:32.837)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2022-04-01' AND timestamp <= '2022-09-02' AND page_id IS NULL;
--      UPDATE 4,431,594
--      Time: 421960.565 ms (07:01.961)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2022-09-01' AND timestamp <= '2022-10-02' AND page_id IS NULL;
--      UPDATE 4,741,951
--      Time: 409400.513 ms (06:49.401)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2022-10-01' AND timestamp <= '2023-01-02' AND page_id IS NULL;
--      UPDATE 7,593,086
--      Time: 510554.424 ms (08:30.554)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2023-01-01' AND timestamp <= '2023-03-02' AND page_id IS NULL;
--      UPDATE 4,918,921
--      Time: 329313.241 ms (05:29.313)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2023-03-01' AND timestamp <= '2023-06-02' AND page_id IS NULL;
--      UPDATE 4,863,866
--      Time: 555951.978 ms (09:15.952)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2023-06-01' AND timestamp <= '2023-09-02' AND page_id IS NULL;
--      UPDATE 2,457,733
--      Time: 207897.406 ms (03:27.897)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2023-09-01' AND timestamp <= '2023-10-02' AND page_id IS NULL;
--      UPDATE 5,536,989
--      Time: 366144.286 ms (06:06.144)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2023-10-01' AND timestamp <= '2023-11-02' AND page_id IS NULL;
--      UPDATE 3,623,275
--      Time: 227178.802 ms (03:47.179)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2023-11-01' AND timestamp <= '2024-01-02' AND page_id IS NULL;
--      UPDATE 5,297,639
--      Time: 315006.395 ms (05:15.006)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2024-01-01' AND timestamp <= '2024-02-02' AND page_id IS NULL;
--      UPDATE 3,640,825
--      Time: 247482.808 ms (04:07.483)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2024-02-01' AND timestamp <= '2024-03-02' AND page_id IS NULL;
--      UPDATE 2,811,205
--      Time: 377106.532 ms (06:17.107)
UPDATE question_attempts SET page_id = split_part(question_id, '|', 1) WHERE timestamp > '2024-03-01'                               AND page_id IS NULL;
--      UPDATE 3,782,425
--      Time: 668469.706 ms (11:08.470)


-- Add the new index and update the stats for the table:
CREATE INDEX CONCURRENTLY question_attempts_by_user_question_page ON public.question_attempts USING btree (user_id, page_id);
--      Time: 740475.043 ms (12:20.475)


-- Run this after each query above, verbose technically optional. The overnight cron-triggered VACUUM will do this too.
VACUUM (VERBOSE, ANALYSE) question_attempts;


-- Add the correct not-null constraint, which should succeed if we have done the right thing above.
-- Sadly doing this naively would take an exclusive lock and sequentially scan the whole table. This would prevent
-- all question attempts for several minutes!
-- This workaround is the best we can do. Validating the not-null constraint does not require the lock if (and only if)
-- these commands are run outside of a transaction. Then the SET NOT NULL can use the existing constraint rather than
-- a full table scan:
ALTER TABLE question_attempts ADD CONSTRAINT question_attempts_page_id_not_null CHECK (page_id IS NOT NULL) NOT VALID;
--      Time: 3.177 ms
ALTER TABLE question_attempts VALIDATE CONSTRAINT question_attempts_page_id_not_null;
--      Time: 118659.416 ms (01:58.659)
ALTER TABLE question_attempts ALTER COLUMN page_id SET NOT NULL;
--      Time: 3.331 ms
ALTER TABLE question_attempts DROP CONSTRAINT question_attempts_page_id_not_null;
--      Time: 60.130 ms


-- Remove the old index when last live API using the old query is gone, to prevent it slowing down writes:
DROP INDEX question_attempts_by_user_question;
