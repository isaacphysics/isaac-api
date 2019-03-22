/*
 * Copyright 2019 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.scheduler;

import com.google.inject.Guice;
import org.apache.commons.io.IOUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseScriptExecutionJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(DatabaseScriptExecutionJob.class);

    private static PostgresSqlDb ds;

    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    public DatabaseScriptExecutionJob() {
        // horrible dependency injection hack this job class needs to be able to independently access the database
        ds = Guice.createInjector(new SegueGuiceConfigurationModule()).getInstance(PostgresSqlDb.class);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // extract information needed to run the job from context map
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        String SQLFile = dataMap.getString("SQLFile");

        try (Connection conn = ds.getDatabaseConnection()) {
            String sqlFileContents = IOUtils.toString(getClass().getClassLoader().getResourceAsStream(SQLFile));

            log.debug(String.format("Executing scheduled SQL job (%s)", SQLFile));
            // JDBC cannot cope with the Postgres ? JSONB operator in PreparedStatements. Since we pass no parameters,
            // and run infrequently, a plain Statement is safe:
            Statement sss = conn.createStatement();
            sss.execute(sqlFileContents);
            log.info(String.format("Scheduled SQL job (%s) completed", SQLFile));

        } catch (IOException | SQLException e) {
            log.error(String.format("Error while trying to execute scheduled job (%s)", SQLFile), e);
        }
    }
}