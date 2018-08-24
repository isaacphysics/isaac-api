package uk.ac.cam.cl.dtg.segue.dos;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by du220 on 04/06/2018.
 */
public class PgTransaction implements ITransaction {

    private Connection conn;

    public PgTransaction(final PostgresSqlDb postgresSqlDb) throws SegueDatabaseException {
        try {
            conn = postgresSqlDb.getDatabaseConnection();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Failure to obtain a connection for a transaction!", e);
        }
    }

    @Override
    public Connection getConnection() {
        return conn;
    }

    @Override
    public void commit() throws SegueDatabaseException {
        try {
            try {
                conn.commit();
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Transaction Commit Failure!", e);
        }
    }
}
