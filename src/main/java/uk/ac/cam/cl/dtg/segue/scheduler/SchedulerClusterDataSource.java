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

import com.google.inject.Injector;
import org.quartz.utils.ConnectionProvider;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.SQLException;


/**
 * This class is a shim to reuse the existing datasource created by guice for quartz cluster management
 */
public class SchedulerClusterDataSource implements ConnectionProvider {

    private static Injector injector;
    private static PostgresSqlDb ds;

    public SchedulerClusterDataSource() {
        // horrible dependency injection hack because quartz insists on initialising its own db connection class.
        injector = SegueGuiceConfigurationModule.getGuiceInjector();
        ds = injector.getInstance(PostgresSqlDb.class);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return ds.getDatabaseConnection();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void initialize() {

    }

}
