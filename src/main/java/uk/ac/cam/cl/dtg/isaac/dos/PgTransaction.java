package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *  @see ITransaction
 */
public class PgTransaction implements ITransaction {

    private final Connection conn;

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
            conn.commit();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Transaction Commit Failure!", e);
        }
    }

    @Override
    public void rollback() throws SegueDatabaseException {
        try {
            conn.rollback();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Transaction Rollback Failure!", e);
        }
    }

    @Override
    public void close() throws SegueDatabaseException {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Transaction Rollback Failure!", e);
        }
    }
}
