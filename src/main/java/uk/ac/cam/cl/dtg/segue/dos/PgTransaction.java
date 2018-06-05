package uk.ac.cam.cl.dtg.segue.dos;

import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by du220 on 04/06/2018.
 */
public class PgTransaction implements ITransaction {

    private final PostgresSqlDb postgresSqlDb;
    private Connection conn = null;

    public PgTransaction(PostgresSqlDb postgresSqlDb) {
        this.postgresSqlDb = postgresSqlDb;

        try {
            conn = postgresSqlDb.getDatabaseConnection();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() {
        return conn;
    }

    @Override
    public void commit() {
        try {
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
